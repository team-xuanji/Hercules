package team.magic.flute.hercules.manager.schedule;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.status.TaskStatus;
import team.magic.flute.hercules.manager.dao.po.HerculesExecutorInfo;
import team.magic.flute.hercules.manager.dao.po.HerculesFailedTaskPo;
import team.magic.flute.hercules.manager.dao.po.HerculesTaskInfo;
import team.magic.flute.hercules.manager.entity.recover.impl.FixedIntervalStrategy;
import team.magic.flute.hercules.manager.global.RecoverEventLevel;
import team.magic.flute.hercules.manager.service.HerculesExecutorInfoService;
import team.magic.flute.hercules.manager.service.HerculesExecutorTasksService;
import team.magic.flute.hercules.manager.service.HerculesFailedTaskService;
import team.magic.flute.hercules.manager.service.HerculesTaskManagerService;
import team.magic.flute.hercules.manager.vo.AsyncRetryOneTaskRequestVO;
import team.magic.flute.hercules.manager.vo.HerculesRecoverTaskInfoVO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the tiered-recovery semantics in
 * {@link ScheduleBusinessProcessor#recoverEvents(RecoverEventLevel)} and the
 * dead-task scan's hand-off into the recover tables. No Spring context, no DB:
 * the MyBatis-Plus services are mocked and the generated UPDATE statements are
 * inspected through the wrapper API to pin the state-machine guards.
 *
 * <p>These pin the invariants the 2026-10 recovery rework relies on:
 * duplicate recover rows for the same task collapse into one recovery;
 * only FAILED tasks are reset to INIT (both the executor-fail path and the
 * dead-task-scan path land the task in FAILED first); a task that is already
 * alive again is never duplicated; a task whose original row is gone is
 * resurrected under its original id.
 */
class RecoverEventsTest {

    private ScheduleBusinessProcessor processor;
    private HerculesFailedTaskService failedTaskService;
    private HerculesExecutorTasksService taskService;
    private HerculesExecutorTasksService scanTasksService;
    private HerculesExecutorInfoService executorInfoService;
    private HerculesTaskManagerService taskManagerService;
    private ManagerInstanceCoordinator coordinator;

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        // LambdaUpdateWrapper.set(...) resolves column names eagerly; without a
        // MyBatis bootstrap the entity TableInfo is absent and resolution throws
        // "can not find lambda cache". Register the entities the wrappers touch.
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), HerculesTaskInfo.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), HerculesFailedTaskPo.class);
    }

    @BeforeEach
    void setUp() {
        processor = new ScheduleBusinessProcessor();
        failedTaskService = mock(HerculesFailedTaskService.class);
        taskService = mock(HerculesExecutorTasksService.class);
        scanTasksService = mock(HerculesExecutorTasksService.class);
        executorInfoService = mock(HerculesExecutorInfoService.class);
        taskManagerService = mock(HerculesTaskManagerService.class);
        coordinator = mock(ManagerInstanceCoordinator.class);

        ReflectionTestUtils.setField(processor, "failedTaskService", failedTaskService);
        ReflectionTestUtils.setField(processor, "herculesExecutorTasksService", taskService);
        ReflectionTestUtils.setField(processor, "tasksService", scanTasksService);
        ReflectionTestUtils.setField(processor, "herculesExecutorInfoService", executorInfoService);
        ReflectionTestUtils.setField(processor, "taskManagerService", taskManagerService);
        ReflectionTestUtils.setField(processor, "managerInstanceCoordinator", coordinator);

        // Single-manager deployment: this manager owns the whole bucket range.
        when(coordinator.getAllRunners()).thenReturn(Collections.singletonList("runner-1"));
        when(coordinator.getCurrentRunnerId()).thenReturn("runner-1");
    }

    /** A recover-row snapshot as convert2FailedTaskPo produces it after the rework:
     *  original id kept, status INIT, retry cursor already incremented. */
    private HerculesTaskInfo snapshot(String taskId) {
        return new HerculesTaskInfo()
                .setId(taskId)
                .setOwnerId("exec-dead")
                .setStatus(TaskStatus.INIT.name())
                .setExecutorRegion("PROD")
                .setPluginGroup("g")
                .setPluginHandle("h")
                .setBucketId(1)
                .setEnable(true)
                .setAsyncRecoverContext(new FixedIntervalStrategy());
    }

    private HerculesFailedTaskPo recoverRow(long rowId, HerculesTaskInfo snapshot) {
        return new HerculesFailedTaskPo()
                .setId(rowId)
                .setTaskInfo(snapshot)
                .setBucketId(1)
                .setNextProcessTime(LocalDateTime.now().minusMinutes(1));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> params(Wrapper<?> wrapper) {
        // WHERE-condition params are registered lazily on segment merge; force
        // both segments to render before reading the param map.
        ((AbstractWrapper<?, ?, ?>) wrapper).getSqlSegment();
        if (wrapper instanceof LambdaUpdateWrapper) {
            ((LambdaUpdateWrapper<?>) wrapper).getSqlSet();
        }
        return ((AbstractWrapper<?, ?, ?>) wrapper).getParamNameValuePairs();
    }

    private static String sqlSet(Wrapper<?> wrapper) {
        return ((LambdaUpdateWrapper<?>) wrapper).getSqlSet();
    }

    // ------------------------------------------------------------------
    // recoverEvents
    // ------------------------------------------------------------------

    @Test
    void duplicateRecoverRowsForSameTaskCollapseIntoOneRecovery() {
        // Two recover rows registered for the same original task (reachable:
        // asyncRetryOneTask has no idempotency key, and both the executor-fail
        // path and the dead-task scan can register one). The pre-fix
        // Collectors.toMap without a merge function threw IllegalStateException
        // here and poisoned the whole batch forever.
        HerculesFailedTaskPo row1 = recoverRow(101L, snapshot("task-1"));
        HerculesFailedTaskPo row2 = recoverRow(102L, snapshot("task-1"));
        when(failedTaskService.list(any(Wrapper.class))).thenReturn(Arrays.asList(row1, row2));
        when(taskService.update(any())).thenReturn(true);

        processor.recoverEvents(RecoverEventLevel.HOT);

        // exactly one conditional UPDATE for the deduped task...
        verify(taskService, times(1)).update(any());
        // ...no fallback insert...
        verify(taskService, never()).saveBatch(anyCollection());
        // ...and both recover rows are consumed.
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Collection> deleted = ArgumentCaptor.forClass(Collection.class);
        verify(failedTaskService).removeBatchByIds(deleted.capture());
        assertTrue(deleted.getValue().contains(101L) && deleted.getValue().contains(102L));
    }

    @Test
    void failedTaskIsResetToInitThroughConditionalUpdate() {
        HerculesFailedTaskPo row = recoverRow(101L, snapshot("task-1"));
        when(failedTaskService.list(any(Wrapper.class))).thenReturn(Collections.singletonList(row));
        when(taskService.update(any())).thenReturn(true);

        processor.recoverEvents(RecoverEventLevel.HOT);

        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(taskService).update(captor.capture());
        Wrapper<?> update = captor.getValue();

        // WHERE pins id + FAILED: this is the idempotency guard that makes the
        // recovery safe under overlapping manager bucket windows.
        Collection<Object> values = params(update).values();
        assertTrue(values.contains("task-1"), "WHERE must pin the original task id");
        assertTrue(values.contains(TaskStatus.FAILED.name()),
                "WHERE must only match FAILED tasks, got params: " + values);
        // SET clears the stale owner and checkpoint (both bound to null params).
        String set = sqlSet(update);
        assertTrue(set.contains("OWNER_ID="), "SET must clear owner: " + set);
        assertTrue(set.contains("CHECK_POINT_INFO="), "SET must clear checkpoint: " + set);
        assertTrue(values.contains(null), "SET must bind null owner/checkpoint: " + values);
        verify(taskService, never()).saveBatch(anyCollection());
        verify(failedTaskService).removeBatchByIds(anyCollection());
    }

    @Test
    void taskWhoseOriginalRowIsGoneIsResurrectedUnderItsOriginalId() {
        HerculesFailedTaskPo row = recoverRow(101L, snapshot("task-1"));
        when(failedTaskService.list(any(Wrapper.class))).thenReturn(Collections.singletonList(row));
        when(taskService.update(any())).thenReturn(false);      // guard matched nothing
        when(taskService.listByIds(anyCollection())).thenReturn(new ArrayList<>());
        when(taskService.saveBatch(anyCollection())).thenReturn(true);

        processor.recoverEvents(RecoverEventLevel.HOT);

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<Collection> inserted = ArgumentCaptor.forClass(Collection.class);
        verify(taskService).saveBatch(inserted.capture());
        assertEquals(1, inserted.getValue().size());
        HerculesTaskInfo resurrected = (HerculesTaskInfo) inserted.getValue().iterator().next();
        assertEquals("task-1", resurrected.getId(), "resurrection must keep the original id (ASSIGN_ID will not re-generate a non-null id)");
        assertEquals(TaskStatus.INIT.name(), resurrected.getStatus());
        assertNull(resurrected.getOwnerId());
        verify(failedTaskService).removeBatchByIds(anyCollection());
    }

    @Test
    void taskThatIsAliveAgainIsNotDuplicatedAndRecoverRowIsConsumed() {
        // The guard only matches FAILED. A task already re-queued (INIT) or
        // re-running (RUNNING) when its recover row fires must not be touched;
        // the pending recover intent is dropped, not executed twice.
        HerculesFailedTaskPo row = recoverRow(101L, snapshot("task-1"));
        when(failedTaskService.list(any(Wrapper.class))).thenReturn(Collections.singletonList(row));
        when(taskService.update(any())).thenReturn(false);
        HerculesTaskInfo aliveAgain = new HerculesTaskInfo()
                .setId("task-1")
                .setStatus(TaskStatus.RUNNING.name())
                .setOwnerId("exec-alive");
        when(taskService.listByIds(anyCollection())).thenReturn(Collections.singletonList(aliveAgain));

        processor.recoverEvents(RecoverEventLevel.HOT);

        verify(taskService, never()).saveBatch(anyCollection());
        verify(failedTaskService).removeBatchByIds(anyCollection());
    }

    @Test
    void failedResurrectionKeepsRecoverRowsForNextRound() {
        // saveBatch failure must NOT consume the recover rows: the recovery is
        // unconfirmed, so the rows stay due and are retried on a later tick.
        HerculesFailedTaskPo row = recoverRow(101L, snapshot("task-1"));
        when(failedTaskService.list(any(Wrapper.class))).thenReturn(Collections.singletonList(row));
        when(taskService.update(any())).thenReturn(false);
        when(taskService.listByIds(anyCollection())).thenReturn(new ArrayList<>());
        when(taskService.saveBatch(anyCollection())).thenReturn(false);

        processor.recoverEvents(RecoverEventLevel.HOT);

        verify(failedTaskService, never()).removeBatchByIds(anyCollection());
    }

    // ------------------------------------------------------------------
    // changeDeadTaskToCancelled — the scan-side hand-off into recover tables
    // ------------------------------------------------------------------

    @Test
    void deadTaskWithRecoverConfigIsMarkedFailedNotCancelled() {
        // A RUNNING task whose owner executor has disappeared, carrying an
        // async-recover context. The scan registers a recover row and must mark
        // the dead execution FAILED (consumed later by recoverEvents), not
        // CANCELLED — CANCELLED would make the recover row a permanent no-op.
        HerculesTaskInfo deadTask = new HerculesTaskInfo()
                .setId("task-1")
                .setStatus(TaskStatus.RUNNING.name())
                .setOwnerId("exec-dead")
                .setUpdateTime(LocalDateTime.now().minusSeconds(700))
                .setAsyncRecoverContext(new FixedIntervalStrategy());
        when(scanTasksService.list(any(Wrapper.class))).thenReturn(Collections.singletonList(deadTask));
        when(executorInfoService.getAllExecutorInfo()).thenReturn(Collections.singletonList(
                new HerculesExecutorInfo().setExecutorId("exec-alive")));
        when(taskManagerService.asyncRetryOneTask(any(AsyncRetryOneTaskRequestVO.class)))
                .thenReturn(BaseResponse.success(new HerculesRecoverTaskInfoVO(new HerculesFailedTaskPo(), null, null)));

        processor.changeDeadTaskToCancelled();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(scanTasksService, times(2)).update(captor.capture());

        List<Wrapper> updates = captor.getAllValues();
        Wrapper failedReset = updates.stream()
                .filter(w -> params(w).values().contains("task-1"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no update addressed task-1: " + updates));
        Collection<Object> values = params(failedReset).values();
        assertTrue(values.contains(TaskStatus.FAILED.name()),
                "dead task with pending recovery must be marked FAILED, got params: " + values);
        for (Wrapper w : updates) {
            assertFalse(params(w).values().contains(TaskStatus.CANCELLED.name()),
                    "no dead task with pending recovery may be marked CANCELLED");
        }
    }
}

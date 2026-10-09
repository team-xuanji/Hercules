# ADR-0015: Recover-row lifecycle — reuse-row recovery with outcome-bound deletion

- Status: Accepted (2026-10-09)
- Scope: `Hercules-manager/.../schedule/ScheduleBusinessProcessor.java` (`recoverEvents`, `changeDeadTaskToCancelled`), `Hercules-manager/.../dao/po/HerculesTaskInfo.java` (`convert2FailedTaskPo`), `Hercules-manager/.../schedule/RecoverTaskDispatch.java`
- Supersedes: the original insert-new-task recovery semantics (recorded here; the old code was removed in the same change)

## Context

A recover row (`HERCULES_RECOVER_TASKS_*`, ADR-0014) is a promise to re-run a failed task later. When the promise comes due, `recoverEvents` must decide what "re-run" means and when the promise has been kept. Three facts constrain the design:

1. **The same task can have several recover rows.** `asyncRetryOneTask` has no idempotency key: the executor-fail path and the dead-task scan can both register one, and retries can stack. Any window of 10 due rows routinely contains duplicates of the same task.
2. **Manager bucket windows can overlap.** Runner membership is only loosely consistent (`ManagerInstanceCoordinator` watermarks), so two managers can legitimately see the same recover row in the same interval.
3. **The task row may be in any state by the time the promise fires.** Registration requires the task to be RUNNING; by recovery time it is FAILED (executor-reported or dead-scan-reported), but it can also be INIT/RUNNING again (already recovered or superseded), CANCELLED, SUCCESS, or physically deleted by the 30/90-day retention sweeper.

The original implementation (kept in git history) inserted a **new** task row per recover row: `peek(x -> x.setId(null))` then `saveBatch`. Under facts 1 and 2 that meant duplicate executions were the *normal* case, not the exception, and a failed `saveBatch` still deleted the recover rows — losing the recovery silently. A rework that kept the original task id but left the state guards half-migrated (snapshot owner nulled, guard on `owner_id = null`) made recovery a 100% silent no-op instead. Both extremes failed for the same reason: **recovery had no idempotency anchor**.

## Decision

1. **The task id is the idempotency anchor.** `convert2FailedTaskPo` keeps the original task id in the snapshot (no longer nulled). Recovery is a single conditional UPDATE — `WHERE id = ? AND status = 'FAILED' SET status = INIT, owner_id = null, check_point_info = null, async_recover_context = <snapshot>` — instead of an insert. Two managers racing on the same row produce one UPDATE winner and one 0-rows loser; the loser takes the safe path. Duplicate rows collapse into one UPDATE per task id (`processedTaskIds`).
2. **Both producers land the task in FAILED.** The dead-task scan marks a dead execution FAILED (guarded by `status = RUNNING`) once a recover row has been registered — CANCELLED would make the pending recover row a permanent no-op, since the recovery guard only matches FAILED. Executor-reported failures were already FAILED.
3. **Row deletion is bound to the outcome, not the attempt.** A recover row is consumed only when its task's fate is confirmed: the conditional UPDATE succeeded (all duplicate rows of that task id are consumed together via `refIds`); the original row exists and is therefore not FAILED — i.e. the intent has been superseded by a live task (SUCCESS / INIT / RUNNING / CANCELLED all mean "already handled or invalidated"); or the row is corrupt (null `task_info`). If the resurrection `saveBatch` fails, the rows are kept and retried on a later tick.
4. **A missing original row is resurrected under its original id.** If the task row was purged, the reset snapshot is re-inserted with the same id (`ASSIGN_ID` never overrides a non-null id), so even the fallback stays idempotent.

The full control flow is documented as a diagram in the `recoverEvents` javadoc; `RecoverEventsTest` pins each branch with mocked-services unit tests.

## Consequences

**Positive**

- Recovery is idempotent end to end: overlapping manager windows, duplicate recover rows, and repeated ticks cannot execute a task twice.
- No zombie rows: every recover row reaches a terminal disposition (consumed on success/superseded/corrupt, retried only on a genuine insert failure), so the `LIMIT 10` window cannot be permanently occupied by undead rows.
- A failed resurrection no longer loses the recovery intent — rows survive until the insert is confirmed.
- Failure state is honest: a dead execution awaiting recovery is FAILED, which `recoverEvents` can match, instead of CANCELLED, which nothing downstream consumes.

**Costs / accepted risks**

- **The boundary condition (documented, not overlooked):** if a task is recovered, runs to a terminal state, and its recover rows stay blocked long enough for the task row to be purged by retention, the fallback re-inserts the task — for a SUCCESS outcome this is a duplicate execution. Bounded by the retention window (30 days for SUCCESS), but real. A fix would need either a tombstone/lineage check on re-insertion or retention that respects pending recover rows.
- **Pre-existing rows are stranded.** Recover rows written before this change carry snapshots with a null task id. Under the new logic they all share the null dedup key: one window recovers at most one of them and consumes the rest. These rows must be purged or replayed at deployment time.
- **Failure forensics shrink.** Reusing the original row overwrites the failed attempt in place, and recover rows are deleted once consumed; a failure handled through HOT leaves only logs. If traceability becomes a requirement, persist `error_info` onto the task row or archive consumed rows to the DEAD tier instead of deleting.
- `asyncRetryOneTask` still has no idempotency key, so duplicate rows keep being *created*; the dedup now happens at consumption time. This ADR is the place to revisit if duplicate-row volume ever becomes noticeable.

## Alternatives considered

- **Insert a new task row per recover row** (the original design) — rejected: duplicate execution under overlapping windows and duplicate rows; recovery lost on `saveBatch` failure. Superseded by this ADR.
- **Widen the recovery guard to `IN (FAILED, CANCELLED)`** instead of changing the dead-task scan — rejected: it would resurrect manually-cancelled tasks that happen to carry a pending recover row; a running plugin's side effects may already be half-applied (ADR-0006), so resurrection is not safe. Restricting the FAILED hand-off to the scan keeps deliberate cancellations terminal.
- **Deterministic derived id per recovery (e.g. `sourceId + retryIndex`)** with a unique constraint — deferred, not rejected: strictly stronger than id-reuse for the fallback insert, but redundant for the primary UPDATE path and adds a retry-index contract to the recover context. Revisit if the fallback path ever becomes hot.

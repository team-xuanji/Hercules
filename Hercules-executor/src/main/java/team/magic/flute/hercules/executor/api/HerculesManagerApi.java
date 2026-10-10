package team.magic.flute.hercules.executor.api;

import feign.HeaderMap;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;
import org.apache.commons.io.IOUtils;
import team.magic.flute.hercules.common.http.*;
import team.magic.flute.hercules.common.plugin.PluginResourceInfo;
import team.magic.flute.hercules.common.util.AESUtils;
import team.magic.flute.hercules.common.util.BinaryCompressUtils;
import team.magic.flute.hercules.common.util.ForyUtils;
import team.magic.flute.hercules.executor.vo.AsyncRetryOneTaskRequestVO;
import team.magic.flute.hercules.executor.vo.ExecutorInfoReportRequestVO;
import team.magic.flute.hercules.executor.vo.FinishOneTaskRequestVO;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import static team.magic.flute.hercules.common.global.Constant.*;

public interface HerculesManagerApi {
    @RequestLine("GET /pluginManager/searchPlugin?pluginGroup={pluginGroup}&pluginHandle={pluginHandle}")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<PluginResourceInfo> getPluginResource(
            @Param("pluginGroup")String pluginGroup,
            @Param("pluginHandle")String pluginHandle
    );

    @RequestLine("POST /taskManager/asyncRetryOneTask")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<PluginResourceInfo> asyncRerunOneTask(
            AsyncRetryOneTaskRequestVO requestVO
    );

    @RequestLine("POST /taskManager/submitOnceTask")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<HerculesRunnableTaskInfo> submitOnceTask(
            HerculesRunnableTaskInfo submitTaskRequestVO
    );

    @RequestLine("POST /executorManager/reportExecutorInfo")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<Boolean> reportExecutorInfo(
            ExecutorInfoReportRequestVO requestVO
    );

    @RequestLine("GET /taskManager/checkTaskStatus?taskId={taskId}")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<HerculesRunnableTaskInfo> checkTaskStatus(
            @Param("taskId")String taskId
    );

    /**
     * Fetch runnable tasks over the binary channel: the response is a
     * Fory-serialized {@link TaskFetchResult} (optionally compressed and/or
     * encrypted via response headers), avoiding JSON serialization overhead.
     *
     * <p>The request is signed via the {@code X-Hercules-*} headers in
     * {@code headers} (op=FETCH, subject=executorId); the signing interceptor
     * fills in timestamp and signature. See ADR-0016.
     *
     * @param executorRegion business group of the executor
     * @param executorId     per-boot instance id
     * @param fetchLimit     maximum number of tasks to return
     * @param aesKey         channel key used only when the response carries an AES IV header
     * @param headers        signing headers (op + subject); the rest are injected by the interceptor
     * @return the fetch result; {@link TaskFetchResult#isEmpty()} when there is nothing to do.
     *         {@link TaskFetchResult#isCrossPartition()} signals an all-bucket fallback scan
     * @throws RuntimeException on non-200 status or unreadable body
     */
    default TaskFetchResult tryFastFetchTasks(
            String executorRegion,
            String executorId,
            Integer fetchLimit,
            String aesKey,
            Map<String,String> headers
    ) {
        byte [] data = new byte[0];
        try (Response response = tryFetchTasksWithByteArray(executorRegion, executorId, fetchLimit, headers)) {
            if (response.status() == 200) {
                Response.Body body = response.body();
                if (body != null) {
                    try (InputStream inputStream = body.asInputStream()) {
                        data = IOUtils.toByteArray(inputStream);
                        Map<String, Collection<String>> headers2 = response.headers();
                        Collection<String> aesIV = headers2.get(HERCULES_BINARY_RESP_AES_IV);
                        if(aesIV!=null && !aesIV.isEmpty()){
                            data = AESUtils.decrypt(data,aesKey,aesIV.stream().findFirst().get());
                        }
                        Collection<String> compressTypeConfig = headers2.get(HERCULES_BINARY_RESP_COMPRESS_TYPE);
                        if(compressTypeConfig!=null && !compressTypeConfig.isEmpty()){
                            String compressTypeStr = compressTypeConfig.stream().findFirst().get();
                            int originalSize = Integer.parseInt(headers2.get(HERCULES_BINARY_RESP_ORIGINALS_SIZE).stream().findFirst().get());
                            HerculesHttpCompressType compressType = HerculesHttpCompressType.valueOf(compressTypeStr);
                            data = BinaryCompressUtils.deCompress(data,compressType,originalSize);
                        }
                    }
                }
            } else {
                throw new RuntimeException("Download failed with status: " + response.status());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read response body", e);
        }
        if(data.length<1){
            return new TaskFetchResult();
        }
        return ForyUtils.deserialize(data, TaskFetchResult.class);
    }

    @RequestLine("GET /taskDispatch/tryFetchTasksWithByteArray?executorRegion={executorRegion}&executorId={executorId}&fetchLimit={fetchLimit}")
    @Headers("Content-Type: application/octet-stream")
    Response tryFetchTasksWithByteArray(
            @Param("executorRegion")String executorRegion,
            @Param("executorId")String executorId,
            @Param("fetchLimit")Integer fetchLimit,
            @HeaderMap Map<String,String> headers
    );

    /**
     * Lock a single task. Server-side this is a compatibility shim over the
     * batch endpoint: same validation, same drift cancellation, and
     * {@code data=false} (not an error) when the task is already claimed by
     * someone else. Prefer {@link #tryLockBatchTask} for the poll loop.
     */
    @RequestLine("PUT /taskDispatch/tryLockOneTask?executorRegion={executorRegion}&executorId={executorId}&taskId={taskId}")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<Boolean> tryLockOneTask(
            @Param("executorRegion")String executorRegion,
            @Param("executorId")String executorId,
            @Param("taskId")String taskId,
            @HeaderMap Map<String,String> headers
    );

    /**
     * Lock a batch of tasks in one round trip. The request is signed over the
     * sorted, comma-joined task id list (see {@code ExecutorInfoUtils.buildSignSubject}).
     * Per-task outcomes are reported in {@link BatchTaskLockProcessResult#getLockResults()};
     * a missing or non-{@link TaskInfoLockResult#SUCCESS} entry means the task
     * must be skipped. Drifted tasks are cancelled server-side.
     *
     * @param lockRequest executor identity, region, and up to
     *                    {@code Constant.BATCH_FETCH_MAX_SIZE} task ids
     * @param headers    signing headers (op=LOCK, subject=sorted-id-list)
     */
    @RequestLine("PUT /taskDispatch/tryLockBatchTask")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<BatchTaskLockProcessResult> tryLockBatchTask(
            BatchLockRequest lockRequest,
            @HeaderMap Map<String,String> headers
    );

    @RequestLine("PUT /taskDispatch/finishOneTask")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<Boolean> finishOneTask(
            FinishOneTaskRequestVO requestVO,
            @HeaderMap Map<String,String> headers
    );

    @RequestLine("PUT /taskDispatch/abandonOneTask?taskId={taskId}&executorId={executorId}")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<Boolean> abandonOneTask(
            @Param("taskId")String taskId,
            @Param("executorId")String executorId,
            @HeaderMap Map<String,String> headers
    );

    @RequestLine("PUT /taskDispatch/failOneTask?taskId={taskId}&executorId={executorId}")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<Boolean> failOneTask(
            @Param("taskId")String taskId,
            @Param("executorId")String executorId,
            @HeaderMap Map<String,String> headers
    );

}

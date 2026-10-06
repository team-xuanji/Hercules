package team.magic.flute.hercules.executor.api;

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

    /**
     * We use a serialization framework to directly transmit byte arrays,
     * avoiding the performance overhead caused by JSON serialization.
     * @param executorRegion
     * @param executorId
     * @param fetchLimit
     * @return
     */
    default TaskFetchResult tryFastFetchTasks(
            String executorRegion,
            String executorId,
            Integer fetchLimit,
            String aesKey,
            String passSign
    ) {
        byte [] data = new byte[0];
        try (Response response = tryFetchTasksWithByteArray(executorRegion, executorId, fetchLimit, passSign)) {
            if (response.status() == 200) {
                Response.Body body = response.body();
                if (body != null) {
                    try (InputStream inputStream = body.asInputStream()) {
                        data = IOUtils.toByteArray(inputStream);
                        Map<String, Collection<String>> headers = response.headers();
                        Collection<String> aesIV = headers.get(HERCULES_BINARY_RESP_AES_IV);
                        if(aesIV!=null && !aesIV.isEmpty()){
                            data = AESUtils.decrypt(data,aesKey,aesIV.stream().findFirst().get());
                        }
                        Collection<String> compressTypeConfig = headers.get(HERCULES_BINARY_RESP_COMPRESS_TYPE);
                        if(compressTypeConfig!=null && !compressTypeConfig.isEmpty()){
                            String compressTypeStr = compressTypeConfig.stream().findFirst().get();
                            int originalSize = Integer.parseInt(headers.get(HERCULES_BINARY_RESP_ORIGINALS_SIZE).stream().findFirst().get());
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

    @RequestLine("GET /taskDispatch/tryFetchTasksWithByteArray?executorRegion={executorRegion}&executorId={executorId}&fetchLimit={fetchLimit}&passSign={passSign}")
    @Headers("Content-Type: application/octet-stream")
    Response tryFetchTasksWithByteArray(
            @Param("executorRegion")String executorRegion,
            @Param("executorId")String executorId,
            @Param("fetchLimit")Integer fetchLimit,
            @Param("passSign") String passSign
    );

    @RequestLine("PUT /taskDispatch/tryLockOneTask?executorRegion={executorRegion}&executorId={executorId}&taskId={taskId}&passSign={passSign}")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<Boolean> tryLockOneTask(
            @Param("executorRegion")String executorRegion,
            @Param("executorId")String executorId,
            @Param("taskId")String taskId,
            @Param("passSign") String passSign
    );

    @RequestLine("PUT /taskDispatch/tryLockBatchTask")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<BatchTaskLockProcessResult> tryLockBatchTask(
            BatchLockRequest lockRequest
    );

    @RequestLine("PUT /taskDispatch/finishOneTask")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<Boolean> finishOneTask(
            FinishOneTaskRequestVO requestVO
    );

    @RequestLine("PUT /taskDispatch/abandonOneTask?taskId={taskId}&executorId={executorId}&passSign={passSign}")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<Boolean> abandonOneTask(
            @Param("taskId")String taskId,
            @Param("executorId")String executorId,
            @Param("passSign") String passSign
    );

    @RequestLine("PUT /taskDispatch/failOneTask?taskId={taskId}&executorId={executorId}&passSign={passSign}")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<Boolean> failOneTask(
            @Param("taskId")String taskId,
            @Param("executorId")String executorId,
            @Param("passSign") String passSign
    );

}

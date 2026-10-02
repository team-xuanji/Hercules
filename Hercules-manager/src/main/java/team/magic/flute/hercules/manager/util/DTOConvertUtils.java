package team.magic.flute.hercules.manager.util;

import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;
import team.magic.flute.hercules.common.status.TaskType;
import team.magic.flute.hercules.common.util.AESUtils;
import team.magic.flute.hercules.common.util.JacksonUtils;
import team.magic.flute.hercules.manager.dao.po.HerculesTaskInfo;

public class DTOConvertUtils {

    public static HerculesRunnableTaskInfo parse2RunnableTaskInfo(HerculesTaskInfo taskInfo, boolean encrypt, String encryptKey, String encryptIV){
        String contextInfo = taskInfo.getContext();
        if(encrypt){
            contextInfo = AESUtils.encrypt(contextInfo,encryptKey,encryptIV);
        }
        return new HerculesRunnableTaskInfo()
                .setStatus(taskInfo.getStatus())
                .setId(taskInfo.getId())
                .setDesc(taskInfo.getDesc())
                .setPluginGroup(taskInfo.getPluginGroup())
                .setPluginHandle(taskInfo.getPluginHandle())
                .setExecutorRegion(taskInfo.getExecutorRegion())
                .setContext(contextInfo)
                .setEncryptIV(encryptIV)
                .setMaxRetryTimes(taskInfo.getMaxRetryTimes())
                .setAsyncRecoverContext(taskInfo.getAsyncRecoverContext()!=null? JacksonUtils.writeValueAsString(taskInfo.getAsyncRecoverContext()) :"")
                .setFromSourceId(taskInfo.getSourceId())
                .setFromType(TaskType.valueOf(taskInfo.getFromType()));
    }

}

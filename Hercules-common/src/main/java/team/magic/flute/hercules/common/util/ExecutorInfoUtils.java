package team.magic.flute.hercules.common.util;

import team.magic.flute.hercules.common.global.ExecutorTaskOps;

public class ExecutorInfoUtils {
    public static String getExecutorSign(String executorIdentityId, String taskId, ExecutorTaskOps taskOp){
        String msg = getMsg(taskId, taskOp);
        return HmacUtils.hmacSha256Hex(executorIdentityId,msg);
    }

    public static boolean verifyExecutorSign(String executorIdentityId,String taskId,ExecutorTaskOps taskOp,String passSign){
        String msg = getMsg(taskId, taskOp);
        return HmacUtils.verify(executorIdentityId, msg, passSign);
    }

    private static String getMsg(String taskId, ExecutorTaskOps taskOp){
        return taskId+":"+taskOp;
    }
}

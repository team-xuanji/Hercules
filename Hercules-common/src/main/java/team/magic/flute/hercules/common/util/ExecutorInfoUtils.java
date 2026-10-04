package team.magic.flute.hercules.common.util;

import team.magic.flute.hercules.common.global.ExecutorTaskOps;

public class ExecutorInfoUtils {

    /**
     * Build the HMAC signature for an executor operation.
     *
     * @param executorIdentityId the per-instance secret minted at boot
     * @param subject            the operation subject: the taskId for
     *                           LOCK/FINISH/FAIL/ABANDON, the executorId for
     *                           FETCH (which has no task)
     * @param taskOp             the operation type
     */
    public static String getExecutorSign(String executorIdentityId, String subject, ExecutorTaskOps taskOp){
        String msg = getMsg(subject, taskOp);
        return HmacUtils.hmacSha256Hex(executorIdentityId,msg);
    }

    public static boolean verifyExecutorSign(String executorIdentityId,String subject,ExecutorTaskOps taskOp,String passSign){
        String msg = getMsg(subject, taskOp);
        return HmacUtils.verify(executorIdentityId, msg, passSign);
    }

    /** Canonical message: subject + ":" + op. Single definition shared by sign and verify. */
    private static String getMsg(String subject, ExecutorTaskOps taskOp){
        return subject+":"+taskOp;
    }
}

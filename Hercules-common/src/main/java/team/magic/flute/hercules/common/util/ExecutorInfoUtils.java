package team.magic.flute.hercules.common.util;

import team.magic.flute.hercules.common.global.ExecutorTaskOps;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

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

    /**
     * Verify an operation signature; comparison is constant-time.
     *
     * @param executorIdentityId the per-instance secret stored at registration time
     * @param subject            must be the same subject that was signed (taskId, or the batch subject from {@link #buildSignSubject})
     * @param taskOp             the operation type
     * @param passSign           the presented signature; null is rejected
     */
    public static boolean verifyExecutorSign(String executorIdentityId,String subject,ExecutorTaskOps taskOp,String passSign){
        String msg = getMsg(subject, taskOp);
        return HmacUtils.verify(executorIdentityId, msg, passSign);
    }

    /** Canonical message: subject + ":" + op. Single definition shared by sign and verify. */
    private static String getMsg(String subject, ExecutorTaskOps taskOp){
        return subject+":"+taskOp;
    }

    /**
     * Build the canonical signed subject for a batch operation.
     *
     * <p>The ids are sorted before joining, so the signature depends on the id
     * <em>set</em>, not the order — signer and verifier may collect the ids in
     * any order and still produce the same subject.
     *
     * @param ids task ids to sign; nulls are dropped
     * @return the comma-joined, sorted id list
     * @throws IllegalArgumentException if ids is null or contains no non-null element
     */
    public static String buildSignSubject(Collection<String> ids){
        if (ids == null) {
            throw new IllegalArgumentException("[CAN NOT BUILD SIGN]:ids is null");
        }
        String joined = ids.stream().filter(Objects::nonNull).sorted().collect(Collectors.joining(","));
        if (joined.isEmpty()) {
            throw new IllegalArgumentException("[CAN NOT BUILD SIGN]:ids is empty");
        }
        return joined;
    }

}

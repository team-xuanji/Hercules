package team.magic.flute.hercules.common.http;

/**
 * Per-task outcome of a batch lock attempt ({@code tryLockBatchTask}).
 */
public enum TaskInfoLockResult {
    /** Task locked by this executor and transitioned to RUNNING. */
    SUCCESS,
    /** No task with this id exists. */
    NOT_EXIST,
    /** Task exists but was not locked: already RUNNING/owned by another executor, disabled, or the conditional update did not match. Safe to retry on a later poll. */
    NOT_CLAIMED,
    /** The pluginHandle is no longer registered under its pluginGroup; the task was cancelled (CANCELLED with a PLUGIN_DRIFTED checkpoint) and will not be retried. */
    PLUGIN_DRIFT,
    /** The task belongs to a different executor region than the requesting executor. */
    REGION_MISMATCH
}

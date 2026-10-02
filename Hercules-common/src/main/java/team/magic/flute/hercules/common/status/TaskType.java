package team.magic.flute.hercules.common.status;

public enum TaskType {
    FROM_CRON,
    ONCE,
    ASYNC_RECOVER,
    /**
     * Maybe not useful
     */
    @Deprecated
    ALIVE
}

package team.magic.flute.hercules.manager.global;

public enum RecoverEventLevel {
    HOT,
    WARM,
    COLD,
    /**
     * The asynchronous recovery task has exceeded the maximum number
     * of recovery attempts. Since persistence is configured,
     * this portion of the task needs to be stored.
     */
    DEAD,
    UNKNOWN;
}

package team.magic.flute.hercules.common.http;

public enum TaskInfoLockResult {
    SUCCESS,
    NOT_EXIST,
    NOT_CLAIMED,
    PLUGIN_DRIFT,
    REGION_MISMATCH
}

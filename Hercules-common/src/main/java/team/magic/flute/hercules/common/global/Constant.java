package team.magic.flute.hercules.common.global;

public class Constant {
    /**
     * Multiple Manager instances calculate the maximum number of buckets for scheduling tasks. For
     * example, instance A only computes scheduling tasks in the range [1, 21), while instance B
     * computes tasks in the range [21, 41). Do not change this.Because it's hard to imagine a
     * scenario where we would need more than 100 computing instances.
     */
    public static final Integer CRON_JOB_MAX_BUCKET_SIZE = 100;

    /**
     * Multiple Executor instances calculate the maximum number of buckets for fetch tasks. For
     * example, instance A only fetch tasks in the range [1, 21), while instance B fetch tasks in
     * the range [21, 41). Do not change this.Because it's hard to imagine a scenario where we would
     * need more than 100 computing instances.
     */
    public static final Integer TASK_MAX_BUCKET_SIZE = 100;

    /** The length of the response body before compression is enabled */
    public static final String HERCULES_BINARY_RESP_ORIGINALS_SIZE =
            "HERCULES_BINARY_RESP_ORIGINALS_SIZE";

    /** The size of the compressed response body */
    public static final String HERCULES_BINARY_RESP_COMPRESSED_SIZE =
            "HERCULES_BINARY_RESP_COMPRESSED_SIZE";

    /** Compression algorithm enabled in the response body. */
    public static final String HERCULES_BINARY_RESP_COMPRESS_TYPE =
            "HERCULES_BINARY_RESP_COMPRESS_TYPE";

    /** IV for AES encryption and decryption */
    public static final String HERCULES_BINARY_RESP_AES_IV = "HERCULES_BINARY_RESP_AES_IV";

    public static final int BATCH_FETCH_MAX_SIZE = 200;
}

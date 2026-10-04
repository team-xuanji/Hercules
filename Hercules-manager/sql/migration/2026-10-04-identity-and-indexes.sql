-- ============================================================================
-- Migration: identity signing + index cleanup
-- Target: databases created BEFORE this revision.
-- Fresh installs must use ../init.sql instead; do NOT run both.
--
-- MySQL 8 has no `IF EXISTS` for ADD COLUMN / DROP INDEX, so run this ONCE.
-- If a statement reports "duplicate column" / "can't DROP ... check that
-- column/key exists", it means that step was already applied — ignore it.
-- ============================================================================

-- 1. Per-executor identity for HMAC operation signing (HMAC-SHA256 rollout).
--    Existing rows stay NULL until the next heartbeat re-registers the executor;
--    operations are rejected (fail-closed) until then.
ALTER TABLE HERCULES_EXECUTOR_INFO
    ADD COLUMN IDENTITY_ID VARCHAR(128) NULL COMMENT 'Executor Identity ID';

-- 2. Fix cron table name case. Linux MySQL compares table names
--    case-sensitively and the code addresses `HERCULES_CRON_TASKS`.
--    Skip this step if the table already has the upper-case name.
RENAME TABLE hercules_cron_tasks TO HERCULES_CRON_TASKS;

-- 3. Index for dead-task / old-task cleanup scans
--    (WHERE STATUS = ? AND UPDATE_TIME < ?).
ALTER TABLE HERCULES_TASK_INFO
    ADD INDEX idx_status_update (STATUS, UPDATE_TIME);

-- 4. Drop indexes the application never reads: executor info is loaded whole
--    and filtered in memory, so only the primary key and the UPDATE_TIME scan
--    (expired-executor cleanup) are useful.
ALTER TABLE HERCULES_EXECUTOR_INFO
    DROP INDEX idx_executor_region,
    DROP INDEX idx_available_slot,
    DROP INDEX idx_max_slot,
    DROP INDEX idx_enable_duckdb,
    DROP INDEX idx_insert_time,
    DROP INDEX idx_region_slot;

-- 5. Trim redundant indexes on the four recovery tables: the only hot query is
--    (BUCKET_ID range + NEXT_PROCESS_TIME), covered by idx_bucket_next_time.
--    idx_bucket_id is a prefix of it; the rest are unused.
ALTER TABLE HERCULES_RECOVER_TASKS_HOT
    DROP INDEX idx_plugin_group,
    DROP INDEX idx_plugin_handle,
    DROP INDEX idx_executor_region,
    DROP INDEX idx_bucket_id,
    DROP INDEX idx_next_process_time,
    DROP INDEX idx_insert_time;

ALTER TABLE HERCULES_RECOVER_TASKS_WARM
    DROP INDEX idx_plugin_group,
    DROP INDEX idx_plugin_handle,
    DROP INDEX idx_executor_region,
    DROP INDEX idx_bucket_id,
    DROP INDEX idx_next_process_time,
    DROP INDEX idx_insert_time;

ALTER TABLE HERCULES_RECOVER_TASKS_COLD
    DROP INDEX idx_plugin_group,
    DROP INDEX idx_plugin_handle,
    DROP INDEX idx_executor_region,
    DROP INDEX idx_bucket_id,
    DROP INDEX idx_next_process_time,
    DROP INDEX idx_insert_time;

ALTER TABLE HERCULES_RECOVER_TASKS_DEAD
    DROP INDEX idx_plugin_group,
    DROP INDEX idx_plugin_handle,
    DROP INDEX idx_executor_region,
    DROP INDEX idx_bucket_id,
    DROP INDEX idx_next_process_time,
    DROP INDEX idx_insert_time;

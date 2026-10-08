-- ============================================================================
-- Migration: forward-chain depth limit
-- Target: databases created BEFORE this revision.
-- Fresh installs must use ../init.sql instead; do NOT run both.
--
-- MySQL 8 has no `IF EXISTS` for ADD COLUMN, so run this ONCE.
-- If it reports "duplicate column", the step was already applied — ignore it.
-- ============================================================================

-- 1. Depth of the forward-chain a task belongs to. The manager derives it at
--    submission (parent depth + 1 for FORWARD tasks, 0 otherwise) and rejects
--    submissions beyond hercules.task.max-chain-depth, bounding runaway chain
--    derivation. Existing rows default to 0 (not a chain task).
ALTER TABLE HERCULES_TASK_INFO
    ADD COLUMN CHAIN_DEPTH int DEFAULT '0' COMMENT 'Forward chain depth (0 = not a chain task)';

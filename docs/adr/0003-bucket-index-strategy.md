# ADR-0003: Dispatch and recovery table index strategy

- Status: Accepted (2026-10-04)
- Scope: `Hercules-manager/sql/init.sql`, `Hercules-manager/sql/migration/2026-10-04-identity-and-indexes.sql`

## Context

Every index is a read acceleration **and** a write amplification on a hot OLTP path (task claim / task finish). The 2026-10-04 revision audited all indexes against the queries the application actually runs:

- **Hot fetch** — `HERCULES_TASK_INFO` is queried by `EXECUTOR_REGION = ? AND STATUS = ? AND ENABLE = ? AND BUCKET_ID BETWEEN ? AND ?` with `ORDER BY INSERT_TIME`, limited per fetch. The previous composite index `(EXECUTOR_REGION, STATUS, ENABLE, BUCKET_ID, OWNER_ID)` matched the predicates but lacked `INSERT_TIME`, so ordering could spill to a filesort.
- **Dead/old task cleanup** — scans `STATUS = ? AND UPDATE_TIME < ?`; previously unindexed.
- **`HERCULES_EXECUTOR_INFO`** — the application loads the whole table into memory (30 s Caffeine cache) and filters in Java; only the primary-key lookup (heartbeat upsert) and the expired-executor cleanup (`UPDATE_TIME`) are real index consumers. Six single-column indexes served no query.
- **Recovery tables (hot/warm/cold/dead)** — the only hot access is a bucket range plus `NEXT_PROCESS_TIME`; the rest of the columns are never filtered on. `idx_bucket_id` was a strict prefix of `idx_bucket_next_time (BUCKET_ID, NEXT_PROCESS_TIME)` and therefore pure write cost.
- Table naming: Linux MySQL (`lower_case_table_names=0`) compares table names case-sensitively; the code addresses `HERCULES_CRON_TASKS` while old databases could have `hercules_cron_tasks`.

## Decision

1. `idx_task_dispatch (EXECUTOR_REGION, STATUS, ENABLE, BUCKET_ID, OWNER_ID, INSERT_TIME)` — one composite covering the fetch predicates plus the ordering column. Caveat recorded honestly: when the `BUCKET_ID` range is very wide, MySQL can still fall back to a filesort because a range predicate precedes `INSERT_TIME` in the key; for the normal selective bucket slice the index lets the optimizer read pre-ordered rows and avoids the sort.
2. Add `idx_status_update (STATUS, UPDATE_TIME)` on `HERCULES_TASK_INFO` for cleanup scans.
3. `HERCULES_EXECUTOR_INFO` keeps only the primary key and `idx_update_time (UPDATE_TIME)`; drop `idx_executor_region`, `idx_available_slot`, `idx_max_slot`, `idx_enable_duckdb`, `idx_insert_time`, `idx_region_slot`.
4. Recovery tables keep only `idx_bucket_next_time (BUCKET_ID, NEXT_PROCESS_TIME)`; drop `idx_plugin_group`, `idx_plugin_handle`, `idx_executor_region`, `idx_bucket_id`, `idx_next_process_time`, `idx_insert_time`.
5. `init.sql` is idempotent for fresh installs (`CREATE TABLE IF NOT EXISTS`, consistent upper-case names). Pre-existing databases use the one-shot migration, which also adds `IDENTITY_ID` (see ADR-0001) and renames the cron table to upper case. MySQL 8 has no `IF EXISTS` for `ADD COLUMN` / `DROP INDEX`, so the migration is documented as run-once with "duplicate/unknown key errors mean already applied — ignore". **Never run both** init and migration against the same database.

## Consequences

**Positive**

- The hottest query (fetch) is served, ordered, and bounded by a single composite index instead of index-merge plus filesort.
- Write amplification drops materially on five tables: every dropped index was either a prefix of a kept index or served no query at all.
- Cleanup scans stop table-scanning `HERCULES_TASK_INFO`.

**Accepted risks**

- Dropping indexes is a bet on the **current query set**. Any future query that filters recovery tables by `PLUGIN_GROUP`/`PLUGIN_HANDLE`/`EXECUTOR_REGION`, or executor info by region/slot, must either add a targeted index back or accept a scan — and should record that decision in a new ADR.
- The migration is not idempotent (MySQL limitation); operators must read its header. Renaming an already-upper-case cron table will error — the header says to ignore it.

## Alternatives considered

- **Keep existing indexes "just in case"** — rejected: an index nobody reads is pure write cost on every task claim/finish/recover write; "maybe useful later" is not a query plan.
- **Separate index on `INSERT_TIME`** — rejected: a standalone ordering index cannot be combined with the equality/range predicates; appending the column to the existing composite is strictly better.

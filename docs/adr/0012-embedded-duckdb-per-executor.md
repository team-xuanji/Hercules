# ADR-0012: Embedded per-executor DuckDB as the OLAP engine

- Status: Accepted (2026-10-09)
- Scope: `Hercules-executor/.../service/impl/ExecutorProcessHandleImpl.java` (`initDuckdbDataSource`, `rebuildDuckdbDataSource`, `buildTaskExecutionContext`, `cleanUpDuckdbConnection`), `Hercules-executor/.../config/RunnerEnv.java` (duckdb flags), `Hercules-common/.../status/TaskExecutionContext.java` (`duckdbConnection`)

## Context

Data-processing plugins (ETL, cross-source joins, staging large intermediate results) need an analytical engine with real SQL and columnar storage. The deployment envelope documented in the README offers nothing beyond MySQL and outbound HTTP (ADR-0004): no external warehouse, no object-store analytics, no spare services to operate. MySQL itself is the system's control plane and must not absorb heavy analytical scans.

DuckDB provides the capability inside the existing footprint: embedded, disk-persistent, runs in the executor's own JVM. But DuckDB's concurrency model is a constraint, not a parameter: one process may hold the writer, and concurrent write attempts fail. Its JNI boundary is also empirically less stable than pure-Java JDBC — close operations can throw, and long-lived instances accumulate user-registered metadata (views, tables, secrets) that callers create and never clean up.

## Decision

1. **One embedded DuckDB per executor**, disk-persistent (`jdbc:duckdb:<path>`), memory limit / thread count / spill directory configured from `RunnerEnv`, `preserve_insertion_order=false`. Executors may disable it entirely per deployment; the flag is advertised in the heartbeat.
2. **Connection pool sized exactly 1** (`maximumPoolSize=1`, `minimumIdle=1`) — not a tuning choice but a direct consequence of the single-writer limitation, documented in code as "DuckDB Limitations, do not change this". Consequence accepted: at most one DuckDB-hungry task runs per executor at a time; scaling DuckDB throughput means more executors, never a bigger pool.
3. **One connection per `process()` call**, acquired from the pool before plugin execution and handed to the plugin via `TaskExecutionContext.duckdbConnection`. The connection is built outside the execution retry loop (deliberately: see costs). Cleanup is null-guarded, idempotent, and falls through to `Closeable` on the context as a second line of defense.
4. **Pool rebuild as the corruption-recovery primitive.** The `HikariDataSource` reference is `volatile` and swapped under a class-level lock. Two triggers, both accepted operational events:
   - **Time-based TTL (8 h), only when fully idle** (`aliveAbleSlot() == totalSlot()`): a scheduled hygiene restart, because in practice users register messy metadata and never clean it up — over time that poisons other tenants of the same embedded instance.
   - **Close failure**: if a connection close throws (DuckDB JNI instability), the whole pool is rebuilt. The stuck connection belongs to the abandoned old pool; subsequent tasks read the new `volatile` reference and get a clean instance. This is deliberately cross-task healing: within one task, retries reuse the connection already in the context.

## Consequences

**Positive**

- Real OLAP (staging bounded by disk, cross-source scans, Parquet/CSV/HTTP ingestion) inside the zero-new-infrastructure envelope.
- JNI instability is contained: a bad close or corrupted instance affects at most the in-flight task plus idle-time rebuilds, never the fleet.
- Executor-level isolation: one tenant's messy DuckDB metadata never crosses executor boundaries; the TTL restart bounds how long the mess accumulates.

**Costs / accepted risks**

- **Single-writer throughput ceiling**: one DuckDB task per executor. Accepted — the slot mechanism already models executors as the scaling unit.
- **Within-task retry reuses the same connection**: if a plugin failure was caused by a corrupted connection, retries on it are likely to fail too, and the task fails after `maxRetryTimes`. A fresh connection per retry was considered and rejected as unnecessary machinery given decision 4's cross-task healing; recorded here so the trade-off is discoverable.
- **TTL rebuild waits for full idleness**: a permanently busy executor keeps an old instance indefinitely. Accepted: busy instances are also the ones whose metadata is being used.
- A rebuild under load (close-failure path) abandons checked-out connections; their tasks fail and are re-executed by the normal recovery path (ADR-0006).

## Alternatives considered

- **External warehouse (ClickHouse, StarRocks, cloud OLAP)** — rejected: violates the deployment envelope; reintroduces the exact middleware-rental problem the README story describes.
- **Pool size > 1** — rejected: DuckDB's single-writer model makes concurrent writers error at the database, not the pool; a larger pool would only queue failures differently.
- **In-memory DuckDB (`:memory:`)** — rejected: staging "limited only by disk" is a feature; memory-resident staging caps task size by RAM.
- **Fresh connection per retry** — rejected for the reason recorded above; revisit if close-failure rebuilds prove insufficient in operation.

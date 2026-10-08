# ADR-0014: Tiered recover tables for asynchronous task recovery

- Status: Accepted (2026-10-09)
- Scope: `Hercules-manager/.../entity/recover/RecoverStrategy.java` (+ `FixedIntervalStrategy`, `GradualDecelerationStrategy`), `Hercules-manager/.../config/RecoverConfig.java`, `Hercules-manager/.../dao/po/HerculesTaskInfo.java` (`convert2FailedTaskPo`), `Hercules-manager/.../service/impl/HerculesTaskManagerServiceImpl.java` (`asyncRetryOneTask`), `HERCULES_RECOVER_TASKS_{HOT,WARM,COLD,DEAD}`

## Context

Some tasks fail in a way synchronous retries cannot fix: a downstream dependency is down for hours, a source system is rate-limiting until tomorrow, or the task is expensive enough that blind hot retrying burns real resources. The executor-side retry loop (`maxRetryTimes`, small, fast) is the wrong tool for all of these — it assumes transient failure on a short clock.

What is needed is policy-driven, manager-owned re-execution: the task's submitter declares *how* it wants to be retried (fixed cadence, decelerating cadence, how long, whether a final record is kept), and something outside both the original executor and the synchronous failure path schedules the attempts.

## Decision

1. **The recovery policy travels with the task.** `asyncRecoverContext` is submitted as polymorphic JSON (`recoverStrategyType` discriminator) deserializing to `RecoverStrategy`, with two built-in policies: `FIXED_INTERVAL` and `GRADUAL_DECELERATION`. The interface answers three questions: is the retry budget exhausted (`processFinished`), when does the next attempt fire (`processAndIncrementNextTimeStamp`), and should a terminal record persist (`isPersistence`). New policies are new implementations, not schema changes.
2. **Escalation is executor-initiated, manager-owned.** On final failure with a non-blank recover context, the executor calls `asyncRerunOneTask`; the manager snapshots the task plus the error into a `HerculesFailedTaskPo` and parks it until its `nextProcessTime`. A scheduler later re-submits due entries as `FROM_TYPE=ASYNC_RECOVER` tasks — they flow through the normal dispatch pipeline (ADR-0004/0005) with no special executor path. The recover task keeps its lineage (`SOURCE_ID` points at the failed task unless it is itself a recover attempt, which prevents unbounded self-chaining).
3. **Due-queue entries are physically tiered by how soon they fire**: `HOT` (< 1 h), `WARM` (< 24 h), `COLD` (≥ 24 h), `DEAD` (terminal / persistence tier). Tier assignment is recomputed as `nextProcessTime` advances. The scan that wakes due entries always runs against `HOT`, whose row count stays small regardless of how large the long-tail backlog grows; a sweeper promotes entries upward as they become due.
4. **Each tier table is indexed for the only hot query it serves** — `(BUCKET_ID range + NEXT_PROCESS_TIME)` (see ADR-0003) — so the due-scan cost is proportional to the tier's near-term volume, not total backlog.

## Consequences

**Positive**

- Failure policies are data, not code paths: a submitter tunes cadence per task without touching the executor or scheduler.
- The hot path (claim → execute → finish) is untouched by recovery machinery; recovery traffic re-enters through the standard dispatch gate and inherits its correctness (locking, ownership, idempotent terminals).
- A million stale "retry tomorrow" entries cost one small `HOT` scan every cycle; the backlog cannot degrade claim latency.

**Costs / accepted risks**

- Four physical tables where one logical queue exists: operational surface (four DDLs, four sweeps). Accepted — the tiering is the performance isolation mechanism, and the tables are structurally identical.
- Policy JSON is interpreted at submit and mutated by the scheduler (`processAndIncrementNextTimeStamp` advances state in place); a stuck mid-update row retries conservatively on the next sweep. Accepted.
- Recovery attempts are full re-executions from the task's context; there is no checkpoint continuation (consistent with ADR-0006's progress-loss position — segment-style decomposition, not checkpoints).

## Alternatives considered

- **In-memory delayed queue on the manager** — rejected: a manager restart would drop the entire backlog; the whole point is durably outliving the failure and the node that observed it. (And the envelope has no Redis — ADR-0004's premise.)
- **Single recover table** — rejected: the due-scan cost would grow with the backlog, coupling long-tail volume to hot-path latency — precisely the disease the tiers cure.
- **One cron row per retry attempt** — rejected: pollutes the cron namespace, loses the retry-budget semantics (`processFinished`), and cannot express deceleration without regenerating rows.
- **Executor sleeps and resubmits itself** — rejected: a dead executor is the common failure context; recovery must not depend on the component that just failed.

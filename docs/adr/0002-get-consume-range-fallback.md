# ADR-0002: Full bucket range fallback for unregistered executors

- Status: Accepted (2026-10-04)
- Scope: `Hercules-manager/.../service/impl/HerculesExecutorInfoServiceImpl.java#getConsumeRange`

## Context

`getConsumeRange(executorId, executorRegion)` decides which slice of the 100 task buckets (`TASK_MAX_BUCKET_SIZE = 100`, buckets live in `[1, 101)`) an executor should scan. The normal path sorts the region's running executor ids and assigns each one a contiguous sub-range, so that parallel executors divide the fetch scan instead of all reading all buckets.

The inputs to that computation are eventually consistent:

- Every executor boot mints a fresh instance id: `"EXECUTOR_INSTANCE_<region>_<GUIDv7>"` (`Hercules-executor/.../config/RunnerEnv.java:39`). A restarted executor is, from the manager's perspective, a brand-new id.
- Executors heartbeat every 10 s (`@Scheduled(fixedRate = 10000)` in `TaskConsumer#reportInfo`).
- The manager caches the executor list for 30 s (Caffeine `expireAfterWrite(30, SECONDS)`).

So after **every restart or rolling deploy**, an executor faces a window of roughly 10–40 s in which it is unknown to the manager. If the unknown case returned an empty range, the executor would sit idle through that window — repeated fleet-wide on every deploy.

Crucially, the bucket range is an **optimization layer, not the correctness layer**. Claiming a task requires `tryLockOneTask`, a conditional `UPDATE` guarded by status and owner. That atomic lock — not the bucket range — prevents double execution. The range only narrows *where executors look*.

## Decision

When the executor list is empty or the requesting executor is not in it, return the full range `[1, TASK_MAX_BUCKET_SIZE + 1)` instead of an empty range, and log a warning naming the executor. The existing inline comment in `getConsumeRange` already documents this reasoning.

The invariant we rely on: **the optimization layer may fail open; the lock layer must never fail open.** The fallback widens the read set only. It cannot cause a task to execute twice, because a task still has to win the conditional-update lock to move to RUNNING.

## Consequences

**Positive**

- Restarts and rolling deploys never starve executors: an unknown executor still fetches, still locks, still works.
- The cost of the fallback is strictly bounded: some extra fetch reads across the full bucket set, and some extra lock attempts that lose. Losing lock races is normal system behavior, not an error path.

**Accepted risks / dependencies**

- This is safe under the **single trust domain** assumption (all executors are operated by the same owner; see ADR-0001 for the trust-boundary discussion). If Hercules ever hosts multiple trust domains, "unknown executor gets the whole bucket space" becomes a read-surface problem and the fallback must be tightened — this ADR is the place a future change should supersede.
- More executors in fallback → more lock contention. If fallback warnings become frequent in production, that is a signal about heartbeat/cache health, not a reason to change this decision.

## Alternatives considered

- **Return an empty range for unknown executors** — theoretically "purer" (an unregistered executor consumes nothing), rejected: it converts every restart into up to ~40 s of starvation and makes rolling deploys visibly lose capacity.
- **Force a synchronous cache refresh / registration before answering** — rejected: couples fetch latency to registration I/O for a benefit (slightly narrower first fetch) that the lock layer makes unnecessary. The list is eventually consistent and that is acceptable here.

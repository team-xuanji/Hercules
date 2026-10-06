# ADR-0005: Conditional UPDATE as the task concurrency primitive

- Status: Accepted (2026-10-06)
- Scope: `Hercules-manager/.../controller/TaskDispatchController.java` (`tryLockBatchTask`, `finishOneTask`, `failOneTask`, `abandonOneTask`, `lockBatchTask`), `Hercules-manager/.../schedule/ScheduleBusinessProcessor.java` (`changeDeadTaskToCancelled`)

## Context

Multiple executors compete to claim tasks, and a task must never have two owners. The conventional toolbox for this is SELECT FOR UPDATE, an optimistic version column, or an external lock service (ZooKeeper/etcd/Redis). All three pull weight this system refuses to carry (ADR-0004): gap locks held across service calls, version plumbing through every mutation path, or another piece of middleware.

The observation that dissolves the problem: a conditional UPDATE *is* an atomic compare-and-set. `UPDATE ... SET status='RUNNING', owner=? WHERE id=? AND status='INIT' AND owner IS NULL` either claims the row or changes nothing, and MySQL guarantees that per row even under concurrent writers.

## Decision

1. **Claim** is a single conditional UPDATE: `status = INIT → RUNNING`, `ownerId NULL → executorId`, `enable = true`. No SELECT-then-UPDATE anywhere on the claim path.
2. **All four terminal mutations are guarded** by `ownerId = caller AND status = RUNNING` in the same UPDATE: finish (→ SUCCESS with checkpoint), fail (→ FAILED), abandon (→ INIT, owner cleared — the only path back to claimable).
3. **Batch claim** (ADR-0007) uses one UPDATE for N tasks, then re-reads to classify; atomicity is per row, so two executors can never co-own a task even within a batch.
4. The state machine is exactly: `INIT → RUNNING → SUCCESS | FAILED | CANCELLED`, plus `RUNNING → INIT` (abandon / dead-task recovery). No other transitions exist.

## Consequences

**Positive**

- Double ownership is impossible by construction, including across executor restarts: a stale executor's finish/fail/abandon no-ops against the `ownerId + RUNNING` guard, so a reassigned task cannot be completed twice by its old owner.
- Every mutation is one round trip, one row lock held for statement duration only.
- The protocol is reviewable in four SQL statements.

**Costs / accepted risks**

- Hot-row contention on the same task id is resolved by "first UPDATE wins"; losers get `NOT_CLAIMED` and retry on a later poll. Under heavy contention on a single business key this is wasted work, bounded by bucket partitioning (ADR-0002/0003).
- The UPDATE-then-reread window in batch claim can report a pessimistic `NOT_CLAIMED` if recovery resets a row in between — safe (retry) but occasionally surprising in logs.

## Alternatives considered

- **SELECT FOR UPDATE** — rejected: extends row/gap lock holding beyond the statement, couples lock lifetime to transaction scope across service-layer logic, and still needs the same predicates.
- **Optimistic version column** — rejected: identical guarantee to the conditional predicate, plus version plumbing on every read-modify-write path. The predicate *is* the version.
- **External lock service** — rejected: middleware (ADR-0004). The database was already there.

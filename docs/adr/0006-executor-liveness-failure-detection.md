# ADR-0006: Failure detection via executor liveness — deliberately no per-task lease

- Status: Accepted (2026-10-06)
- Scope: `Hercules-manager/.../schedule/ScheduleBusinessProcessor.java` (`changeDeadTaskToCancelled`, `removeTooOldExecutorInfo`), `Hercules-executor/.../schedule/TaskConsumer.java` (`reportInfo`)

## Context

The standard answer to "is this task still alive?" is a per-task lease: the executor renews a heartbeat on every running task, the manager reassigns tasks whose lease expired. This system does not have that, and the absence is a decision, not an oversight.

What exists instead: an executor reports its own heartbeat every 10 s; executor records expire after 180 s without a report; a sweeper recovers RUNNING tasks whose **owner executor no longer exists** and whose lock is older than 600 s. Two consequences follow, both accepted:

1. **A stuck plugin on a live executor is invisible.** If a task deadlocks or blocks on I/O forever while its executor keeps heartbeating, nothing detects it. There is also no per-task timeout.
2. **Cancel does not reach the execution site.** `CANCELLED` is a terminal state; a running plugin thread is never interrupted. A task cancelled while running will still run to completion and its `finishOneTask` will no-op against the state guard (ADR-0005) — side effects, if any, already happened.

## Decision

1. Task liveness is **derived from executor liveness**: a RUNNING task is presumed healthy as long as its owner heartbeats. Recovery acts only on owner-dead tasks (sweeper conditions: owner absent from the live executor set AND lock older than 600 s).
2. **No per-task lease, no per-task heartbeat, no cancel signal** is implemented. Plugins that need bounded runtime must enforce it themselves.
3. This ADR exists to record the *non-decision*: the next person who sees a permanently RUNNING stuck task must be able to discover that this was weighed, and what it would cost to change.

## Consequences

**Positive**

- Zero executor-side bookkeeping per task; the hot path (claim → execute → finish) carries no lease traffic.
- Failover still works for the case that matters most (executor death): the task is requeued and re-executed from scratch after the sweeper window.
- Simplicity: no lease table, no expiry sweeper per task, no clock-skew sensitivity.

**Costs / accepted risks**

- **Stuck-task blindness** (described above). Mitigation today: none, beyond operator vigilance and log prefixes.
- **Progress loss on recovery**: a task that ran 3 of 4 hours before its executor died restarts from zero (`maxRetryTimes` full re-execution). Segment-style decomposition (chain tasks with cursors) is the intended answer, not checkpoints inside the executor.
- Cancel latency equals "whenever the plugin happens to notice" — effectively never for running tasks.

## Alternatives considered

- **Per-task lease renewal** — deferred, not rejected: the correct fix for stuck detection. Costs executor-side renewal bookkeeping, a manager-side lease sweep, and a plugin contract for cooperative cancellation. Revisit when long-running tasks become first-class; until then the executor-liveness model is the accepted position.
- **Per-task timeout with thread interrupt** — rejected: `Thread.interrupt` is not a safe kill mechanism for arbitrary plugin code (JDBC, DuckDB JNI, native locks); a plugin that ignores the interrupt gains nothing, one that handles it wrongly corrupts state.
- **Manager → executor cancel callback** — rejected: violates the one-directional control plane (ADR-0004).

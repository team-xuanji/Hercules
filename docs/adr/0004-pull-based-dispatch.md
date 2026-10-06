# ADR-0004: Pull-based task dispatch — no MQ, no push channel

- Status: Accepted (2026-10-06)
- Scope: `Hercules-executor/.../schedule/TaskConsumer.java`, `Hercules-executor/.../api/HerculesManagerApi.java`, `Hercules-manager/.../controller/TaskDispatchController.java`, dispatch flow overall

## Context

The standard shape for task distribution is a message queue (Kafka/RocketMQ) with push consumption, or a control plane (K8s Operator / custom scheduler) that invokes workers directly. Both were rejected before the first line of this system was written:

- **Executors deploy where only outbound connections are possible** — private subnets, customer networks, NAT. Any design where the manager must open connections *to* executors requires inbound addressing, firewall rules, and a registry. The executor already needs outbound-only access to download plugin JARs and to reach OSS; the task channel reuses exactly that capability.
- **Middleware operating cost exceeds the problem size.** This is a small team's internal platform, not a multi-tenant product. Running a broker means running a broker's failure modes.
- **Correctness was already solved by the database.** Task claim, completion, and recovery need exactly one atomic primitive, which MySQL already provides (see ADR-0005). A queue would add at-least-once redelivery and idempotency concerns that the DB design does not have.

## Decision

1. Executors **pull**: `TaskConsumer` polls the manager over signed HTTP (fetch → lock, see ADR-0001/0007), executes locally in a bounded thread pool, and reports terminal states back.
2. MySQL is the **single source of truth** for task state. There is no ACK channel and no in-flight bookkeeping outside the DB.
3. The control plane is **strictly one-directional**: the manager never calls the executor. Heartbeats carry everything the manager needs (capacity, loaded plugins, whitelist, identity).
4. Because there is no ACK mechanism, **the manager is the fallback**: dead-executor recovery re-queues or cancels stalled tasks (see ADR-0006).

## Consequences

**Positive**

- An executor runs anywhere an outbound HTTP connection works. Deployment topology is not a system concern.
- Failover semantics come from the DB, not from broker redelivery logic: a task is either claimed (RUNNING, owned) or not.
- No middleware to version, secure, monitor, or lose.

**Costs / accepted risks**

- **Pickup latency**: 10 s poll interval plus a 30 s backoff watermark when idle or busy. Worst case ~40 s from submit to start. Mitigation path (long-poll fetch) is a local change, not an architecture change.
- **Polling load**: mitigated by bucket partitioning (ADR-0002) and adaptive fetch sizing; still nonzero.
- **No cancel delivery**: CANCELLED is a state transition, not a signal — a running plugin is never interrupted by the manager. See ADR-0006.
- **No per-task execution visibility** beyond status columns and logs.

## Alternatives considered

- **MQ with push consumption** — rejected: inbound connectivity assumption plus broker operations; the DB already provides atomic claim (ADR-0005).
- **Manager invokes executors via REST** — rejected: executor addressing/registry, inbound firewall policy, and re-implementing claim/failover over RPC. Evaluated in detail and declined; see conversation history around dynamic endpoint registration.
- **Long-poll / notify-assisted pull** — deferred, not rejected: keeps every property above while removing most of the latency floor. The natural next step, recorded here so the latency cost is never treated as permanent.

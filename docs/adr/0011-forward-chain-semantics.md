# ADR-0011: Forward chain task semantics — at-least-once, deterministic derived IDs, bounded depth

- Status: Accepted (2026-10-09)
- Scope: `Hercules-executor/.../service/impl/ExecutorProcessHandleImpl.java` (`tryForwardTask`, `process`), `Hercules-manager/.../service/impl/HerculesTaskManagerServiceImpl.java` (`submitOnceTask`, `resolveChainDepth`), `Hercules-common/.../status/TaskExecutionContext.java` (`forwardRequest`, `forwardRequestMustWait`), `Hercules-common/.../http/HerculesRunnableTaskInfo.java` (`buildDefaultUniId`, `chainDepth`)

## Context

A plugin can chain work by placing `HerculesRunnableTaskInfo` objects into `TaskExecutionContext.forwardRequest` during `execute()`. Four questions had no answer when a submission could be retried:

1. **Replay**: the executor submits a forward, the response is lost, it retries. Was the first attempt persisted? Without an answer, retries either duplicate the chain step or never happen.
2. **Identity**: forwards usually have no ID. The manager's idempotent submit (ADR-0010 era) keys on ID — no ID, no idempotency.
3. **Cycles**: a buggy plugin that forwards to itself (or A→B→A′ with a mutated context) generates tasks without bound.
4. **Cross-parent collision**: two parents forwarding the identical payload must not converge onto one child.

The legacy `forwardRequestMustWait` flag pretended to address chain reliability but only controlled whether a submission error failed the parent; its name promised "wait for completion", which was never implemented. There is no MQ and therefore no transactional outbox: the manager's MySQL is the only durable record, and every forward crosses an HTTP boundary twice.

## Decision

1. **Chains are at-least-once, ordered forward-before-success.** The executor submits all forwards *before* reporting the parent successful. A crash in the window between "forwards persisted" and "parent finished" leaves the parent RUNNING; the liveness reaper (ADR-0006) re-executes it, and dedup absorbs the replay. The opposite order (finish, then forward) would make that same crash lose the chain silently behind a SUCCESS parent. If forwards still fail after executor-side retries, the parent is failed — **loud, never silently truncated**.

2. **Deterministic derived IDs.** A forward without an ID (or with a blank one) gets `md5(pluginGroup|pluginHandle|context|executorRegion|fromType|fromSourceId)`. The executor fills `fromType=FORWARD` and `fromSourceId=<parentTaskId>` first, so the parent's identity is part of the hash: same parent + same payload ⇒ same ID (replay-safe), different parent + same payload ⇒ different ID (no cross-parent collision). An explicit plugin-set ID always wins — the escape hatch for exactly-once intentions.

3. **Manager-side idempotent submit.** `submitOnceTask` saves and catches `DuplicateKeyException` → returns the already-persisted task. Retry-after-timeout is therefore harmless, and the whole protocol survives a lost response. Idempotent replays return the existing row without re-running depth checks (it passed at first insert).

4. **Bounded depth instead of cycle detection.** The manager derives chain depth as parent.depth + 1 for FORWARD tasks — never trusting a client-supplied value — and rejects submissions beyond `hercules.task.max-chain-depth` (default 32, validated ≥ 1 at startup). Depth is monotonic along any chain, so any cycle (A→B→A′→…) eventually exceeds the limit and is cut; no visited-set graph walk is needed. Depth is stored on the task row (`CHAIN_DEPTH`); derivation widens to `long` so a row poisoned to `Integer.MAX_VALUE` cannot overflow the increment back into negative.

5. **`forwardRequestMustWait` is deprecated and ignored.** Submission strictness is now the only mode. "Wait for completion" was rejected for three recorded reasons: it holds executor slots for the whole chain; chained derivation (A→B→C) exhausts every slot and deadlocks; and it requires polling child status across a one-directional control plane (ADR-0004). Planned replacement: short-lived **check tasks** that verify the previous step, forward the next, and exit — no slot held across the chain.

6. **Exact duplicates within one forward batch collapse** (identical payloads under one parent dedupe to one task via the in-batch cache). An occurrence counter in the derived ID was considered and dropped; the explicit-ID escape hatch covers anyone who genuinely needs N identical children.

## Consequences

**Positive**

- Forward chains survive executor crashes, response loss, and parent re-execution — the common failure modes of a pull-based system without adding any coordination machinery.
- Runaway recursion and accidental cycles cost at most `max-chain-depth` tasks each.
- Chain lineage (`FROM_TYPE=FORWARD`, `SOURCE_ID=parentId`, `CHAIN_DEPTH`) is queryable in the task table for debugging.

**Costs / accepted risks**

- **Duplicates under mutation**: a plugin that forwards "the same" step with a slightly different context each time defeats dedup by design. Only the depth limit bounds it. Accepted — a misbehaving plugin is an operator problem, and 32 is a cheap lesson.
- Deterministic IDs expose payload equality via ID equality to anyone who can read the task table. Accepted under the DB-trusted threat model (ADR-0009).
- A deterministic-rejection (depth exceeded) costs the executor 3 pointless retries before the parent fails. Accepted; a distinct business code for fail-fast is future polish.

## Alternatives considered

- **Plan snapshot / outbox table written with the parent task** — rejected: the parent never writes to the manager itself; inserting an outbox row from the executor mid-flight reintroduces the two-writer problem across HTTP. The parent task row *is* the outbox (decision 1).
- **Manager-side content dedup window** (reject a forward whose payload hash was seen recently) — rejected: probabilistic, race-prone, and redundant once IDs are deterministic.
- **Occurrence counter in the derived ID** (`fwd-{parent}-{hash}-{n}`) — rejected: ordering guarantees for a `Collection` are not part of the plugin contract, and the counter only disambiguates exact duplicates, which decision 6 deliberately collapses anyway.
- **Wait-for-completion semantics** — rejected as recorded in decision 5; the check-task mechanism supersedes it.

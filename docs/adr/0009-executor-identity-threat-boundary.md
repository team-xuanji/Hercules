# ADR-0009: Executor identity model and threat boundary

- Status: Accepted (2026-10-06)
- Scope: `Hercules-executor/.../config/RunnerEnv.java` (`runnerIdentityId`), `Hercules-common/.../util/{ExecutorInfoUtils,HmacUtils}.java`, `Hercules-manager/.../controller/TaskDispatchController.java` (`isInvalidExecutorOp`), `HERCULES_EXECUTOR_INFO.IDENTITY_ID` (migration `2026-10-04-identity-and-indexes.sql`)

## Context

Task operations mutate business state (claim, complete, fail, cancel) and must be authenticated as coming from a genuine executor. Two protection layers exist and must not be conflated:

1. **Channel protection** — request/response bodies are AES-encrypted with a single global `httpEncryptKey`. This defends the wire. It is *not* authorization: every genuine executor holds the same key, so key possession proves membership in the fleet, nothing more.
2. **Operation authorization** — the layer this ADR defines.

The conventional choices for layer 2 are mTLS, OAuth/OIDC, or pre-shared per-executor keys from a provisioning system. All three carry operational machinery (cert lifecycle, token service, key distribution) that a small internal platform should not run.

## Decision

1. **Identity is self-minted**: each executor generates `EXECUTOR_IDENTITY_<region>_<GUIDv7>` at boot. Nothing is provisioned or pre-shared.
2. **Registration is TOFU**: the identity travels inside the AES-encrypted heartbeat; the manager stores it in `HERCULES_EXECUTOR_INFO.IDENTITY_ID`. First report of an instance id wins; the row is refreshed every heartbeat.
3. **Operations are signed**: `passSign = HMAC-SHA256(identityId, subject + ":" + op)` with constant-time verification. Subjects: taskId for LOCK/FINISH/FAIL/ABANDON, executorId for FETCH (ADR-0001), the sorted id set for batch lock (ADR-0007).
4. **The declared trust boundary is DB read access.** Identities are stored plaintext; anyone who can read the table can sign operations. This is accepted under the threat model "the database is trusted infrastructure" — a DB *writer* needs no signature at all, since they can UPDATE task rows directly. Hardening the read path (configured identities, hashing) is a different trade, deferred.
5. **Rejection responses are uniform** (`Invalid executor identity signature.`) with reasons logged server-side under `[INVALID_EXECUTOR_OP]` — the endpoints must not act as oracles for which check a probe failed (ADR-0001).

## Consequences

**Positive**

- Zero provisioning machinery: an executor is trusted the moment it proves possession of an identity it just made up — the security comes from the identity being unguessable (128-bit GUIDv7), not from distribution.
- One validation gate for all five ops; rejection telemetry is centralized and greppable.

**Costs / accepted risks**

- **Replay**: the MAC is deterministic (no nonce/timestamp); a captured request can be replayed verbatim. Accepted because every op is bound to (subject, op) and guarded by the task state machine (ADR-0005) — replaying FINISH re-sets the same value, replaying LOCK no-ops after claim. Residual risk is documented, not overlooked.
- **Identity lifetime = process lifetime**: no revocation or rotation story; a compromised executor is untrusted only after restart. Config-injected identity would fix this and is deferred, not rejected.
- **TOFU exposure**: the first reporter of an instance id defines its identity. Within a network where heartbeat traffic is channel-protected, an interloper must beat the real executor to registration inside one heartbeat window — accepted for the same trust posture as the admin plane.

## Alternatives considered

- **Pre-shared / config-injected identity** — deferred: enables revocation, stable identity across restarts, and keeping secrets out of the DB. Deferred because the current threat model does not require it; this ADR is the place to revisit when that changes.
- **mTLS or OAuth** — rejected: certificate/token lifecycle is a second system's worth of operations, against the poverty philosophy (ADR-0004).
- **Hashing identities at rest** — rejected for now: the manager needs the raw key to verify, so hashing only protects against offline table dumps, which the threat model already covers via DB access control.

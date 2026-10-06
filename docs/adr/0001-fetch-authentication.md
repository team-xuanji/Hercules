# ADR-0001: HMAC signing for the task fetch API

- Status: Accepted (2026-10-04)
- Scope: `Hercules-manager/.../controller/TaskDispatchController.java`, `Hercules-common/.../util/ExecutorInfoUtils.java`, `Hercules-executor/.../api/HerculesManagerApi.java`, `Hercules-executor/.../schedule/TaskConsumer.java`

## Context

Hercules has two protection layers with different responsibilities:

1. **Channel protection** — request/response bodies are AES-encrypted with a single global `httpEncryptKey` shared by the manager and every executor. This defends against network outsiders. It is **not** an authorization mechanism: any genuine executor holds the same key.
2. **Operation authorization** — every mutating task op (`LOCK`, `FINISH`, `FAIL`, `ABANDON`) carries `passSign = HMAC-SHA256(identityId, subject + ":" + op)`, keyed by the per-executor `runnerIdentityId` minted at boot (`EXECUTOR_IDENTITY_<region>_<GUIDv7>`). A leaked config key therefore exposes the channel but does not grant execution rights.

Until this revision, the read path (`GET /taskDispatch/tryFetchTasksWithByteArray`) only checked that the executor **exists**. The loaded entity was discarded; `executorRegion` was trusted as a plain query parameter. Consequence: any genuine executor could read another region's task queue by passing that region's parameter value. Execution rights were never at risk (the lock HMAC is per-op and per-task), but the read surface crossed region boundaries.

Additional constraints at decision time:

- The region of an executor is a deployment property, fixed by configuration. Who may deploy an executor as region X is governed by config management / infrastructure — the same trust boundary as the admin plane.
- Manager and executor live in the same repository and are deployed together.

## Decision

1. `ExecutorTaskOps` gains `FETCH`. The fetch endpoint now requires `passSign` and validates through the same gate as every other op — `isInvalidExecutorOp(executorId, executorId, passSign, FETCH, executorRegion)` — which enforces, in order: executor is registered, claimed region equals registered region, signature verifies.
2. The **signed subject is `executorId`** (the per-boot instance id), not the region. The signature answers "who is asking"; region membership is answered authoritatively by comparing the request parameter against the executor's DB record. Tampering with the region parameter fails the equality check before signature semantics even matter.
3. All validation failures return the **same response body** — `Invalid executor identity signature.` — regardless of which of the three checks failed. The specific reason (unknown executor / region mismatch / signature failure) is logged server-side under the structured `[INVALID_EXECUTOR_OP]` tag with full context. This prevents the endpoint from being used as an oracle that enumerates which check a probe passed.
4. On the client side, `TaskConsumer` signs `HMAC-SHA256(runnerIdentityId, executorId + ":" + FETCH)` before every fetch.

## Consequences

**Positive**

- Read and execute paths now sit at the same authorization level: registered + region-matched + HMAC-holding. The cross-region read exposure described above is closed.
- One validation gate for five ops instead of divergent per-endpoint checks; rejection telemetry is centralized and greppable.

**Costs / accepted risks**

- `passSign` is a required parameter. Old executors that do not send it are rejected with HTTP 400 immediately, so **manager and executor must ship in the same release**. Given same-repo deployment this is acceptable today; if a mixed-version window ever matters, temporarily make the parameter optional, log its absence as a warning, and tighten later.
- Existing databases get `IDENTITY_ID` as NULL; rows are re-registered by the next heartbeat (10 s interval; manager executor-list cache is 30 s). Until then the signature key is absent and ops are rejected — **fail-closed** by design, for a window of at most tens of seconds.
- The HMAC is deterministic (no nonce/timestamp), so a captured valid request could be replayed unchanged. This is consistent with the pre-existing op-signature design and the single-manager deployment model; adding replay protection would require protocol versioning and is deliberately out of scope.
- The shared AES key remains what it always was: channel protection, not authorization.

## Alternatives considered

- **Sign the region as subject** — rejected: region membership is already enforced by a server-side comparison against the registration record; signing it adds no attacker-relevant guarantee and couples the signature to a value the server re-derives anyway.
- **Optional `passSign` for gray rollout** — deferred, not rejected: correct for mixed-version fleets; unnecessary for the current same-repo deployment model. See Consequences.

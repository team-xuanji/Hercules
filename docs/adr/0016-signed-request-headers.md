# ADR-0016: Signed request headers (canonical message + timestamp window)

Date: 2026-10-09
Status: Accepted
Supersedes: signing detail of [ADR-0009](0009-executor-identity-threat-boundary.md) (the `passSign` scheme).

## Context

ADR-0009 established the executor identity threat boundary and an initial
signing scheme: every executor→manager task operation carried a `passSign`
parameter — `HMAC(identityId, subject + ":" + op)` — placed in the HTTP query
string (fetch, lock, abandon, fail) or request body (batch lock, finish).
That scheme had three structural weaknesses:

1. **Payload not covered.** The signature bound only `subject` and `op`; the
   request body and query parameters were unsigned. A network attacker or a
   compromised intermediary could alter `checkPointInfo`, `fetchLimit`, or the
   task-id list without invalidating the signature.
2. **No replay protection.** There was no timestamp or nonce. A captured
   `passSign` could be replayed indefinitely. The system relied entirely on
   task-state-machine idempotency (conditional UPDATE on `status = INIT`) to
   absorb replays — which it does for LOCK, but the fetch endpoint returned
   tasks on every replay, leaking task data to a passive observer with a
   captured signature.
3. **Secret leakage surface.** `passSign` traveled in query strings, which are
   logged by proxies, access logs, and browser history — the signature value
   itself was not secret (it's a MAC, not a key), but its placement increased
   the replay-capture surface and made log hygiene harder.

The identity model itself (ADR-0009) is unchanged: `identityId` is the
per-executor signing key, provisioned at executor startup, known to the
manager via `HerculesExecutorInfo`.

## Decision

Replace `passSign` with a header-based HMAC-SHA256 signing protocol covering
the full request payload plus a timestamp, with no compatibility layer.

### Headers

Every signed request carries four headers:

| Header | Value |
|---|---|
| `X-Hercules-Op` | Operation enum name: `FETCH`, `LOCK`, `FINISH`, `FAIL`, `ABANDON` |
| `X-Hercules-Subject` | `executorId` for FETCH; `taskId` for single ops; comma-joined sorted task ids for batch LOCK |
| `X-Hercules-Timestamp` | Epoch milliseconds, string |
| `X-Hercules-Signature` | Lowercase hex HMAC-SHA256, key = executor `identityId` |

### Canonical message

Client and server build a byte-identical canonical message — five fields
joined with `\n`:

```
OP + "\n" + SUBJECT + "\n" + TIMESTAMP + "\n"
    + CANONICAL_QUERY + "\n" + SHA256_HEX(BODY)
```

- **CANONICAL_QUERY**: decoded query parameters, sorted by name ascending;
  same-name values sorted by string ascending; `name=value` joined with `&`;
  no percent-encoding (inputs are already decoded on both sides). Empty = `""`.
  On the client, derived from the Feign `RequestTemplate.queries()`; on the
  server, from `HttpServletRequest.getParameterMap()`.
- **SHA256_HEX(BODY)**: lowercase hex SHA-256 of the raw wire body bytes;
  no body = `""`.

The single definition lives in `ExecutorInfoUtils.buildCanonicalMessage`,
shared by `signRequest` and `verifyRequest` so signer and verifier cannot
drift.

### Timestamp freshness window

`FRESHNESS_WINDOW_MS = 300_000` (5 minutes). The server rejects any request
whose `|now - timestamp|` exceeds the window or whose timestamp is
unparseable. Rejection is uniform ("Invalid executor identity signature.")
across all failure causes (unknown executor, region mismatch, missing header,
stale timestamp, bad signature) so the endpoints do not act as oracles for
which check a probe failed.

### Replay within the window

Replays within the 5-minute window are still possible — the protocol relies
on the existing task-state-machine idempotency to absorb them:

- **LOCK**: conditional UPDATE (`status = INIT` → `RUNNING`) is idempotent;
  a replayed lock on an already-locked task is a no-op (`NOT_CLAIMED`).
- **FINISH/FAIL/ABANDON**: conditional UPDATE on `status = RUNNING` is
  idempotent; a replayed finish/fail on an already-terminal task is a no-op.
- **FETCH**: returns tasks not yet locked; a replayed fetch may return
  different tasks (those inserted after the original fetch), but the executor
  that captures the signature already has a legitimate identity. The residual
  risk is task-data leakage to a passive observer with a captured signature
  within 5 minutes — accepted as low, because the observer must also be on
  the executor→manager network path.

### Why no nonce table

A server-side nonce table would provide true one-time replay prevention, but
it was **explicitly deferred**:

1. **Feign automatic retry conflict.** Feign's default retry behavior
   (and the existing `FeignInterceptor`/gateway retry path) re-sends a request
   on transient failure. A one-time nonce would cause the retry to fail
   signature verification on the server, breaking the retry semantics that the
   executor depends on for resilience. Supporting both retry and one-time
   nonces would require the client to skip nonce rotation on retry, which is
   itself a replay window — defeating the purpose.
2. **Operational cost.** A nonce table adds a DB write on every signed request
   (including the high-frequency fetch), GC of expired nonces, and a new
   failure mode (nonce store unavailable → all executor ops fail). The
   task-state-machine idempotency already absorbs the practical replay risk.
3. **No new DB schema.** This change is scoped to common/executor/manager
   code; a nonce table would require a schema migration, which is out of scope.

The 5-minute window bounds the replay exposure to a short, loggable interval;
conditional-UPDATE idempotency makes the bound sufficient for the mutating
ops. If the fetch replay risk is later judged unacceptable, a per-fetch nonce
or sequence number can be added without changing the canonical message format.

### Body caching

The manager's `CachedBodyFilter` (`OncePerRequestFilter`) caches the raw body
bytes for `/taskDispatch/` PUT requests, because Spring's `@RequestBody`
deserialization consumes the input stream before signature verification reads
it. The cached bytes are stored in request attribute `hercules.rawBody` and
replayed via an `HttpServletRequestWrapper`, so the signature is computed
over the same bytes the server deserializes. GET requests have no body and
pass through.

## Consequences

- **Old `passSign` removed.** No compatibility layer — the query/body
  `passSign` parameter and `getExecutorSign`/`verifyExecutorSign` are deleted.
  An executor on the old protocol will fail all signed endpoints with HTTP 400.
- **`BatchLockRequest.passSign` and `FinishOneTaskRequestVO.passSign` removed**
  from both common and manager VOs.
- **Feign `SigningRequestInterceptor`** is registered on the Feign builder and
  signs every request that carries the op/subject headers. Unsigned endpoints
  (heartbeat, plugin lookup, status check) are unaffected.
- **QA plaintext `tryFetchTasks` endpoint** is not signed and is unchanged.
- **Region semantics unchanged**: ABANDON/FAIL/FINISH pass `null` for
  `executorRegion` (region check skipped); FETCH/LOCK pass the executor's
  registered region.
- **Twelve-labors gateway signing** (`ApiGatewayUtil`) is a separate mechanism
  and is not touched.

## Open items

- If fetch replay within 5 minutes becomes a concern, add a per-fetch
  sequence number to the canonical message (no format change needed — add
  an 8th field or fold into SUBJECT).
- Consider logging signature verification failures with enough detail for
  incident response without leaking which check failed (currently logged
  under `[INVALID_EXECUTOR_OP]` with the executor id and op).

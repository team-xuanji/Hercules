# ADR-0013: Binary fetch channel — Fory + ZSTD + AES-GCM as a fixed pipeline

- Status: Accepted (2026-10-09)
- Scope: `Hercules-manager/.../controller/TaskDispatchController.java` (`tryFetchTasksWithByteArray`, `fetchTasks`), `Hercules-executor/.../api/HerculesManagerApi.java` (`tryFastFetchTasks`), `Hercules-executor/.../schedule/TaskConsumer.java` (poll loop)

## Context

Every executor polls the manager for runnable tasks, and a poll can return up to `BATCH_FETCH_MAX_SIZE` (200) tasks whose `context` fields routinely carry kilobytes of JSON each. At the default 10 s poll interval per executor, the fetch path is one of the hottest in the system. Shipping these payloads as an HTTP-JSON body costs serialization and parse time plus GC pressure on both sides, every round, forever.

The channel must also fit the existing envelope: no new infrastructure, and authentication stays exactly ADR-0001 (HMAC-signed request, per-executor identity). Any payload protection must reuse the already-distributed channel key rather than invent a second key system. Fetch is the only endpoint whose payload justifies this treatment — task contexts are bulk, repetitive, and opaque to the manager.

## Decision

1. **Fetch has exactly one wire format: binary.** `tryFetchTasksWithByteArray` returns `application/octet-stream`; there is no JSON variant of the fetch endpoint. The rest of the API (submit, lock, finish, status) stays JSON — the split is by payload shape, not by endpoint age.
2. **A fixed encode pipeline for non-empty results**: `Fory serialize → ZSTD compress → AES-GCM encrypt`. The decoder peels the same layers in reverse (decrypt → decompress → deserialize), keyed entirely off response headers:
   - `HERCULES_BINARY_RESP_AES_IV` — present iff the body is encrypted; carries the per-response IV.
   - `HERCULES_BINARY_RESP_COMPRESS_TYPE` / `..._ORIGINALS_SIZE` / `..._COMPRESSED_SIZE` — the compression layer and the uncompressed Fory size the decompressor needs.
   Header-driven decoding keeps the client tolerant: a response without the IV header skips decryption instead of failing, which is what makes the empty case (below) work and leaves room for a future unencrypted deployment profile without a client change.
3. **A zero-length body is a valid, expected result** — the empty poll is the common case and must be the cheapest one. It bypasses both layers (no IV header → no decrypt; zero bytes → empty `TaskFetchResult`, not an error).
4. **Field-level context encryption is disabled inside this payload** (`fetchTasks(..., encrypt=false)`): the outer AES-GCM envelope covers the whole body, so encrypting each `context` again would be double work for zero additional coverage. Other endpoints that return single tasks keep the field-level path.
5. **The request side is unchanged**: query parameters, HMAC-signed per ADR-0001. The optimization is scoped to the response payload only.

## Consequences

**Positive**

- The hot path drops JSON serialization/parse cost on both ends; ZSTD on repetitive Fory bytes shrinks the wire further before encryption.
- One key, one identity story: payload protection reuses the channel key from ADR-0001; nothing new to distribute or rotate.
- The manager decides the protection profile per response via headers; the executor needs no version negotiation protocol.

**Costs / accepted risks**

- **Two wire formats for task data coexist** (JSON everywhere except fetch). The boundary is one endpoint in each direction; decode failure is loud, and the shared `TaskFetchResult` type is the single schema source. Accepted.
- **Compression-before-encryption is mandatory in this pipeline**; compressing ciphertext would be ineffective. The fixed order removes the temptation to "optimize" it later — changing the order requires a header or endpoint change, which is the correct amount of friction.
- The empty-poll WARN on the manager side ("Unable to retrieve task information") fires on every quiet poll round per executor. Log-noise cost, accepted here and noted for a future sweep.
- Fory is schema-less binary: a type change compiles fine and breaks at runtime. Accepted: `TaskFetchResult` is small, stable, and shared via the common module; the failure mode is immediate and recoverable by deployment pairing.

## Alternatives considered

- **JSON fetch** — rejected: the poll loop is the system's steady-state heartbeat; paying parse-and-GC costs every 10 s per executor for opaque strings buys nothing.
- **Opt-in/layer-negotiated protection** — rejected in favor of the fixed pipeline: with only one deployment profile in practice, negotiation is speculative generality. Header-driven *decoding* is retained (decision 2) so the client can tolerate profile changes without a protocol version.
- **gRPC / protobuf** — rejected: schema and toolchain weight for a single hot endpoint, plus an ingress story (HTTP/2 proxies) the deployment envelope does not promise.
- **Serialize only `context` as bytes inside a JSON envelope** — rejected: partial solutions age badly; the next field that grows hot would restart the debate. Whole-message binary ends it.

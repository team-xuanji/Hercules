# ADR-0007: Batch task lock — one UPDATE, then re-read to classify

- Status: Accepted (2026-10-06)
- Scope: `Hercules-manager/.../controller/TaskDispatchController.java` (`tryLockBatchTask`, `lockBatchTask`, `findMissMatchTasks`, `cancelIfDriftedTask`), `Hercules-executor/.../schedule/TaskConsumer.java` (`process`), `Hercules-common/.../http/{BatchLockRequest,BatchTaskLockProcessResult,TaskInfoLockResult}.java`

## Context

A poll cycle fetches up to the executor's queue capacity (capped at `BATCH_FETCH_MAX_SIZE = 200`) tasks, each of which previously required its own lock round trip (`tryLockOneTask`). At 200 tasks that is 200 sequential HTTP calls per cycle — the lock path, not execution, had become the polling cost center. Signing every fetch and lock also meant 200 signature computations with per-task subjects.

Requirements for the batch design: no weakening of the no-double-ownership guarantee (ADR-0005); explicit per-task outcomes (a batch response of "partly ok" must say which); and signature semantics that do not depend on list ordering.

## Decision

1. **One endpoint**, `PUT /taskDispatch/tryLockBatchTask`, ≤ `BATCH_FETCH_MAX_SIZE` ids per request. The single-task `tryLockOneTask` is a thin shim over it, preserving its old response contract (`data=false` = already claimed, not an error).
2. **Pipeline per batch**: one query loads all tasks → region/existence pre-classification in memory → plugin drift checked per *group* (one registry query per group, not per task) with drifted tasks bulk-cancelled → **one conditional UPDATE** claims every remaining task → a re-read classifies each id as SUCCESS (now owned by caller) or NOT_CLAIMED.
3. **The UPDATE is atomic per row** (ADR-0005), so batch claiming cannot produce co-ownership; races between the UPDATE and the re-read (e.g. dead-task recovery resetting a row) can only yield a pessimistic NOT_CLAIMED, which callers retry safely.
4. **Signature subject is the sorted, comma-joined id list** (`ExecutorInfoUtils.buildSignSubject`): signer and verifier may collect ids in any order and still produce the same subject. Per-task op binding is preserved because LOCK/FINISH/FAIL/ABANDON remain per-task subjects.
5. Per-task outcomes are an explicit enum (`TaskInfoLockResult`: SUCCESS / NOT_CLAIMED / NOT_EXIST / PLUGIN_DRIFT / REGION_MISMATCH); callers must tolerate a missing map entry or a null map as "not locked".

## Consequences

**Positive**

- N lock round trips → 1 per poll cycle; signature computations → 1 per batch.
- Plugin drift detection dropped from one registry+storage lookup **per task** to one DB-level lookup **per group**.
- Outcomes are machine-readable; the consumer skips non-SUCCESS tasks with one warn line instead of inferring failure from HTTP codes.

**Costs / accepted risks**

- The UPDATE-then-reread pattern costs one extra read per batch and can misreport a concurrently-reset task as NOT_CLAIMED (safe, occasionally noisy).
- `BATCH_FETCH_MAX_SIZE` couples manager validation and client fetch sizing; both reference the same constant, and a larger value is a one-line change with index-scan implications (ADR-0003).
- The empty-request early return yields a success with a null result map — documented on `BatchTaskLockProcessResult`, defended on the caller.

## Alternatives considered

- **Loop of single-task locks** — rejected: N round trips, the exact cost this ADR eliminates.
- **SELECT ... FOR UPDATE then batch UPDATE in one transaction** — rejected: gap locks and longer lock holding for a classification problem that a re-read solves more cheaply.
- **Separate claim/ledger table** — rejected: an extra write per claim buys nothing the conditional UPDATE does not already provide.

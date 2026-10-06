# Architecture Decision Records

One decision per file, in English only.
Status: Proposed | Accepted | Superseded by ADR-XXXX.

| ADR | Decision | Status |
|---|---|---|
| [0001](0001-fetch-authentication.md) | HMAC signing for the task fetch API | Accepted |
| [0002](0002-get-consume-range-fallback.md) | Full bucket range fallback for unregistered executors | Accepted |
| [0003](0003-bucket-index-strategy.md) | Dispatch/recovery table index strategy | Accepted |
| [0004](0004-pull-based-dispatch.md) | Pull-based task dispatch — no MQ, no push channel | Accepted |
| [0005](0005-conditional-update-locking.md) | Conditional UPDATE as the task concurrency primitive | Accepted |
| [0006](0006-executor-liveness-failure-detection.md) | Failure detection via executor liveness, deliberately no per-task lease | Accepted |
| [0007](0007-batch-lock-protocol.md) | Batch task lock — one UPDATE, then re-read to classify | Accepted |
| [0008](0008-plugin-classloading-model.md) | Plugin classloading — parent delegation, lazy download, version rotation | Accepted |
| [0009](0009-executor-identity-threat-boundary.md) | Executor identity model and threat boundary | Accepted |

## Convention

- New ADRs take the next number; numbers are never reused.
- A decision is never edited away once Accepted — supersede it with a new ADR that links back.
- Context should record the constraints that made the decision right at the time, so future readers know which assumptions they may safely break.

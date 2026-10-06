# Architecture Decision Records

One decision per file, in English only.
Status: Proposed | Accepted | Superseded by ADR-XXXX.

| ADR | Decision | Status |
|---|---|---|
| [0001](0001-fetch-authentication.md) | HMAC signing for the task fetch API | Accepted |
| [0002](0002-get-consume-range-fallback.md) | Full bucket range fallback for unregistered executors | Accepted |
| [0003](0003-bucket-index-strategy.md) | Dispatch/recovery table index strategy | Accepted |

## Convention

- New ADRs take the next number; numbers are never reused.
- A decision is never edited away once Accepted — supersede it with a new ADR that links back.
- Context should record the constraints that made the decision right at the time, so future readers know which assumptions they may safely break.

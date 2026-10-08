# ADR-0010: Single shared task wire DTO — `HerculesRunnableTaskInfo` for both directions

- Status: Accepted (2026-10-09)
- Scope: `Hercules-common/.../http/HerculesRunnableTaskInfo.java`, `Hercules-manager/.../util/DTOConvertUtils.java` (`parse2TaskInfo`, `parse2RunnableTaskInfo`), `Hercules-manager/.../controller/TaskManagerController.java` (`submitOnceTask`), `Hercules-twelve-labors` (client API)

## Context

The submit endpoint historically bound its own request type (`SubmitOnceTypeTaskRequestVO` in the manager, plus a second hand-copied, already-deprecated duplicate inside `Hercules-twelve-labors`), while the fetch/forward/status paths already used `HerculesRunnableTaskInfo`. The same concepts therefore existed under up to three names per field (`aesIV` vs `encryptIV`; `fromSourceId` vs `sourceId`), with no compiler-level link between the two sides of any HTTP boundary.

This drift caused a real bug, not a theoretical one: executor-forwarded chain tasks carry `fromType=FORWARD` and `fromSourceId=<parentId>`, but the manager's submit VO had no matching properties — Jackson binds by exact name, so both values were silently dropped and every forward task was persisted as `fromType=ONCE` with no lineage. The first repair attempt repeated the mistake in mirror image (fields named `fromTaskType`/`sourceId` on one side, `fromType`/`fromSourceId` on the other), binding to nothing. The project has no global naming strategy and no `@JsonProperty` aliases: exact-name binding is the contract, and the contract was unwritten.

## Decision

1. **One type serves both directions.** `HerculesRunnableTaskInfo` is the request body of `submitOnceTask` (business submissions and executor forwards) and the response type of fetch, lock, status, and submit. Both ADR-0011's lineage fields now survive the wire by construction.
2. **All conversion lives in one class.** `parse2TaskInfo` (request → `HerculesTaskInfo` PO) moved from the deleted VO into `DTOConvertUtils`, next to the existing `parse2RunnableTaskInfo` (PO → response). The two directions of one contract are visible in one file.
3. **Validation annotations live on the shared DTO** (`@NotBlank` on region/group/handle/context) and are enforced only where `@Valid` is present — the submit endpoint. Other paths (fetch responses, executor forwards) deserialize without triggering them, preserving previous behavior.
4. **Field names were unified, not aliased**: `aesIV` is gone in favor of the existing `encryptIV`; `hashKey` moved into the shared type. Aliases were rejected (see below).
5. The duplicated VO copies (manager + twelve-labors) are **deleted**, not deprecated — deprecation on a wire type only delays the drift it exists to prevent.

## Consequences

**Positive**

- Contract drift inside the repo is now a compile error, not a runtime silent-drop. Adding a field (e.g. `chainDepth`, ADR-0011) is visible to every party at once.
- The forward lineage bug class is closed at the root: there is no "other type" for values to get lost between.
- One fewer concept inventory for new contributors.

**Costs / accepted risks**

- **Breaking change for out-of-repo callers**: any external submit client that sent `aesIV` must rename to `encryptIV`. Accepted: the project is pre-1.0, in-repo docs never recorded `aesIV`, and a silent-bind bug is worse than a loud rename.
- The shared DTO accumulates fields that matter only to some paths (`status`, `encryptIV`, `chainDepth`). Mitigation: javadoc on each field states which direction populates it and which ignores it.

## Alternatives considered

- **Keep separate VOs per endpoint** — rejected: this was the status quo, and it produced the exact bug this ADR exists to prevent. Manual "keep them in sync" discipline has no enforcement mechanism.
- **`@JsonProperty` aliases on both names** — rejected: two names for one concept, forever, with the ambiguity compounding at every new field. Renaming is a one-time cost; aliasing is a permanent tax.
- **Rename via deprecation cycle (accept both old and new for one release)** — rejected as process overhead for a pre-1.0 project with no known external consumer of the affected field.

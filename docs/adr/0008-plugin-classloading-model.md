# ADR-0008: Plugin classloading — parent delegation, lazy download, version rotation

- Status: Accepted (2026-10-06)
- Scope: `Hercules-executor/.../service/impl/ExecutorProcessHandleImpl.java` (`loadPlugin`), `Hercules-executor/.../config/TaskPluginContext.java`, `Hercules-common/.../classloader/LocalJarURLStreamHandlerFactory.java`

## Context

Plugins are business-logic JARs uploaded to the manager and distributed to executors on demand. The classloading design trades between two forces:

- **Sharing**: plugins should reuse the executor's utilities (logging, Jackson, `Hercules-common`) without repackaging them.
- **Isolation**: plugins must not corrupt each other or the executor through dependency conflicts, and a plugin upgrade must not break in-flight tasks.

The two forces point at opposite designs (share everything vs isolate everything); this ADR records where the line was drawn and why.

## Decision

1. **One `URLClassLoader` per plugin group per version**, with **parent delegation** to the executor's own classloader: plugins see executor classes and mark shared dependencies `provided`; conflicting dependencies are the plugin author's problem, solved by shading.
2. **Lazy acquisition**: a group's JARs are downloaded on first task touch, keyed by a 20 s version cache (`pluginVersionCache`); a version change downloads and constructs a new classloader, discovered via `ServiceLoader<TaskPlugin>`.
3. **Version rotation**: old classloaders are retained — at most three historical versions — so tasks already running on version N complete on version N while new tasks load version N+1. Old classloaders are closed only after rotation, never from under a running task.
4. **Drift is handled at lock time, not load time**: a task whose `pluginHandle` is no longer registered under its `pluginGroup` is cancelled by the manager before an executor ever sees it (PLUGIN_DRIFT, ADR-0007).

## Consequences

**Positive**

- Plugin JARs stay small: common utilities come from the parent, `provided` scope, no fat-JAR requirement beyond the plugin's own third-party deps.
- Upgrade is zero-downtime per executor: in-flight tasks are never migrated mid-run.
- Failure containment: a broken new version only breaks tasks that start after rotation; previously loaded versions keep working.

**Costs / accepted risks**

- **Dependency conflicts are possible by construction** — a plugin needing a different version of a parent-provided library must shade it. Documented in the plugin development guide; support cost is real but bounded.
- Up to four versions of a group's classes can coexist briefly in memory (current + three retained).
- A plugin can in principle cast to executor internals; there is no sandbox. Accepted: plugins are trusted code from the same organization (see ADR-0009 for the trust posture).

## Alternatives considered

- **Child-first isolation** — rejected: restores dependency safety but destroys sharing; every plugin would have to fat-JAR (and version-pin) all common utilities, and `provided` semantics would break.
- **One classloader per task** — rejected: classloading cost on the hot path, no cacheability, and no meaningful isolation gain over per-group loaders.
- **Process/container isolation per plugin** — rejected: directly against the poverty philosophy (ADR-0004); process-per-plugin multiplies memory and deployment surface by the plugin count.

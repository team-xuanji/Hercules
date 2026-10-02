Extension points are specific implementations of certain business logic. Basically, extension points can be written freely and load any dependencies they want.

However, the current extension points use a parent delegation class loader, which means the plugin itself can access the executor's classes.
This has both advantages and disadvantages:
Some utility class dependencies, logging, etc., do not need to be introduced as additional dependencies, just mark them as provided in the package.

But you may need to spend time troubleshooting dependencies, or you can directly create a shaded package to solve it once and for all.

Extension point packages should not use the Spring framework, bare JAR is sufficient. Because the extension point's driving method is simply creating a new object. It cannot drive such a heavy framework like Spring.

You can check the examples in plugin-test to see how to develop extension points.

Extension point development requires the following work:
- In the extension point module, implement team.magic.flute.hercules.common.PluginRegister and write registration information.
- In the extension point module, implement team.magic.flute.hercules.common.TaskPlugin and write the extension point implementation.
- Package as fatJar and upload to manager. The executor will automatically load it.
- If you want to do testing, introduce Context dependencies in test scope.
- If the executor needs to return results to the business side, write the return results to TaskExecutionContext.checkPointResult.
- TaskExecutionContext.checkPointResult will be written back to the system, and third-party users can query the execution status and results of this task from the manager interface.

The executor currently has DuckDB added at the upper layer and passes it to extension points through context. Extension points can use DuckDB to do many useful things.
For example, cross-source RDS read/write, OSS read/write, OLAP calculations, etc. If needed, we can consider passing ORM framework dependencies down.
Currently integrated tools in context:
- DuckDB, a lightweight OLAP database that can do many things. For example, AI calculations, cross-source RDS read/write, OSS read/write, HTTP/HTTPS read/write, OLAP calculations, full-text search, vector calculations, etc.
- To be determined, but we don't rule out adding some other ORMs. Just not available now.

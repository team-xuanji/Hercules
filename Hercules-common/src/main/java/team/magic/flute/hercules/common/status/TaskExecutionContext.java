package team.magic.flute.hercules.common.status;

import lombok.Data;
import lombok.experimental.Accessors;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.util.Collection;
import java.util.Map;

/**
 * Task Execution Context
 *
 * <p>This class represents the execution context provided to plugins during task execution.
 * It contains all the necessary resources, configuration, and state information that a plugin
 * needs to perform its work within the Hercules execution environment.
 *
 * <p>Key features:
 * <ul>
 *   <li>DuckDB integration for data processing and staging</li>
 *   <li>Task chaining capabilities for workflow orchestration</li>
 *   <li>Extensible context map for custom resources</li>
 *   <li>Result collection and checkpoint management</li>
 *   <li>Automatic resource cleanup through Closeable interface</li>
 * </ul>
 *
 * <p>The context is automatically managed by the Hercules Executor and should not be
 * manually instantiated by plugin developers. It is passed to the plugin's execute method
 * and provides access to all execution resources.
 *
 * <p>Example usage in a plugin:
 * <pre>{@code
 * public void execute(TaskExecutionContext context) throws Exception {
 *     // Access DuckDB for data processing
 *     Connection duckdb = context.getDuckdbConnection();
 *
 *     // Get task configuration
 *     String config = context.getExecutionContext();
 *
 *     // Set results for the business side
 *     context.setCheckPointResult("Processing completed successfully");
 *
 *     // Chain additional tasks if needed
 *     Collection<HerculesSubmitOnceTaskRequest> chainTasks = new ArrayList<>();
 *     chainTasks.add(new HerculesSubmitOnceTaskRequest()...);
 *     context.setForwardRequest(chainTasks);
 * }
 * }</pre>
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain = true)
public class TaskExecutionContext implements Closeable {
    /**
     * DuckDB database connection for data processing and staging.
     *
     * <p>Each Executor comes with a built-in DuckDB instance that is disk-persistent.
     * This powerful analytical database can be used for various tasks including:
     * <ul>
     *   <li>Staging datasets (limited only by available disk space)</li>
     *   <li>Cross-source OLAP analysis and data transformation</li>
     *   <li>Cross-source data synchronization and ETL operations</li>
     *   <li>Complex analytical queries and aggregations</li>
     *   <li>Integration with external data sources (MySQL, OSS, HTTP, etc.)</li>
     * </ul>
     *
     * <p>For detailed DuckDB capabilities, see the
     * <a href="https://duckdb.org/docs/stable/">official documentation</a>.
     */
    private Connection duckdbConnection;

    /**
     * Task execution configuration and parameters.
     *
     * <p>Contains the JSON string with task-specific configuration, parameters,
     * and context data that the plugin needs to perform its work. This is typically
     * provided when the task is submitted to the Hercules system.
     */
    private String executionContext;

    /**
     * Unique task identifier.
     *
     * <p>The unique ID assigned to this task execution instance. This ID can be used
     * for logging, tracking, and correlation purposes throughout the task lifecycle.
     */
    private String id;

    /**
     * Task execution result for business consumption.
     *
     * <p>Plugins can set this field to return results to the business side.
     * The result will be persisted and made available through the Hercules Manager
     * API for third-party systems to query task execution status and outcomes.
     */
    private String checkPointResult;

    /**
     * Additional context resources and utilities.
     *
     * <p>This extensible map can contain various resources and utilities that the
     * executor provides to plugins, such as:
     * <ul>
     *   <li>Message queue clients (Kafka, RabbitMQ, etc.)</li>
     *   <li>Database connections to external systems</li>
     *   <li>HTTP clients with specific configurations</li>
     *   <li>Custom utilities and services</li>
     * </ul>
     *
     * <p>Resources in this map that implement {@link Closeable} will be automatically
     * closed when the context is disposed.
     */
    private Map<String, Object> anotherContextMap;

    /**
     * Chain task requests for workflow orchestration.
     *
     * <p>Plugins can use this field to trigger additional tasks as part of a workflow.
     * After the current plugin completes, the executor will submit these tasks for
     * execution. This enables complex multi-step workflows and task dependencies.
     *
     * <p>Note: Currently, plugin relationships are not centrally managed, so plugins
     * must handle their own task chaining logic and dependencies.
     */
    private Collection<HerculesRunnableTaskInfo> forwardRequest;

    /**
     * Whether to wait for chain task completion.
     *
     * <p>Controls the execution behavior for chain tasks:
     * <ul>
     *   <li>{@code false} (default): Chain tasks execute asynchronously and don't affect
     *       the current task's success/failure status</li>
     *   <li>{@code true}: The current task will only be considered successful if all
     *       chain tasks also complete successfully</li>
     * </ul>
     */
    private boolean forwardRequestMustWait=false;

    /**
     * Clean up and close all resources associated with this execution context.
     *
     * <p>This method is automatically called by the Hercules Executor when task execution
     * completes (either successfully or with an error). Plugin developers do not need to
     * call this method manually.
     *
     * <p>The cleanup process includes:
     * <ul>
     *   <li>Closing the DuckDB connection</li>
     *   <li>Closing any {@link Closeable} resources in the context map</li>
     *   <li>Releasing any other system resources</li>
     * </ul>
     *
     * @throws IOException if an error occurs during resource cleanup
     */
    @Override
    public void close() throws IOException {
        try{
            if(duckdbConnection!=null && !duckdbConnection.isClosed()){
                duckdbConnection.close();
            }
            if(anotherContextMap!=null && !anotherContextMap.isEmpty()){
                for (Object value : anotherContextMap.values()) {
                    if(value instanceof Closeable){
                        ((Closeable) value).close();
                    }
                }
            }
        }catch (Exception e){
            throw new IOException(e);
        }
    }
}

package team.magic.flute.hercules.executor.service;

import team.magic.flute.hercules.common.http.PluginDesc;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;

import java.io.IOException;
import java.util.Map;

/**
 * Executor Process Handle Interface
 *
 * <p>This interface defines the core operations for task execution processing in the Hercules Executor.
 * It provides methods for task handling, resource management, and plugin loading capabilities.
 *
 * <p>The interface abstracts the execution engine functionality, allowing for different implementations
 * while maintaining a consistent API for task processing and resource management.
 *
 * <p>Key responsibilities include:
 * <ul>
 *   <li>Task execution and lifecycle management</li>
 *   <li>Execution slot management and capacity monitoring</li>
 *   <li>Dynamic plugin loading and management</li>
 *   <li>Resource allocation and cleanup</li>
 * </ul>
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public interface ExecutorProcessHandle {

    /**
     * Handle and execute a task.
     *
     * <p>This method processes a task by loading the required plugins, setting up the execution
     * environment, and delegating the actual execution to the appropriate plugin implementation.
     *
     * @param taskInfo the task information containing execution details and context
     * @throws IllegalStateException if the executor is busy and cannot process new tasks
     */
    void handle(HerculesRunnableTaskInfo taskInfo);

    /**
     * Get the total number of execution slots available.
     *
     * <p>Returns the maximum number of tasks that can be executed concurrently by this executor.
     * This value is typically configured based on system resources and performance requirements.
     *
     * @return the total number of execution slots
     */
    long totalSlot();

    /**
     * Get the number of available execution slots.
     *
     * <p>Returns the current number of free execution slots that can accept new tasks.
     * This is calculated as total slots minus currently active tasks.
     *
     * @return the number of available execution slots
     */
    long aliveAbleSlot();

    long queueCapacity();

    boolean isNowBusy();

    /**
     * Load plugins for a specific plugin group.
     *
     * <p>This method downloads and loads plugin JAR files for the specified plugin group,
     * making them available for task execution. The plugins are cached for performance
     * and automatically updated when new versions are available.
     *
     * @param pluginGroup the plugin group identifier to load
     * @throws IOException if plugin loading fails due to network or file system issues
     */
    void loadPlugin(String pluginGroup) throws IOException;

    /**
     * Display the currently loaded plugin information for the executor
     * @return
     */
    Map<String, PluginDesc> showCurrentLoadPlugin();
}

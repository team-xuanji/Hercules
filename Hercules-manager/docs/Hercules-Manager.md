# Hercules Manager Module

## Overview

The Hercules Manager is the central control plane of the Hercules task execution system, providing comprehensive task orchestration, scheduling, and plugin management capabilities. It serves as the primary interface for task submission, monitoring, and system administration.

## Core Functionality

### 1. Task Management

The Hercules Manager provides complete lifecycle management for tasks through the `TaskManagerController`:

#### Features:
- **One-time Task Submission**: Submit individual tasks for immediate execution
- **Task Status Monitoring**: Real-time status checking and progress tracking
- **Task Rerun Capability**: Restart failed or completed tasks
- **Asynchronous Retry Operations**: Advanced retry mechanisms with custom parameters

#### Key Components:
- `HerculesTaskManagerService`: Core service interface for task operations
- `HerculesTaskManagerServiceImpl`: Implementation handling task lifecycle
- `HerculesTaskInfo`: Data model representing task metadata and state

### 2. Scheduled Task Management (Cron Jobs)

Advanced scheduling capabilities through the `CronTaskManager` controller:

#### Features:
- **Cron Job Definition**: Create and update scheduled tasks using standard cron expressions
- **Dynamic Control**: Enable, disable, or delete scheduled jobs at runtime
- **Execution History**: View recent task executions for monitoring and debugging
- **Misfire Handling**: Support for missed execution scenarios

#### Key Components:
- `HerculesCronJobManagerService`: Service interface for cron job management
- `CronTaskDispatch`: Scheduler component that processes cron jobs
- `HerculesCronJobs`: Data model for scheduled job definitions

### 3. Plugin Management

Comprehensive plugin lifecycle management through the `PluginRegisterController`:

#### Features:
- **Plugin Registration**: Upload and register JAR files containing plugin implementations
- **Plugin Discovery**: Search and browse available plugins by group or handle
- **Implementation Inspection**: View detailed plugin implementation information
- **Plugin Removal**: Clean removal of plugin groups and their resources

#### Key Components:
- `HerculesPluginManagerService`: Core plugin management service
- `PluginResourceInfo`: Plugin metadata and resource information
- `FileStorage`: Abstraction layer for plugin file storage (supports OSS)

### 4. System Architecture

#### High Availability Features:
- **Multi-Instance Support**: Distributed deployment with leader election
- **Runner Management**: Automatic instance discovery and heartbeat monitoring
- **Load Distribution**: Bucket-based task distribution across instances

#### Data Management:
- **Task Recovery System**: Multi-tier recovery strategy (HOT/WARM/COLD/DEAD)
- **Metadata Compaction**: Automatic cleanup of stale task information
- **Database Abstraction**: MyBatis-Plus integration with MySQL

## Database Schema

### Core Tables:

1. **HERCULES_TASK_INFO**: Main task information and metadata
2. **hercules_cron_tasks**: Scheduled job definitions and configurations
3. **HERCULES_PLUGIN**: Plugin group information and resources
4. **HERCULES_PLUGIN_IMPL**: Plugin implementation details
5. **HERCULES_RECOVER_TASKS_***: Multi-tier task recovery tables (HOT/WARM/COLD/DEAD)
6. **HERCULES_MANAGER_RUNNER_INSTANCE**: Manager instance registry for HA

## REST API Endpoints

### Task Management (`/taskManager`)
- `POST /submitOnceTask` - Submit a one-time task
- `GET /checkTaskStatus` - Check task status by ID
- `PUT /rerunTask` - Rerun a specific task
- `POST /asyncRetryOneTask` - Asynchronous task retry

### Cron Job Management (`/cronTaskManager`)
- `POST /createOrUpdate` - Create or update cron job
- `PUT /enable` - Enable scheduled job
- `PUT /disable` - Disable scheduled job
- `DELETE /delete` - Delete cron job
- `GET /showTopNCronTask` - View recent executions

### Plugin Management (`/pluginManager`)
- `GET /searchPlugin` - Search plugins by criteria
- `GET /searchAllPlugin` - List all plugins
- `GET /searchPluginImplInfo` - Get implementation details
- `POST /register` - Register new plugins
- `GET /removePlugin` - Remove plugin group

## Configuration

### Application Properties:
- **Database Configuration**: MySQL connection settings
- **File Storage**: OSS integration for plugin storage
- **Scheduling**: Configurable execution intervals and timeouts
- **Recovery Strategy**: Multi-tier recovery timing configuration

### Environment Support:
- Multiple environment profiles (qa, prod, test)
- Configurable logging with structured output
- Docker containerization support

## Key Design Patterns

### 1. Service Layer Architecture
- Clear separation between controllers, services, and data access
- Interface-based design for extensibility
- Dependency injection for loose coupling

### 2. Distributed Processing
- Bucket-based sharding for horizontal scaling
- Leader election for singleton operations
- Heartbeat-based instance management

### 3. Recovery Strategy
- Multi-tier recovery system based on failure age
- Automatic promotion between recovery tiers
- Configurable retry policies and timeouts

## Integration Points

### With Hercules Executor:
- Task submission and status updates
- Plugin resource distribution
- Async retry coordination

### With External Systems:
- OSS for plugin storage
- MySQL for persistent data
- REST APIs for external integration

## Monitoring and Observability

### Built-in Features:
- Structured logging with trace IDs
- Task execution metrics
- Plugin registration events
- System health indicators

### Operational Capabilities:
- Real-time task status monitoring
- Cron job execution history
- Plugin availability tracking
- Instance health monitoring

## Security Considerations

- Input validation on all API endpoints
- File upload security for plugin registration
- Database access control
- Environment-specific configuration isolation

## Scalability Features

- Horizontal scaling through instance clustering
- Bucket-based load distribution
- Asynchronous processing capabilities
- Configurable resource limits and timeouts

The Hercules Manager serves as the cornerstone of the Hercules ecosystem, providing robust, scalable, and reliable task orchestration capabilities for enterprise-grade workloads.

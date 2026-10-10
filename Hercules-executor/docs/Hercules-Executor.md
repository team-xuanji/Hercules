# Hercules Executor Module

## Overview

The Hercules Executor is the execution engine of the Hercules task system, responsible for consuming tasks from the Hercules Manager and executing them using dynamically loaded plugins. It serves as the distributed worker component that provides scalable task processing capabilities.

## Architecture Overview

The Hercules Executor follows a **stateless, HTTP-only architecture** for all task management operations. It communicates exclusively with the Hercules Manager through RESTful APIs, eliminating the need for direct database connections for task lifecycle management.

### Key Architectural Principles:
- **HTTP-Only Communication**: All task operations use RESTful APIs via Feign client
- **Stateless Design**: No persistent state or database dependencies for task management
- **Distributed-Ready**: Designed for horizontal scaling and multi-instance deployments
- **Plugin-Based Execution**: Dynamic plugin loading with isolated execution environments

### HTTP API Operations:
- `tryFetchTasksWithByteArray`: Poll for available tasks (binary payload, encrypted with the configured key; request signed via `X-Hercules-*` HMAC headers, see ADR-0016; plaintext `tryFetchTasks` is QA-only)
- `tryLockBatchTask`: Lock a batch of tasks in one signed request (up to `BATCH_FETCH_MAX_SIZE`, 200)
- `tryLockOneTask`: Lock a single task for exclusive execution (compatibility shim over the batch endpoint; used in cross-partition fetch mode)
- `finishOneTask`: Report successful task completion
- `asyncRerunOneTask`: Request task retry coordination
- `reportExecutorInfo`: Report executor status and capacity

## Core Functionality

### 1. Task Execution Engine

The Hercules Executor provides robust task execution capabilities through the `ExecutorProcessHandle`:

#### Features:
- **Multi-threaded Execution**: Configurable thread pool with slot-based capacity management
- **Task Lifecycle Management**: Complete task status tracking from initialization to completion
- **Plugin-based Processing**: Dynamic loading and execution of business-specific plugins
- **Retry Mechanisms**: Automatic retry with configurable attempts and backoff strategies

#### Key Components:
- `ExecutorProcessHandleImpl`: Core implementation handling task execution logic
- `TaskConsumer`: Scheduled service that polls and dispatches tasks via HTTP APIs
- `HerculesManagerApi`: Feign client for all task lifecycle operations

### 2. Dynamic Plugin Loading

Advanced plugin management system supporting runtime plugin loading and execution:

#### Features:
- **Remote Plugin Loading**: Download and load plugins from HTTP/HTTPS and OSS sources
- **Plugin Caching**: Intelligent caching with automatic version checking and updates
- **Classloader Isolation**: Secure plugin execution with proper classloader management
- **Multi-protocol Support**: Flexible download mechanisms for different storage systems

#### Key Components:
- `DownloaderManager`: Centralized management of protocol-specific downloaders
- `HttpDownloader`/`HttpsDownloader`: HTTP-based plugin resource downloaders
- `FileStorage`: Abstraction layer for plugin storage operations

### 3. Resource Management

Comprehensive resource management and optimization:

#### Features:
- **Execution Slot Management**: Dynamic capacity monitoring and allocation
- **Memory Management**: Configurable memory limits and garbage collection optimization
- **Temporary File Cleanup**: Automatic cleanup of downloaded resources and temporary files
- **HTTP Connection Pooling**: Efficient HTTP connection management for API communication

#### Key Components:
- `RunnerEnv`: Environment configuration and resource limits
- `Constant`: System constants and configuration values

### 4. Integration Layer

Seamless integration with Hercules Manager and external systems:

#### Features:
- **Manager API Integration**: RESTful communication with Hercules Manager
- **Task Status Reporting**: Real-time status updates and progress reporting
- **Chain Task Support**: Support for task chaining and workflow execution
- **Async Retry Coordination**: Coordinated retry mechanisms with the manager

#### Key Components:
- `HerculesManagerApi`: Feign client for manager communication
- `FeignConfig`: HTTP client configuration and error handling
- `ApiGatewayUtil`: API gateway integration utilities

## Architecture Components

### Execution Flow
1. **Task Polling**: TaskConsumer periodically polls for available tasks via HTTP API (`tryFetchTasksWithByteArray`, binary payload encrypted with the configured key). The fetch size adapts to remaining queue capacity, capped at `BATCH_FETCH_MAX_SIZE` (200). When the manager cannot compute this executor's bucket range yet (e.g. right after a restart), the response is flagged `crossPartition` and covers all buckets.
2. **Whitelist Filtering**: Tasks whose `pluginHandle` is not in the executor's whitelist are dropped before locking.
3. **Task Locking**: Tasks are locked via HTTP API — normally in one batch (`tryLockBatchTask`); in cross-partition mode, per task (`tryLockOneTask`) to reduce contention.
4. **Plugin Loading**: Required plugins are downloaded and loaded dynamically
5. **Task Execution**: Tasks are executed in isolated thread pools with proper resource management
6. **Status Reporting**: Execution status and results are reported back via HTTP API (`finishOneTask`)
7. **Resource Cleanup**: Temporary resources are cleaned up after execution

### Plugin System
- **Plugin Discovery**: Automatic discovery of plugin implementations using ServiceLoader
- **Version Management**: Intelligent plugin version checking and cache invalidation
- **Resource Isolation**: Each plugin group maintains isolated resources and dependencies
- **Error Handling**: Comprehensive error handling and recovery mechanisms

### Data Processing Integration
- **DuckDB Support**: Optional DuckDB integration for advanced data processing
- **SQL Execution**: Direct SQL execution capabilities within task context
- **Data Pipeline**: Support for complex ETL and data transformation workflows

## Configuration

### Environment Variables:
- **EXECUTOR_SLOT_SIZE**: Maximum concurrent task execution slots
- **BUSINESS_KEY**: Business group identifier for task routing
- **ENABLE_DUCKDB**: Enable/disable DuckDB integration
- **DUCKDB_MEM_GB_SIZE**: DuckDB memory allocation limit
- **DUCKDB_SPILL_GB_SIZE**: DuckDB disk spill buffer size

### Application Properties:
- **HTTP API Configuration**: Hercules Manager API endpoints and authentication settings
- **File Storage**: OSS/HTTP configuration for plugin downloads
- **Feign Client**: HTTP client configuration with retry policies and timeouts
- **Optional DuckDB**: Database configuration for optional data processing capabilities (not for task management)
- **Logging**: Structured logging with trace ID support

### Deployment Profiles:
- Multiple environment support (qa, prod, test)
- Docker containerization with resource limits
- Kubernetes deployment configurations
- Health check and monitoring endpoints

## Key Design Patterns

### 1. Plugin Architecture
- Service Provider Interface (SPI) pattern for plugin discovery
- Classloader isolation for plugin execution
- Factory pattern for downloader management

### 2. Resource Management
- HTTP connection pooling for API communication
- Caching strategies for plugin resources
- Automatic resource cleanup and lifecycle management

### 3. Distributed Processing
- Slot-based capacity management for horizontal scaling
- Lock-based coordination for multi-instance deployments
- Stateless design for easy scaling and failover

## Integration Points

### With Hercules Manager:
- HTTP-based task consumption and status reporting via RESTful APIs
- Plugin resource discovery and download through HTTP endpoints
- Async retry coordination and chain task execution via API calls

### With Plugin System:
- Dynamic plugin loading and execution
- Plugin lifecycle management
- Resource sharing and isolation

### With External Systems:
- OSS integration for plugin storage
- HTTP/HTTPS resource downloading
- Optional DuckDB integration for data processing (not task metadata)

## Monitoring and Observability

### Built-in Features:
- Structured logging with correlation IDs
- Task execution metrics and timing
- Plugin loading and caching statistics
- Resource utilization monitoring

### Operational Capabilities:
- Real-time capacity monitoring
- Plugin version tracking
- Error rate and retry statistics
- Performance metrics and profiling

## Security and Reliability

### Security Features:
- Plugin classloader isolation
- Signed operations: every task op (fetch/lock/finish/fail/abandon) carries a per-executor HMAC-SHA256 signature in `X-Hercules-*` headers, covering the canonical message (op + subject + timestamp + method + path + query + SHA-256 of body) with a 5-minute freshness window (ADR-0016)
- Secure resource downloading with validation
- Environment-specific configuration isolation
- Input validation and sanitization

### Reliability Features:
- Automatic retry mechanisms with exponential backoff
- Graceful degradation and error recovery
- Resource leak prevention and cleanup
- Health checks and self-monitoring

## Scalability Features

- Horizontal scaling through multiple executor instances
- Configurable execution capacity per instance
- Efficient resource utilization and management
- Support for high-throughput task processing

The Hercules Executor serves as the robust execution engine of the Hercules ecosystem, providing scalable, reliable, and efficient task processing capabilities for enterprise-grade workloads.

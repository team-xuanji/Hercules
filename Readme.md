# Hercules - Distributed Task Orchestration & Data Processing Platform

> **A poor man's distributed scheduler.** No MQ. No ZooKeeper. No etcd. No Redis. No actor framework.
> Just a MySQL, an HTTP client, and a deep reluctance to run more middleware.

![heracles_logo.png](heracles_logo.png)

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7+-green.svg)](https://spring.io/projects/spring-boot)
[![DuckDB](https://img.shields.io/badge/DuckDB-Integrated-yellow.svg)](https://duckdb.org/)

## 📚 Documentation

- **[BUILD.md](BUILD.md)** - Comprehensive build guide and troubleshooting
- **[Configuration Guide](Hercules-twelve-labors/docs/configuration-guide.md)** - Detailed configuration instructions
- **[Environment Variables Guide](Hercules-twelve-labors/docs/environment-variables-guide.md)** - Environment variable configuration
- **[Plugin Security Guide](Hercules-twelve-labors/docs/plugin-security-guide.md)** - Plugin security and white list configuration
- **[Database Initialization](Hercules-twelve-labors/docs/database-initialization.md)** - Database setup and initialization

## Overview

Hercules takes its name from the hero of twelve impossible labors — not because this engine is big, but because a loosely-coupled plugin architecture lets one small system play many roles. Its architecture philosophy, however, is the exact opposite of its name: **if it can be avoided, it is not added.**

The whole system rests on one humble premise — you already have a MySQL. From that premise, we built:

- **Locks out of the database**: a conditional UPDATE *is* a distributed lock; MySQL's atomicity *is* the mutual exclusion.
- **Elections out of heartbeats**: oldest living instance wins, UUIDv7 ordering is good enough.
- **Identity out of self-minting + HMAC**: each executor generates its own identity at boot and registers it via encrypted heartbeat — TOFU under a DB-trusted threat model; every operation is then HMAC-signed per instance.
- **Compute out of embedded DuckDB**: executors carry their own OLAP engine — no external warehouse required, and each executor can enable or disable it per deployment.
- **Scheduling out of slots**: executors advertise their capacity over heartbeat and only pull work while they have a free slot — a deliberately minimal but real resource scheduler, enough to serve as the core of task orchestration.

### Key Features

- **🚀 Distributed Task Execution**: Scalable task distribution across multiple executor nodes
- **🔌 Plugin Architecture**: Extensible plugin system for various business scenarios
- **📊 Integrated Analytics**: Built-in DuckDB for lightweight OLAP operations
- **🌐 Business Access Gateway**: Unified interface for complex business integrations
- **📈 Real-time Monitoring**: Comprehensive task status tracking and monitoring
- **🔄 Flexible Scheduling**: Support for both synchronous and asynchronous execution
- **☁️ Cloud-Native**: Microservices architecture with containerization support

### Use Cases

#### Core Platform Capabilities
- **Complex ETL Pipelines**: Multi-stage data extraction, transformation, and loading
- **Business Data Processing**: Real-time and batch business data operations
- **Workflow Orchestration**: Complex business workflow management
- **Microservices Integration**: Unified task execution across distributed services
- **Plugin-based Processing**: Extensible processing through custom plugins

#### Twelve-Labors Extension Examples
- **Data Export & Analytics**: Multi-format data export with compression support (current example)
- **Report Generation**: Automated business report creation and distribution
- **Data Synchronization**: Cross-system data synchronization workflows
- **Notification Services**: Multi-channel notification and alerting systems
- **File Processing**: Batch file processing and transformation
- **API Integration**: Simplified interfaces for complex API orchestration
- **Business Intelligence**: Custom BI data pipeline management
- **Compliance Reporting**: Automated compliance and audit report generation

> **Note**: The current Twelve-Labors implementation focuses on data export as an example. The module is designed to be extended for any business-specific use case that benefits from simplified interfaces over the core Manager APIs.

## Design Philosophy: Poverty-Driven Architecture

With a healthy budget, this could have grown into the standard "Kafka + ZooKeeper + K8s Operator + Prometheus" shape.
But the reality is: operating a distributed middleware stack often costs more than the problem it solves.

So every "standard approach" got replaced with "whatever was already on hand":

| Standard approach | Budget edition | What it costs |
|---|---|---|
| ZooKeeper / etcd election | MySQL heartbeat + UUIDv7 seniority (master only cleans up; work is bucket-sharded across all live instances) | No strong consensus — fine for idempotent GC, don't run trading on it |
| MQ task dispatch | HTTP polling + conditional-update claim | Polling latency, in exchange for zero middleware |
| K8s Operator | HTTP executors with heartbeats | You own the process lifecycle |
| Prometheus / Grafana | Structured log prefixes (`[PLUGIN_DRIFTED]`, `[INVALID_EXECUTOR_OP]`) | No dashboards; diagnose with grep |
| OAuth / mTLS | Self-minted identity + per-instance HMAC signing | Trust boundary = DB read access (identities are stored there) |

### The price of poverty

A budget edition is not free. We don't hide it:

- **No strong election** — good enough for cleanup jobs, no consensus guarantees.
- **Logs are the only observability** — no metrics dashboard, but every critical event carries a stable prefix you can alert on.
- **Everything rides on one MySQL** — if it's down, the platform is down. HA replication is your call.
- **Thin unit test coverage** — integration-tested against real dependencies; unit tests for the core dispatch paths are being backfilled.

## Architecture Overview

Hercules follows a modular, microservices architecture designed for scalability and maintainability:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Hercules Ecosystem                           │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌──────────────────────────────────────┐ │
│  │ Business Access │    │         External Systems            │ │
│  │    Gateway      │◄──►│  (APIs, Databases, File Systems)   │ │
│  │ (twelve-labors) │    │                                      │ │
│  │   [OPTIONAL]    │    └──────────────────────────────────────┘ │
│  └─────────────────┘                                            │
│           │                                                      │
│           ▼                                                      │
│  ┌─────────────────┐    ┌──────────────────────────────────────┐ │
│  │ Hercules Manager│◄──►│         Plugin Registry             │ │
│  │ (Orchestration) │◄──►│     (Execution Plugins)             │ │
│  │   [CORE]        │    └──────────────────────────────────────┘ │
│  └─────────────────┘                                            │
│           │                                                      │
│           ▼                                                      │
│  ┌─────────────────┐    ┌──────────────────────────────────────┐ │
│  │ Task Scheduler  │◄──►│      Task Distribution Storage       │ │
│  │   & Dispatcher  │    │    (MySQL Tables - Current)         │ │
│  └─────────────────┘    └──────────────────────────────────────┘ │
│           │                                                      │
│           ▼                                                      │
│  ┌─────────────────┐    ┌──────────────────────────────────────┐ │
│  │ Hercules        │    │         Storage Layer               │ │
│  │   Executors     │◄──►│  (Object Storage, File Systems)     │ │
│  │ (HTTP Clients)  │    │                                      │ │
│  └─────────────────┘    └──────────────────────────────────────┘ │
│                                                                 │
│  Core Workflow: User ──────────────────────► Manager           │
│  Optional Workflow: User ──► Twelve-Labors ──► Manager         │
│  Task Distribution: Manager ◄──HTTP APIs──► Executors         │
└─────────────────────────────────────────────────────────────────┘
```

### Core Modules

#### 🎯 **Hercules-Manager**
- **Purpose**: Central orchestration and management hub
- **Responsibilities**:
  - Task definition and lifecycle management
  - Plugin registration and version control
  - Scheduling and task distribution
  - System monitoring and health checks
- **Key Features**:
  - RESTful API for task management
  - Plugin hot-swapping capabilities
  - Distributed task scheduling
  - Real-time status monitoring

#### ⚡ **Hercules-Executor**
- **Purpose**: Lightweight distributed task execution engine with HTTP-only communication
- **Responsibilities**:
  - HTTP-based task fetching and status updates via Feign client
  - Plugin loading and execution with security controls
  - Resource management and optimization
  - RESTful communication with Manager for all operations
- **Key Features**:
  - **Stateless Architecture**: No direct database access, pure HTTP communication
  - **Feign Client Integration**: RESTful task management via `HerculesManagerApi`
  - **HTTP Task Lifecycle**: `tryFetchTasksWithByteArray` → `tryLockOneTask` → `finishOneTask`/`failOneTask`
  - **Multi-threaded Execution**: Configurable task execution slots with thread pool
  - **Dynamic Plugin Loading**: White list security and remote plugin downloading
  - **Region-based Organization**: Better resource management and geographical distribution
  - **DuckDB Integration**: High-performance data processing with automatic connection management

#### 🔌 **Hercules-Executor-Plugin**
- **Purpose**: Extensible plugin framework
- **Responsibilities**:
  - Standardized plugin interface
  - Plugin lifecycle management
  - Custom business logic implementation
- **Key Features**:
  - Hot-pluggable architecture
  - Version compatibility management
  - Resource-aware execution
  - Custom configuration support

#### 🌐 **Hercules-Twelve-Labors (Business Access Gateway) - Optional Extension Module**
- **Purpose**: Extensible business integration and access layer
- **Design Philosophy**:
  - Provide simplified API interfaces for complex business scenarios
  - Extensible and customizable based on actual business requirements
  - Current data export functionality is only a temporary example, not a final limitation
- **Core Responsibilities**:
  - Business logic encapsulation and abstraction
  - Simplification of complex task workflows
  - User-friendly API interface design
  - Business-specific data processing and validation
- **Current Example Features**:
  - Multi-format data export (CSV, JSON, Parquet, XLSX) - temporary example implementation
  - Task submission and status monitoring
  - File download and management
- **Extension Capabilities**:
  - Can add any business-specific functionality
  - Support for custom workflow orchestration
  - Integration with external systems and services
  - Support for complex business rules and validation

> **Important Note**: Twelve-Labors is an optional extension module, and its current data export functionality is only a temporary example implementation. The true value of this module lies in providing simplified access interfaces for various complex business scenarios. Users can extend or replace its functionality based on actual requirements, not limited to file downloads. Core task orchestration functionality is fully implemented through the Manager module.

#### 🛠️ **Hercules-Common**
- **Purpose**: Shared utilities and common functionality
- **Responsibilities**:
  - Common data structures and utilities
  - Shared configuration management
  - Cross-module communication protocols
- **Key Features**:
  - Standardized data models
  - Configuration management
  - Logging and monitoring utilities
  - Security and authentication helpers

#### 📊 **Hercules-Service**
- **Purpose**: Internal data interface service
- **Responsibilities**:
  - Third-party data caching and management
  - Internal service orchestration
  - Data consistency and integrity
- **Key Features**:
  - Data caching and optimization
  - Service discovery and registration
  - Health monitoring and alerting
  - Performance metrics collection

## System Interaction Diagrams

### Core Task Submission Flow (Direct to Manager)
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   User/Client   │    │    Manager      │    │    Executor     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │ 1. Register Plugin    │                       │
         ├──────────────────────►│                       │
         │ 2. Plugin Registered  │                       │
         │◄──────────────────────┤                       │
         │                       │                       │
         │ 3. Submit Task        │                       │
         ├──────────────────────►│                       │
         │ 4. Task Created       │                       │
         │◄──────────────────────┤                       │
         │                       │                       │
         │                       │ 5. Poll for Tasks    │
         │                       │◄──────────────────────┤
         │                       │ 6. Return Task Info  │
         │                       ├──────────────────────►│
         │                       │                       │
         │                       │                       │ 7. Execute Task
         │                       │                       ├─────────────►
         │                       │                       │              │
         │                       │                       │◄─────────────┘
         │                       │ 8. Update Status     │
         │                       │◄──────────────────────┤
         │                       │                       │
         │ 9. Query Status       │                       │
         ├──────────────────────►│                       │
         │ 10. Return Status     │                       │
         │◄──────────────────────┤                       │
```

### Optional Business Access Flow (via Twelve-Labors)
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   User/Client   │    │ Twelve-Labors   │    │    Manager      │    │    Executor     │
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │                       │
         │ 1. Submit Export Task │                       │                       │
         ├──────────────────────►│                       │                       │
         │                       │ 2. Validate Request  │                       │
         │                       ├──────────────────────►│                       │
         │                       │                       │ 3. Create Task Record│
         │                       │                       ├──────────────────────►│
         │                       │                       │                   Database
         │                       │ 4. Return Task ID     │                       │
         │                       │◄──────────────────────┤                       │
         │ 5. Task ID Response   │                       │                       │
         │◄──────────────────────┤                       │                       │
         │                       │                       │                       │
         │                       │                       │ 6. Poll for Tasks    │
         │                       │                       │◄──────────────────────┤
         │                       │                       │ 7. Return Task Info  │
         │                       │                       ├──────────────────────►│
         │                       │                       │                       │
         │                       │                       │ 8. Download Plugin   │
         │                       │                       │◄──────────────────────┤
         │                       │                       │ 9. Plugin Binary     │
         │                       │                       ├──────────────────────►│
         │                       │                       │                       │
         │                       │                       │                       │ 10. Execute Task
         │                       │                       │                       ├─────────────►
         │                       │                       │                       │              │
         │                       │                       │                       │◄─────────────┘
         │                       │                       │ 11. Update Status    │
         │                       │                       │◄──────────────────────┤
         │                       │                       │                   Database
         │                       │                       │                       │
         │ 12. Check Status      │                       │                       │
         ├──────────────────────►│                       │                       │
         │                       │ 13. Query Task Status│                       │
         │                       ├──────────────────────►│                       │
         │                       │ 14. Return Status    │                       │
         │                       │◄──────────────────────┤                       │
         │ 15. Status Response   │                       │                       │
         │◄──────────────────────┤                       │                       │
         │                       │                       │                       │
         │ 16. Download Result   │                       │                       │
         ├──────────────────────►│                       │                       │
         │                       │ 17. Get Download URL │                       │
         │                       ├──────────────────────►│                       │
         │                       │ 18. Return URL       │                       │
         │                       │◄──────────────────────┤                       │
         │ 19. File Download     │                       │                       │
         │◄──────────────────────┤                       │                       │
```

### Plugin Management Flow
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│    Manager      │    │   File Storage  │    │    Executor     │
│                 │    │   (OSS/S3)      │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │ 1. Upload Plugin      │                       │
         ├──────────────────────►│                       │
         │ 2. Store Binary       │                       │
         │◄──────────────────────┤                       │
         │                       │                       │
         │ 3. Register Plugin    │                       │
         ├─────────────────────────────────────────────► Database
         │                       │                       │
         │                       │ 4. Request Plugin    │
         │◄──────────────────────────────────────────────┤
         │ 5. Plugin Info        │                       │
         ├──────────────────────────────────────────────►│
         │                       │                       │
         │ 6. Download Plugin    │                       │
         ├──────────────────────►│                       │
         │ 7. Plugin Binary      │                       │
         │◄──────────────────────┤                       │
         │ 8. Forward Binary     │                       │
         ├──────────────────────────────────────────────►│
         │                       │                       │
         │                       │                       │ 9. Load & Execute
         │                       │                       ├─────────────►
         │                       │                       │              │
         │                       │                       │◄─────────────┘
```

### Task Recovery and Monitoring Flow
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│    Manager      │    │   Database      │    │    Executor     │
│  (Scheduler)    │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │ 1. Scan Failed Tasks  │                       │
         ├──────────────────────►│                       │
         │ 2. Return Failed List │                       │
         │◄──────────────────────┤                       │
         │                       │                       │
         │ 3. Move to Recovery   │                       │
         ├──────────────────────►│                       │
         │    (HOT/WARM/COLD)    │                       │
         │                       │                       │
         │ 4. Schedule Retry     │                       │
         ├──────────────────────►│                       │
         │                       │                       │
         │                       │ 5. Poll Recovery     │
         │                       │◄──────────────────────┤
         │                       │ 6. Return Retry Task │
         │                       ├──────────────────────►│
         │                       │                       │
         │                       │                       │ 7. Retry Execution
         │                       │                       ├─────────────►
         │                       │                       │              │
         │                       │                       │◄─────────────┘
         │                       │ 8. Update Result     │
         │                       │◄──────────────────────┤
         │                       │                       │
         │ 9. Monitor Health     │                       │
         ├──────────────────────►│                       │
         │ 10. System Metrics    │                       │
         │◄──────────────────────┤                       │
```

### Data Flow Architecture
```
┌─────────────────────────────────────────────────────────────────┐
│                        Data Flow Overview                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐         │
│  │    User     │    │   Business  │    │   System    │         │
│  │   Request   │───►│   Logic     │───►│  Execution  │         │
│  └─────────────┘    └─────────────┘    └─────────────┘         │
│         │                   │                   │              │
│         ▼                   ▼                   ▼              │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐         │
│  │ Twelve-     │    │  Manager    │    │  Executor   │         │
│  │ Labors      │◄──►│  Service    │◄──►│  Service    │         │
│  └─────────────┘    └─────────────┘    └─────────────┘         │
│         │                   │                   │              │
│         ▼                   ▼                   ▼              │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                 MySQL Database                          │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐       │   │
│  │  │Task Records │ │Plugin Info  │ │System State │       │   │
│  │  └─────────────┘ └─────────────┘ └─────────────┘       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                │                               │
│                                ▼                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              File Storage (OSS/S3)                     │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐       │   │
│  │  │Plugin Files │ │Export Data  │ │System Logs  │       │   │
│  │  └─────────────┘ └─────────────┘ └─────────────┘       │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Component Interaction Matrix
```
┌─────────────────┬─────────────────┬─────────────────┬─────────────────┐
│   Component     │ Twelve-Labors   │    Manager      │    Executor     │
├─────────────────┼─────────────────┼─────────────────┼─────────────────┤
│ Twelve-Labors   │       -         │   REST API      │       -         │
│                 │                 │   (HTTP/JSON)   │                 │
├─────────────────┼─────────────────┼─────────────────┼─────────────────┤
│ Manager         │   REST API      │       -         │   REST API      │
│                 │   (HTTP/JSON)   │                 │   (HTTP/JSON)   │
├─────────────────┼─────────────────┼─────────────────┼─────────────────┤
│ Executor        │       -         │   REST API      │       -         │
│                 │                 │   (HTTP/JSON)   │                 │
├─────────────────┼─────────────────┼─────────────────┼─────────────────┤
│ Database        │   SQL Queries   │   SQL Queries   │       -         │
│                 │   (MyBatis-Plus)│   (MyBatis-Plus)│   (HTTP Only)   │
├─────────────────┼─────────────────┼─────────────────┼─────────────────┤
│ File Storage    │       -         │   Object API    │   Object API    │
│                 │                 │   (OSS/S3)      │   (OSS/S3)      │
├─────────────────┼─────────────────┼─────────────────┼─────────────────┤
│ DuckDB          │       -         │       -         │   Optional      │
│ (Data Process)  │                 │                 │   (JDBC/SQL)    │
└─────────────────┴─────────────────┴─────────────────┴─────────────────┘
```

todo:
- The current overall process is similar to asynchronous scheduling. If possible, we can add synchronous execution.
- There is currently a lack of monitoring mechanism for plugin updates. For example, after I update a plugin, I don’t know whether the executor has updated the plugin, and I’m not entirely sure about the performance of the project’s libraries. If the library performance is acceptable, I plan to report executor information to the database.
- For OSS, we should abstract a FileIO class to easily support multiple storage systems(S3,WEB-HTTPFS,HDFS....).
- For task distribution, it should also support an abstract class. Task distribution can be implemented using message queues (Redis, RabbitMQ, etc.). Currently, MySQL tables are used as the storage medium for task message forwarding.

## Quick Start Guide

### Prerequisites

- **Java 8+**: Required for running all Hercules services (JDK 8, 11, 17+ all supported)
- **Maven 3.6+**: For building the project from source
- **MySQL 8.0+**: Database for task management and system state
- **Object Storage**: OSS (Alibaba Cloud) or S3 (AWS) for plugin and file storage
- **(Optional) Docker & Docker Compose**: For containerized deployment

> **Note**: The project supports JDK 8+ with upgraded Spring Boot version. Tested and compatible with JDK 8, 11, and 17+.

> **Important**: The current version uses MySQL tables as the storage medium for task message forwarding. This provides reliable task distribution without requiring additional message queue infrastructure. Future versions will support extensible message distribution mediums (Redis, RabbitMQ, etc.).

### Building from Source

> **📖 Detailed Build Guide**: For comprehensive build instructions, troubleshooting, and advanced build options, see [BUILD.md](BUILD.md)

#### 1. Clone the Repository
```bash
git clone https://github.com/team-xuanji/hercules.git
cd hercules
```

#### 2. Build All Modules
```bash
# Build all modules with Maven (skip tests)
mvn clean package -Dmaven.test.skip=true

# Or build with tests (recommended for development)
mvn clean package
```

#### 3. Build Individual Modules
```bash
# Build specific module with dependencies (recommended approach)
# Build Manager module
mvn clean package -Dmaven.test.skip=true -pl Hercules-manager -am

# Build Executor module
mvn clean package -Dmaven.test.skip=true -pl Hercules-executor -am

# Build Twelve-Labors module
mvn clean package -Dmaven.test.skip=true -pl Hercules-twelve-labors -am

# Build Common module only
mvn clean package -Dmaven.test.skip=true -pl Hercules-common
```

**Maven Parameters Explained:**
- `-pl` (--projects): Specifies which module to build
- `-am` (--also-make): Also build required dependency modules
- `-Dmaven.test.skip=true`: Skip test compilation and execution

#### 4. Build Output
After successful build, JAR files will be available in each module's `target/` directory:
- `Hercules-manager/target/hercules-manager-{version}.jar`
- `Hercules-executor/target/hercules-executor-{version}.jar`
- `Hercules-twelve-labors/target/hercules-twelve-labors-{version}.jar`

#### 5. Build Docker Images (Optional)
```bash
# Build Docker images for all services
docker build -t hercules-manager:latest -f Hercules-manager/Dockerfile .
docker build -t hercules-executor:latest -f Hercules-executor/Dockerfile .
docker build -t hercules-twelve-labors:latest -f Hercules-twelve-labors/Dockerfile .
```

### Build Requirements

- **Memory**: At least 2GB RAM for Maven build process
- **Disk Space**: At least 1GB free space for build artifacts
- **Network**: Internet connection required for downloading Maven dependencies

### Build Troubleshooting

#### Common Build Issues

1. **Maven Dependencies Not Found**
   ```bash
   # Clear Maven cache and rebuild
   mvn dependency:purge-local-repository
   mvn clean compile package -DskipTests
   ```

2. **Java Version Mismatch**
   ```bash
   # Check Java version
   java -version
   javac -version

   # Ensure JAVA_HOME is set correctly
   echo $JAVA_HOME
   ```

3. **Memory Issues During Build**
   ```bash
   # Increase Maven memory
   export MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=512m"
   mvn clean package -DskipTests
   ```

4. **Module Dependency Issues**
   ```bash
   # Build modules with dependencies (recommended)
   mvn clean package -Dmaven.test.skip=true -pl Hercules-manager -am
   mvn clean package -Dmaven.test.skip=true -pl Hercules-executor -am
   mvn clean package -Dmaven.test.skip=true -pl Hercules-twelve-labors -am

   # Or install common module first, then build others
   mvn clean install -Dmaven.test.skip=true -pl Hercules-common
   ```

#### Verify Build Success
```bash
# Check if JAR files are created
ls -la */target/*.jar

# Verify JAR file integrity
java -jar Hercules-manager/target/hercules-manager-*.jar --version
```

### Database Configuration

#### 1. Create MySQL Database
```sql
CREATE DATABASE hercules CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'hercules'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON hercules.* TO 'hercules'@'%';
FLUSH PRIVILEGES;
```

#### 2. Initialize Database Tables
Execute the initialization SQL scripts to create required tables:

```bash
# Execute Hercules-Manager initialization script (creates core system tables)
mysql -u hercules -p hercules < Hercules-manager/sql/init.sql

# Execute Hercules-Twelve-Labors initialization script (creates business access gateway tables)
mysql -u hercules -p hercules < Hercules-twelve-labors/sql/init.sql
```

**Hercules-Manager Tables Created (9 tables):**
- `hercules_cron_tasks` - Scheduled tasks management (uses EXECUTOR_REGION)
- `HERCULES_MANAGER_RUNNER_INSTANCE` - Manager instance tracking
- `HERCULES_PLUGIN_IMPL` - Plugin implementation registry
- `HERCULES_PLUGIN` - Plugin information and resources
- `HERCULES_TASK_INFO` - Task execution information (uses EXECUTOR_REGION)
- `HERCULES_RECOVER_TASKS_*` - Task recovery tables (HOT/WARM/COLD/DEAD, use EXECUTOR_REGION)
- `HERCULES_EXECUTOR_INFO` - Executor information and capabilities

**Hercules-Twelve-Labors Tables Created (2 tables):**
- `HERCULES_CMS_DATA_DOWNLOAD_TASK_DEF` - Data export task definitions (includes EXECUTOR_REGION field)
- `HERCULES_CMS_DATA_DOWNLOAD_TASK` - Data export task execution records

**Sample Data Included:**
- 3 task definitions with EXECUTOR_REGION='PROD'
- 3 task execution records with different statuses (SUCCESS, RUNNING, INIT)

> **Note**: The initialization scripts include sample data for testing. You can modify or remove the sample data as needed for your environment.

#### 2. Configure Each Service

Each Hercules service requires its own configuration file. Create `application-prod.yml` (or `application-dev.yml`) for each service:

##### Hercules-Manager Configuration
```yaml
# Hercules-manager/src/main/resources/application-prod.yml
spring:
  datasource:
    url: "jdbc:mysql://localhost:3306/hercules?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8&autoReconnect=true&allowMultiQueries=true&nullCatalogMeansCurrent=true"
    username: "hercules"
    password: "your_password"

# File storage for plugin management (OSS or S3)
file-storage:
  oss:
    endpoint: "oss-cn-beijing.aliyuncs.com"  # or your S3 endpoint
    bucket: "hercules-plugins"
    access: "your_access_key"
    secret: "your_secret_key"
    root-path: "hercules/prod/plugin/"
    support-http-download: false

# MyBatis-Plus configuration
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

##### Hercules-Executor Configuration
```yaml
# Hercules-executor/src/main/resources/application-prod.yml
# Note: Executor uses HTTP-only communication, no database configuration needed
# Only API and file storage configurations are required

# API configuration for communication with Hercules-Manager (REQUIRED)
api:
  app-open-api:
    host: http://localhost:8080  # Hercules-Manager URL
    name: hercules-manager
    version: v1
    caller: hercules-executor
    sign: your_api_signature
    headers:
      whiteListAk: your_whitelist_key

# Feign client retry configuration
feign:
  retry:
    period: 100
    max-period: 1000
    max-attempts: 3

# File storage for plugin downloads (OSS or S3)
file-storage:
  oss:
    endpoint: "oss-cn-beijing.aliyuncs.com"  # or your S3 endpoint
    bucket: "hercules-plugins"
    access: "your_access_key"
    secret: "your_secret_key"
    root-path: "hercules/prod/plugin/"
    support-http-download: false
```

##### Hercules-Twelve-Labors Configuration
```yaml
# Hercules-twelve-labors/src/main/resources/application-prod.yml
spring:
  datasource:
    url: "jdbc:mysql://localhost:3306/hercules?useUnicode=true&characterEncoding=utf8&autoReconnect=true&allowMultiQueries=true&nullCatalogMeansCurrent=true"
    username: "hercules"
    password: "your_password"

# API configuration for communication with Hercules-Manager
api:
  app-open-api:
    host: http://localhost:8080  # Hercules-Manager URL
    name: hercules-manager
    version: v1
    caller: hercules-twelve-labors
    sign: your_api_signature
    headers:
      whiteListAk: your_whitelist_key
```

### Basic Usage Workflow

#### Step 1: Start Services
```bash
# 1. Start MySQL database
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=hercules \
  -e MYSQL_USER=hercules \
  -e MYSQL_PASSWORD=your_password \
  -p 3306:3306 mysql:8.0

# 2. Wait for MySQL to be ready, then initialize database
sleep 30
mysql -h localhost -u hercules -p hercules < Hercules-manager/sql/init.sql
mysql -h localhost -u hercules -p hercules < Hercules-twelve-labors/sql/init.sql

# 3. Start Hercules Manager (must start first)
java -jar hercules-manager.jar

# 4. Start Hercules Executor (depends on Manager)
java -jar hercules-executor.jar

# 5. Start Hercules Twelve Labors (Business Access Gateway)
java -jar hercules-twelve-labors.jar
```

> **Important**: Always start services in this order:
> 1. MySQL Database + Initialize tables
> 2. Hercules-Manager (core orchestration service)
> 3. Hercules-Executor (task execution service)
> 4. Hercules-Twelve-Labors (business access gateway)

### Basic Usage Workflow

#### Core Workflow (Direct Manager Access)
The primary way to use Hercules is through direct interaction with the Manager module:

> **Architecture Note**: Hercules-Executor uses HTTP-only communication with the Manager. The executor:
> - Fetches tasks via `GET /taskDispatch/tryFetchTasksWithByteArray` (binary payload, encrypted with `hercules.security.http-encrypt-key`; plaintext `tryFetchTasks` is `qa`-profile only)
> - Locks tasks via `PUT /taskDispatch/tryLockOneTask`
> - Reports completion via `PUT /taskDispatch/finishOneTask`
> - Reports failures via `PUT /taskDispatch/failOneTask`
> - Signs every operation (including fetch) with a per-executor HMAC (`passSign`)
> - No direct database access - all operations through RESTful APIs

1. **Register Execution Plugins**
```bash
# Register a data export plugin
curl -X POST http://localhost:8080/api/plugins \
  -H "Content-Type: application/json" \
  -d '{
    "pluginName": "data-exporter",
    "pluginGroup": "data-processing",
    "version": "1.0.0",
    "description": "Multi-format data export plugin"
  }'
```

2. **Define Scheduled Tasks** (Optional)
```bash
# Create a task definition for scheduled execution
curl -X POST http://localhost:8080/api/task-definitions \
  -H "Content-Type: application/json" \
  -d '{
    "businessKey": "daily-report-export",
    "taskName": "Daily Sales Report Export",
    "pluginGroup": "data-processing",
    "pluginName": "data-exporter",
    "sqlTemplate": "SELECT * FROM sales WHERE date = ${#export_date}",
    "exportFormat": "xlsx",
    "schedule": "0 0 8 * * ?"
  }'
```

3. **Submit One-time Tasks**
```bash
# Submit a task for immediate execution
curl -X POST http://localhost:8080/api/tasks/submit \
  -H "Content-Type: application/json" \
  -d '{
    "businessKey": "ad-hoc-export",
    "parameters": {
      "export_date": "2024-12-19",
      "format": "csv"
    }
  }'
```

#### Optional Business Access Workflow (via Twelve-Labors)
For simplified business operations, you can optionally use the Twelve-Labors module. The current implementation provides data export functionality as an example:

```bash
# Current example: Data export through business access gateway
curl -X POST http://localhost:8081/api/data-export/submit \
  -H "Content-Type: application/json" \
  -d '{
    "businessKey": "sales-data-export",
    "exportFormat": "xlsx",
    "parameters": {
      "start_date": "2024-12-01",
      "end_date": "2024-12-19"
    }
  }'
```

**Future Extension Examples:**
```bash
# Example: Report generation service
curl -X POST http://localhost:8081/api/reports/generate \
  -H "Content-Type: application/json" \
  -d '{
    "reportType": "monthly-sales",
    "parameters": {
      "month": "2024-12",
      "department": "sales"
    }
  }'

# Example: Data synchronization service
curl -X POST http://localhost:8081/api/sync/execute \
  -H "Content-Type: application/json" \
  -d '{
    "sourceSystem": "crm",
    "targetSystem": "warehouse",
    "syncType": "incremental"
  }'
```

> **Note**: Twelve-Labors is an extensible module that currently demonstrates data export functionality. You can extend it to provide simplified interfaces for any complex business workflow. The module serves as a template for creating business-specific API layers over the core Hercules platform.

## Advanced Usage & Configuration

### Custom Plugin Development
```java
@Component
public class CustomDataProcessor implements ExecutionPlugin {

    @Override
    public ExecutionResult execute(TaskContext context) {
        // Your custom business logic here
        return ExecutionResult.success("Processing completed");
    }

    @Override
    public String getPluginName() {
        return "custom-data-processor";
    }
}
```

### Business Integration Layer
The Hercules-Twelve-Labors module provides a business access gateway that simplifies integration:

```java
@RestController
@RequestMapping("/api/business")
public class BusinessController {

    @PostMapping("/process-data")
    public ResponseEntity<TaskResult> processBusinessData(
            @RequestBody BusinessDataRequest request) {
        // Business-specific processing logic
        return taskSubmissionService.submitAndWait(request);
    }
}
```

## Configuration Guide

### Environment Variables

Environment variables are only used by the Hercules-Executor module for runtime configuration. The Executor communicates with Manager via HTTP APIs only - no direct database access. API and file storage configurations are specified in `application-{profile}.yml` files.

#### Hercules-Executor Environment Variables
```bash
# Executor identification and capacity
EXECUTOR_REGION=PROD                      # Executor region identifier (default: TEST)
EXECUTOR_REGION_DESC="Production Executor" # Executor region description (default: "I am Iron Man. (｀∀´)Ψ *snap!*")
EXECUTOR_SLOT_SIZE=4                      # Number of concurrent task slots (default: 2)

# Plugin security configuration
PLUGIN_WHITE_LIST=data-export,report-gen,etl-processor  # Comma-separated list of allowed plugin handles (default: empty - all plugins allowed)

# DuckDB configuration for data processing
ENABLE_DUCKDB=true                        # Enable DuckDB integration (default: true)
DUCKDB_MEM_GB_SIZE=2                      # DuckDB memory size in GB (default: 1)
DUCKDB_SPILL_GB_SIZE=500                  # DuckDB spill size in GB (default: 200)
THREAD_COUNT=4                            # DuckDB thread count (default: 1)

# DuckDB storage paths
DUCKDB_STORAGE_PATH=/opt/hercules/duckdb/data.db           # DuckDB database file path
DUCKDB_SPILL_PATH=/opt/hercules/duckdb/spill/              # DuckDB spill directory
```

> **Important**: Database connections, API configurations, and file storage settings are configured in `application-{profile}.yml` files for each service, not through environment variables.

### Application Configuration Files

Database connections, API configurations, and file storage settings are configured in `application-{profile}.yml` files for each service:

#### Hercules-Manager Configuration
```yaml
# application-prod.yml
spring:
  datasource:
    url: "jdbc:mysql://localhost:3306/hercules?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8"
    username: "hercules"
    password: "your_password"

file-storage:
  oss:
    endpoint: "oss-cn-beijing.aliyuncs.com"
    bucket: "hercules-plugins"
    access: "your_access_key"
    secret: "your_secret_key"
    root-path: "hercules/prod/plugin/"
```

#### Hercules-Executor Configuration
```yaml
# application-prod.yml
# Note: Executor uses HTTP-only communication, no database configuration needed

# API configuration for communication with Hercules-Manager (REQUIRED)
api:
  app-open-api:
    host: http://localhost:8080  # Hercules-Manager URL
    name: hercules-manager
    version: v1
    caller: hercules-executor
    sign: your_api_signature
    headers:
      whiteListAk: your_whitelist_key

# Feign client configuration
feign:
  retry:
    period: 100
    max-period: 1000
    max-attempts: 3

# File storage for plugin downloads
file-storage:
  oss:
    endpoint: "oss-cn-beijing.aliyuncs.com"
    bucket: "hercules-plugins"
    access: "your_access_key"
    secret: "your_secret_key"
    root-path: "hercules/prod/plugin/"
```

#### Hercules-Twelve-Labors Configuration
```yaml
# application-prod.yml
spring:
  datasource:
    url: "jdbc:mysql://localhost:3306/hercules?useUnicode=true&characterEncoding=utf8"
    username: "hercules"
    password: "your_password"

api:
  app-open-api:
    host: http://localhost:8080
    name: hercules-manager
    version: v1
    caller: hercules-twelve-labors
    sign: your_api_signature
```

> **Note**: File storage for data exports is configured in the Manager module. Twelve-Labors communicates with Manager through API calls and does not directly access file storage.

## Roadmap & Future Enhancements

### Current Development Focus
- ✅ **Database-based Task Distribution**: Currently implemented using MySQL tables for reliable task forwarding
- ✅ **Asynchronous Task Execution**: Fully implemented and optimized
- 🔄 **Synchronous Execution Support**: In development for real-time scenarios
- 🔄 **Enhanced Plugin Monitoring**: Real-time plugin update tracking
- 🔄 **Multi-Storage Support**: Abstract FileIO layer for S3, HDFS, WebHDFS
- 🔄 **Extensible Message Distribution**: Abstract task distribution layer supporting Redis, RabbitMQ, Kafka

### Planned Features
- **🎯 Advanced Scheduling**: Cron-based and event-driven scheduling
- **📊 Enhanced Analytics**: Built-in dashboards and reporting
- **🔐 Security Enhancements**: OAuth2, RBAC, and audit logging
- **🌍 Multi-Region Support**: Cross-region task distribution
- **🤖 AI/ML Integration**: Machine learning pipeline support
- **📱 Mobile Dashboard**: Mobile-friendly monitoring interface

## Contributing

We welcome contributions from the community! Please see our [Contributing Guide](CONTRIBUTING.md) for details on how to get started.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Support & Community

- **📖 Documentation**: [https://github.com/team-xuanji/Hercules](https://github.com/team-xuanji/Hercules)
- **💬 Community Forum**: [https://github.com/team-xuanji/Hercules](https://github.com/team-xuanji/Hercules)
- **🐛 Issue Tracker**: [GitHub Issues](https://github.com/team-xuanji/hercules/issues)
- **📧 Email Support**: plashspeed@foxmail.com

---

**Built with ❤️ by the Hercules Team**
# Hercules Configuration Guide

## Overview

This guide provides detailed configuration instructions for all Hercules services based on the actual configuration structure. Each service requires its own `application-{profile}.yml` file.

**Important**: Hercules-Twelve-Labors is an optional module. The core Hercules functionality works with just Manager and Executor modules. Twelve-Labors provides additional business-specific interfaces for simplified operations.

## Database Initialization

Before configuring the services, you must initialize the database with the required tables.

### 1. Create Database
```sql
CREATE DATABASE hercules CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'hercules'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON hercules.* TO 'hercules'@'%';
FLUSH PRIVILEGES;
```

### 2. Execute Initialization Scripts
```bash
# Navigate to the project root directory
cd /path/to/hercules

# Execute Hercules-Manager initialization script
mysql -u hercules -p hercules < Hercules-manager/sql/init.sql

# Execute Hercules-Twelve-Labors initialization script
mysql -u hercules -p hercules < Hercules-twelve-labors/sql/init.sql
```

### 3. Verify Table Creation
```sql
USE hercules;
SHOW TABLES;

-- You should see the following tables:
-- hercules_cron_tasks
-- HERCULES_MANAGER_RUNNER_INSTANCE
-- HERCULES_PLUGIN_IMPL
-- HERCULES_PLUGIN
-- HERCULES_TASK_INFO
-- HERCULES_RECOVER_TASKS_HOT
-- HERCULES_RECOVER_TASKS_WARM
-- HERCULES_RECOVER_TASKS_COLD
-- HERCULES_RECOVER_TASKS_DEAD
-- HERCULES_CMS_DATA_DOWNLOAD_TASK_DEF
-- HERCULES_CMS_DATA_DOWNLOAD_TASK
```

### 4. Sample Data
The initialization scripts include sample data for testing:
- **Task Definitions**: 3 sample export task definitions (CSV, Parquet, JSON)
- **Task Records**: 3 sample task execution records with different statuses

You can modify or remove this sample data based on your requirements.

## Service Configuration Files

### 1. Hercules-Manager Configuration

**File**: `Hercules-manager/src/main/resources/application-prod.yml`

```yaml
spring:
  datasource:
    url: "jdbc:mysql://localhost:3306/hercules?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8&autoReconnect=true&allowMultiQueries=true&nullCatalogMeansCurrent=true"
    username: "hercules"
    password: "your_password"

# File storage configuration for plugin management
file-storage:
  oss:
    endpoint: "oss-cn-beijing.aliyuncs.com"
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

**Alternative S3 Configuration:**
```yaml
file-storage:
  s3:
    endpoint: "s3.amazonaws.com"
    region: "us-east-1"
    bucket: "hercules-plugins"
    access-key: "your_access_key"
    secret-key: "your_secret_key"
    root-path: "hercules/prod/plugin/"
    support-http-download: false
```

### 2. Hercules-Executor Configuration

**File**: `Hercules-executor/src/main/resources/application-prod.yml`

```yaml
spring:
  datasource:
    url: "jdbc:mysql://localhost:3306/hercules?useUnicode=true&characterEncoding=utf8&autoReconnect=true&allowMultiQueries=true&nullCatalogMeansCurrent=true"
    username: "hercules"
    password: "your_password"

# File storage configuration (same as Manager for plugin downloads)
file-storage:
  oss:
    endpoint: "oss-cn-beijing.aliyuncs.com"
    bucket: "hercules-plugins"
    access: "your_access_key"
    secret: "your_secret_key"
    root-path: "hercules/prod/plugin/"
    support-http-download: false

# API configuration for Manager communication
api:
  app-open-api:
    host: http://localhost:8080  # Hercules-Manager URL
    name: hercules-manager
    version: v1
    caller: hercules-executor
    sign: your_api_signature
    headers:
      whiteListAk: your_whitelist_key
```

### 3. Hercules-Twelve-Labors Configuration

**File**: `Hercules-twelve-labors/src/main/resources/application-prod.yml`

```yaml
spring:
  datasource:
    url: "jdbc:mysql://localhost:3306/hercules?useUnicode=true&characterEncoding=utf8&autoReconnect=true&allowMultiQueries=true&nullCatalogMeansCurrent=true"
    username: "hercules"
    password: "your_password"

# API configuration for Manager communication
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

## Environment Variables

Environment variables are **only used by the Hercules-Executor module** for runtime configuration. All other configurations (database, file storage, API settings) are specified in `application-{profile}.yml` files.

### Hercules-Executor Environment Variables

Based on `team.magic.flute.hercules.executor.config.RunnerEnv`:

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

### Application Configuration Files

Database connections, API configurations, and file storage settings are configured in `application-{profile}.yml` files for each service:

## Task Distribution Mechanism

**Important**: The current version uses MySQL database tables for task distribution, not external message queues. The task distribution works as follows:

1. **Manager** creates task records in database tables
2. **Executor** polls database tables for pending tasks
3. **Twelve-Labors** communicates with Manager via API calls
4. All task state changes are persisted in database tables

This approach provides:
- **Reliability**: ACID properties ensure task consistency
- **Simplicity**: No additional message queue infrastructure needed
- **Monitoring**: Easy to monitor tasks using standard database tools
- **Backup**: Task state included in database backups

## Service Communication

### Core Architecture
```
User ──API──► Hercules-Manager ──Database──► Hercules-Executor
                     │                              │
                     ▼                              ▼
                 MySQL Database ◄─────────────────────
                 (Task Distribution)
```

### Optional Business Layer
```
User ──API──► Hercules-Twelve-Labors ──API──► Hercules-Manager ──Database──► Hercules-Executor
                                                     │                              │
                                                     ▼                              ▼
                                                 MySQL Database ◄─────────────────────
                                                 (Task Distribution)
```

### Communication Flow
1. **Core Flow**: **User** → **Manager** → **Database** → **Executor**
2. **Optional Flow**: **User** → **Twelve-Labors** → **Manager** → **Database** → **Executor**
3. **Manager** → **Database**: Task records creation and management
4. **Executor** → **Database**: Task polling and status updates
5. **Executor** → **Manager**: Plugin downloads and status reporting

## Docker Deployment

### Preparation
Before starting the Docker containers, prepare the initialization scripts:

```bash
# Create a directory for SQL scripts
mkdir -p ./docker/sql

# Copy initialization scripts
cp Hercules-manager/sql/init.sql ./docker/sql/manager-init.sql
cp Hercules-twelve-labors/sql/init.sql ./docker/sql/twelve-labors-init.sql

# Create a combined initialization script
cat ./docker/sql/manager-init.sql ./docker/sql/twelve-labors-init.sql > ./docker/sql/init-all.sql
```

### Docker Compose Example
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: hercules
      MYSQL_USER: hercules
      MYSQL_PASSWORD: your_password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./docker/sql/init-all.sql:/docker-entrypoint-initdb.d/init.sql:ro
    command: --default-authentication-plugin=mysql_native_password

  hercules-manager:
    image: hercules-manager:latest
    environment:
      SPRING_PROFILES_ACTIVE: prod
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    volumes:
      - ./config/manager-application-prod.yml:/app/config/application-prod.yml:ro

  hercules-executor:
    image: hercules-executor:latest
    environment:
      SPRING_PROFILES_ACTIVE: prod
      # Executor-specific environment variables
      EXECUTOR_REGION: PROD
      EXECUTOR_REGION_DESC: "Production Executor"
      EXECUTOR_SLOT_SIZE: 4
      PLUGIN_WHITE_LIST: "data-export,report-gen,etl-processor"
      ENABLE_DUCKDB: true
      DUCKDB_MEM_GB_SIZE: 2
      DUCKDB_SPILL_GB_SIZE: 500
      THREAD_COUNT: 4
      DUCKDB_STORAGE_PATH: /opt/hercules/duckdb/data.db
      DUCKDB_SPILL_PATH: /opt/hercules/duckdb/spill/
    depends_on:
      - hercules-manager
    volumes:
      - ./config/executor-application-prod.yml:/app/config/application-prod.yml:ro
      - hercules-duckdb-data:/opt/hercules/duckdb

  hercules-twelve-labors:
    image: hercules-twelve-labors:latest
    environment:
      SPRING_PROFILES_ACTIVE: prod
    ports:
      - "8081:8081"
    depends_on:
      - hercules-manager
    volumes:
      - ./config/twelve-labors-application-prod.yml:/app/config/application-prod.yml:ro

volumes:
  mysql_data:
  hercules-duckdb-data:
```

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Check MySQL server is running
   - Verify connection string parameters
   - Ensure database and user exist

2. **File Storage Access Denied**
   - Verify OSS/S3 credentials
   - Check bucket permissions
   - Ensure endpoint is correct

3. **Service Communication Failed**
   - Check API configuration
   - Verify Manager service is accessible
   - Check network connectivity

4. **Plugin Download Failed**
   - Ensure Executor has same file storage config as Manager
   - Check plugin file exists in storage
   - Verify file permissions

### Monitoring

Enable actuator endpoints for monitoring:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

This configuration guide reflects the actual implementation and provides accurate setup instructions for the Hercules system.

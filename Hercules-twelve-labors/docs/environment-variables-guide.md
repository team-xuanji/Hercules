# Hercules Environment Variables Configuration Guide

## Overview

This guide clarifies the correct usage of environment variables in the Hercules system. **Environment variables are only used by the Hercules-Executor module**. All other configurations (database connections, API settings, file storage) are specified in `application-{profile}.yml` files.

## Important Clarification

### What Uses Environment Variables
- ✅ **Hercules-Executor**: Uses environment variables for runtime configuration
- ❌ **Hercules-Manager**: Uses `application-{profile}.yml` files only
- ❌ **Hercules-Twelve-Labors**: Uses `application-{profile}.yml` files only

### What Goes in application-{profile}.yml
- Database connection settings
- File storage configuration (OSS/S3)
- API communication settings
- MyBatis-Plus configuration
- Spring Boot configuration

## Hercules-Executor Environment Variables

Based on `team.magic.flute.hercules.executor.config.RunnerEnv` class:

### Executor Identity and Capacity

| Environment Variable | Default Value | Description |
|---------------------|---------------|-------------|
| `EXECUTOR_REGION` | `TEST` | Executor region identifier (e.g., PROD, TEST, DEV) |
| `EXECUTOR_REGION_DESC` | `"I am Iron Man. (｀∀´)Ψ *snap!*"` | Human-readable executor region description |
| `EXECUTOR_SLOT_SIZE` | `2` | Number of concurrent task execution slots |

### Plugin Security Configuration

| Environment Variable | Default Value | Description |
|---------------------|---------------|-------------|
| `PLUGIN_WHITE_LIST` | `""` (empty) | Comma-separated list of allowed plugin handles. Empty means all plugins are allowed |

### DuckDB Configuration

| Environment Variable | Default Value | Description |
|---------------------|---------------|-------------|
| `ENABLE_DUCKDB` | `true` | Enable/disable DuckDB integration |
| `DUCKDB_MEM_GB_SIZE` | `1` | DuckDB memory allocation in GB |
| `DUCKDB_SPILL_GB_SIZE` | `200` | DuckDB spill storage size in GB |
| `THREAD_COUNT` | `1` | Number of threads for DuckDB operations |

### DuckDB Storage Paths

| Environment Variable | Default Value | Description |
|---------------------|---------------|-------------|
| `DUCKDB_STORAGE_PATH` | `/tmp/duckdb/data.db` | Path to DuckDB database file |
| `DUCKDB_SPILL_PATH` | `/tmp/duckdb/spill/` | Directory for DuckDB spill files |

## Configuration Examples

### Development Environment
```bash
# Executor environment variables for development
export EXECUTOR_REGION=DEV
export EXECUTOR_REGION_DESC="Development Executor"
export EXECUTOR_SLOT_SIZE=2
export PLUGIN_WHITE_LIST=""  # Allow all plugins in development
export ENABLE_DUCKDB=true
export DUCKDB_MEM_GB_SIZE=1
export DUCKDB_SPILL_GB_SIZE=100
export THREAD_COUNT=2
export DUCKDB_STORAGE_PATH=/opt/hercules/dev/duckdb/data.db
export DUCKDB_SPILL_PATH=/opt/hercules/dev/duckdb/spill/
```

### Production Environment
```bash
# Executor environment variables for production
export EXECUTOR_REGION=PROD
export EXECUTOR_REGION_DESC="Production Executor - Node 1"
export EXECUTOR_SLOT_SIZE=8
export PLUGIN_WHITE_LIST="data-export,report-generator,etl-processor,notification-sender"
export ENABLE_DUCKDB=true
export DUCKDB_MEM_GB_SIZE=4
export DUCKDB_SPILL_GB_SIZE=1000
export THREAD_COUNT=8
export DUCKDB_STORAGE_PATH=/opt/hercules/prod/duckdb/data.db
export DUCKDB_SPILL_PATH=/opt/hercules/prod/duckdb/spill/
```

### Docker Environment
```yaml
# docker-compose.yml
services:
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
    volumes:
      - hercules-duckdb-data:/opt/hercules/duckdb
      - ./config/executor-application-prod.yml:/app/config/application-prod.yml:ro
```

## Application Configuration Files

### Hercules-Manager (application-prod.yml)
```yaml
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

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### Hercules-Executor (application-prod.yml)
```yaml
spring:
  datasource:
    url: "jdbc:mysql://localhost:3306/hercules?useUnicode=true&characterEncoding=utf8"
    username: "hercules"
    password: "your_password"

file-storage:
  oss:
    endpoint: "oss-cn-beijing.aliyuncs.com"
    bucket: "hercules-plugins"
    access: "your_access_key"
    secret: "your_secret_key"
    root-path: "hercules/prod/plugin/"

api:
  app-open-api:
    host: http://localhost:8080
    name: hercules-manager
    version: v1
    caller: hercules-executor
    sign: your_api_signature
    headers:
      whiteListAk: your_whitelist_key

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
```

### Hercules-Twelve-Labors (application-prod.yml)
```yaml
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
    headers:
      whiteListAk: your_whitelist_key

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
```

## Best Practices

### Environment Variable Management
1. **Use Environment-Specific Values**: Set different values for DEV, TEST, PROD environments
2. **Resource Allocation**: Adjust `EXECUTOR_SLOT_SIZE` and DuckDB memory based on available resources
3. **Storage Paths**: Use absolute paths and ensure directories exist
4. **Monitoring**: Use descriptive `EXECUTOR_REGION_DESC` values for easier identification
5. **Plugin Security**: Configure `PLUGIN_WHITE_LIST` to restrict plugin execution in production environments

### Plugin White List Configuration
The `PLUGIN_WHITE_LIST` environment variable provides security control over which plugins can be executed by the executor:

- **Empty Value**: All plugins are allowed (suitable for development)
- **Comma-Separated List**: Only specified plugin handles are allowed
- **Example**: `"data-export,report-generator,etl-processor"`
- **Security**: Prevents execution of unauthorized or potentially harmful plugins
- **Flexibility**: Can be configured per executor region for different security requirements

### Application Configuration Management
1. **Profile-Specific Files**: Use separate `application-{profile}.yml` files for different environments
2. **Secret Management**: Use external secret management systems for sensitive data
3. **Configuration Validation**: Validate configurations during startup
4. **Documentation**: Document all configuration changes

### Docker Deployment
1. **Volume Mapping**: Map configuration files as read-only volumes
2. **Persistent Storage**: Use named volumes for DuckDB data
3. **Environment Separation**: Use different environment variable values per container
4. **Health Checks**: Implement health checks to verify configuration

## Troubleshooting

### Common Issues

1. **Executor Not Starting**
   - Check if DuckDB storage paths exist and are writable
   - Verify memory allocation settings are reasonable
   - Ensure `EXECUTOR_SLOT_SIZE` is appropriate for available resources
   - Validate `EXECUTOR_REGION` is set correctly for the environment

2. **Plugin Execution Denied**
   - Check if plugin handle is included in `PLUGIN_WHITE_LIST`
   - Verify plugin handle spelling and case sensitivity
   - Ensure `PLUGIN_WHITE_LIST` is properly formatted (comma-separated, no spaces around commas)

2. **Database Connection Failed**
   - Verify database configuration in `application-{profile}.yml`
   - Check if database server is accessible
   - Validate credentials and connection string

3. **File Storage Access Denied**
   - Verify OSS/S3 credentials in `application-{profile}.yml`
   - Check bucket permissions and endpoint configuration
   - Ensure network connectivity to storage service

4. **API Communication Failed**
   - Verify API configuration in `application-{profile}.yml`
   - Check if Manager service is accessible
   - Validate API signatures and whitelist keys

### Debugging Tips

1. **Enable Debug Logging**: Set appropriate log levels in `application-{profile}.yml`
2. **Check Environment Variables**: Use `printenv` to verify environment variable values
3. **Validate Configuration**: Use Spring Boot Actuator endpoints to check configuration
4. **Monitor Resources**: Check CPU, memory, and disk usage for DuckDB operations

## Migration from Previous Configuration

If you are upgrading from a previous version with different environment variable names:

### Environment Variable Name Changes
- `BUSINESS_KEY` → `EXECUTOR_REGION`
- `BUSINESS_DESC` → `EXECUTOR_REGION_DESC`
- New: `PLUGIN_WHITE_LIST` (for plugin security)

### Migration Steps
1. **Update Environment Variables**: Replace old variable names with new ones
2. **Configure Plugin Security**: Add `PLUGIN_WHITE_LIST` if plugin restrictions are needed
3. **Update Docker Compose**: Use new environment variable names in docker-compose.yml
4. **Test Configuration**: Verify all services start correctly with new configuration
5. **Update Documentation**: Update any internal documentation to reflect correct configuration

### Migration from Incorrect Configuration
If you previously used environment variables for database or API configuration:

1. **Move to Application Files**: Transfer all non-executor configurations to `application-{profile}.yml`
2. **Update Docker Compose**: Remove incorrect environment variables from docker-compose.yml
3. **Test Configuration**: Verify all services start correctly with new configuration
4. **Update Documentation**: Update any internal documentation to reflect correct configuration

This guide ensures proper configuration management across all Hercules services while maintaining the flexibility and performance benefits of the system.

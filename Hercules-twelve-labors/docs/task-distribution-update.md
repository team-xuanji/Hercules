# Task Distribution Update Documentation

## Overview

This document summarizes the updates made to the Hercules documentation regarding the task distribution mechanism. The current version uses MySQL tables as the storage medium for task message forwarding instead of traditional message queues.

## Changes Made

### 1. Architecture Diagrams Updated

**English Version (Readme.md)**:
```
│  ┌─────────────────┐    ┌──────────────────────────────────────┐ │
│  │ Task Scheduler  │◄──►│      Task Distribution Storage       │ │
│  │   & Dispatcher  │    │    (MySQL Tables - Current)         │ │
│  └─────────────────┘    └──────────────────────────────────────┘ │
```

**Chinese Version (Readme-CN.md)**:
```
│  ┌─────────────────┐    ┌──────────────────────────────────────┐ │
│  │ 任务调度器      │◄──►│       任务分发存储                   │ │
│  │   与分发器      │    │    (MySQL表 - 当前版本)             │ │
│  └─────────────────┘    └──────────────────────────────────────┘ │
```

### 2. Prerequisites Updated

Added clarification that MySQL is used for both metadata storage and task distribution:

**English**: 
- **MySQL Database**: For metadata storage and task distribution

**Chinese**: 
- **MySQL数据库**: 用于元数据存储和任务分发

### 3. Database Configuration Section Added

#### MySQL Database Setup
```sql
CREATE DATABASE hercules CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'hercules'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON hercules.* TO 'hercules'@'%';
FLUSH PRIVILEGES;
```

#### Application Configuration
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hercules?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC
    username: hercules
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

# MyBatis-Plus configuration
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  mapper-locations: classpath*:/mapper/**/*.xml

# Hercules specific configuration
hercules:
  task-distribution:
    # Current implementation uses database tables
    type: database
    # Future extensible options: redis, rabbitmq, kafka
    database:
      table-prefix: hercules_task_
      polling-interval: 5000  # milliseconds
      batch-size: 100
```

### 4. Environment Variables Updated

**Before**:
```bash
# Task distribution
HERCULES_TASK_QUEUE_TYPE=redis
HERCULES_REDIS_HOST=localhost
HERCULES_REDIS_PORT=6379
```

**After**:
```bash
# Task distribution (currently uses database tables)
HERCULES_TASK_DISTRIBUTION_TYPE=database
HERCULES_TASK_POLLING_INTERVAL=5000
HERCULES_TASK_BATCH_SIZE=100

# Future extensible options (not yet implemented):
# HERCULES_TASK_DISTRIBUTION_TYPE=redis
# HERCULES_REDIS_HOST=localhost
# HERCULES_REDIS_PORT=6379
```

### 5. Roadmap Section Updated

**English**:
- ✅ **Database-based Task Distribution**: Currently implemented using MySQL tables for reliable task forwarding
- 🔄 **Extensible Message Distribution**: Abstract task distribution layer supporting Redis, RabbitMQ, Kafka

**Chinese**:
- ✅ **基于数据库的任务分发**: 当前使用MySQL表实现可靠的任务转发机制
- 🔄 **可扩展消息分发**: 抽象任务分发层，支持Redis、RabbitMQ、Kafka

### 6. Todo Section Updated

Updated the todo item about task distribution to clarify current implementation:

**Before**:
- For task distribution, it should also support an abstract class. Task distribution can actually be implemented using a message queue.

**After**:
- For task distribution, it should also support an abstract class. Task distribution can be implemented using message queues (Redis, RabbitMQ, etc.). Currently, MySQL tables are used as the storage medium for task message forwarding.

## Key Messages Communicated

1. **Current Implementation**: MySQL tables are used for task distribution
2. **ORM Framework**: Uses MyBatis-Plus instead of JPA/Hibernate for database operations
3. **Reliability**: This approach provides reliable task distribution without additional infrastructure
4. **Future Extensibility**: The system is designed to support multiple message distribution mediums
5. **Configuration Guidance**: Clear instructions on how to configure the database and MyBatis-Plus
6. **Migration Path**: Future versions will support Redis, RabbitMQ, Kafka, etc.

## Technology Stack Clarification

### Database Access Layer
- **ORM Framework**: MyBatis-Plus (not JPA/Hibernate)
- **Database**: MySQL 8.0+ recommended
- **Connection Pool**: HikariCP (Spring Boot default)
- **Migration**: MyBatis-Plus handles table creation and updates

## Benefits of Current Approach

1. **Simplicity**: No additional message queue infrastructure required
2. **Reliability**: Database ACID properties ensure reliable task distribution
3. **Consistency**: Single database for both metadata and task distribution
4. **Monitoring**: Easy to monitor and debug using standard database tools
5. **Backup**: Task distribution state is included in database backups

## Future Enhancements

The documentation now clearly indicates that future versions will support:
- Redis for high-performance scenarios
- RabbitMQ for complex routing requirements
- Kafka for high-throughput streaming scenarios
- Other message queue systems as needed

This provides users with a clear understanding of the current implementation while setting expectations for future extensibility.

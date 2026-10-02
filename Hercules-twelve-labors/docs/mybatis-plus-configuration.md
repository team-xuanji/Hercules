# MyBatis-Plus Configuration Guide

## Overview

Hercules uses MyBatis-Plus as the ORM framework instead of JPA/Hibernate. This document provides detailed configuration guidance for MyBatis-Plus in the Hercules ecosystem.

## Why MyBatis-Plus?

1. **Performance**: Better performance for complex queries compared to JPA
2. **Flexibility**: More control over SQL generation and execution
3. **Simplicity**: Reduces boilerplate code while maintaining SQL control
4. **Chinese Ecosystem**: Well-supported in Chinese development community
5. **Active Development**: Regular updates and feature enhancements

## Configuration

### Basic Configuration

```yaml
# MyBatis-Plus configuration
mybatis-plus:
  # MyBatis configuration
  configuration:
    # Enable camelCase mapping
    map-underscore-to-camel-case: true
    # SQL logging (disable in production)
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    # Cache configuration
    cache-enabled: true
    lazy-loading-enabled: true
    aggressive-lazy-loading: false
  
  # Global configuration
  global-config:
    # Database configuration
    db-config:
      # Primary key strategy
      id-type: ASSIGN_ID
      # Logical delete configuration
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      # Table prefix
      table-prefix: hercules_
    
    # Banner configuration
    banner: true
  
  # Mapper XML locations
  mapper-locations: classpath*:/mapper/**/*.xml
  # Type aliases package
  type-aliases-package: team.magic.flute.hercules.twelve.labors.dao.entity
```

### Database Connection Configuration

```yaml
spring:
  datasource:
    # MySQL configuration
    url: jdbc:mysql://localhost:3306/hercules?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: hercules
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
    
    # HikariCP connection pool configuration
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      auto-commit: true
      idle-timeout: 30000
      pool-name: HerculesHikariCP
      max-lifetime: 1800000
      connection-timeout: 30000
      connection-test-query: SELECT 1
```

### Production Configuration

```yaml
# Production optimized configuration
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    # Disable SQL logging in production
    # log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
    cache-enabled: true
    lazy-loading-enabled: true
    aggressive-lazy-loading: false
    # Enable second level cache
    cache-enabled: true
  
  global-config:
    db-config:
      id-type: ASSIGN_ID
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      table-prefix: hercules_
      # Optimistic locking
      version-column-name: version
    
    banner: false  # Disable banner in production
  
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: team.magic.flute.hercules.twelve.labors.dao.entity

spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:hercules}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC
    username: ${DB_USERNAME:hercules}
    password: ${DB_PASSWORD:your_password}
    driver-class-name: com.mysql.cj.jdbc.Driver
    
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50
      auto-commit: true
      idle-timeout: 30000
      pool-name: HerculesHikariCP
      max-lifetime: 1800000
      connection-timeout: 30000
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000
```

## Entity Configuration

### Base Entity Example

```java
@Data
@TableName("hercules_cms_data_download_task")
public class CmsDataDownloadTaskPO {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long taskId;
    
    @TableField("business_key")
    private String businessKey;
    
    @TableField("task_status")
    private String taskStatus;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
    
    @Version
    @TableField("version")
    private Integer version;
}
```

### Mapper Interface Example

```java
@Mapper
public interface CmsDataDownloadTaskMapper extends BaseMapper<CmsDataDownloadTaskPO> {
    
    /**
     * Custom query with pagination
     */
    IPage<CmsDataDownloadTaskPO> selectTasksByBusinessKey(
        IPage<CmsDataDownloadTaskPO> page,
        @Param("businessKey") String businessKey,
        @Param("status") String status
    );
    
    /**
     * Batch update task status
     */
    int batchUpdateTaskStatus(
        @Param("taskIds") List<Long> taskIds,
        @Param("status") String status
    );
}
```

## Common Configurations

### Pagination Plugin

```java
@Configuration
public class MyBatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // Pagination plugin
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        
        // Optimistic locking plugin
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        // SQL performance analysis plugin (development only)
        if (isDevelopment()) {
            interceptor.addInnerInterceptor(new IllegalSQLInnerInterceptor());
        }
        
        return interceptor;
    }
    
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MyMetaObjectHandler();
    }
}
```

### Auto-fill Handler

```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
        this.strictInsertFill(metaObject, "version", Integer.class, 1);
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

## Task Distribution Tables

### Task Queue Table

```sql
CREATE TABLE hercules_task_queue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    task_data TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    priority INT DEFAULT 0,
    retry_count INT DEFAULT 0,
    max_retry INT DEFAULT 3,
    executor_id VARCHAR(100),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    execute_time DATETIME,
    complete_time DATETIME,
    deleted TINYINT DEFAULT 0,
    version INT DEFAULT 1,
    INDEX idx_status_priority (status, priority),
    INDEX idx_executor_id (executor_id),
    INDEX idx_create_time (create_time)
);
```

### Task Lock Table

```sql
CREATE TABLE hercules_task_lock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lock_key VARCHAR(200) NOT NULL UNIQUE,
    lock_value VARCHAR(100) NOT NULL,
    expire_time DATETIME NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_expire_time (expire_time)
);
```

## Best Practices

1. **Use Logical Delete**: Configure logical delete for audit trails
2. **Enable Optimistic Locking**: Prevent concurrent update conflicts
3. **Configure Connection Pool**: Optimize database connections
4. **Use Pagination**: Always use pagination for large result sets
5. **SQL Logging**: Enable in development, disable in production
6. **Index Optimization**: Create appropriate indexes for query performance
7. **Transaction Management**: Use `@Transactional` appropriately

## Troubleshooting

### Common Issues

1. **Table Not Found**: Check table prefix configuration
2. **Column Mapping**: Verify camelCase mapping is enabled
3. **Connection Timeout**: Adjust connection pool settings
4. **SQL Injection**: Use parameterized queries
5. **Performance**: Enable SQL analysis in development

### Monitoring

```yaml
# Enable actuator for monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

This configuration ensures optimal performance and maintainability for the Hercules task distribution system using MyBatis-Plus.

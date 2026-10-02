# Hercules Plugin Development Guide

## Overview

Hercules plugins are the core execution units that implement specific business logic within the Hercules task execution system. They provide a flexible, extensible architecture that allows developers to create custom task processors for various business scenarios.

## Plugin Architecture

### Core Concepts

**Extension Points**: Plugins are specific implementations of business logic that can be dynamically loaded and executed by the Hercules Executor. They operate within a controlled environment with access to powerful tools like DuckDB for data processing.

**Class Loading**: Plugins use a parent delegation class loader, which means they can access executor classes. This design has both advantages and disadvantages:
- **Advantages**: Utility classes, logging, and common dependencies don't need to be included separately - just mark them as `provided`
- **Disadvantages**: May require dependency troubleshooting, or you can create shaded JARs to avoid conflicts

**Framework Restrictions**: Plugin packages should NOT use Spring framework. Use bare JARs only, as plugins are instantiated with simple `new` operations and cannot support heavy frameworks like Spring.

## Plugin Development Process

### 1. Core Interfaces Implementation

Every plugin must implement two essential interfaces:

#### A. PluginRegister Interface
```java
@AutoService(team.magic.flute.hercules.common.plugin.PluginRegister.class)
public class PluginRegister implements team.magic.flute.hercules.common.plugin.PluginRegister {
    @Override
    public Map<String, PluginRegisterInfo> getRegisterInfo() {
        Map<String, PluginRegisterInfo> map = new HashMap<>();
        PluginRegisterInfo pluginRegisterInfo = new PluginRegisterInfo()
                .setClassName("com.example.MyPlugin")
                .setDesc("My Custom Plugin")
                .setPluginName("my_plugin_v1");
        map.put(pluginRegisterInfo.getPluginName(), pluginRegisterInfo);
        return map;
    }
}
```

#### B. TaskPlugin Interface
```java
@AutoService(TaskPlugin.class)
public class MyPlugin implements TaskPlugin {
    @Override
    public String getName() {
        return "my_plugin_v1";
    }

    @Override
    public void execute(TaskExecutionContext context) throws Exception {
        // Your business logic here
        Connection duckdbConnection = context.getDuckdbConnection();
        // Use DuckDB for data processing
        // Set results in context.setCheckPointResult() if needed
    }
}
```

### 2. Development Steps

1. **Create Plugin Module**: Set up a new Maven module for your plugin
2. **Implement Interfaces**: Implement both `PluginRegister` and `TaskPlugin` interfaces
3. **Add Dependencies**: Include necessary dependencies with appropriate scopes
4. **Business Logic**: Implement your specific business logic in the `execute` method
5. **Testing**: Create tests using the provided context dependencies
6. **Packaging**: Build as a fat JAR for deployment
7. **Upload**: Upload to Hercules Manager for automatic loading by executors

### 3. Project Structure Example

```
my-plugin/
├── pom.xml
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/example/
│   │           ├── PluginRegister.java
│   │           └── MyPlugin.java
│   └── test/
│       └── java/
│           └── com/example/
│               └── MyPluginTest.java
└── META-INF/
    └── services/
        ├── team.magic.flute.hercules.common.plugin.PluginRegister
        └── team.magic.flute.hercules.common.plugin.TaskPlugin
```

## TaskExecutionContext Features

### DuckDB Integration

The execution context provides a powerful DuckDB connection that enables:

- **Cross-source Data Operations**: Read/write from RDS, OSS, HTTP/HTTPS
- **OLAP Calculations**: Advanced analytical processing
- **AI Computations**: Machine learning and AI workloads
- **Full-text Search**: Text processing and search capabilities
- **Vector Calculations**: Vector operations and similarity search
- **Data Staging**: Use as temporary storage for complex workflows

### Context Properties

```java
public class TaskExecutionContext {
    private Connection duckdbConnection;        // DuckDB connection
    private String executionContext;            // Task context data
    private String id;                         // Task ID
    private String checkPointResult;           // Results to return
    private Map<String, Object> anotherContextMap; // Additional context
    private Collection<HerculesSubmitOnceTaskRequest> forwardRequest; // Chain tasks
    private boolean forwardRequestMustWait;    // Chain execution control
}
```

### Result Handling

To return results to the business side:
```java
context.setCheckPointResult("Your result data here");
```

The `checkPointResult` will be written back to the system, and third-party users can query task execution status and results through the manager interface.

### Chain Task Execution

Plugins can trigger additional tasks:
```java
Collection<HerculesSubmitOnceTaskRequest> chainTasks = new ArrayList<>();
chainTasks.add(new HerculesSubmitOnceTaskRequest()
    .setPluginGroup("another_group")
    .setPluginHandle("another_plugin")
    .setContext("chain task context"));
context.setForwardRequest(chainTasks);
context.setForwardRequestMustWait(true); // Wait for chain completion
```

## DuckDB Capabilities

### Installation and Extensions

```java
try (Statement statement = duckdbConnection.createStatement()) {
    statement.execute("INSTALL mysql");    // MySQL connector
    statement.execute("INSTALL httpfs");   // HTTP/S3 file system
    statement.execute("INSTALL aws");      // AWS services
    statement.execute("INSTALL fts");      // Full-text search
    statement.execute("INSTALL vss");      // Vector similarity search
}
```

### OSS Integration Example

```java
// Create OSS secret
String ossSecretSql = """
    CREATE OR REPLACE SECRET oss_secret (
        TYPE s3,
        PROVIDER config,
        KEY_ID 'your_access_key',
        SECRET 'your_secret_key',
        REGION 'your_region',
        ENDPOINT 'your_oss_endpoint'
    );
    """;
statement.execute(ossSecretSql);

// Read from OSS
statement.execute("SELECT * FROM 's3://bucket/path/file.csv'");
```

### MySQL Integration Example

```java
// Attach MySQL database
String attachSql = """
    ATTACH 'host=localhost user=root port=3306 database=mydb password=pass' 
    AS mysql_db (TYPE mysql, READ_ONLY);
    """;
statement.execute(attachSql);

// Query MySQL data
ResultSet rs = statement.executeQuery("SELECT * FROM mysql_db.users");
```

## Testing

### Local Testing Setup

1. **Add Test Dependencies**:
```xml
<dependency>
    <groupId>team.magic-flute</groupId>
    <artifactId>Hercules-common</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

2. **Create Test Context**:
```java
@Test
public void testPlugin() throws Exception {
    DuckDBConnection conn = (DuckDBConnection) 
        DriverManager.getConnection("jdbc:duckdb::memory:");
    
    TaskExecutionContext context = new TaskExecutionContext()
        .setDuckdbConnection(conn)
        .setExecutionContext("test context")
        .setId("test-task-id");
    
    MyPlugin plugin = new MyPlugin();
    plugin.execute(context);
    
    // Verify results
    assertNotNull(context.getCheckPointResult());
}
```

## Deployment

### 1. Build Fat JAR

Configure Maven to build a fat JAR with all dependencies:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.2.4</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 2. Upload to Manager

1. Access Hercules Manager web interface
2. Navigate to Plugin Management
3. Upload your fat JAR file
4. Specify plugin group name
5. Manager will automatically register and distribute to executors

### 3. Task Execution

Once uploaded, your plugin can be used in tasks:

```json
{
    "pluginGroup": "my_plugins",
    "pluginHandle": "my_plugin_v1",
    "context": "task execution parameters",
    "businessKey": "business_identifier"
}
```

## Best Practices

### 1. Error Handling
```java
@Override
public void execute(TaskExecutionContext context) throws Exception {
    try {
        // Your business logic
    } catch (Exception e) {
        log.error("Plugin execution failed", e);
        context.setCheckPointResult("ERROR: " + e.getMessage());
        throw e; // Re-throw for executor handling
    }
}
```

### 2. Resource Management
```java
@Override
public void execute(TaskExecutionContext context) throws Exception {
    try (Statement stmt = context.getDuckdbConnection().createStatement()) {
        // Use resources in try-with-resources
    }
}
```

### 3. Configuration Management
```java
public class MyPlugin implements TaskPlugin {
    private MyConfig parseConfig(String contextStr) {
        return JacksonUtils.readValue(contextStr, MyConfig.class);
    }
    
    @Override
    public void execute(TaskExecutionContext context) throws Exception {
        MyConfig config = parseConfig(context.getExecutionContext());
        // Use configuration
    }
}
```

### 4. Logging
```java
@Slf4j
public class MyPlugin implements TaskPlugin {
    @Override
    public void execute(TaskExecutionContext context) throws Exception {
        log.info("Starting plugin execution for task: {}", context.getId());
        // Business logic
        log.info("Plugin execution completed successfully");
    }
}
```

## Advanced Features

### 1. Multi-format Data Export
Use DuckDB's COPY command for various export formats:
```java
String exportSql = """
    COPY (SELECT * FROM my_table) 
    TO 's3://bucket/export.csv' 
    (FORMAT CSV, HEADER true);
    """;
```

### 2. Real-time Data Processing
Combine with streaming data sources:
```java
// Process streaming data with DuckDB
statement.execute("CREATE TABLE stream_buffer AS SELECT * FROM read_json_auto('stream_data.json')");
```

### 3. Machine Learning Integration
Use DuckDB extensions for ML workloads:
```java
statement.execute("INSTALL vss");
statement.execute("SELECT vector_similarity(embedding1, embedding2) FROM ml_data");
```

This guide provides a comprehensive foundation for developing Hercules plugins. The combination of flexible plugin architecture and powerful DuckDB integration enables sophisticated data processing and business logic implementation.

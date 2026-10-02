# Hercules 插件开发指南

## 概述

Hercules 插件是在 Hercules 任务执行系统中实现特定业务逻辑的核心执行单元。它们提供了灵活、可扩展的架构，允许开发者为各种业务场景创建自定义任务处理器。

## 插件架构

### 核心概念

**扩展点**：插件是业务逻辑的具体实现，可以被 Hercules Executor 动态加载和执行。它们在受控环境中运行，可以访问 DuckDB 等强大工具进行数据处理。

**类加载**：插件使用双亲委派类加载器，这意味着它们可以访问执行器的类。这种设计有利有弊：
- **优势**：工具类、日志和通用依赖不需要单独包含 - 只需标记为 `provided`
- **劣势**：可能需要排查依赖问题，或者可以创建 shaded JAR 来避免冲突

**框架限制**：插件包不应使用 Spring 框架。只使用裸 JAR，因为插件通过简单的 `new` 操作实例化，无法支持 Spring 等重型框架。

## 插件开发流程

### 1. 核心接口实现

每个插件必须实现两个基本接口：

#### A. PluginRegister 接口
```java
@AutoService(team.magic.flute.hercules.common.plugin.PluginRegister.class)
public class PluginRegister implements team.magic.flute.hercules.common.plugin.PluginRegister {
    @Override
    public Map<String, PluginRegisterInfo> getRegisterInfo() {
        Map<String, PluginRegisterInfo> map = new HashMap<>();
        PluginRegisterInfo pluginRegisterInfo = new PluginRegisterInfo()
                .setClassName("com.example.MyPlugin")
                .setDesc("我的自定义插件")
                .setPluginName("my_plugin_v1");
        map.put(pluginRegisterInfo.getPluginName(), pluginRegisterInfo);
        return map;
    }
}
```

#### B. TaskPlugin 接口
```java
@AutoService(TaskPlugin.class)
public class MyPlugin implements TaskPlugin {
    @Override
    public String getName() {
        return "my_plugin_v1";
    }

    @Override
    public void execute(TaskExecutionContext context) throws Exception {
        // 在这里实现业务逻辑
        Connection duckdbConnection = context.getDuckdbConnection();
        // 使用 DuckDB 进行数据处理
        // 如需要，在 context.setCheckPointResult() 中设置结果
    }
}
```

### 2. 开发步骤

1. **创建插件模块**：为插件设置新的 Maven 模块
2. **实现接口**：实现 `PluginRegister` 和 `TaskPlugin` 接口
3. **添加依赖**：包含必要的依赖项并设置适当的作用域
4. **业务逻辑**：在 `execute` 方法中实现特定的业务逻辑
5. **测试**：使用提供的上下文依赖创建测试
6. **打包**：构建为 fat JAR 用于部署
7. **上传**：上传到 Hercules Manager，执行器会自动加载

### 3. 项目结构示例

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

## TaskExecutionContext 功能

### DuckDB 集成

执行上下文提供强大的 DuckDB 连接，支持：

- **跨源数据操作**：从 RDS、OSS、HTTP/HTTPS 读写数据
- **OLAP 计算**：高级分析处理
- **AI 计算**：机器学习和 AI 工作负载
- **全文搜索**：文本处理和搜索功能
- **向量计算**：向量操作和相似性搜索
- **数据暂存**：用作复杂工作流的临时存储

### 上下文属性

```java
public class TaskExecutionContext {
    private Connection duckdbConnection;        // DuckDB 连接
    private String executionContext;            // 任务上下文数据
    private String id;                         // 任务 ID
    private String checkPointResult;           // 返回结果
    private Map<String, Object> anotherContextMap; // 附加上下文
    private Collection<HerculesSubmitOnceTaskRequest> forwardRequest; // 链式任务
    private boolean forwardRequestMustWait;    // 链式执行控制
}
```

### 结果处理

向业务端返回结果：
```java
context.setCheckPointResult("您的结果数据");
```

`checkPointResult` 将被写回系统，第三方用户可以通过管理器接口查询任务执行状态和结果。

### 链式任务执行

插件可以触发附加任务：
```java
Collection<HerculesSubmitOnceTaskRequest> chainTasks = new ArrayList<>();
chainTasks.add(new HerculesSubmitOnceTaskRequest()
    .setPluginGroup("another_group")
    .setPluginHandle("another_plugin")
    .setContext("链式任务上下文"));
context.setForwardRequest(chainTasks);
context.setForwardRequestMustWait(true); // 等待链式完成
```

## DuckDB 功能

### 安装和扩展

```java
try (Statement statement = duckdbConnection.createStatement()) {
    statement.execute("INSTALL mysql");    // MySQL 连接器
    statement.execute("INSTALL httpfs");   // HTTP/S3 文件系统
    statement.execute("INSTALL aws");      // AWS 服务
    statement.execute("INSTALL fts");      // 全文搜索
    statement.execute("INSTALL vss");      // 向量相似性搜索
}
```

### OSS 集成示例

```java
// 创建 OSS 密钥
String ossSecretSql = """
    CREATE OR REPLACE SECRET oss_secret (
        TYPE s3,
        PROVIDER config,
        KEY_ID '您的访问密钥',
        SECRET '您的密钥',
        REGION '您的区域',
        ENDPOINT '您的OSS端点'
    );
    """;
statement.execute(ossSecretSql);

// 从 OSS 读取
statement.execute("SELECT * FROM 's3://bucket/path/file.csv'");
```

### MySQL 集成示例

```java
// 附加 MySQL 数据库
String attachSql = """
    ATTACH 'host=localhost user=root port=3306 database=mydb password=pass' 
    AS mysql_db (TYPE mysql, READ_ONLY);
    """;
statement.execute(attachSql);

// 查询 MySQL 数据
ResultSet rs = statement.executeQuery("SELECT * FROM mysql_db.users");
```

## 测试

### 本地测试设置

1. **添加测试依赖**：
```xml
<dependency>
    <groupId>team.magic-flute</groupId>
    <artifactId>Hercules-common</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

2. **创建测试上下文**：
```java
@Test
public void testPlugin() throws Exception {
    DuckDBConnection conn = (DuckDBConnection) 
        DriverManager.getConnection("jdbc:duckdb::memory:");
    
    TaskExecutionContext context = new TaskExecutionContext()
        .setDuckdbConnection(conn)
        .setExecutionContext("测试上下文")
        .setId("test-task-id");
    
    MyPlugin plugin = new MyPlugin();
    plugin.execute(context);
    
    // 验证结果
    assertNotNull(context.getCheckPointResult());
}
```

## 部署

### 1. 构建 Fat JAR

配置 Maven 构建包含所有依赖的 fat JAR：

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

### 2. 上传到管理器

1. 访问 Hercules Manager Web 界面
2. 导航到插件管理
3. 上传您的 fat JAR 文件
4. 指定插件组名称
5. 管理器将自动注册并分发到执行器

### 3. 任务执行

上传后，您的插件可以在任务中使用：

```json
{
    "pluginGroup": "my_plugins",
    "pluginHandle": "my_plugin_v1",
    "context": "任务执行参数",
    "businessKey": "业务标识符"
}
```

## 最佳实践

### 1. 错误处理
```java
@Override
public void execute(TaskExecutionContext context) throws Exception {
    try {
        // 您的业务逻辑
    } catch (Exception e) {
        log.error("插件执行失败", e);
        context.setCheckPointResult("错误: " + e.getMessage());
        throw e; // 重新抛出供执行器处理
    }
}
```

### 2. 资源管理
```java
@Override
public void execute(TaskExecutionContext context) throws Exception {
    try (Statement stmt = context.getDuckdbConnection().createStatement()) {
        // 在 try-with-resources 中使用资源
    }
}
```

### 3. 配置管理
```java
public class MyPlugin implements TaskPlugin {
    private MyConfig parseConfig(String contextStr) {
        return JacksonUtils.readValue(contextStr, MyConfig.class);
    }
    
    @Override
    public void execute(TaskExecutionContext context) throws Exception {
        MyConfig config = parseConfig(context.getExecutionContext());
        // 使用配置
    }
}
```

### 4. 日志记录
```java
@Slf4j
public class MyPlugin implements TaskPlugin {
    @Override
    public void execute(TaskExecutionContext context) throws Exception {
        log.info("开始执行插件，任务: {}", context.getId());
        // 业务逻辑
        log.info("插件执行成功完成");
    }
}
```

## 高级功能

### 1. 多格式数据导出
使用 DuckDB 的 COPY 命令进行各种格式导出：
```java
String exportSql = """
    COPY (SELECT * FROM my_table) 
    TO 's3://bucket/export.csv' 
    (FORMAT CSV, HEADER true);
    """;
```

### 2. 实时数据处理
与流数据源结合：
```java
// 使用 DuckDB 处理流数据
statement.execute("CREATE TABLE stream_buffer AS SELECT * FROM read_json_auto('stream_data.json')");
```

### 3. 机器学习集成
使用 DuckDB 扩展进行 ML 工作负载：
```java
statement.execute("INSTALL vss");
statement.execute("SELECT vector_similarity(embedding1, embedding2) FROM ml_data");
```

本指南为开发 Hercules 插件提供了全面的基础。灵活的插件架构与强大的 DuckDB 集成相结合，支持复杂的数据处理和业务逻辑实现。

# Hercules Manager 模块

## 概述

Hercules Manager 是 Hercules 任务执行系统的中央控制平面，提供全面的任务编排、调度和插件管理功能。它作为任务提交、监控和系统管理的主要接口。

## 核心功能

### 1. 任务管理

Hercules Manager 通过 `TaskManagerController` 提供完整的任务生命周期管理：

#### 功能特性：
- **一次性任务提交**：提交单个任务立即执行
- **任务状态监控**：实时状态检查和进度跟踪
- **任务重新运行**：重启失败或已完成的任务
- **异步重试操作**：具有自定义参数的高级重试机制

#### 核心组件：
- `HerculesTaskManagerService`：任务操作的核心服务接口
- `HerculesTaskManagerServiceImpl`：处理任务生命周期的实现
- `HerculesTaskInfo`：表示任务元数据和状态的数据模型

### 2. 定时任务管理（Cron 作业）

通过 `CronTaskManager` 控制器提供高级调度功能：

#### 功能特性：
- **Cron 作业定义**：使用标准 cron 表达式创建和更新定时任务
- **动态控制**：运行时启用、禁用或删除定时作业
- **执行历史**：查看最近的任务执行记录用于监控和调试
- **错过执行处理**：支持错过执行场景的处理

#### 核心组件：
- `HerculesCronJobManagerService`：cron 作业管理的服务接口
- `CronTaskDispatch`：处理 cron 作业的调度器组件
- `HerculesCronJobs`：定时作业定义的数据模型

### 3. 插件管理

通过 `PluginRegisterController` 提供全面的插件生命周期管理：

#### 功能特性：
- **插件注册**：上传和注册包含插件实现的 JAR 文件
- **插件发现**：按组或处理器搜索和浏览可用插件
- **实现检查**：查看详细的插件实现信息
- **插件移除**：清理移除插件组及其资源

#### 核心组件：
- `HerculesPluginManagerService`：核心插件管理服务
- `PluginResourceInfo`：插件元数据和资源信息
- `FileStorage`：插件文件存储的抽象层（支持 OSS）

### 4. 系统架构

#### 高可用特性：
- **多实例支持**：分布式部署与领导者选举
- **运行器管理**：自动实例发现和心跳监控
- **负载分配**：基于桶的任务分配跨实例

#### 数据管理：
- **任务恢复系统**：多层恢复策略（HOT/WARM/COLD/DEAD）
- **元数据压缩**：自动清理过期任务信息
- **数据库抽象**：MyBatis-Plus 与 MySQL 集成

## 数据库模式

### 核心表：

1. **HERCULES_TASK_INFO**：主要任务信息和元数据
2. **hercules_cron_tasks**：定时作业定义和配置
3. **HERCULES_PLUGIN**：插件组信息和资源
4. **HERCULES_PLUGIN_IMPL**：插件实现详情
5. **HERCULES_RECOVER_TASKS_***：多层任务恢复表（HOT/WARM/COLD/DEAD）
6. **HERCULES_MANAGER_RUNNER_INSTANCE**：HA 的管理器实例注册表

## REST API 端点

### 任务管理 (`/taskManager`)
- `POST /submitOnceTask` - 提交一次性任务
- `GET /checkTaskStatus` - 按 ID 检查任务状态
- `PUT /rerunTask` - 重新运行特定任务
- `POST /asyncRetryOneTask` - 异步任务重试

### Cron 作业管理 (`/cronTaskManager`)
- `POST /createOrUpdate` - 创建或更新 cron 作业
- `PUT /enable` - 启用定时作业
- `PUT /disable` - 禁用定时作业
- `DELETE /delete` - 删除 cron 作业
- `GET /showTopNCronTask` - 查看最近执行记录

### 插件管理 (`/pluginManager`)
- `GET /searchPlugin` - 按条件搜索插件
- `GET /searchAllPlugin` - 列出所有插件
- `GET /searchPluginImplInfo` - 获取实现详情
- `POST /register` - 注册新插件
- `GET /removePlugin` - 移除插件组

## 配置

### 应用属性：
- **数据库配置**：MySQL 连接设置
- **文件存储**：插件存储的 OSS 集成
- **调度**：可配置的执行间隔和超时
- **恢复策略**：多层恢复时间配置

### 环境支持：
- 多环境配置文件（qa、prod、test）
- 结构化输出的可配置日志
- Docker 容器化支持

## 关键设计模式

### 1. 服务层架构
- 控制器、服务和数据访问的清晰分离
- 基于接口的可扩展性设计
- 依赖注入实现松耦合

### 2. 分布式处理
- 基于桶的分片实现水平扩展
- 单例操作的领导者选举
- 基于心跳的实例管理

### 3. 恢复策略
- 基于故障时间的多层恢复系统
- 恢复层之间的自动升级
- 可配置的重试策略和超时

## 集成点

### 与 Hercules Executor：
- 任务提交和状态更新
- 插件资源分发
- 异步重试协调

### 与外部系统：
- OSS 用于插件存储
- MySQL 用于持久化数据
- REST API 用于外部集成

## 监控和可观测性

### 内置功能：
- 带有跟踪 ID 的结构化日志
- 任务执行指标
- 插件注册事件
- 系统健康指标

### 运维能力：
- 实时任务状态监控
- Cron 作业执行历史
- 插件可用性跟踪
- 实例健康监控

## 安全考虑

- 所有 API 端点的输入验证
- 插件注册的文件上传安全
- 数据库访问控制
- 环境特定的配置隔离

## 可扩展性特性

- 通过实例集群实现水平扩展
- 基于桶的负载分配
- 异步处理能力
- 可配置的资源限制和超时

Hercules Manager 作为 Hercules 生态系统的基石，为企业级工作负载提供强大、可扩展和可靠的任务编排能力。

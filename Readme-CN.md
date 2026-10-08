# Hercules - 分布式任务编排与数据处理平台

> **丐版分布式调度框架。** 没有 MQ，没有 ZooKeeper，没有 etcd，没有 Redis，没有 Actor 框架——
> 只有一个 MySQL、一个 HTTP 客户端，和一颗不想再运维中间件的心。

![heracles_logo.png](heracles_logo.png)

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7+-green.svg)](https://spring.io/projects/spring-boot)
[![DuckDB](https://img.shields.io/badge/DuckDB-集成-yellow.svg)](https://duckdb.org/)

## 📚 文档导航

- **[BUILD.md](BUILD.md)** - 全面的构建指南和故障排除
- **[配置指南](Hercules-twelve-labors/docs/configuration-guide.md)** - 详细的配置说明
- **[环境变量指南](Hercules-twelve-labors/docs/environment-variables-guide.md)** - 环境变量配置
- **[插件安全指南](Hercules-twelve-labors/docs/plugin-security-guide.md)** - 插件安全和白名单配置
- **[数据库初始化](Hercules-twelve-labors/docs/database-initialization.md)** - 数据库设置和初始化

## 系统概述

Hercules 之名，取自那位完成十二项不可能任务的大力神——不是因为这套引擎有多大，而是因为松耦合的插件架构让一个小系统能扮演很多角色。但它的架构哲学，恰恰与名字相反：**能不加的组件，一个都不加。**

### 演进故事

我们并不是一上来就想造一个"丐版"调度器。第一版设计是教科书式的企业级技术栈：用 MQ 做任务分发，用 Redis 做锁和选主，用 ZooKeeper 做协调，再配一层服务发现。它在我们自己的机房里跑得非常漂亮。

问题出在交付环节。真实的部署目标横跨多云与组织边界——公有云、私有云、客户内网、跨部门的网络域。每个边界有自己的规则：有的只允许出向 HTTP，有的强制复用现场已有的 MySQL、除此之外什么都不能加，而且没有任何一个边界能跨线共享中间件集群。每跨过一个边界，可行解空间就被削掉一层，而能活过所有边界交集的东西所剩无几：一个现成的 MySQL，加一条朴素的 HTTP。这个 footprint 不是偏好，而是可行性的边界。

于是我们逐个组件追问：它到底*是干嘛的*。MQ 提供 at-least-once 投递——一次条件更新加一个清理进程就能做到；Redis 锁提供互斥——同一行数据的 compare-and-swap 就能做到；注册中心提供活性感知——一条心跳记录加"最老者胜出"就能做到。每个中间件都是一个通用解法，为某一个具体问题付了一整套的租金——而 MySQL 恰好可以成为*那个*具体解法。而在只允许出向 HTTP 的地方，推送通道压根不存在——拉取式分发不是风格选择，是仅剩的通路。

每砍掉一个组件，感受到的都是轻松而不是牺牲：需要考虑的脑裂场景变少了，出问题只需要读一处日志，没有版本兼容矩阵。过去由中间件强制执行的那些纪律，转移到了代码里——从隐含假设变成了显式约束（见 ADR）。

与此同时，需求还在不断涌来，而且来自越来越多的团队——包括一些半技术的用户，他们想自助发布自己的业务逻辑。如果让内核去吸收这一切，它会重新膨胀成我们刚刚逃离的那套系统。于是变化被推向外围：胶水插件、类加载器隔离、惰性下载、版本轮转。**内核越来越小，生态越来越大。**

所以"穷人框架"描述的是我们*落到*的地方，而不是我们*出发*的地方。真正的驱动力是约束优先的设计——而这个约束最终被证明是一笔资产。`docs/adr` 里的每一篇 ADR，记录的都是一次深思熟虑的取舍，而不是妥协。

整套系统建立在一个朴素到近乎寒酸的前提上——你已经有一个 MySQL 了。用这个前提，我们干成了这些事：

- **拿数据库当锁**：条件更新就是分布式锁，MySQL 的原子性就是互斥保证
- **拿心跳当选举**：最老的活着实例当 master，UUIDv7 排序够用
- **拿自铸身份 + HMAC 当身份**：执行器启动时自生成 identity，经加密心跳上报即完成注册——DB 可信模型下的 TOFU；之后每个操作按实例验签
- **拿内嵌 DuckDB 当算力**：执行器自带 OLAP 引擎，不依赖外部数据仓库，且每台执行器可自行决定启用与否
- **拿槽位当调度**：执行器通过心跳上报容量、有空闲槽位才拉取任务——刻意做小的资源调度器，但足以撑起任务编排的核心

### 核心特性

- **🚀 分布式任务执行**: 跨多个执行器节点的可扩展任务分发
- **🔌 插件化架构**: 面向各种业务场景的可扩展插件系统
- **📊 集成分析能力**: 内置DuckDB支持轻量级OLAP操作
- **🌐 业务访问网关**: 复杂业务集成的统一接口
- **📈 实时监控**: 全面的任务状态跟踪和监控
- **🔄 灵活调度**: 支持同步和异步执行
- **☁️ 云原生**: 支持容器化的微服务架构

### 应用场景

#### 核心平台能力
- **复杂ETL流水线**: 多阶段数据提取、转换和加载
- **业务数据处理**: 实时和批量业务数据操作
- **工作流编排**: 复杂业务工作流管理
- **微服务集成**: 分布式服务间的统一任务执行
- **插件化处理**: 通过自定义插件实现可扩展处理

#### Twelve-Labors扩展示例
- **数据导出与分析**: 支持压缩的多格式数据导出 (当前示例实现)
- **报表生成**: 自动化业务报表创建和分发
- **数据同步**: 跨系统数据同步工作流
- **通知服务**: 多渠道通知和告警系统
- **文件处理**: 批量文件处理和转换
- **API集成**: 复杂API编排的简化接口
- **商业智能**: 自定义BI数据管道管理
- **合规报告**: 自动化合规和审计报告生成

> **注意**: 当前Twelve-Labors实现专注于数据导出作为示例。该模块设计为可扩展，适用于任何受益于简化接口而非直接使用核心Manager API的业务特定用例。

## 设计哲学：穷出来的架构

如果预算充足，这套东西本可以长成 "Kafka + ZooKeeper + K8s Operator + Prometheus" 的标准形态。
但现实是：运维一套分布式中间件的成本，常常比它解决的问题本身还贵。

于是每一个"标准做法"在这里都被替换成了"手边有什么用什么"：

| 标准做法 | 丐版做法 | 代价 |
|---|---|---|
| ZooKeeper / etcd 选举 | MySQL 心跳 + UUIDv7 最老优先（master 只做清理，工作由所有存活实例分桶分担） | 无强一致——够用于幂等清理，别拿它做交易系统 |
| MQ 任务派发 | HTTP 轮询 + 条件更新认领 | 有轮询延迟，换来零中间件依赖 |
| K8s Operator | 会心跳的 HTTP 执行器 | 进程生命周期自己管 |
| Prometheus / Grafana | 结构化日志前缀（`[PLUGIN_DRIFTED]`、`[INVALID_EXECUTOR_OP]`） | 没有大盘，排查靠 grep |
| OAuth / mTLS | 自铸身份 + 按实例 HMAC 签名 | 信任边界 = DB 读权限（identity 存于库中） |

### 穷的代价

丐版不是没有代价，我们不藏着：

- **没有强一致选举** —— 够用于清理类任务，不承诺共识
- **观测只有日志** —— 没有指标大盘，但关键事件都有稳定前缀可告警
- **一切押在一个 MySQL 上** —— 它挂了，系统就停了；想高可用请自备主从
- **单元测试覆盖薄** —— 以真实依赖做集成测试；核心调度链路的单测正在补齐

## 系统架构

Hercules采用模块化的微服务架构，专为可扩展性和可维护性而设计：

```
┌─────────────────────────────────────────────────────────────────┐
│                    Hercules 生态系统                            │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌──────────────────────────────────────┐ │
│  │   业务访问网关   │    │           外部系统                   │ │
│  │ (twelve-labors) │◄──►│  (APIs, 数据库, 文件系统)           │ │
│  │   [可选模块]    │    │                                      │ │
│  └─────────────────┘    └──────────────────────────────────────┘ │
│           │                                                      │
│           ▼                                                      │
│  ┌─────────────────┐    ┌──────────────────────────────────────┐ │
│  │ Hercules 管理器 │◄──►│         插件注册中心                 │ │
│  │   (编排调度)    │◄──►│       (执行插件)                     │ │
│  │   [核心模块]    │    └──────────────────────────────────────┘ │
│  └─────────────────┘                                            │
│           │                                                      │
│           ▼                                                      │
│  ┌─────────────────┐    ┌──────────────────────────────────────┐ │
│  │ 任务调度器      │◄──►│       任务分发存储                   │ │
│  │   与分发器      │    │    (MySQL表 - 当前版本)             │ │
│  └─────────────────┘    └──────────────────────────────────────┘ │
│           │                                                      │
│           ▼                                                      │
│  ┌─────────────────┐    ┌──────────────────────────────────────┐ │
│  │ Hercules        │◄──►│         存储层                       │ │
│  │   执行器集群    │    │  (数据库, 对象存储等)               │ │
│  │   (分布式)      │    │                                      │ │
│  └─────────────────┘    └──────────────────────────────────────┘ │
│                                                                 │
│  核心工作流: 用户 ──────────────────────► Manager               │
│  可选工作流: 用户 ──► Twelve-Labors ──► Manager                 │
│  任务分发: Manager ◄──HTTP APIs──► Executors                  │
└─────────────────────────────────────────────────────────────────┘
```

### 核心模块

#### 🎯 **Hercules-Manager (管理器)**
- **功能定位**: 中央编排和管理中心
- **核心职责**:
  - 任务定义和生命周期管理
  - 插件注册和版本控制
  - 调度和任务分发
  - 系统监控和健康检查
- **主要特性**:
  - RESTful API任务管理
  - 插件热插拔能力
  - 分布式任务调度
  - 实时状态监控

#### ⚡ **Hercules-Executor (执行器)**
- **功能定位**: 基于HTTP通信的轻量级分布式任务执行引擎
- **核心职责**:
  - 通过Feign客户端进行HTTP任务获取和状态更新
  - 带安全控制的插件加载和执行
  - 资源管理和优化
  - 与Manager的RESTful通信处理所有操作
- **主要特性**:
  - **无状态架构**: 无直接数据库访问，纯HTTP通信
  - **Feign客户端集成**: 通过`HerculesManagerApi`进行RESTful任务管理
  - **HTTP任务生命周期**: `tryFetchTasksWithByteArray` → `tryLockBatchTask`/`tryLockOneTask` → `finishOneTask`/`failOneTask`
  - **多线程执行**: 可配置任务执行插槽和线程池
  - **动态插件加载**: 白名单安全和远程插件下载
  - **基于区域的组织架构**: 更好的资源管理和地理分布
  - **DuckDB集成**: 高性能数据处理，自动连接管理

#### 🔌 **Hercules-Executor-Plugin (执行插件)**
- **功能定位**: 可扩展插件框架
- **核心职责**:
  - 标准化插件接口
  - 插件生命周期管理
  - 自定义业务逻辑实现
- **主要特性**:
  - 热插拔架构
  - 版本兼容性管理
  - 资源感知执行
  - 自定义配置支持

#### 🌐 **Hercules-Twelve-Labors (业务访问网关) - 可选扩展模块**
- **功能定位**: 可扩展的业务集成和访问层
- **设计理念**:
  - 为复杂业务场景提供简化的API接口
  - 可根据实际业务需求扩展和定制功能
  - 当前实现的数据导出功能仅为示例，非最终限制
- **核心职责**:
  - 业务逻辑封装和抽象
  - 复杂任务流程的简化
  - 用户友好的API接口设计
  - 业务特定的数据处理和验证
- **当前示例功能**:
  - 多格式数据导出 (CSV, JSON, Parquet, XLSX) - 临时示例实现
  - 任务提交和状态监控
  - 文件下载和管理
- **扩展能力**:
  - 可添加任何业务特定功能
  - 支持自定义工作流编排
  - 可集成外部系统和服务
  - 支持复杂的业务规则和验证

> **重要说明**: Twelve-Labors是一个可选的扩展模块，其当前的数据导出功能只是临时的示例实现。该模块的真正价值在于为各种复杂业务场景提供简化的访问接口。用户可以根据实际需求扩展或替换其功能，而不仅限于文件下载。核心的任务编排功能完全通过Manager模块实现。

#### 🛠️ **Hercules-Common (公共模块)**
- **功能定位**: 共享工具和通用功能
- **核心职责**:
  - 通用数据结构和工具
  - 共享配置管理
  - 跨模块通信协议
- **主要特性**:
  - 标准化数据模型
  - 配置管理
  - 日志和监控工具
  - 安全和认证助手

#### 📊 **Hercules-Service (内部服务)**
- **功能定位**: 内部数据接口服务
- **核心职责**:
  - 第三方数据缓存和管理
  - 内部服务编排
  - 数据一致性和完整性
- **主要特性**:
  - 数据缓存和优化
  - 服务发现和注册
  - 健康监控和告警
  - 性能指标收集

## 系统交互时序图

### 核心任务提交流程 (直接访问Manager)
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   用户/客户端   │    │    Manager      │    │    Executor     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │ 1. 注册插件           │                       │
         ├──────────────────────►│                       │
         │ 2. 插件注册成功       │                       │
         │◄──────────────────────┤                       │
         │                       │                       │
         │ 3. 提交任务           │                       │
         ├──────────────────────►│                       │
         │ 4. 任务创建成功       │                       │
         │◄──────────────────────┤                       │
         │                       │                       │
         │                       │ 5. 轮询任务           │
         │                       │◄──────────────────────┤
         │                       │ 6. 返回任务信息       │
         │                       ├──────────────────────►│
         │                       │                       │
         │                       │                       │ 7. 执行任务
         │                       │                       ├─────────────►
         │                       │                       │              │
         │                       │                       │◄─────────────┘
         │                       │ 8. 更新状态           │
         │                       │◄──────────────────────┤
         │                       │                       │
         │ 9. 查询状态           │                       │
         ├──────────────────────►│                       │
         │ 10. 返回状态          │                       │
         │◄──────────────────────┤                       │
```

### 可选业务访问流程 (通过Twelve-Labors)
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   用户/客户端   │    │ Twelve-Labors   │    │    Manager      │    │    Executor     │
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │                       │
         │ 1. 提交导出任务       │                       │                       │
         ├──────────────────────►│                       │                       │
         │                       │ 2. 验证请求           │                       │
         │                       ├──────────────────────►│                       │
         │                       │                       │ 3. 创建任务记录       │
         │                       │                       ├──────────────────────►│
         │                       │                       │                   数据库
         │                       │ 4. 返回任务ID         │                       │
         │                       │◄──────────────────────┤                       │
         │ 5. 任务ID响应         │                       │                       │
         │◄──────────────────────┤                       │                       │
         │                       │                       │                       │
         │                       │                       │ 6. 轮询任务           │
         │                       │                       │◄──────────────────────┤
         │                       │                       │ 7. 返回任务信息       │
         │                       │                       ├──────────────────────►│
         │                       │                       │                       │
         │                       │                       │ 8. 下载插件           │
         │                       │                       │◄──────────────────────┤
         │                       │                       │ 9. 插件二进制         │
         │                       │                       ├──────────────────────►│
         │                       │                       │                       │
         │                       │                       │                       │ 10. 执行任务
         │                       │                       │                       ├─────────────►
         │                       │                       │                       │              │
         │                       │                       │                       │◄─────────────┘
         │                       │                       │ 11. 更新状态         │
         │                       │                       │◄──────────────────────┤
         │                       │                       │                   数据库
         │                       │                       │                       │
         │ 12. 检查状态          │                       │                       │
         ├──────────────────────►│                       │                       │
         │                       │ 13. 查询任务状态      │                       │
         │                       ├──────────────────────►│                       │
         │                       │ 14. 返回状态          │                       │
         │                       │◄──────────────────────┤                       │
         │ 15. 状态响应          │                       │                       │
         │◄──────────────────────┤                       │                       │
         │                       │                       │                       │
         │ 16. 下载结果          │                       │                       │
         ├──────────────────────►│                       │                       │
         │                       │ 17. 获取下载链接      │                       │
         │                       ├──────────────────────►│                       │
         │                       │ 18. 返回链接          │                       │
         │                       │◄──────────────────────┤                       │
         │ 19. 文件下载          │                       │                       │
         │◄──────────────────────┤                       │                       │
```

### 插件管理流程
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│    Manager      │    │   文件存储      │    │    Executor     │
│                 │    │   (OSS/S3)      │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │ 1. 上传插件           │                       │
         ├──────────────────────►│                       │
         │ 2. 存储二进制         │                       │
         │◄──────────────────────┤                       │
         │                       │                       │
         │ 3. 注册插件           │                       │
         ├─────────────────────────────────────────────► 数据库
         │                       │                       │
         │                       │ 4. 请求插件           │
         │◄──────────────────────────────────────────────┤
         │ 5. 插件信息           │                       │
         ├──────────────────────────────────────────────►│
         │                       │                       │
         │ 6. 下载插件           │                       │
         ├──────────────────────►│                       │
         │ 7. 插件二进制         │                       │
         │◄──────────────────────┤                       │
         │ 8. 转发二进制         │                       │
         ├──────────────────────────────────────────────►│
         │                       │                       │
         │                       │                       │ 9. 加载并执行
         │                       │                       ├─────────────►
         │                       │                       │              │
         │                       │                       │◄─────────────┘
```

### 任务恢复与监控流程
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│    Manager      │    │     数据库      │    │    Executor     │
│   (调度器)      │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │ 1. 扫描失败任务       │                       │
         ├──────────────────────►│                       │
         │ 2. 返回失败列表       │                       │
         │◄──────────────────────┤                       │
         │                       │                       │
         │ 3. 移至恢复表         │                       │
         ├──────────────────────►│                       │
         │   (HOT/WARM/COLD)     │                       │
         │                       │                       │
         │ 4. 调度重试           │                       │
         ├──────────────────────►│                       │
         │                       │                       │
         │                       │ 5. 轮询恢复任务       │
         │                       │◄──────────────────────┤
         │                       │ 6. 返回重试任务       │
         │                       ├──────────────────────►│
         │                       │                       │
         │                       │                       │ 7. 重试执行
         │                       │                       ├─────────────►
         │                       │                       │              │
         │                       │                       │◄─────────────┘
         │                       │ 8. 更新结果           │
         │                       │◄──────────────────────┤
         │                       │                       │
         │ 9. 监控健康状态       │                       │
         ├──────────────────────►│                       │
         │ 10. 系统指标          │                       │
         │◄──────────────────────┤                       │
```

### 数据流架构图
```
┌─────────────────────────────────────────────────────────────────┐
│                        数据流概览                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐         │
│  │   用户      │    │   业务      │    │   系统      │         │
│  │   请求      │───►│   逻辑      │───►│   执行      │         │
│  └─────────────┘    └─────────────┘    └─────────────┘         │
│         │                   │                   │              │
│         ▼                   ▼                   ▼              │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐         │
│  │ Twelve-     │    │  Manager    │    │  Executor   │         │
│  │ Labors      │◄──►│  服务       │◄──►│  服务       │         │
│  └─────────────┘    └─────────────┘    └─────────────┘         │
│         │                   │                   │              │
│         ▼                   ▼                   ▼              │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                 MySQL 数据库                           │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐       │   │
│  │  │任务记录     │ │插件信息     │ │系统状态     │       │   │
│  │  └─────────────┘ └─────────────┘ └─────────────┘       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                │                               │
│                                ▼                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              文件存储 (OSS/S3)                         │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐       │   │
│  │  │插件文件     │ │导出数据     │ │系统日志     │       │   │
│  │  └─────────────┘ └─────────────┘ └─────────────┘       │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 组件交互矩阵
```
┌─────────────────┬─────────────────┬─────────────────┬─────────────────┐
│     组件        │ Twelve-Labors   │    Manager      │    Executor     │
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
│ 数据库          │   SQL查询       │   SQL查询       │       -         │
│                 │   (MyBatis-Plus)│   (MyBatis-Plus)│   (仅HTTP通信)  │
├─────────────────┼─────────────────┼─────────────────┼─────────────────┤
│ 文件存储        │       -         │   对象API       │   对象API       │
│                 │                 │   (OSS/S3)      │   (OSS/S3)      │
├─────────────────┼─────────────────┼─────────────────┼─────────────────┤
│ DuckDB          │       -         │       -         │   可选          │
│ (数据处理)      │                 │                 │   (JDBC/SQL)    │
└─────────────────┴─────────────────┴─────────────────┴─────────────────┘
```

## 快速开始指南

### 环境要求

- **Java 8+**: 运行所有Hercules服务的必需环境 (支持JDK 8, 11, 17+)
- **Maven 3.6+**: 从源码构建项目
- **MySQL 8.0+**: 任务管理和系统状态数据库
- **对象存储**: OSS (阿里云) 或 S3 (AWS) 用于插件和文件存储
- **(可选) Docker & Docker Compose**: 用于容器化部署

> **注意**: 项目支持JDK 8+，已升级Spring Boot版本。经过测试，兼容JDK 8、11和17+。

> **重要说明**: 当前版本使用MySQL表作为任务消息转发的存储介质。这提供了可靠的任务分发机制，无需额外的消息队列基础设施。未来版本将支持可扩展的消息分发介质（Redis、RabbitMQ等）。

### 从源码构建

> **📖 详细构建指南**: 如需全面的构建说明、故障排除和高级构建选项，请参阅 [BUILD.md](BUILD.md)

#### 1. 克隆仓库
```bash
git clone https://github.com/team-xuanji/hercules.git
cd hercules
```

#### 2. 构建所有模块
```bash
# 使用Maven构建所有模块 (跳过测试)
mvn clean package -Dmaven.test.skip=true

# 或者包含测试构建 (推荐用于开发)
mvn clean package
```

#### 3. 构建单个模块
```bash
# 构建特定模块及其依赖 (推荐方式)
# 构建Manager模块
mvn clean package -Dmaven.test.skip=true -pl Hercules-manager -am

# 构建Executor模块
mvn clean package -Dmaven.test.skip=true -pl Hercules-executor -am

# 构建Twelve-Labors模块
mvn clean package -Dmaven.test.skip=true -pl Hercules-twelve-labors -am

# 仅构建Common模块
mvn clean package -Dmaven.test.skip=true -pl Hercules-common
```

**Maven参数说明:**
- `-pl` (--projects): 指定要构建的模块
- `-am` (--also-make): 同时构建所需的依赖模块
- `-Dmaven.test.skip=true`: 跳过测试编译和执行

#### 4. 构建输出
构建成功后，JAR文件将在各模块的`target/`目录中：
- `Hercules-manager/target/hercules-manager-{version}.jar`
- `Hercules-executor/target/hercules-executor-{version}.jar`
- `Hercules-twelve-labors/target/hercules-twelve-labors-{version}.jar`

#### 5. 构建Docker镜像 (可选)
```bash
# 为所有服务构建Docker镜像
docker build -t hercules-manager:latest -f Hercules-manager/Dockerfile .
docker build -t hercules-executor:latest -f Hercules-executor/Dockerfile .
docker build -t hercules-twelve-labors:latest -f Hercules-twelve-labors/Dockerfile .
```

### 构建要求

- **内存**: Maven构建过程至少需要2GB RAM
- **磁盘空间**: 构建产物至少需要1GB可用空间
- **网络**: 需要互联网连接以下载Maven依赖

### 构建故障排除

#### 常见构建问题

1. **Maven依赖未找到**
   ```bash
   # 清理Maven缓存并重新构建
   mvn dependency:purge-local-repository
   mvn clean package -Dmaven.test.skip=true
   ```

2. **Java版本不匹配**
   ```bash
   # 检查Java版本
   java -version
   javac -version

   # 确保JAVA_HOME设置正确
   echo $JAVA_HOME
   ```

3. **构建过程中内存问题**
   ```bash
   # 增加Maven内存
   export MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=512m"
   mvn clean package -Dmaven.test.skip=true
   ```

4. **模块依赖问题**
   ```bash
   # 构建模块及其依赖 (推荐方式)
   mvn clean package -Dmaven.test.skip=true -pl Hercules-manager -am
   mvn clean package -Dmaven.test.skip=true -pl Hercules-executor -am
   mvn clean package -Dmaven.test.skip=true -pl Hercules-twelve-labors -am

   # 或者先安装公共模块，再构建其他模块
   mvn clean install -Dmaven.test.skip=true -pl Hercules-common
   ```

#### 验证构建成功
```bash
# 检查JAR文件是否创建
ls -la */target/*.jar

# 验证JAR文件完整性
java -jar Hercules-manager/target/hercules-manager-*.jar --version
```

### 数据库配置

#### 1. 创建MySQL数据库
```sql
CREATE DATABASE hercules CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'hercules'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON hercules.* TO 'hercules'@'%';
FLUSH PRIVILEGES;
```

#### 2. 初始化数据库表
执行初始化SQL脚本来创建必需的表：

```bash
# 执行Hercules-Manager初始化脚本 (创建核心系统表)
mysql -u hercules -p hercules < Hercules-manager/sql/init.sql

# 执行Hercules-Twelve-Labors初始化脚本 (创建业务访问网关表)
mysql -u hercules -p hercules < Hercules-twelve-labors/sql/init.sql
```

**Hercules-Manager创建的表 (9个表)：**
- `hercules_cron_tasks` - 定时任务管理 (使用EXECUTOR_REGION)
- `HERCULES_MANAGER_RUNNER_INSTANCE` - 管理器实例跟踪
- `HERCULES_PLUGIN_IMPL` - 插件实现注册表
- `HERCULES_PLUGIN` - 插件信息和资源
- `HERCULES_TASK_INFO` - 任务执行信息 (使用EXECUTOR_REGION)
- `HERCULES_RECOVER_TASKS_*` - 任务恢复表 (HOT/WARM/COLD/DEAD，使用EXECUTOR_REGION)
- `HERCULES_EXECUTOR_INFO` - 执行器信息和能力

**Hercules-Twelve-Labors创建的表 (2个表)：**
- `HERCULES_CMS_DATA_DOWNLOAD_TASK_DEF` - 数据导出任务定义 (包含EXECUTOR_REGION字段)
- `HERCULES_CMS_DATA_DOWNLOAD_TASK` - 数据导出任务执行记录

**包含的示例数据：**
- 3个任务定义，EXECUTOR_REGION='PROD'
- 3个任务执行记录，不同状态 (SUCCESS, RUNNING, INIT)

> **注意**: 初始化脚本包含用于测试的示例数据。您可以根据环境需要修改或删除示例数据。

#### 2. 配置各个服务

每个Hercules服务都需要自己的配置文件。为每个服务创建`application-prod.yml`（或`application-dev.yml`）：

##### Hercules-Manager 配置
```yaml
# Hercules-manager/src/main/resources/application-prod.yml
spring:
  datasource:
    url: "jdbc:mysql://localhost:3306/hercules?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8&autoReconnect=true&allowMultiQueries=true&nullCatalogMeansCurrent=true"
    username: "hercules"
    password: "your_password"

# 文件存储用于插件管理 (OSS 或 S3)
file-storage:
  oss:
    endpoint: "oss-cn-beijing.aliyuncs.com"  # 或您的S3端点
    bucket: "hercules-plugins"
    access: "your_access_key"
    secret: "your_secret_key"
    root-path: "hercules/prod/plugin/"
    support-http-download: false

# MyBatis-Plus配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

##### Hercules-Executor 配置
```yaml
# Hercules-executor/src/main/resources/application-prod.yml
spring:
  datasource:
    url: "jdbc:mysql://localhost:3306/hercules?useUnicode=true&characterEncoding=utf8&autoReconnect=true&allowMultiQueries=true&nullCatalogMeansCurrent=true"
    username: "hercules"
    password: "your_password"

# 文件存储用于插件下载 (OSS 或 S3)
file-storage:
  oss:
    endpoint: "oss-cn-beijing.aliyuncs.com"  # 或您的S3端点
    bucket: "hercules-plugins"
    access: "your_access_key"
    secret: "your_secret_key"
    root-path: "hercules/prod/plugin/"
    support-http-download: false

# API配置用于与Hercules-Manager通信
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

##### Hercules-Twelve-Labors 配置
```yaml
# Hercules-twelve-labors/src/main/resources/application-prod.yml
spring:
  datasource:
    url: "jdbc:mysql://localhost:3306/hercules?useUnicode=true&characterEncoding=utf8&autoReconnect=true&allowMultiQueries=true&nullCatalogMeansCurrent=true"
    username: "hercules"
    password: "your_password"

# API配置用于与Hercules-Manager通信
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

### 安装与部署

#### 1. 克隆代码仓库
```bash
git clone https://github.com/team-xuanji/hercules.git
cd hercules
```

#### 2. 构建项目
```bash
# 构建所有模块
mvn clean package -DskipTests

# 或构建特定模块
mvn clean package -pl hercules-manager -DskipTests -am
mvn clean package -pl hercules-executor -DskipTests -am
mvn clean package -pl hercules-twelve-labors -DskipTests -am
```

#### 3. 启动服务
```bash
# 1. 启动MySQL数据库
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=hercules \
  -e MYSQL_USER=hercules \
  -e MYSQL_PASSWORD=your_password \
  -p 3306:3306 mysql:8.0

# 2. 等待MySQL就绪，然后初始化数据库
sleep 30
mysql -h localhost -u hercules -p hercules < Hercules-manager/sql/init.sql
mysql -h localhost -u hercules -p hercules < Hercules-twelve-labors/sql/init.sql

# 3. 启动Hercules管理器 (必须首先启动)
java -jar hercules-manager.jar

# 4. 启动Hercules执行器 (依赖于管理器)
java -jar hercules-executor.jar

# 5. 启动Hercules业务访问网关
java -jar hercules-twelve-labors.jar
```

> **重要**: 请按以下顺序启动服务：
> 1. MySQL数据库 + 初始化表结构
> 2. Hercules-Manager (核心编排服务)
> 3. Hercules-Executor (任务执行服务)
> 4. Hercules-Twelve-Labors (业务访问网关)

### 基本使用流程

#### 核心工作流程 (直接访问Manager)
使用Hercules的主要方式是直接与Manager模块交互：

1. **注册执行插件**
```bash
# 注册数据导出插件
curl -X POST http://localhost:8080/api/plugins \
  -H "Content-Type: application/json" \
  -d '{
    "pluginName": "data-exporter",
    "pluginGroup": "data-processing",
    "version": "1.0.0",
    "description": "多格式数据导出插件"
  }'
```

2. **定义调度任务** (可选)
```bash
# 创建定时执行的任务定义
curl -X POST http://localhost:8080/api/task-definitions \
  -H "Content-Type: application/json" \
  -d '{
    "businessKey": "daily-report-export",
    "taskName": "每日销售报表导出",
    "pluginGroup": "data-processing",
    "pluginName": "data-exporter",
    "sqlTemplate": "SELECT * FROM sales WHERE date = ${#export_date}",
    "exportFormat": "xlsx",
    "schedule": "0 0 8 * * ?"
  }'
```

3. **提交一次性任务**
```bash
# 提交任务立即执行
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

#### 可选业务访问流程 (通过Twelve-Labors)
为了简化业务操作，您可以选择使用Twelve-Labors模块。当前实现提供数据导出功能作为示例：

```bash
# 当前示例: 通过业务访问网关进行数据导出
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

**未来扩展示例:**
```bash
# 示例: 报表生成服务
curl -X POST http://localhost:8081/api/reports/generate \
  -H "Content-Type: application/json" \
  -d '{
    "reportType": "monthly-sales",
    "parameters": {
      "month": "2024-12",
      "department": "sales"
    }
  }'

# 示例: 数据同步服务
curl -X POST http://localhost:8081/api/sync/execute \
  -H "Content-Type: application/json" \
  -d '{
    "sourceSystem": "crm",
    "targetSystem": "warehouse",
    "syncType": "incremental"
  }'
```

> **注意**: Twelve-Labors是一个可扩展模块，当前演示数据导出功能。您可以扩展它为任何复杂业务工作流提供简化接口。该模块作为在核心Hercules平台上创建业务特定API层的模板。

#### 步骤4: 监控任务执行
```bash
# 注册数据导出插件
curl -X POST http://localhost:8080/api/plugins \
  -H "Content-Type: application/json" \
  -d '{
    "pluginName": "data-exporter",
    "pluginGroup": "data-processing",
    "version": "1.0.0",
    "description": "多格式数据导出插件"
  }'
```

#### 步骤2: 定义任务模板 (可选)
```bash
# 创建定时执行的任务定义
curl -X POST http://localhost:8080/api/task-definitions \
  -H "Content-Type: application/json" \
  -d '{
    "businessKey": "daily-report-export",
    "taskName": "每日销售报表导出",
    "pluginGroup": "data-processing",
    "pluginName": "data-exporter",
    "sqlTemplate": "SELECT * FROM sales WHERE date = ${#export_date}",
    "exportFormat": "xlsx",
    "schedule": "0 0 8 * * ?"
  }'
```

#### 步骤3: 提交任务执行

**方式A: 直接任务提交**
```bash
# 提交任务立即执行
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

**方式B: 业务访问网关 (推荐)**
```bash
# 使用业务访问网关进行简化操作
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

#### 步骤4: 监控任务执行
```bash
# 检查任务状态
curl http://localhost:8081/api/tasks/{taskId}/status

# 获取任务执行历史
curl http://localhost:8081/api/tasks/history?businessKey=sales-data-export

# 下载导出文件
curl http://localhost:8081/api/tasks/{taskId}/download
```

### 高级用法

#### 自定义插件开发
```java
@Component
public class CustomDataProcessor implements ExecutionPlugin {

    @Override
    public ExecutionResult execute(TaskContext context) {
        // 您的自定义业务逻辑
        return ExecutionResult.success("处理完成");
    }

    @Override
    public String getPluginName() {
        return "custom-data-processor";
    }
}
```

#### 业务集成层
Hercules-Twelve-Labors模块提供了简化集成的业务访问网关：

```java
@RestController
@RequestMapping("/api/business")
public class BusinessController {

    @PostMapping("/process-data")
    public ResponseEntity<TaskResult> processBusinessData(
            @RequestBody BusinessDataRequest request) {
        // 业务特定的处理逻辑
        return taskSubmissionService.submitAndWait(request);
    }
}
```

## 配置指南

### 环境变量配置

环境变量仅用于Hercules-Executor模块的运行时配置。数据库和文件存储配置在各服务的`application-{profile}.yml`文件中指定，而不是环境变量。

#### Hercules-Executor 环境变量
```bash
# 执行器标识和容量配置
EXECUTOR_REGION=PROD                      # 执行器区域标识 (默认: TEST)
EXECUTOR_REGION_DESC="生产环境执行器"      # 执行器区域描述 (默认: "I am Iron Man. (｀∀´)Ψ *snap!*")
EXECUTOR_SLOT_SIZE=4                      # 并发任务槽数量 (默认: 2)

# 插件安全配置
PLUGIN_WHITE_LIST=data-export,report-gen,etl-processor  # 逗号分隔的允许插件句柄列表 (默认: 空 - 允许所有插件)

# DuckDB数据处理配置
ENABLE_DUCKDB=true                        # 启用DuckDB集成 (默认: true)
DUCKDB_MEM_GB_SIZE=2                      # DuckDB内存大小(GB) (默认: 1)
DUCKDB_SPILL_GB_SIZE=500                  # DuckDB溢出大小(GB) (默认: 200)
THREAD_COUNT=4                            # DuckDB线程数 (默认: 1)

# DuckDB存储路径
DUCKDB_STORAGE_PATH=/opt/hercules/duckdb/data.db           # DuckDB数据库文件路径
DUCKDB_SPILL_PATH=/opt/hercules/duckdb/spill/              # Duckdb溢出目录
```

> **重要**: 数据库连接、API配置和文件存储设置在各服务的`application-{profile}.yml`文件中配置，而不是通过环境变量。

### 应用配置文件

数据库连接、API配置和文件存储设置在各服务的`application-{profile}.yml`文件中配置：

#### Hercules-Manager 配置
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

#### Hercules-Executor 配置
```yaml
# application-prod.yml
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

api:
  app-open-api:
    host: http://localhost:8080
    name: hercules-manager
    version: v1
    caller: hercules-executor
    sign: your_api_signature
```

#### Hercules-Twelve-Labors 配置
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

> **注意**: 数据导出的文件存储在Manager模块中配置。Twelve-Labors通过API调用与Manager通信，不直接访问文件存储。

### 重要配置说明

#### OSS vs S3 配置
Hercules支持多种对象存储，您可以选择使用阿里云OSS或AWS S3：

**使用阿里云OSS:**
```yaml
file-storage:
  oss:
    endpoint: "oss-cn-beijing.aliyuncs.com"
    bucket: "hercules-plugins"
    access: "your_access_key"
    secret: "your_secret_key"
    root-path: "hercules/prod/plugin/"
```

**使用AWS S3:**
```yaml
file-storage:
  s3:
    endpoint: "s3.amazonaws.com"
    region: "us-east-1"
    bucket: "hercules-plugins"
    access-key: "your_access_key"
    secret-key: "your_secret_key"
    root-path: "hercules/prod/plugin/"
```

#### 服务间通信配置
所有服务都需要配置与Hercules-Manager的通信：

```yaml
api:
  app-open-api:
    host: http://hercules-manager:8080  # Manager服务地址
    name: hercules-manager
    version: v1
    caller: hercules-executor  # 或 hercules-twelve-labors
    sign: your_api_signature
    headers:
      whiteListAk: your_whitelist_key
```

#### MyBatis-Plus通用配置
所有服务的MyBatis-Plus配置：

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  mapper-locations: classpath*:/mapper/**/*.xml
```

## 发展路线图与未来增强

### 当前开发重点
- ✅ **基于数据库的任务分发**: 当前使用MySQL表实现可靠的任务转发机制
- ✅ **异步任务执行**: 已完全实现和优化
- 🔄 **同步执行支持**: 正在开发中，用于实时场景
- 🔄 **增强插件监控**: 实时插件更新跟踪
- 🔄 **多存储支持**: 抽象FileIO层支持S3、HDFS、WebHDFS
- 🔄 **可扩展消息分发**: 抽象任务分发层，支持Redis、RabbitMQ、Kafka

### 计划功能
- **🎯 高级调度**: 基于Cron和事件驱动的调度
- **📊 增强分析**: 内置仪表板和报告
- **🔐 安全增强**: OAuth2、RBAC和审计日志
- **🌍 多区域支持**: 跨区域任务分发
- **🤖 AI/ML集成**: 机器学习流水线支持
- **📱 移动仪表板**: 移动友好的监控界面

### 性能优化
- **⚡ 任务执行**: 改进资源利用率和并行处理
- **💾 内存管理**: 优化大数据集的内存使用
- **🚀 启动时间**: 更快的应用启动和插件加载
- **📈 可扩展性**: 增强水平扩展能力

## 贡献指南

我们欢迎社区贡献！请查看我们的[贡献指南](CONTRIBUTING.md)了解如何开始。

### 开发环境设置
```bash
# 克隆并设置开发环境
git clone https://github.com/team-xuanji/hercules.git
cd hercules

# 安装依赖
mvn clean install

# 运行测试
mvn test

# 启动开发服务器
./scripts/start-dev.sh
```

## 许可证

本项目采用Apache License 2.0许可证 - 详见[LICENSE](LICENSE)文件。

## 支持与社区

- **📖 文档**: [https://hercules-docs.example.com](https://hercules-docs.example.com)
- **💬 社区论坛**: [https://community.hercules.example.com](https://community.hercules.example.com)
- **🐛 问题跟踪**: [GitHub Issues](https://github.com/team-xuanji/hercules/issues)
- **📧 邮件支持**: support@hercules.example.com

---

**由Hercules团队用❤️构建**


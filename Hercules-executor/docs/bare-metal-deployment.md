# Hercules Executor 裸机部署指南

## 适用范围

本文说明如何在普通物理机 / 虚拟机（裸机）上以 systemd 托管方式运行 Hercules Executor，无需 Docker、Kubernetes 或独立的 executor 网络边界 profile。同一份构建产物 JAR 可在多个网络边界复用，仅靠外置配置和 region 区分。

> 本文档只覆盖部署运行。构建步骤请参考根目录 [BUILD.md](../../BUILD.md) 与 [Readme-CN.md](../../Readme-CN.md)。Executor 的架构与 HTTP API 请参考 [Hercules-Executor-CN.md](./Hercules-Executor-CN.md)。

## 1. 部署模型与配置边界

### 1.1 一份 JAR，多处裸机运行

Executor 构建产物为单一可执行 Spring Boot JAR（`spring-boot-maven-plugin` 打包，`finalName=Hercules-executor`）。该 JAR：

- 通过 HTTP/Feign 客户端 `HerculesManagerApi` 连接 Hercules Manager，拉取任务、上报状态、下载插件；
- 不直接访问 MySQL 数据源。代码中出现的 `HikariDataSource` 仅用于本地 DuckDB 引擎（`initDuckdbDataSource()`），与 Manager 的业务库无关；
- 任务管理全链路无状态，水平扩展靠多实例 + region 完成。

因此不需要为不同网络边界构建不同的 executor profile，也不需要在 JAR 内置 per-boundary 配置。所有差异化通过外置 Spring YAML + 环境变量注入。

### 1.2 Spring profile 与 EXECUTOR_REGION 的语义

两者职责不同，不要混用：

| 维度 | 机制 | 含义 |
| --- | --- | --- |
| 部署阶段 | `SPRING_PROFILES_ACTIVE`（如 `prod`） | 决定加载哪份 `application-{profile}.yml`，对应一套环境（qa / prod 等） |
| 业务/路由边界 | `EXECUTOR_REGION` 环境变量 | Manager 侧用于任务路由与隔离；`RunnerEnv.executorRegion` 由此读取 |

- 等价、可互相替代的 executor 实例应使用**相同** region（Manager 会按 region 调度到这一组实例）；
- 跨网络 / 业务边界（例如内网区与外网区、A 业务与 B 业务）应使用**不同** region；
- `EXECUTOR_REGION` 未设置时默认值为 `TEST`（见 `Constant.EXECUTOR_REGION` 与 `RunnerEnv.setEnvironment`）。生产部署**必须显式设置**，避免误入 TEST 分组；
- 同一 region 内多实例：`runnerInstanceId` / `runnerIdentityId` 会在启动时用 `GUID.v7()` 自动拼接 region 生成唯一标识，无需人工配置。

### 1.3 现状提示（不夸大校验能力）

当前 Java 代码**不会**对缺失的 region 或密钥做强校验：

- `EXECUTOR_REGION`、`EXECUTOR_REGION_DESC`、`EXECUTOR_SLOT_SIZE`、DuckDB 各项、`PLUGIN_WHITE_LIST` 均有代码层默认值；但这些默认值不构成完整可运行配置，TEST 环境仍需提供可达的 Manager API、匹配的 HTTP 加密密钥，以及插件存储配置；
- `application.yml` 中的 `hercules.security.http-encrypt-key` 当前是占位值 `"xxxxxxxxxx="`，`api.app-open-api.sign` 等也是占位；
- 生产部署需由**部署流程**（外置配置 + 启动前自检）保证必填项被覆盖，不能依赖应用层报错。

## 2. 运行环境要求

| 项 | 要求 |
| --- | --- |
| JRE | 与项目编译目标兼容：Java 8（`maven.compiler.source/target=1.8`）。可使用 JDK 8 对应的 JRE；更高版本 JRE 通常可运行，但项目以 Java 8 为准，按 8 部署最稳妥 |
| OS | 任意支持 systemd 的 Linux 发行版 |
| 网络 | 能访问 Hercules Manager 的 HTTP 端点、对象存储（OSS）endpoint |
| 磁盘 | DuckDB data / spill 目录可写；插件缓存目录可写 |
| 端口 | Executor 默认监听 `8080`，context-path `/hercules-executor/v1`（见 `application.yml`） |

## 3. 目录与用户准备

以 `hercules` 用户运行，配置与运行时数据分离：

```bash
# 1. 专用用户与组（非 root）
sudo groupadd -r hercules
sudo useradd -r -g hercules -d /var/lib/hercules -s /usr/sbin/nologin hercules

# 2. 程序目录（存放 JAR，只读挂载）
sudo mkdir -p /opt/hercules
sudo chown -R root:hercules /opt/hercules
sudo chmod 750 /opt/hercules

# 3. 外置配置目录（运行用户只读）
sudo mkdir -p /etc/hercules/executor
sudo chown -R root:hercules /etc/hercules/executor
sudo chmod 750 /etc/hercules/executor

# 4. 运行时数据目录（运行用户可写）
sudo mkdir -p /var/lib/hercules/executor /var/log/hercules
sudo chown -R hercules:hercules /var/lib/hercules/executor /var/log/hercules
sudo chmod 750 /var/lib/hercules/executor /var/log/hercules

# 5. DuckDB 独立 data / spill 目录（每个实例独立路径，见 §6）
sudo mkdir -p /var/lib/hercules/executor/duckdb/data \
             /var/lib/hercules/executor/duckdb/spill
sudo chown -R hercules:hercules /var/lib/hercules/executor/duckdb
```

复制构建产物（在构建机上）：

```bash
cp Hercules-executor/target/Hercules-executor.jar /opt/hercules/Hercules-executor.jar
```

> 仅复制 `target/Hercules-executor.jar` 这一个 JAR。Spring Boot 已将依赖打入该 fat-jar，无需单独分发 lib。

## 4. 外置配置文件

### 4.1 `/etc/hercules/executor/application-prod.yml`

启动时通过 `--spring.config.additional-location=file:/etc/hercules/executor/` 加载，Spring Boot 会自动读取该目录下的 `application-prod.yml`（profile=prod）。**不要**将真实密钥写入此文件后提交到仓库。

```yaml
# /etc/hercules/executor/application-prod.yml
# 机密值用 ${ENV_VAR} 占位，实际值由 /etc/hercules/executor/runtime.env 注入

api:
  app-open-api:
    host: https://manager.internal.example.com   # Hercules Manager 的 HTTP 基地址
    name: hercules-manager                        # Manager 服务名（拼接到 URL 路径）
    version: v1                                   # Manager API 版本
    caller: hercules-executor                     # 调用方标识，用于 Manager 鉴权
    sign: ${HERCULES_API_SIGN}                    # API 签名密钥，机密，环境变量注入
    headers:
      whiteListAk: ${HERCULES_WHITE_LIST_AK}      # Manager 网关白名单 AK，机密

# 对象存储：插件 jar 下载 / 产物上传
file-storage:
  oss:
    endpoint: oss-cn-zhangjiakou.aliyuncs.com
    bucket: hercules-prod
    access: ${OSS_ACCESS_KEY}                     # 机密
    secret: ${OSS_SECRET_KEY}                     # 机密
    root-path: hercules/prod/plugin/
    support-http-download: false                  # true 时走 OSS 预签名 HTTP 下载；false 走 SDK

# Feign 重试（拉取任务 / 调用 Manager 时的退避策略）
feign:
  retry:
    period: 100
    max-period: 1000
    max-attempts: 5

# HTTP 加解密密钥（AES GCM 256），用于与 Manager 之间的任务载荷加解密
# 对应 RunnerEnv.httpEncryptKey，属性键固定为 hercules.security.http-encrypt-key
hercules:
  security:
    http-encrypt-key: ${HERCULES_HTTP_ENCRYPT_KEY}   # 机密
```

字段与代码的对应关系：

- `api.app-open-api.*` → `ApiConfigBean`（`@ConfigurationProperties(prefix = "api.app-open-api")`，见 `ApiConfig`）。`host+name+version` 拼成 Feign target URL；
- `file-storage.oss.*` → `OssFileStorage` 的 `@Value` 注入字段；
- `hercules.security.http-encrypt-key` → `RunnerEnv.httpEncryptKey`（`@Value("${hercules.security.http-encrypt-key}")`）。

> Executor **不需要** `spring.datasource.*` / MySQL 配置。若你看到示例里有 `spring.datasource`，那是 Manager 的配置，不要拷到 executor 这份文件里。

### 4.2 `/etc/hercules/executor/runtime.env`

环境变量文件，由 systemd `EnvironmentFile=` 加载。所有机密值从此注入。

```bash
# /etc/hercules/executor/runtime.env
# === 机密（必须从受控来源注入，禁止硬编码明文并提交）===
HERCULES_API_SIGN=            # 来自权限受控的本地文件 / Secret Manager
HERCULES_WHITE_LIST_AK=
OSS_ACCESS_KEY=
OSS_SECRET_KEY=
HERCULES_HTTP_ENCRYPT_KEY=

# === 运行时配置（RunnerEnv 读取的 ENV，见 Constant.java）===
SPRING_PROFILES_ACTIVE=prod
EXECUTOR_REGION=PROD_CN_INTERNAL        # 生产必须显式设置；与 Manager 路由边界对齐
EXECUTOR_REGION_DESC="Production CN internal executor"
EXECUTOR_SLOT_SIZE=8                   # 并发任务槽数（默认 4，按机器 CPU/内存调整）
PLUGIN_WHITE_LIST=                      # 留空=允许全部插件；逗号分隔的插件 handle 白名单

# DuckDB（如启用）
ENABLE_DUCKDB=true
DUCKDB_MEM_GB_SIZE=2
DUCKDB_SPILL_GB_SIZE=500
THREAD_COUNT=4
DUCKDB_STORAGE_PATH=/var/lib/hercules/executor/duckdb/data/executor.db
DUCKDB_SPILL_PATH=/var/lib/hercules/executor/duckdb/spill/
```

机密注入方式（任选其一，均禁止进入版本库）：

- **本地受控文件**：将明文写入 `runtime.env`，并 `chmod 600`，属主 `root:hercules`；该文件由运维分发，不进 git；
- **Secret Manager**：启动前由部署脚本拉取并写入 `runtime.env`（或导出到进程环境），脚本执行后清屏。

> 安全提醒：环境变量在进程内可见（`/proc/<pid>/environ`，同主机 root / 同 UID 进程可读）。`runtime.env` 权限设为 `0600`、属主 `root:hercules`，并限制主机登录面与 root 访问面。对密钥轮换有要求的场景优先用 Secret Manager 动态注入。

`runtime.env` 权限：

```bash
sudo chmod 600 /etc/hercules/executor/runtime.env
sudo chown root:hercules /etc/hercules/executor/runtime.env
```

## 5. systemd 服务

`/etc/systemd/system/hercules-executor.service`：

```ini
[Unit]
Description=Hercules Executor
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=hercules
Group=hercules
WorkingDirectory=/var/lib/hercules/executor
EnvironmentFile=/etc/hercules/executor/runtime.env
Environment=SPRING_PROFILES_ACTIVE=prod
ExecStart=/usr/bin/java -jar /opt/hercules/Hercules-executor.jar \
  --spring.config.additional-location=file:/etc/hercules/executor/
Restart=on-failure
RestartSec=10s
SuccessExitStatus=143
StandardOutput=append:/var/log/hercules/executor.log
StandardError=append:/var/log/hercules/executor.log
LimitNOFILE=65536

# 安全加固
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full
ProtectHome=true
ReadWritePaths=/var/lib/hercules/executor /var/log/hercules
CapabilityBoundingSet=
AmbientCapabilities=

[Install]
WantedBy=multi-user.target
```

要点说明：

- `--spring.config.additional-location=file:/etc/hercules/executor/` 使外置 `application-prod.yml` 优先于 JAR 内置 `application.yml`；生产包**无需**在 JAR 里新增 per-boundary 配置；
- `SPRING_PROFILES_ACTIVE=prod` 同时用 `Environment=` 显式声明与 `runtime.env` 里的值互为兜底，确保 profile 生效；
- `WorkingDirectory` 指向可写目录，避免 DuckDB / 插件缓存因工作目录不可写失败；
- 以非 root `hercules` 用户运行，配合 `NoNewPrivileges` / `ProtectSystem` 加固。

部署与运维命令：

```bash
# 安装
sudo systemctl daemon-reload
sudo systemctl enable --now hercules-executor

# 日常操作
sudo systemctl start hercules-executor
sudo systemctl restart hercules-executor
sudo systemctl status hercules-executor

# 查看日志
sudo journalctl -u hercules-executor -f
# 或
sudo tail -f /var/log/hercules/executor.log

# 更新 JAR
sudo systemctl stop hercules-executor
sudo cp /path/to/build/Hercules-executor.jar /opt/hercules/Hercules-executor.jar
sudo systemctl start hercules-executor
```

## 6. DuckDB 注意事项

DuckDB 由 `ExecutorProcessHandleImpl.initDuckdbDataSource()` 创建，仅当 `ENABLE_DUCKDB=true` 时启用。部署时：

- **目录必须可写**：`DUCKDB_STORAGE_PATH` 所在目录和 `DUCKDB_SPILL_PATH` 指向的目录，运行用户 `hercules` 必须有写权限；
- **每个实例独立路径**：`DUCKDB_STORAGE_PATH` 指向独立的 `.db` 文件，`DUCKDB_SPILL_PATH` 指向独立的 spill 目录。**不要**让多个 executor 进程共用同一个数据库文件——DuckDB 的连接池被硬编码为 `maximumPoolSize=1` / `minimumIdle=1`，多进程共享文件会导致锁冲突；
- **同机多实例**：在物理机上跑多个 executor 时，给每个实例不同的 `DUCKDB_STORAGE_PATH` / `DUCKDB_SPILL_PATH`（例如 `.../duckdb/data/region-a.db` 与 `.../duckdb/data/region-b.db`）；
- **关闭 DuckDB**：若该实例不使用 DuckDB 数据处理能力，设 `ENABLE_DUCKDB=false` 即可，无需准备相关目录。

`THREAD_COUNT` 的代码默认值为 `Constant.DEFAULT_DUCKDB_MEM_GB_SIZE`（即 1），属历史复用，建议显式设置为目标并发线程数。

## 7. 部署后自检

按以下顺序验证，确认 executor 已正确接入 Manager：

1. **profile 生效**：日志中确认 `The following profiles are active: prod`；
2. **effective region 正确**：日志或 Manager 侧确认 `executorRegion=PROD_CN_INTERNAL`（你设置的值），不是 `TEST`；
3. **Manager URL 可达**：日志中 Feign 目标地址为 `host/name/version` 拼接结果；用 `curl` 验证 `${host}/${name}/${version}` 网络 reachable；
4. **插件存储可达**：确认 `file-storage.oss.endpoint` / `bucket` 可访问，access/secret 正确（启动时若 Manager 返回插件资源，会尝试从 OSS 下载）；
5. **心跳 / 注册**：`TaskConsumer` 会周期性调用 `reportExecutorInfo` 上报状态与容量；观察 Manager 侧是否出现该 `runnerInstanceId`；
6. **slot 容量**：确认 `EXECUTOR_SLOT_SIZE` 生效（Manager 侧容量视图或 executor 日志中的并发槽数）。

常见排错：

| 现象 | 排查方向 |
| --- | --- |
| 启动报 `http-encrypt-key` 解析失败 | `runtime.env` 中 `HERCULES_HTTP_ENCRYPT_KEY` 未注入，或 `application-prod.yml` 占位变量名拼写错误 |
| Feign 调用 Manager 401/403 | `caller` / `sign` / `headers.whiteListAk` 与 Manager 侧配置不一致 |
| 插件下载失败 | OSS `access`/`secret`/`bucket`/`root-path` 错误，或 `support-http-download` 与 Manager 侧预期不符 |
| DuckDB 启动失败 / 锁冲突 | data/spill 目录不可写，或多个进程共用同一 `.db` 文件 |
| region 仍为 TEST | `runtime.env` 未被 systemd 加载（检查 `EnvironmentFile=` 路径；systemd system manager 需要能读取该文件，推荐 `root:hercules`、权限 `0600`）|
| 占位密钥未覆盖 | `application.yml` 内置 `http-encrypt-key: "xxxxxxxxxx="` 仍是占位，需确认 `application-prod.yml` 覆盖成功且 ENV 已注入 |

## 8. 多边界部署小结

在同一台或不同台裸机上部署多个 executor 实例覆盖不同网络边界时：

- 共用同一份 `Hercules-executor.jar`；
- 每实例一套 `/etc/hercules/executor-{region}/`（或独立主机上的 `/etc/hercules/executor/`）配置目录与 `runtime.env`；
- 用不同的 `EXECUTOR_REGION` 区分边界，对应 Manager 的路由分组；
- DuckDB 路径、端口（如需共机）各自独立；
- systemd 服务名区分（如 `hercules-executor@region-a.service` / `hercules-executor@region-b.service`），便于分别启停与日志隔离。

---

核对事项（部署前自检清单）：

- [ ] `runtime.env` 权限 `0600`、属主 `root:hercules`，机密未进入版本库
- [ ] `application-prod.yml` 所有 `${ENV_VAR}` 占位都在 `runtime.env` 中有对应变量
- [ ] `SPRING_PROFILES_ACTIVE=prod` 生效
- [ ] `EXECUTOR_REGION` 已显式设置为生产值（非 TEST）
- [ ] Manager `host` / `name` / `version` 与 Manager 实际地址一致
- [ ] OSS `endpoint` / `bucket` / `access` / `secret` / `root-path` 正确
- [ ] `hercules.security.http-encrypt-key` 已被生产密钥覆盖（非 `xxxxxxxxxx=`）
- [ ] DuckDB data / spill 目录可写，且每实例独立路径
- [ ] systemd 以非 root 用户 `hercules` 运行，`Restart=on-failure`
- [ ] JRE 版本与 Java 8 兼容

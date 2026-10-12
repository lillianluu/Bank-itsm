# ITSM 后端(itsm-server)

ITSM 一体化 IT 服务管理平台的后端服务:基于 **Spring Boot 3.2.5 + Flowable 7 + MyBatis-Plus + PostgreSQL + Redis** 的模块化单体(Modular Monolith)。多模块只是物理分层,最终产物仍是**一个 FatJar、一个进程**;检索 / 文件存储 / 事件总线三大能力以 SPI + Starter 形式实现**可插拔**,未部署 ES / S3 / MQ 等中间件时零外部依赖即可运行(仅需 PostgreSQL + Redis)。

## 一、模块总览

```
                 ┌──────────────────────────────────────────────┐
                 │                itsm-boot(启动器)             │
                 └──────┬───────────────────────────────┬───────┘
                        │                               │
        ┌───────────────▼───────────────┐   ┌───────────▼───────────────┐
        │        业务模块层 ×5            │   │     能力 Starter ×3        │
        │  platform / ticket / catalog  │   │  search / storage / event │
        │  knowledge / report           │   │  (SPI 实现可插拔)           │
        └───────────────┬───────────────┘   └───────────┬───────────────┘
                        │                               │
                 ┌──────▼──────┐                 ┌──────▼──────┐
                 │ itsm-engine │                 │ itsm-common │◄── 三方 BOM:
                 │ (技术能力层) │────────────────►│  (SPI 接口)  │    itsm-dependencies
                 └──────┬──────┘                 └──────▲──────┘
                        │                               │
                 ┌──────▼──────┐                        │
                 │ itsm-domain │────────────────────────┘
                 │  (领域模型)  │
                 └─────────────┘
```

依赖方向严格单向:`common ← domain ← engine ← module-* ← boot`,由 Maven 编译期强制,任何模块不得反向依赖。

## 二、各模块定义与功能

### itsm-dependencies —— 版本清单(BOM)

- **定位**:纯版本管理,无任何代码;统一三方依赖与内部模块的版本。
- **结构说明**:独立 POM(不继承 itsm-parent),避免"parent 导入 BOM + BOM 继承 parent"造成的自引用循环。
- **管理的关键版本**:

| 依赖 | 版本 | 用途 |
|---|---|---|
| spring-boot-dependencies | 3.2.5 | Web/安全/校验/actuator 全家桶版本基线 |
| flowable-spring-boot-starter | 7.0.1 | 工作流引擎 |
| mybatis-plus-spring-boot3-starter | 3.5.7 | ORM |
| jjwt | 0.12.5 | JWT 签发/校验(V1.0 认证用) |
| springdoc-openapi | 2.5.0 | OpenAPI 文档 |
| elasticsearch-java | 8.11.4 | 可选:ES 增强检索 |
| software.amazon.awssdk:bom | 2.25.60 | 可选:S3/MinIO 对象存储 |

### itsm-common —— 通用基建(被所有人依赖,不依赖任何业务)

| 包 | 内容 | 说明 |
|---|---|---|
| `com.itsm.common.api` | `Result` / `PageResult` / `ErrorCode` | 统一响应结构 `{code, message, data, traceId}`(code=0 成功)、分页结构、错误码规范 |
| `com.itsm.common.exception` | `BusinessException` | 业务异常,由全局异常处理器转换为统一响应 |
| `com.itsm.common.event` | `DomainEvent` / `DomainEventListener` | 领域事件基类(含 topic/eventId)与监听接口 |
| `com.itsm.common.spi.search` | `SearchEngine` + `SearchDocument` / `SearchQuery` / `SearchHit` | **检索 SPI**:统一索引/检索契约 |
| `com.itsm.common.spi.storage` | `ObjectStorage` + `FileUpload` / `StoredFile` | **存储 SPI**:put/get/remove/presignedUrl |
| `com.itsm.common.spi.event` | `EventBus` | **事件 SPI**:publish / subscribe |

> SPI 接口是可插拔架构的"插座":业务模块只面向这些接口编程,具体实现由 Starter 按配置装配。

### itsm-domain —— 领域模型层

纯业务概念,不含技术设施:

- `base.BaseDO`:审计基类(创建人/时间、更新人/时间、逻辑删除),所有表的 DO 继承;
- `enums.*`:`TicketType`(事件/服务请求等工单类型)、`TicketStatus`(工单状态机)、`PriorityLevel`(P1~P4 优先级)、`UrgencyLevel`(紧急度)、`ImpactLevel`(影响度)、`ChannelType`(来源渠道:门户/邮件/IM/电话/监控/OpenAPI)、`SlaTimerState`(SLA 时钟状态);
- `event.TicketCreatedEvent`:工单创建领域事件(驱动通知/分流等下游动作)。

### itsm-engine —— 技术能力层(业务模块的"发动机")

| 包 | 类 | 功能 |
|---|---|---|
| `flowable` | `FlowableEngineConfig` | Flowable 引擎与 Spring 事务/数据源整合的定制装配 |
| `sla` | `SlaClockService` / `RedisSlaClockService` | SLA 时钟:基于 Redis 的计时器(响应/解决时限),支持暂停/恢复/到期触发 |
| `form` | `FormSchema` / `FormControl` / `FormRule` / `FormValidator` | 动态表单:控件定义、联动/显隐规则、服务端统一校验 |
| `rule` | `RuleEngine` / `RoutingContext` / `DefaultRuleEngine` | 分流规则引擎:按条件路由工单(分组/人员/升级),默认条件表达式实现 |

### itsm-search-starter —— 检索能力(可插拔)

- 默认实现 `PgFtsSearchEngine`:基于 PostgreSQL 全文检索(tsvector + GIN,配合 `search_index` 表),**零额外中间件**;
- 可选实现 `EsSearchEngine`:Elasticsearch 8 客户端接入(骨架,待 V1.1 补全);
- 装配类 `ItsSearchAutoConfiguration` + 配置 `ItsSearchProperties`;
- **开关**:`itsm.search.engine = pg(默认) | es`;未引入 ES 依赖时 ES 分支整体不可达。

### itsm-storage-starter —— 文件存储能力(可插拔)

- 默认实现 `LocalObjectStorage`:本地磁盘(`itsm.storage.local.path`),按日期/UUID 组织 key,防路径穿越,下载统一走鉴权代理地址(仅适用单实例);
- 可选实现 `S3ObjectStorage`:AWS SDK v2,兼容 S3 / MinIO(path-style),多副本部署时使用;
- 装配类 `ItsStorageAutoConfiguration` + 配置 `ItsStorageProperties`;
- **开关**:`itsm.storage.type = local(默认) | s3`。

### itsm-event-starter —— 事件总线(可插拔)

- 默认实现 `InProcessEventBus`:进程内同步分发(编程式订阅 + Spring 事件桥接,单监听器异常隔离),**零外部依赖**;
- 可选实现 `KafkaEventBus`(classpath 存在 spring-kafka 时装配)、`RocketMqEventBus`(骨架,待接入 rocketmq-spring);
- 装配类 `ItsEventAutoConfiguration` + 配置 `ItsEventProperties`(含 Outbox 重试参数);
- **开关**:`itsm.event.bus = inprocess(默认) | kafka | rocketmq`;
- TODO:Outbox 表 + 定时补偿保证至少一次投递。

### itsm-module-platform —— 平台域业务模块

用户/组织/角色权限(RBAC)、数据字典、系统参数、通知消息、操作审计、文件访问代理、外部集成。

当前骨架内容:`PingController`(`/api/v1/ping` 存活探针)、`AuthController`(`/api/v1/auth/login|refresh`,TODO:JWT 签发 + SSO/LDAP 对接)。

### itsm-module-ticket —— 工单域业务模块(核心)

事件单 / 服务请求全生命周期:创建→受理→处理→转派→挂起→解决→关闭→重开,工作台与评价。

当前骨架内容:`TicketService` 接口(实现需串联:单号生成→落库→Flowable 启流(businessKey=单号)→SLA 时钟启动→事件发布)、`TicketController`(`/api/v1/tickets` 创建/列表/时间线)、`TicketCreateCmd`。

### itsm-module-catalog —— 服务目录模块

服务目录树与目录条目(发布/停用)、条目绑定动态表单与流程模板。当前骨架:`CatalogController`(`/api/v1/catalogs`、`/api/v1/catalog-items`)。

### itsm-module-knowledge —— 知识库模块

知识空间/文章、审核发布流、全文检索(走统一 `SearchEngine` SPI)。当前骨架:`KbController`(`/api/v1/kb/articles`)。

### itsm-module-report —— 报表与看板模块

服务台运营看板(今日新建/待受理/处理中/超期)、预置统计报表、数据大屏。当前骨架:`ReportController`(`/api/v1/dashboard/overview`)。

### itsm-boot —— 唯一启动器与打包模块

- `ItsmApplication`:`@SpringBootApplication(scanBasePackages = {boot, engine, module})` + `@MapperScan` 各业务模块 mapper 包;三大 Starter 通过 `META-INF/spring/...AutoConfiguration.imports` 条件装配;
- `config.SecurityConfig`:Spring Security 6 无状态配置(骨架阶段放行全部接口,TODO:JWT 过滤器 + 数据权限);
- `config.OpenApiConfig`:OpenAPI 文档(`/swagger-ui.html`,Bearer JWT Scheme 预留);
- `application.yaml`:全部配置与三大开关的默认值,支持环境变量外置(`ITS_DB_URL` / `ITS_DB_USER` / `ITS_DB_PASSWORD` / `ITS_REDIS_HOST` / `ITS_REDIS_PORT` / `ITS_STORAGE_PATH` / `ITS_S3_*` 等);
- `logback-spring.xml`:控制台 + 文件滚动双 Appender(100MB/30 天/5GB 上限);
- `db/migration/V1__init.sql`:Flyway 基线(`sys_config`、`sys_dict`、`search_index` 及初始字典);
- 打包:spring-boot-maven-plugin **分层 FatJar**(layertools),产物 `itsm-boot/target/itsm-boot.jar`。

## 三、快速开始

```bash
# 构建(全部模块)
mvn -DskipTests package

# 运行(需本地 PostgreSQL + Redis)
java -jar itsm-boot/target/itsm-boot.jar

# 或 Docker 单机编排(boot + PG + Redis,可选 MinIO/ES)
docker compose -f deploy/docker/docker-compose.yml up -d
```

构建上下文即本目录(`itsm-server/`):

```bash
docker build -f deploy/docker/Dockerfile -t itsm/server:dev .
```

## 四、可插拔能力配置示例

```yaml
itsm:
  search:
    engine: pg          # pg(默认)| es —— 引入 ES 依赖并切换后自动启用 EsSearchEngine
  storage:
    type: local         # local(默认)| s3 —— 切 s3 需配 endpoint/bucket/ak/sk
  event:
    bus: inprocess      # inprocess(默认)| kafka | rocketmq
```

原则:**不引入对应依赖、不改配置 = 零外部依赖运行**;升级部署形态时仅加依赖 + 改开关,业务代码零改动。

## 五、约定的 TODO(V1.0 演进方向)

1. `TicketService` 完整链路:落库 → Flowable 启流 → SLA 时钟 → 事件发布 → 通知;
2. JWT 认证 + RBAC 数据权限 Guard(替换 SecurityConfig 放行配置);
3. 各业务模块 Mapper / DO / Service 实现,数据库增量脚本(V2+);
4. ES 检索实现补全、RocketMQ 接入、Outbox 补偿任务;
5. 前端工程见仓库根目录 `itsm-ui/`。


## 六、本地 Docker 运行前后端

在仓库根目录执行:

```bash
mvn -f itsm-server/pom.xml -DskipTests package
docker compose -f itsm-server/deploy/docker/docker-compose.yml up -d --build
```

- 前端: http://localhost:5173 (Nginx 提供静态页面,`/api/` 代理到后端)
- 后端: http://localhost:8080
- 默认管理员: `admin` / `admin123`
- PostgreSQL: `localhost:5432/itsm`,默认用户名和密码均为 `itsm`
- Redis: `localhost:6379`

PostgreSQL、Redis 和附件使用 Docker 命名卷持久化。后端启动时自动执行 Flyway 迁移并初始化 Flowable 表。

```bash
# 查看运行状态和后端日志
docker compose -f itsm-server/deploy/docker/docker-compose.yml ps
docker compose -f itsm-server/deploy/docker/docker-compose.yml logs -f itsm

# 停止所有服务并保留数据卷
docker compose -f itsm-server/deploy/docker/docker-compose.yml down

# 再次启动
docker compose -f itsm-server/deploy/docker/docker-compose.yml up -d
```

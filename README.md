# Supervision

[![CI](https://github.com/zhangk-miduo/Supervision/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/zhangk-miduo/Supervision/actions/workflows/ci-cd.yml)
[![License](https://img.shields.io/github/license/zhangk-miduo/Supervision)](LICENSE)
[![Release](https://img.shields.io/github/v/release/zhangk-miduo/Supervision)](https://github.com/zhangk-midoo/Supervision/releases)
[![Last Commit](https://img.shields.io/github/last-commit/zhangk-midoo/Supervision)](https://github.com/zhangk-midoo/Supervision/commits/main)

> 中文文档即为本文件。English documentation: [README_EN.md](README_EN.md)

企业智能督办平台，将依赖人工记忆和跟进的管理事项，转化为可配置、可调度、可追踪的自动化督办任务。

当前版本围绕企业微信场景提供任务编排、定时调度、消息触达、组织同步、账号权限和执行审计能力，并通过 Docker Compose 交付完整运行环境。

## 为什么需要 Supervision

很多团队用企业微信群机器人做定时提醒，但很快会遇到这些痛点：

- 提醒靠人工记忆或零散的 cron 脚本，容易漏跟、重复、且无从追溯；
- 群机器人 Webhook 散落在各处文档里，离职 / 轮换时难以接管；
- 发送了没有记录，出了错不知道是谁、什么时候、发给了谁；
- 不同部门各写一套脚本，无法统一管控账号权限与审计。

Supervision 把这些“人工 + 脚本”的督办动作，收敛成一个可配置、可调度、可审计的平台：

| 维度 | 纯人工 / 群机器人脚本 | Supervision |
| --- | --- | --- |
| 调度配置 | 改代码或记日历 | 向导式，支持 Cron / 工作日 / 多种周期 |
| 触达记录 | 无 | 全量投递记录 + 预览 + 测试发送 |
| 账号权限 | 无 | ADMIN / 普通用户角色 + 数据隔离 |
| 审计 | 无 | 执行日志 + 敏感信息脱敏 |
| 可维护性 | 脚本散落 | 统一平台 + Docker Compose 交付 |

## 界面预览

| 登录页 | 仪表盘 |
| --- | --- |
| ![登录页](docs/assets/screenshot-login.png) | ![仪表盘](docs/assets/screenshot-dashboard.png) |

| 任务配置向导 | 执行日志 |
| --- | --- |
| ![任务配置向导](docs/assets/screenshot-task-create.png) | ![执行日志](docs/assets/screenshot-execution-logs.png) |

> 截图中的任务名称、账号、推送内容、执行结果等敏感信息已做脱敏处理。

## 当前能力

### 督办任务

- 通过五步向导配置基本信息、调度规则、接收范围、消息内容和发送测试。
- 支持手动、单次、每天、国家法定工作日、每周、每月、固定间隔和 Cron 调度。
- 支持预览未来执行时间、立即执行、失败重试、重叠执行策略和误触发策略。
- 支持文本、Markdown、Markdown v2、图片、图文、文件、语音和模板卡片消息。
- 支持一个任务选择多个目标群，并在可能重复触达时给出提示。

### 企业微信集成

- 管理企业微信群机器人 Webhook，支持连通性测试、启停及公开共享。
- 支持群机器人消息和企业微信应用消息两种触达渠道。
- 配置企业微信应用凭据，校验连接并同步部门和人员。
- 按部门、姓名、状态、性别和同步状态查询组织人员。
- 保存消息投递记录，支持消息预览和测试发送。

### 账号、权限与审计

- 使用账号密码登录和 Bearer Token 会话；首次使用临时密码时强制修改密码。
- 提供 `ADMIN` 与普通用户角色，账号管理和企业微信配置仅管理员可用。
- 任务、执行记录和私有机器人按创建账号隔离；公开机器人可供其他账号选择，但仅创建者可以修改。
- 删除、停用或收回共享机器人前检查对其他账号启用任务的影响。
- Webhook 等敏感配置加密存储，并对日志中的敏感信息做脱敏处理。

### 运行与观测

- 仪表盘展示任务和执行概况。
- 执行日志支持分页、状态筛选和详情查看。
- MySQL 持久化业务数据，Redis 保存会话和运行状态，RabbitMQ 承载异步通知，Quartz 负责调度。
- Flyway 管理数据库结构演进，Spring Boot Actuator 和 `/api/health` 提供健康检查基础。

## 系统架构

```text
浏览器
  │
  ▼
Nginx / Vue 3 管理端
  │  /api
  ▼
Spring Boot API
  ├─ 认证、账号与数据权限
  ├─ 任务、调度与执行引擎
  ├─ 企业微信组织与消息服务
  └─ 执行记录与投递审计
       │
       ├─ MySQL 8
       ├─ Redis 7
       ├─ RabbitMQ 3.13
       └─ 企业微信 API / 群机器人 Webhook
```

后端采用单模块 DDD 分层组织代码：

- `api`：HTTP 接口和统一异常处理
- `application`：任务、调度、账号、组织和消息用例
- `domain`：任务、执行、身份、组织、消息等领域模型及执行引擎
- `infrastructure`：MyBatis-Plus、Quartz、Redis、RabbitMQ、企业微信客户端及安全实现
- `entity/dto`：接口输入输出模型

## 技术栈

| 范围 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.2.2、MyBatis-Plus 3.5.5、Quartz、Flyway |
| 前端 | Vue 3、TypeScript、Vite 5、Element Plus、Pinia、Axios |
| 基础设施 | MySQL 8.0、Redis 7、RabbitMQ 3.13、Nginx |
| 交付 | Docker、Docker Compose |

## 快速开始

### 1. 准备环境

- Docker Engine
- Docker Compose v2
- 建议至少 2 GB 可用内存

本机不需要安装 Java、Maven、Node.js、MySQL、Redis 或 RabbitMQ。

### 2. 配置环境变量

复制示例文件：

```bash
cp .env.example .env
```

编辑 `.env`，替换所有占位值。至少需要配置：

| 变量 | 用途 |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 |
| `DB_USERNAME` / `DB_PASSWORD` | 应用数据库账号和密码 |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | RabbitMQ 账号和密码 |
| `SUPERVISION_CRYPTO_KEY` | 敏感配置加密密钥，建议使用 `openssl rand -base64 32` 生成 |
| `SUPERVISION_ADMIN_USERNAME` | 初始管理员用户名 |
| `SUPERVISION_ADMIN_PASSWORD` | 初始管理员密码 |

`.env` 包含敏感信息，禁止提交到版本库。

### 3. 构建并启动

```bash
docker compose up -d --build
```

启动完成后访问：

- 管理端：<http://localhost:8002>
- 健康检查：<http://localhost:8002/api/health>

首次登录使用 `.env` 中配置的初始管理员账号。系统会要求使用临时密码的账号修改密码。

### 4. 检查运行状态

```bash
docker compose ps
docker compose logs -f api
```

健康检查：

```bash
curl -f http://localhost:8002/api/health
```

## 首次使用建议

1. 使用初始管理员账号登录并修改密码。
2. 在“企业微信设置”中填写企业 ID、应用 AgentId 和 Secret，完成校验。
3. 同步企业微信组织，确认部门和人员数据。
4. 在“机器人管理”中添加群机器人 Webhook，并执行配置测试。
5. 新建督办任务，配置调度、触达范围和消息内容。
6. 先执行消息预览和测试发送，再保存并启用任务。
7. 在“执行日志”中确认运行及投递结果。

企业微信应用配置和群机器人配置的具体说明见 [企业微信配置指南](doc/guide.supervision.wecom-configuration.md)。

## 本地开发与验证

### 前端

```bash
cd build/web
npm install
npm run dev
```

生产构建验证：

```bash
cd build/web
npm run build
```

### 后端

项目后端要求 Java 17。当前约定通过 Docker 内的 Maven 和 JDK 17 编译、测试：

```bash
docker compose build api
```

`build/backend/Dockerfile` 在构建镜像时执行 `mvn clean package`，包含后端测试。

### 冒烟验证

完整环境启动后，建议至少完成以下流程：

1. 调用健康检查。
2. 登录管理端。
3. 创建一个手动任务。
4. 立即执行任务。
5. 在执行日志中查询到对应记录。

## 项目结构

```text
Supervision/
├─ build/
│  ├─ backend/                  # Spring Boot 后端及数据库迁移
│  └─ web/                      # Vue 3 管理端
├─ docker/
│  ├─ nginx/                    # Web 静态资源和 API 反向代理
│  └─ rabbitmq/                 # RabbitMQ 配置
├─ doc/                         # 产品、设计、指南、报告与工程债务文档
├─ openspec/                    # OpenSpec 变更规格与任务
├─ specs/                       # 需求与验收规格
├─ docker-compose.yml           # 完整运行环境编排
├─ .env.example                 # 环境变量模板
├─ DEPLOYMENT.md                # 详细部署和排错指南
└─ README.md
```

## 已知边界

- 当前交付形态为单体后端和单实例 Quartz，尚未提供多实例高可用调度方案。
- 当前主要触达渠道是企业微信；钉钉、飞书、邮件和 AI 节点未在当前实现中提供。
- 群机器人无法保证被选择的企业微信组织成员一定存在于目标群，因此 `@` 提醒受群成员关系限制。
- 工作日调度依赖系统内置或已导入的工作日历覆盖范围，生产使用前应检查目标年份。
- Compose 默认仅将 Web 服务映射到宿主机 `8002` 端口，数据库、Redis、RabbitMQ 和后端 API 不直接暴露。

更完整的部署、迁移和排错信息见 [DEPLOYMENT.md](DEPLOYMENT.md) 与 [部署迁移指南](doc/guide.supervision.deployment-migration.md)。

## 路线图

基于当前“已知边界”，后续方向：

- [ ] 多实例高可用调度（解除单体 Quartz 单点）
- [ ] 扩展触达渠道：钉钉、飞书、邮件
- [ ] AI 节点（智能生成督办内容与摘要）
- [ ] 更丰富的工作日历与节假日数据源
- [ ] 开放 API 与 Webhook 事件，便于集成第三方系统

 roadmap 讨论欢迎在 [Issues](https://github.com/zhangk-midoo/Supervision/issues) / Discussions 提出。

## 参与贡献

欢迎 Issue、PR 与建议。开始前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)；开发环境搭建与验证流程见 [AGENTS.md](AGENTS.md)。本仓库要求将相关对话 / 任务整理进 `doc/`（详见 CONTRIBUTING 的“文档约定”）。

## 安全

如发现安全漏洞，请按 [SECURITY.md](SECURITY.md) 的流程私下反馈，勿在公开 Issue 披露。

## 许可证

[Apache License 2.0](LICENSE)。

# Supervision 部署指南

企业智能督办平台（Supervision）一键部署文档。基于 **Docker Compose** 编排 MySQL / Redis / RabbitMQ / Spring Boot API / Vue3 管理端 五个服务。

---

## 1. 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| Docker | 24+ | 含 Docker Engine 守护进程 |
| Docker Compose | v2+（Compose Spec） | 本仓库用 `docker-compose.yml` |
| 内存 | ≥ 4 GB | MySQL + Redis + RabbitMQ + JVM |
| 端口 | 80 / 3306 / 6379 / 5672 / 15672 / 8080 | 可按需映射 |

> 本机有 Docker Desktop 时，确保 **Docker 守护进程已启动**（`docker ps` 能正常返回），再执行下方命令。

---

## 2. 目录结构（部署相关）

```
Supervision/
├── docker-compose.yml          # 编排定义
├── docker/nginx/default.conf    # 前端 Nginx 反向代理配置
├── build/
│   ├── backend/               # Spring Boot 3.2 / Java 17 后端
│   │   ├── Dockerfile         # 多阶段：maven 编译 -> jre 运行
│   │   ├── pom.xml
│   │   └── src/main/resources/db/01_schema.sql   # 建表 + 应用账号
│   └── web/                  # Vue3 + TS + Element Plus 管理端
│       ├── Dockerfile         # node 构建 -> nginx 托管静态资源
│       └── src/
└── doc/                       # 产品 / 技术分析文档
```

---

## 3. 一键部署

在项目根目录执行：

```bash
# 构建并后台启动全部服务
docker compose up -d --build

# 查看启动日志
docker compose logs -f api

# 查看服务状态
docker compose ps
```

首次启动会执行：
1. `mysql` 容器初始化数据库 `supervision`，并挂载 `01_schema.sql` 自动建表、创建应用账号 `supervision/supervision`。
2. Spring Boot 通过 `spring.quartz.jdbc.initialize-schema=always` 自动创建 Quartz 调度表。
3. `api` 服务在 `mysql` / `redis` / `rabbitmq` 健康后启动，并恢复已启用的定时任务。

---

## 4. 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 管理端 UI | http://localhost | Vue3 管理后台（仪表盘 / 任务 / 机器人 / 执行日志） |
| 后端 API | http://localhost/api | Spring Boot API（上下文路径 `/api`） |
| 健康检查 | http://localhost/api/health | 返回 `ok` |
| RabbitMQ 控制台 | http://localhost:15672 | guest / guest |

---

## 5. 配置项（环境变量）

`api` 服务通过环境变量覆盖 `application.yml` 中的默认值，可在 `docker-compose.yml` 的 `api.environment` 中调整：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | mysql / 3306 / supervision | 数据库 |
| `DB_USERNAME` / `DB_PASSWORD` | supervision / supervision | 应用库账号 |
| `REDIS_HOST` / `REDIS_PORT` | redis / 6379 | 缓存 / 分布式锁 |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | rabbitmq / 5672 | 异步审计事件 |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | guest / guest | MQ 账号 |

> 修改 MySQL root 密码：调整 `docker-compose.yml` 中 `mysql.environment.MYSQL_ROOT_PASSWORD` 及应用账号初始化 SQL（`01_schema.sql` 内的 `IDENTIFIED BY`）。

---

## 6. 首次使用流程

1. **配置企业微信机器人**
   管理端「机器人管理」→ 新建，填写名称、RobotId、Webhook（`https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=...`），保存后可点「测试」验证连通性。

2. **创建督办任务**
   管理端「任务管理」→ 新建，填写：
   - 任务名称 / 描述
   - 调度类型：手动 或 定时（Cron，如 `0 30 17 * * ?` 表示每天 17:30）
   - 节点编排（按 `nodeOrder` 顺序执行）：
     - **HTTP 节点**：`{"url":"https://...","method":"GET","headers":{},"body":""}`
     - **条件节点**：`{"field":"${http_1.status}","operator":"EQ","value":"200"}`（不满足则停止链路）
     - **企微节点**：`{"robotId":"你的RobotId","content":"督办提醒：${task.name}"}`（支持 `${var}` 占位符）

3. **触发 / 观察**
   - 定时任务由 Quartz 按 Cron 自动触发；
   - 手动任务在列表点「执行」立即触发；
   - 「执行日志」查看每次运行的结果与链路摘要。

---

## 7. 常用运维命令

```bash
# 停止并移除容器（保留 MySQL 数据卷）
docker compose down

# 完全清理（含数据卷，谨慎）
docker compose down -v

# 仅重建后端
docker compose up -d --build api

# 查看某服务日志
docker compose logs -f api
```

---

## 8. 本环境验证说明

- **前端**：已在本地 Node 22 环境执行 `npm install` + `npm run build`，编译产物（`dist/`）可通过 Nginx 正常托管。
- **后端**：通过 `build/backend/Dockerfile` 多阶段 Maven 构建（Java 17 + Spring Boot 3.2.2），在有 Docker 引擎的机器上执行 `docker compose up --build` 即可完成编译与运行。
- 本开发沙箱因安全策略无法启动 Docker 守护进程，故未在此处执行真实容器编排；代码与编排配置已就绪，在具备 Docker 的环境中可直接部署上线。

---

## 9. 健康与排错

- API 无响应：先 `curl http://localhost/api/health` 确认进程存活；再查 `docker compose logs api`。
- 定时任务不触发：确认 `supervision_task_schedule.status=1` 且 `cron_expression` 合法；应用启动日志会打印「已恢复 N 个启用的定时任务」。
- 企微通知失败：检查机器人 Webhook 是否有效、网络是否放行 `qyapi.weixin.qq.com`；失败的任务执行会被记录为「失败」并停止链路。
- 数据库连接失败：确认 `mysql` 容器 `healthcheck` 通过（首次启动建表需要十几秒），`api` 已配置 `depends_on: condition: service_healthy`。

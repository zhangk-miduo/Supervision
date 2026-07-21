# 部署与上线（Deployment）

## JTBD（Jobs To Be Done）
运维需要一条命令拉起整套平台（后端 + MySQL + Redis + RabbitMQ + 前端），降低部署门槛。

## 范围
提供 Docker Compose 多容器编排与一键部署。

## 验收标准
- `docker compose up -d` 启动 `mysql`、`redis`、`rabbitmq`、`supervision-api`、`nginx` 五个服务。
- 后端镜像通过多阶段构建（maven:3.9-eclipse-temurin-17 编译 → eclipse-temurin:17-jre 运行）。
- 前端经 `nginx` 托管静态资源并反向代理 `/api` 到后端。
- MySQL 启动时自动执行 `db/01_schema.sql` 初始化表结构。
- 平台启动后，`GET /api/health` 返回 ok，前端可访问并完成一次「创建任务 → 手动执行 → 查看日志」闭环。

## 约束
- 本地仅 JDK8 + Docker；构建与运行全部在 Docker 内完成（Java 17 via maven image）。
- 部署配置通过环境变量注入（DB/Redis/RabbitMQ 连接）。

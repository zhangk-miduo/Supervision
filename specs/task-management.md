# 任务管理（Task Management）

## JTBD（Jobs To Be Done）
督办管理员需要以低门槛方式创建/编辑/启停/删除自动化督办任务，并能在调试或应急时手动触发执行，而无需编写代码。

## 范围
提供督办任务的 CRUD 与手动执行能力。

## 验收标准
- 通过 `/api/tasks` POST 创建任务，`GET` 分页查询任务列表，`PUT /api/tasks/{id}` 更新，`DELETE /api/tasks/{id}` 删除。
- 任务含 `status`（1启用/0停用）、`schedule_type`（0手动/1定时）、`name`、`description`、`created_by` 字段。
- `POST /api/tasks/{id}/execute` 立即触发一次执行，返回 `executionId`，并在 `supervision_task_execution` 落一条记录。
- 手动执行时不依赖 Quartz，直接调用执行引擎按顺序跑节点链。

## 约束
- 单 Maven 模块 Spring Boot 应用，DDD 包分层（`api/application/domain/infrastructure/entity`）。
- 持久层 MyBatis-Plus，表 `supervision_task`。
- 包名前缀 `com.company.supervision`。

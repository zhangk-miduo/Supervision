# 定时调度（Quartz Scheduling）

## JTBD（Jobs To Be Done）
系统需要按 Cron 表达式自动触发督办任务，替代人工记忆与检查。

## 范围
提供基于 Quartz 的 Cron 定时触发与启停管理。

## 验收标准
- 任务 `schedule_type=1` 时，在 `supervision_task_schedule` 存 `cron_expression` 与 `status`。
- Quartz Job 触发时加载任务节点链 → 调用 `TaskExecutionEngine` 执行。
- 任务启用/停用时，同步注册/移除 Quartz Trigger。
- 支持集群模式（Quartz 持久化到 MySQL）。

## 约束
- 使用 Quartz（非 XXL-JOB），与 Spring Boot 集成。
- Cron 表达式示例：每天 17:30 → `0 30 17 * * ?`。

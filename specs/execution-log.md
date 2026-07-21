# 执行日志与监控（Execution Log）

## JTBD（Jobs To Be Done）
运维与管理员需要查询每次任务执行的状态、结果与时间，做到全程留痕、可追溯。

## 范围
提供执行记录的查询与状态追踪。

## 验收标准
- `GET /api/executions` 分页查询执行记录（task_id/status/起止时间过滤）。
- `supervision_task_execution` 记录 `status`（0成功/1失败/2执行中）、`result`、`start_time`、`end_time`。
- 执行成功/失败均落库；异常时 `status=1` 且 `result` 含错误信息。

## 约束
- 与手动执行、定时执行共用同一执行记录表。
- 列表接口支持按任务名/状态/时间范围过滤。

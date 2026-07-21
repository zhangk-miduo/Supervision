# 流程节点与执行引擎（Node & Execution Engine）

## JTBD（Jobs To Be Done）
系统需要把任务拆成有序节点（HTTP → Condition → Wechat）依次执行，前序节点结果作为后续节点上下文，实现「查询—判断—通知」自动化闭环。

## 范围
提供三类节点的策略式执行器与轻量执行引擎。

## 验收标准
- 定义 `NodeExecutor` 接口：`NodeResult execute(TaskNode node, ExecutionContext ctx)`。
- `HttpNodeExecutor`：按 config（url/method/headers/body）调用外部接口，结果写入上下文。
- `ConditionNodeExecutor`：基于 `field/operator/value` 判断（支持 =,!=,>,<,>=,<=,contains,empty），决定后续分支是否继续。
- `WechatNodeExecutor`：通过 `WechatClient` 调用企业微信机器人 Webhook 发送通知。
- 执行引擎 `TaskExecutionEngine`：加载某任务的节点链（按 `node_order` 排序），依次执行，节点间通过 `ExecutionContext` 传递数据；全程写入 `supervision_task_execution`。
- 临时上下文存 Redis：`supervision:task:context:{executionId}`，带过期时间；分布式锁 `supervision:task:lock:{taskId}` 防并发。

## 约束
- 节点类型枚举：`http / condition / wechat`。
- 节点配置以 JSON 存于 `supervision_task_node.config`。
- 不引入 BPM/n8n 等重型引擎。

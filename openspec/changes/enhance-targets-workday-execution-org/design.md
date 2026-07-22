# 设计文档：增强企微目标、国家工作日、执行可观测性与组织视图

## 1. 总体关系

```text
逻辑群 WecomGroup
└─ 消息推送 WecomWebhook (1..N)
       ▲
       │ webhookIds
任务消息定义
       │
       ▼
一次 TaskExecution
├─ MessageDelivery：目标 A
├─ MessageDelivery：目标 B
└─ MessageDelivery：目标 C
       │
       ▼
执行日志聚合：成功数 / 失败数 / 可读原因
```

国家工作日日历独立于 Quartz 规则：Quartz 产生每日候选触发，运行守卫和未来时间预览共同调用同一个 `NationalWorkdayCalendar`，避免预览与执行口径不一致。

## 2. 群与消息推送模型

新增 `supervision_wecom_group`：

```text
id, tenant_key, group_name, status, remark, created_at, updated_at
```

扩展 `supervision_wecom_webhook`：

```text
group_id, push_name, system_code, webhook_cipher,
status, last_tested_at, created_at, updated_at
```

- `system_code` 由系统生成，用于兼容旧 `robotId`，不作为用户输入。
- 群名是管理员维护的逻辑分组；Webhook 不能作为群名称发现接口。
- 推送选择器以 Webhook ID 为稳定值，标签为 `群名 · 推送名称`，按群分组。
- 同群同时选择多个推送时展示重复触达警告；默认每群限选一个，可由明确的高级确认解除。
- 删除群前必须确认无推送；已被任务引用的推送使用停用而非物理删除。

旧数据迁移：旧 `name` 迁入 `push_name`，创建“待补充群名”的迁移群并提示管理员补齐；保留旧 `robot_id` 映射，读取旧任务单个 `webhookId` 时转换为单元素列表。

## 3. 多目标消息定义与发送

消息定义改为：

```json
{
  "channel": "GROUP_WEBHOOK",
  "webhookIds": [12, 18, 23],
  "messageType": "text",
  "content": { "content": "..." },
  "mentionMode": "NONE"
}
```

保存时批量验证目标存在、启用且可解密。执行时对每个目标独立创建 Delivery，幂等键为：

```text
task-{taskId}-execution-{executionId}-webhook-{webhookId}
```

默认策略为继续发送剩余目标。聚合状态：

- `SUCCESS`：全部目标成功。
- `PARTIAL_SUCCESS`：至少一个成功且至少一个失败。
- `FAILED`：所有目标失败，或执行前校验失败且未产生有效投递。
- `RUNNING`：仍有目标发送或重试中。

目标级重试只处理失败/可重试目标。投递记录保存群名、推送名、消息摘要快照及脱敏失败详情。

## 4. 国家工作日

新增年度覆盖表和例外日期表：

```text
workday_calendar_year:
year, status, source_url, source_document, version,
published_at, imported_at, checksum

workday_calendar_exception:
year, date, day_type(WORKDAY/HOLIDAY), holiday_name, note
```

判定规则：

1. 年度必须处于已发布、校验通过状态。
2. 例外日期优先。
3. 无例外时周一至周五为工作日，周六周日为休息日。
4. 年度覆盖缺失时返回 `UNKNOWN`，不得伪装成普通工作日。

`WORKDAY` 模式保存每天一个或多个执行时间。Quartz 使用每日 Cron 产生候选，`SupervisionJob` 执行前查询日历：`WORKDAY` 执行，`HOLIDAY` 记录跳过，`UNKNOWN` 跳过并产生管理员告警。未来五次预览使用同一判定器。

年度数据由受版本控制的数据包导入，元数据指向国务院年度通知。系统设置展示覆盖年份、来源、校验和与更新时间，并在覆盖不足时告警。

## 5. 执行快照与可读日志

为保证历史真实性，执行开始时保存：

```text
task_name_snapshot, trigger_type,
message_type_snapshot, message_summary_snapshot,
target_count, schedule_decision, schedule_decision_reason
```

投递记录保存：

```text
group_id_snapshot, group_name_snapshot,
push_name_snapshot, content_summary_snapshot,
normalized_code, normalized_message, technical_detail_redacted
```

列表 API 返回专用 DTO，不直接暴露实体：任务名称、触发方式、聚合状态、目标成功/总数、消息摘要、起止时间和耗时。详情 API 返回任务/消息快照、逐目标投递、重试时间线和权限控制的脱敏技术详情。

错误翻译层把企微及基础设施异常标准化：目标失效、限流、网络超时、凭据/权限、格式校验、未知错误。原始响应只作为折叠技术详情，必须脱敏。

修正现有状态筛选反转：后端 `SUCCESS=0`、`FAILED=1`、`RUNNING=2`，新增部分成功时应使用稳定字符串状态 DTO，前端不再自行解释数字。

## 6. 组织人员管理

页面布局：

```text
同步概览与筛选工具栏
├─ 最近同步状态/时间
├─ 姓名或企微账号、成员状态、性别、同步状态
└─ 查询、重置、同步企微、同步日志

左：组织树与人数       右：成员列表 / 部门详情
```

人员扩展：`gender`、`sync_status`、`sync_error_redacted`、`primary_department_id`；保留 `wecom_user_id`，界面称“企微账号”。若企业需要工号，后续通过配置映射企微扩展字段，不能默认等同 userid。

部门树接口返回直接/递归成员数和状态；成员分页接口支持部门、关键词、成员状态、性别、同步状态筛选。部门选择只过滤，不提供手工维护。成员详情展示多部门关系和最近同步信息。

“账号状态”统一改为“成员状态/在职状态”，登录账号仍由账号管理模块维护。同步部分失败时保留历史成员并记录脱敏失败，不创建本地临时人员。

## 7. 安全与权限

- Webhook 只加密存储，选择器和日志不返回密文、完整 URL 或 key。
- 手机、邮箱继续脱敏，查看更详细资料需明确权限。
- 日历导入、企微同步、Webhook 管理和技术详情查看分别审计。
- 任务保存与执行均重新验证目标，防止配置停用后的错误发送。

## 8. 发布与兼容

1. 先增加新表/新列和兼容读取，不删除旧字段。
2. 迁移机器人与任务消息定义，旧执行记录标记快照不完整。
3. 发布后引导管理员补齐旧推送的群名。
4. 完成多目标、工作日、日志和组织页面上线验证。
5. 至少一个稳定发布周期后，再单独提出旧 `robotId`、单 `webhookId` 和原始 result 展示路径清理。

## 9. 验证策略

- 单元测试：日历判定、候选预览、目标级幂等、聚合状态、错误翻译。
- 迁移测试：旧机器人、旧单目标任务、旧执行记录和重复迁移。
- 集成测试：三个目标混合结果、目标级重试、工作日调休、年度缺失、组织筛选计数。
- 前端测试：分组多选、同群重复警告、日志详情、组织树与筛选状态。
- 真实验收：至少两个企微群、同群两个推送、法定休息日样例、周末调休样例及同步组织数据。

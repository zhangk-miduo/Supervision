# 设计文档：增强调度、账号体系与企业微信集成

## 1. 总体架构

```text
浏览器
├─ 登录 / 首次改密 / 账号管理
├─ 组织人员 / 同步日志
├─ 系统设置 / 企微配置
└─ 任务向导 / 调度 / 接收范围 / 消息编辑
        │
        ▼
Spring Boot API
├─ Identity：Account / Role / Session / LoginAudit
├─ Organization：Department / Person / SyncLog
├─ Integration：WeComConfig / Token / DirectoryClient
├─ Messaging：Channel / Template / Recipient / Delivery
├─ Scheduling：ScheduleRule / TriggerCompiler / Quartz
└─ Task：Task / Node / Execution
        │
        ├─ MySQL：业务事实与审计
        ├─ Redis：登录状态、企微 Token、锁与限流
        ├─ Quartz：调度触发
        └─ 企业微信：通讯录、应用消息、群消息推送
```

领域间只通过应用服务和稳定 ID 关联。账号不继承企微人员生命周期；任务消息不直接依赖页面提交的企微原始 JSON。

## 2. 关键决策

### 2.1 账号与人员分离

- `Account` 负责登录、密码、角色、锁定、状态和审计。
- `Person` 负责企微 userid、姓名、部门、职位、联系方式和触达。
- `Account.person_id` 可空且唯一，一个人员最多绑定一个账号。
- 企微同步只维护 Person/Department，不创建或删除 Account。
- 人员离职后本地标记停用；若绑定账号则产生安全告警，不自动停用账号，避免外部同步误伤管理员账号。

### 2.2 调度保存业务语义

调度表保存版本化规则，而非仅保存编译后的 Cron。应用服务把规则编译为一个或多个 Quartz Trigger。复杂规则无法由单个 Cron 表达时，使用多个 Trigger 或日历过滤器。

推荐字段：

```text
schedule_mode, rule_version, rule_json, timezone,
start_at, end_at, misfire_policy, overlap_policy,
retry_policy_json, holiday_policy, status, next_fire_time
```

规则更新采用“校验 → 预计算未来触发 → 事务保存 → 替换 Quartz Trigger”。替换失败不得留下数据库规则与调度器不一致的半完成状态。

### 2.3 企微双通道

- `GROUP_WEBHOOK`：发送到固定群，支持群内 @，不能保证组织人员一定在群中。
- `APP_MESSAGE`：使用自建应用发送给成员、部门或标签，不依赖群成员关系。

统一消息定义保存 `channel`、`message_type`、`content_json` 和版本。渠道适配器负责转换为企微请求体，业务层不拼接原始 JSON。

### 2.4 敏感配置

- 密码使用自适应哈希算法保存，禁止可逆加密密码。
- Secret 与 Webhook 使用应用级密钥加密，接口只返回掩码和“是否已配置”。
- access_token 仅缓存在 Redis，并在企微返回 Token 失效错误时执行一次安全刷新重试。
- 日志统一剥离密码、Authorization、Secret、Webhook key 和完整手机号。

## 3. 数据模型

### 身份

- `supervision_account`
- `supervision_role`
- `supervision_account_role`
- `supervision_login_audit`

账号包含 `username`、`password_hash`、`person_id`、`must_change_password`、`failed_login_count`、`locked_until`、`last_login_at` 和状态。

### 组织

- `supervision_department`
- `supervision_person`
- `supervision_person_department`
- `supervision_org_sync_log`

部门和人员以 `(tenant_key, wecom_external_id)` 唯一。即使首期为单企业，也保留 `tenant_key`，避免未来迁移主键。

### 企微与消息

- `supervision_wecom_config`
- `supervision_wecom_webhook`
- `supervision_message_template`
- `supervision_task_recipient`
- `supervision_message_delivery`

投递记录保存任务、执行、渠道、目标摘要、消息类型、幂等键、请求批次、企微返回码、状态、失败原因、重试次数和时间。

### 调度

扩展 `supervision_task_schedule`，必要时新增 `supervision_task_trigger` 保存一个规则编译出的多个 Trigger。Quartz 内部表仍由框架维护。

## 4. 认证流程

```text
管理员创建账号 + 临时密码
        │
        ▼
密码哈希入库，must_change_password=true
        │
首次登录成功
        ▼
仅签发 CHANGE_PASSWORD_ONLY 会话
        │
修改密码 + 撤销旧会话
        ▼
must_change_password=false，正常登录
```

连续失败达到阈值后短时锁定。管理员重置密码会撤销现有会话，并重新要求首次改密。所有创建、停用、重置和角色变更写入审计。

## 5. 组织同步流程

```text
管理员点击同步
→ 获取分布式同步锁
→ 验证企微配置并获取 Token
→ 拉取部门
→ 按允许范围批量拉取人员
→ 在事务中 upsert 部门、人员和关系
→ 未出现在本次完整快照中的对象标记停用
→ 写入同步统计和错误明细
```

同步需要幂等。部分拉取失败时不得执行“缺失即停用”；只有完整快照成功后才允许收敛本地状态。后续增量回调复用相同 upsert 服务。

## 6. 调度交互与策略

前端提供手动、一次、每天、每周、每月和高级 Cron。普通模式不显示 Cron，只显示人类可读摘要和未来五次执行时间。

- 错过执行：忽略、立即补跑、只补最近一次。
- 重叠执行：跳过、排队；首期不建议允许并行。
- 节假日：首期支持工作日过滤扩展点，不内置未经维护的法定节假日表。
- 重试属于执行策略，不通过额外 Cron 模拟。
- 所有时间以规则时区解释，数据库保存 UTC 时间点。

## 7. 消息编辑与投递

任务向导分为基本信息、调度规则、接收范围、消息内容、预览测试五步。消息类型使用结构化组件：

- P0：文本、Markdown/Markdown v2、图文、文本通知模板卡片。
- P1：图片、文件、图文展示模板卡片。
- P2：语音。

群消息的人员提醒保存企微 userid。文本可使用 mentioned_list；Markdown 根据接口能力生成提醒语法；Markdown v2 不提供不受支持的提醒方式。@所有人作为互斥选项，避免同时保存大量人员。

发送前创建带幂等键的 Delivery。适配器返回标准结果，失败按可重试与不可重试分类。群消息推送地址执行每分钟 20 条的本地限流，并保留企微限流响应。

## 8. API 轮廓

```text
POST /api/auth/login
POST /api/auth/change-password
POST /api/auth/logout
GET/POST/PUT /api/accounts
POST /api/accounts/{id}/reset-password

GET /api/departments/tree
GET /api/persons
POST /api/wecom/sync
GET /api/wecom/sync-logs
GET/PUT /api/settings/wecom
POST /api/settings/wecom/verify

POST /api/schedules/preview
POST /api/messages/preview
POST /api/messages/test-send
GET /api/message-deliveries
```

现有任务创建接口升级为结构化 schedule、recipients 和 messageDefinition；迁移期可短暂兼容 `cronExpression` 和旧 WECHAT 节点配置，但新建任务只写新模型。

## 9. 迁移与兼容

1. 先修复当前前端 `scheduleType` 判断和消息类型未生效问题。
2. 数据库迁移创建新表和新列，现有任务转换为 `MANUAL` 或 `CRON` 规则。
3. 现有机器人迁移为 `GROUP_WEBHOOK` 配置，Webhook 加密后清除明文列。
4. 提供一次性初始管理员创建方式，不能在生产使用固定默认密码。
5. 新旧任务读取兼容一个发布周期，确认迁移后移除旧 JSON 入口。

## 10. 风险与应对

- 企微权限范围不足：配置验证返回可读诊断，同步日志记录缺少的权限或范围。
- 群成员不可查询：选择器旁持续展示限制，精确送达推荐应用消息。
- 同步误停用：仅完整快照成功后执行停用收敛。
- 凭据泄漏：加密、掩码、日志脱敏、最小权限和轮换审计。
- 调度重复执行：幂等执行键、分布式锁、重叠策略和 Quartz misfire 策略共同约束。
- 大批量收件人：后台分批、限流、投递明细，不在单个同步请求中阻塞页面。

## 11. 验证策略

- 单元测试：密码状态机、调度规则编译、未来触发计算、同步收敛、消息请求映射。
- 集成测试：认证权限、MySQL 迁移、Redis Token、Quartz Trigger、消息投递幂等。
- 契约测试：用企微官方请求样例验证八种群消息结构。
- 真实联调：测试企业完成通讯录读取、应用消息、群消息、@人员、@所有人和限流验证。
- 回归测试：现有任务 CRUD、立即执行、执行日志和机器人测试发送。
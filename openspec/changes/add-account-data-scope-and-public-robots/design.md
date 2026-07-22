# 设计文档：账号数据隔离与公开机器人共享

## 1. 数据范围模型

```text
认证会话(accountId, roles)
          │
          ▼
DataScope(accountId, isAdmin)
          │
          ├─ READ
          │   ├─ 普通账号：owner_account_id = accountId
          │   └─ 管理员：不加 owner 过滤
          │
          ├─ MUTATE
          │   └─ owner_account_id = accountId（管理员也不自动越权）
          │
          └─ USE ROBOT
              └─ owner_account_id = accountId OR is_public = 1
```

控制器从 `AuthInterceptor.SESSION_ATTRIBUTE` 取得会话并构造不可伪造的数据范围上下文。应用服务和查询仓储显式接收该上下文，禁止从请求 DTO、查询参数或前端本地存储推断归属。

对不属于读取范围的资源 ID 统一按“资源不存在”处理，避免通过 403、不同错误文案或响应时序泄露其他账号是否创建了该资源。授权逻辑必须覆盖列表与单资源入口，不能仅依赖前端菜单或按钮。

## 2. 数据模型

### 2.1 任务

在 `supervision_task` 增加：

```text
owner_account_id BIGINT NULL
INDEX idx_task_owner_id(owner_account_id, id)
```

新建任务时从会话写入且不可修改。旧 `created_by VARCHAR(64)` 暂保留一个兼容发布周期，仅用于历史回填，不再接受客户端写入，也不再参与授权。

### 2.2 执行日志

在 `supervision_task_execution` 增加：

```text
owner_account_id BIGINT NULL
triggered_by_account_id BIGINT NULL
INDEX idx_execution_owner_id(owner_account_id, id)
```

`owner_account_id` 在执行记录创建时从任务复制，包括手动、Quartz 定时和跳过执行记录。`triggered_by_account_id` 仅在人工立即执行时记录实际操作账号，定时执行为空；它用于审计，不改变日志归属。

不能只通过当前任务表连接判断执行日志归属，因为任务当前采用物理删除，历史执行可能在任务删除后继续存在。

### 2.3 机器人、Webhook 与逻辑群

以 `supervision_wecom_webhook` 作为可调用消息推送的归属和公开状态事实来源，增加：

```text
owner_account_id BIGINT NULL
is_public TINYINT NOT NULL DEFAULT 0
INDEX idx_webhook_owner_public(owner_account_id, is_public, status)
```

`supervision_wecom_group` 同样增加 `owner_account_id`，唯一键由 `(tenant_key, group_name)` 调整为 `(tenant_key, owner_account_id, group_name)`。同一账号可在一个逻辑群下维护多个 webhook；不同账号使用相同显示群名时互不共享备注、状态或配置。webhook 所有者必须与所属逻辑群所有者一致。

旧 `supervision_wechat_robot` 与 webhook 当前按 ID 一一兼容。迁移期仍保留旧表，但所有权、公开状态和授权只以 webhook 为准，避免出现两个冲突来源。

### 2.4 账号展示

列表和详情使用专用 DTO 关联 `supervision_account`，统一返回：

```text
creatorAccountId
creatorUsername
creatorDisplayName
ownedByCurrentUser
canEdit
```

机器人额外返回 `isPublic` 和 `canUse`。账号禁用后不删除，历史关联仍可解析。默认展示当前 `display_name`；本变更不保存创建当时名称快照。

## 3. 权限矩阵

| 操作 | 本人私有机器人 | 本人公开机器人 | 他人公开机器人 | 他人私有机器人 | 管理员查看他人数据 |
|---|---:|---:|---:|---:|---:|
| 管理列表/详情查看 | 是 | 是 | 非敏感摘要 | 否 | 是，敏感值仍掩码 |
| 任务选择与任务发送 | 是 | 是 | 是 | 否 | 仅本人或公开 |
| 任务级测试发送 | 是 | 是 | 是 | 否 | 仅本人或公开 |
| 编辑/公开切换/停用 | 是 | 是 | 否 | 否 | 否 |
| 机器人配置测试 | 是 | 是 | 否 | 否 | 否 |

管理员全量查看用于审计和排查，不改变机器人调用范围，也不自动授予配置变更权。如需平台管理员应急停用，应新增独立端点、权限码、原因和审计记录。

任务权限采用相同的读取/变更分离：普通账号只读写自己的任务；管理员可以读取全部任务和执行日志，但普通更新、删除和立即执行接口仍要求任务所有者。角色动作权限在所有权校验之外另行判定，不在本变更内重构。

## 4. 查询与服务边界

### 4.1 机器人

- 管理列表：普通账号默认返回本人机器人，可通过明确的“公开可用”视图查询公开摘要；管理员审计视图返回全部。
- 可选机器人：返回启用、可解密且满足 `owner = current OR is_public = 1` 的 webhook。
- 详情：非创建者访问公开机器人只返回选择器级摘要，不进入配置编辑详情。
- 变更、停用、公开切换、配置测试：统一调用 `requireOwner`。
- 消息预览不发送，不要求机器人；任务级测试发送必须调用 `requireUsable`。

### 4.2 任务

- 列表和详情使用 `readScope`；创建写入当前账号；更新、删除和立即执行使用 `requireOwner`。
- 保存消息目标时，每个 webhook 都执行 `requireUsable`，拒绝私有他人机器人、停用项和无效密文。
- 任务详情读取时，若历史目标已收回共享，仍可显示安全快照和“当前不可用”状态，不能回显配置。

### 4.3 执行日志

- 列表和详情直接按执行表 `owner_account_id` 使用 `readScope`。
- Delivery 只可通过有权访问的 Execution 详情取得，禁止仅凭 execution ID 或 delivery ID 绕过。
- 管理员可按创建账号筛选机器人、任务和执行日志；普通账号提交其他 creatorAccountId 筛选时忽略或拒绝。

### 4.4 组织人员

`/organization/departments`、`/organization/persons` 及任务人员选择器继续对所有正常会话共享读取，不增加 `owner_account_id`。`Account.person_id` 只表示可选身份绑定，不参与任务或机器人数据范围。

## 5. 公开共享生命周期

```text
PRIVATE ──创建者公开──▶ PUBLIC ──创建者收回──▶ PRIVATE
   │                       │                       │
仅本人可用            所有账号可用于任务       外部任务后续执行失败
```

公开切换、停用或删除前，后端统计引用该 webhook 且归属为其他账号的启用任务数量。前端展示影响数量并二次确认。确认后立即改变可用性，不保留隐式永久授权。

任务保存只是第一次校验。任务级测试与每次实际发送前都必须再次校验共享状态；收回后不得发送到该 webhook，也不得静默替换目标。执行和投递记录使用稳定错误码（例如 `ROBOT_SHARE_REVOKED`、`ROBOT_DISABLED`）及用户可读原因。

公开机器人继续共享同一个 webhook 的频率限制。Delivery 通过 execution/task 归属记录实际调用账号，管理员可以审计哪个账号消耗了额度。公开状态不改变凭据加密、日志脱敏和响应掩码规则。

## 6. 历史数据迁移

迁移分阶段进行，避免错误归属造成数据泄露：

1. 新列先允许为空并建立索引，不立即添加非空约束。
2. 任务的旧 `created_by` 与账号 `username` 精确匹配时回填 owner。
3. 执行记录从已确定归属的任务复制 owner。
4. 机器人、群组及无法匹配的任务若能确定唯一初始管理员，可在部署确认后归属该账号；不能确定时保持未归属。
5. 未归属数据仅管理员可见，普通账号查询永远不包含 `owner_account_id IS NULL`。
6. 提供只读迁移报告和受控认领步骤；所有生产数据归属确认后，再以独立迁移收紧非空约束。

所有历史机器人默认 `is_public = 0`，禁止升级后意外向所有账号公开。迁移不得删除旧字段、任务、执行、机器人、Webhook 或投递历史。

## 7. API 与前端契约

建议接口保持现有路径，扩展查询与 DTO：

```text
GET /api/robots?view=owned|public|all&creatorAccountId=
GET /api/robots/selectable
GET /api/robots/{id}/usage-impact
PUT /api/robots/{id}                 # 仅所有者，可更新 isPublic

GET /api/tasks?creatorAccountId=
GET /api/executions?creatorAccountId=
```

前端机器人页提供“我创建的”“公开可用的”标签；管理员额外提供“全部”。公开可用列表只显示非敏感摘要和创建账号/名称，非所有者不渲染编辑、停用和配置测试操作。任务和执行日志增加创建账号、创建人名称列；管理员提供创建人筛选。

前端的 `canEdit` 只用于交互，不是授权依据。所有 API 仍必须在后端验证会话与所有权。

## 8. 验证策略

- 数据迁移：可匹配、不可匹配、唯一管理员、多管理员、任务已删除和旧机器人数据。
- 查询隔离：两个普通账号、管理员、未归属历史数据，覆盖分页总数和创建人筛选。
- 直接 ID 越权：任务/机器人/执行详情、更新、删除、立即执行、配置测试和 Delivery 明细。
- 共享机器人：默认私有、公开选择、非创建者发送、非创建者修改拒绝、管理员只读。
- 共享收回：保存后改私有、停用、执行期再次校验、稳定失败码、零实际发送。
- 定时执行：无请求会话时从任务复制 owner，跳过记录同样保留 owner。
- 敏感字段：公开摘要、管理员查看、错误和日志均不包含 webhook key、密文或 Secret。
- 前端：标签页、创建人列、操作隐藏、影响数量确认及共享收回错误展示。

## 9. 发布与回滚

先部署可读 nullable owner 字段和兼容查询，再回填历史归属，最后启用新前端。发布前备份 MySQL，并输出每类数据的总数、已归属数和未归属数。若升级失败，应用回滚仍可忽略新增列；不要在同一变更中删除旧 `created_by` 或旧机器人兼容表。

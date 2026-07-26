# 登录身份、组织人员与管理菜单缺陷排查报告

> **版本**：v1.0 | **日期**：2026-07-23 | **状态**：探索完成，待创建变更并实施

## 用户反馈

1. 登录后右上角显示的名称不正确。
2. 非管理员登录后，“组织人员”没有显示已经同步的数据。
3. 无管理权限的用户不应看到“账号管理”和“企业微信设置”。

## 排查结论

### 组织人员空白

组织部门和人员接口本身没有按账号隔离，设计上属于所有正常登录账号共享的企业数据。直接原因在前端 `PersonList.vue`：页面初始化先并行请求部门树和 `/wecom/sync-logs`，随后才请求人员列表。后端 `AuthInterceptor` 把所有 `/wecom/sync*` 接口限制为管理员，普通账号读取同步日志时返回权限错误，导致初始化 Promise 中断，人员列表请求没有执行。

建议把共享查询与管理员操作拆开：所有登录账号加载部门树和人员列表；只有管理员才加载同步日志并展示“同步企业微信”“查看同步日志”。即使辅助请求失败，也不能阻断人员列表。后端继续保持组织人员共享读取、同步与配置仅管理员可用。

### 管理菜单可见性

侧栏现有实现已用登录响应中的 `roles.includes('ADMIN')` 隐藏“账号管理”和“企业微信设置”，路由守卫也会阻止普通账号直接进入，后端还有接口权限校验。因此目标权限模型已经存在。

实施时应把角色判断收敛为统一的当前用户/权限状态，避免各页面分别解析 `localStorage`；同时保留侧栏隐藏、路由守卫和后端鉴权三层控制。组织人员页中的管理员按钮也应使用同一判断。需要用真实普通账号确认其登录响应角色不含 `ADMIN`，并排除浏览器遗留登录态。

### 右上角名称

前端展示登录响应缓存中的 `displayName`。后端当前直接取 `supervision_account.display_name`，即使账号设置了 `person_id`，也不会读取所绑定组织人员的姓名。因此，如果产品期望展示企微同步姓名，现有实现必然不正确；如果期望展示账号管理中维护的显示名称，则需要核对具体账号数据和旧登录缓存。

建议明确名称优先级为“绑定人员姓名 > 账号显示名称 > 登录账号”，由后端在登录/当前用户契约中返回最终展示名称，前端只消费该字段。账号或人员名称变化后，已有 Redis 会话和浏览器缓存不会自动刷新，实施方案还需要选择重新登录刷新，或新增当前用户接口动态读取。

## 影响模块

- 前端：`build/web/src/App.vue`、`build/web/src/router/index.ts`、`build/web/src/views/organization/PersonList.vue`
- 后端：`AuthService`、`AccountMapper`、`AuthInterceptor`，以及人员查询仓储
- 测试：登录展示名优先级、普通账号组织共享读取、管理员操作隐藏与直接访问拦截

## 风险与待确认项

- “正确名称”尚未给出具体期望值；实施前需确认是账号显示名称还是绑定企微人员姓名。
- 仅隐藏菜单不是授权措施，后端鉴权必须保留。
- 若继续把完整登录响应长期存入 `localStorage`，角色和名称修改后会存在陈旧状态。

## 推荐后续行动

退出探索模式后创建一个 OpenSpec 修复变更，补齐展示名契约、组织页权限拆分及自动化测试，再实施和执行前端生产构建、后端 Docker Maven 测试与管理员/普通账号冒烟。

## Implementation update (2026-07-23)

Status: implemented locally; production account smoke test remains.

- Login now resolves the header display name from the bound organization person first, then the account display name, then the username.
- Added unit coverage for the display-name priority and both fallback levels.
- The organization page loads shared department/person data independently from administrator-only synchronization logs.
- Synchronization status, manual synchronization, and log actions are hidden from non-administrators.
- Account Management and WeCom Settings retain sidebar visibility checks, route guards, and backend authorization.
- OpenSpec tasks 1.8 and 2.10 record the completed defect work.

Validation: the Vite production build passed with 1,679 transformed modules. Backend Docker build could not run because the local Docker Desktop Linux engine was not running. A deployed smoke test should use one bound normal account, one unbound normal account, and one administrator; existing sessions must sign out and sign in again to receive the refreshed display name.

No database migration is required.

## Production deployment (2026-07-23)

Status: deployed and healthy.

- Used the current production source tree as the release baseline because the production compose file contains environment-specific differences and the local worktree contains unrelated changes.
- Overlaid only `AuthService.java`, `AuthServiceTest.java`, `PersonList.vue`, and the existing deployment scripts.
- The deployment script created a source and MySQL backup at `/opt/backups/supervision-20260723-113057` before installing the release.
- The backend Docker build completed Maven clean package and tests successfully.
- The frontend Docker build completed successfully with 1,679 transformed modules.
- MySQL, Redis, RabbitMQ, API, and Web containers are healthy; API and Web restart counts are zero.
- Internal and public `/api/health` return HTTP 200 with `ok`; the public root returns HTTP 200.
- Unauthenticated organization-person access returns HTTP 401, confirming authentication remains enforced.
- No API `ERROR` log entries were present in the first five minutes after deployment.

Authenticated role and display-name acceptance still requires business account access. Existing users must sign out and sign in again so a new session receives the resolved display name.

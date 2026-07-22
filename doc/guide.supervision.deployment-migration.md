# 部署与迁移备忘录

> **版本**：v1.0 | **日期**：2026-07-22 | **状态**：待验证

## 变更内容

后端现使用 Flyway 管理 `V1` 至 `V6` 迁移。Dockerfile 构建阶段执行 Maven 测试，不再跳过测试。旧任务自动迁移为 CRON 语义规则；旧机器人复制到新的群消息推送表，运行后应确认加密重写完成。

## 必需环境变量

- `SUPERVISION_ADMIN_USERNAME`：默认 admin。
- `SUPERVISION_ADMIN_PASSWORD`：首次启动必填，不提供固定默认密码。
- `SUPERVISION_CRYPTO_KEY`：生产必须设置随机高强度值并安全备份。

## 验证命令

```powershell
docker compose build api
docker compose build web
docker compose up -d
docker compose ps
curl -f http://localhost:8002/api/health
```

## 回滚

先备份数据库，再参考 `build/backend/src/main/resources/db/migration/rollback_enhance_scheduling_auth_wecom.sql`。该脚本会删除新增业务表，仅作为人工回滚参考，不应由 Flyway 自动执行。
## 本次迁移补充

V7__scrub_legacy_webhook_plaintext.sql 会在加密 Webhook 迁移完成后，把旧 supervision_wechat_robot.webhook_url 列统一替换为掩码。上线前必须提供稳定的 SUPERVISION_CRYPTO_KEY；更换密钥前应先设计密钥轮换流程，否则已有 Secret 与 Webhook 无法解密。
## 2026-07-22 服务器部署请求

用户要求将当前实现部署到服务器。仓库已确认采用根目录 docker-compose.yml，通过 8002 端口提供 Web 与 API 服务；但本地仓库、SSH 配置和环境变量中均未发现目标服务器地址、SSH 用户、认证方式及远程部署目录，因此尚未执行远程写入或服务重启。

执行部署前还必须在目标环境提供稳定的 SUPERVISION_CRYPTO_KEY 与符合策略的 SUPERVISION_ADMIN_PASSWORD，且不得把真实凭据提交到仓库。取得连接信息后应依次执行：服务器环境检查、现有数据备份、上传当前源码、注入生产环境变量、Docker Compose 构建启动、健康检查与关键链路冒烟验证。
### 连接检查结果

2026-07-22 使用用户提供的服务器 [redacted-public-host]、ubuntu 账号尝试 SSH 连接。TCP 22 端口连接超时，后续 Test-NetConnection 也在 30 秒内超时；认证阶段未开始，服务器没有发生任何写入或服务变更。继续部署需要确认实际 SSH 端口，并在腾讯云安全组、服务器防火墙及 sshd 中允许该端口访问。用户已在对话中暴露服务器密码，恢复连接后应优先轮换密码并配置 SSH Key。
## 2026-07-22 服务器部署结果

- 目标：[redacted-public-host]，部署目录 /opt/supervision，公网端口 8002。
- 方式：保留服务器 .env 与 Docker 数据卷，迁移前执行 MySQL 逻辑备份，服务器内完成后端 Maven 编译/测试与前端 Vite 构建，再原地切换版本。
- 构建验证：后端 mvn clean package 通过并执行测试；前端 1679 个模块构建成功。
- 数据迁移：Flyway V0 至 V8 全部成功。V5 修复旧调度表已有 timezone 字段导致的重复列问题；V8 兼容旧账号表的状态、锁定、失败次数及角色字段。
- 运行状态：MySQL、Redis、RabbitMQ、API、Web 五个容器均健康。
- 公网验证：http://[redacted-public-host]:8002/ 返回 200，/api/health 返回 200/ok。
- 认证验证：管理员首次登录返回 200，签发受限会话并要求首次修改密码。
- 当前代码备份：/opt/backups/supervision-20260722-170312；另有多份迁移前数据库压缩备份位于 /opt/backups/。
- 临时密码与 SSH 凭据未写入文档。管理员完成首次登录后必须立即修改密码。
## 2026-07-22 登录故障修复

部署后用户反馈登录接口返回成功但页面提示失败。根因是 Axios 响应拦截器已返回 ApiResult，登录页仍按 AxiosResponse.data.data 读取，导致 Token 未保存；同时旧账号角色 USER 被 V8 映射成 OPERATOR。

修复内容：
- 前端响应拦截器恢复返回完整 AxiosResponse，使现有页面统一按 response.data.data 取值。
- 新增 V9__bootstrap_admin_role.sql，将初始 admin 账号绑定 ADMIN 角色并移除错误的 OPERATOR 映射。
- 撤销用户在对话中公开的会话 Token。
- 重新完成后端 Maven 编译/测试、前端 1679 模块构建和服务器部署。

复测结果：登录 API 返回 200，roles 为 ADMIN，mustChangePassword 为 true；服务器前端资源为 index-CPEk-21Y.js，测试 Token 已撤销。修复版本备份目录为 /opt/backups/supervision-20260722-172007。
## 本地一键更新脚本

本次新增 `scripts/deploy.ps1` 和 `scripts/deploy-server.sh`。本地脚本打包工作区（排除 `.git`、`.env`、依赖与构建产物）、通过 SSH 上传，并调用远端脚本完成更新。远端脚本会先备份当前源码和 MySQL，再覆盖源码、保留服务器 `.env`、构建 API/Web、启动容器并检查 `/api/health`。

默认部署到 `ubuntu@[redacted-public-host]:/opt/supervision`：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\deploy.ps1
```

推荐使用 SSH Key，避免交互输入密码：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\deploy.ps1 -IdentityFile C:\安全目录\supervision.pem
```

也可覆盖 `-Server`、`-User`、`-Port`、`-RemoteDir`。脚本不会保存 SSH 密码、企微密钥或 `.env`；服务器必须预先存在 `/opt/supervision/.env`。若数据库容器未运行、备份失败、构建失败或健康检查失败，部署会停止并保留 `/opt/backups/supervision-时间戳` 供人工恢复。
## 2026-07-22 一键脚本首次运行记录

- 第一次运行在本地解析阶段停止：Windows PowerShell 5 无法正确识别无 BOM UTF-8 中文脚本；已改为带 BOM UTF-8，并通过 PowerShell 5 解析检查，服务器未发生写入。
- 第二次运行进入 SSH 阶段后等待交互认证，已主动终止；系统 OpenSSH 使用 `BatchMode=yes` 复核显示服务器网络可达，但本机没有可用 SSH Key，认证返回 `Permission denied (publickey,password)`，尚未执行远端备份、构建或重启。
- 脚本已改为明确调用 `C:\Windows\System32\OpenSSH\ssh.exe`/`scp.exe`，避免命中受限环境代理。用户可在本地交互终端运行并输入密码；自动化运行推荐先配置 SSH Key，再使用 `-IdentityFile`。
## Windows 创建并配置部署 SSH Key

在本机 PowerShell 生成专用 Ed25519 密钥：

```powershell
ssh-keygen -t ed25519 -a 100 -f "$env:USERPROFILE\.ssh\supervision_ed25519" -C "supervision-deploy"
```

建议为私钥设置口令。生成后，`supervision_ed25519` 是只能保存在本机的私钥，`supervision_ed25519.pub` 是可安装到服务器的公钥。

使用现有 Ubuntu 密码将公钥追加到服务器：

```powershell
Get-Content "$env:USERPROFILE\.ssh\supervision_ed25519.pub" | ssh ubuntu@[redacted-public-host] "umask 077; mkdir -p ~/.ssh; cat >> ~/.ssh/authorized_keys; chmod 600 ~/.ssh/authorized_keys"
```

验证密钥登录：

```powershell
ssh -i "$env:USERPROFILE\.ssh\supervision_ed25519" ubuntu@[redacted-public-host]
```

验证成功后执行一键部署：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\deploy.ps1 -IdentityFile "$env:USERPROFILE\.ssh\supervision_ed25519"
```

不要发送、上传或提交无 `.pub` 后缀的私钥。确认密钥登录和 sudo 权限正常后，应轮换曾在对话中暴露的服务器密码；如决定关闭密码登录，应先保留一个已验证的管理会话，修改 sshd 配置后用第二个终端复测，避免锁在服务器之外。
## 2026-07-22 SSH Key 创建结果

- 已在本机创建项目专用 Ed25519 密钥：私钥 `C:\Users\midoo\.ssh\supervision_ed25519`，公钥为同名 `.pub` 文件。
- 密钥指纹：`SHA256:ASqCcF12kT6rHS8KCHC5yhWsa8VJ6iCRQ9DPtFOU/rM`。
- 公钥已追加到 `ubuntu@[redacted-public-host]:~/.ssh/authorized_keys`，并设置目录 700、文件 600 权限。
- 使用该私钥执行 `BatchMode=yes` 非交互验证成功，远端主机名为 `VM-0-9-ubuntu`，登录用户为 `ubuntu`。
- 临时密码辅助文件已在安装命令结束时删除，密码和私钥均未写入项目。
- 后续一键部署应使用：`powershell -ExecutionPolicy Bypass -File .\scripts\deploy.ps1 -IdentityFile "$env:USERPROFILE\.ssh\supervision_ed25519"`。
## 2026-07-22 目标群修复部署结果

- 使用项目专用 SSH Key 执行 `scripts/deploy.ps1`，最终部署成功，地址为 `http://[redacted-public-host]:8002/`。
- 首次尝试因 `/opt/backups` 权限停止，未修改服务；脚本改为使用免密 sudo 创建受保护备份目录。
- 第二次尝试因 `ubuntu` 无 Docker socket 权限停止，未覆盖或重启服务；脚本改为统一通过 `sudo docker` 执行容器操作。
- 第三次构建因本地 120 秒调用超时被中断；使用 10 分钟窗口重跑幂等部署后成功。
- 最终备份目录：`/opt/backups/supervision-20260722-195708`，包含部署前源码及 MySQL 逻辑备份。
- 后端 Docker Maven 构建通过；前端 Vite 构建通过并转换 1679 个模块。
- Flyway V10 `legacy organization compatibility` 执行成功；人员表已确认包含 `tenant_key`、`wecom_user_id`、`name`、`status`。
- MySQL、Redis、RabbitMQ、API、Web 五个容器最终均为 healthy，API 最近五分钟没有 ERROR/Exception；公网 `/api/health` 返回 HTTP 200 和 `ok`。
- 尚未使用有效后台登录会话完成目标群选择、任务保存和真实企微测试发送，因此 OpenSpec 4.3/4.4 仍保留待验收。
## 2026-07-22 生产升级

执行 scripts/deploy.ps1 -IdentityFile <ssh-key> 完成升级，备份目录为 /opt/backups/supervision-20260722-221644。Flyway V11 成功，五容器健康，外部地址为 http://[redacted-public-host]:8002/。API/Web 已配置 Docker json-file 日志轮转（10MB × 3）；此前约 25GB 的 supervision-api 历史容器日志已清空且不可恢复，数据库、代码与备份未删除。
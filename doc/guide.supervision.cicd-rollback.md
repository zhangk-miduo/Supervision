# Supervision 自动部署与版本回滚指南

> **版本**：v1.1 | **日期**：2026-07-26 | **状态**：已验证

## 适用场景

本指南用于 Supervision 开源仓库的持续集成、生产自动发布和应用版本回滚。代码进入 `main` 后先通过前后端构建验证，再经 SSH 发布到 Linux 服务器；生产凭据只保存在 GitHub Secrets 或服务器本地，不进入 Git 历史。

## 已落地结论

- Pull Request 只执行前端构建和后端 Docker/Maven 测试，不发布。
- `main` 推送验证成功后自动发布，版本号为完整 Git commit SHA。
- 版本保存在 `/opt/supervision/releases/<commit-sha>`，`current` 符号链接指向运行版本。
- 发布前备份 MySQL；健康检查失败时自动切回上一个应用版本。
- 默认保留最近 5 个应用版本，发布和回滚通过文件锁串行执行。
- 代码回滚默认不恢复数据库，避免覆盖发布后产生的新数据。

## 一、服务器首次准备

服务器需要 Linux、Docker Engine、Docker Compose v2、`curl`、`flock`，并允许部署用户执行 Docker。将 `<deploy-user>` 替换为实际用户：

```bash
sudo install -d -m 750 -o <deploy-user> -g <deploy-user> /opt/supervision
sudo install -d -m 750 -o <deploy-user> -g <deploy-user> /opt/backups/supervision
sudo cp /path/to/Supervision/.env.example /opt/supervision/.env
sudo chown <deploy-user>:<deploy-user> /opt/supervision/.env
chmod 600 /opt/supervision/.env
```

编辑 `/opt/supervision/.env`，将占位符替换为独立强密码。此文件不得复制回仓库。

为 GitHub Actions 单独创建 Ed25519 SSH 密钥：公钥写入服务器部署用户的 `~/.ssh/authorized_keys`，私钥只保存到 GitHub Environment Secret。不要使用登录密码进行自动部署。

部署用户需要无交互执行脚本中的 `sudo docker` 和目录初始化命令。建议通过 `/etc/sudoers.d/supervision-deploy` 做最小授权；使用 `sudo -n docker compose version` 验证时不得要求输入密码。

## 二、配置 GitHub Secrets

进入仓库 **Settings → Environments → New environment**，创建 `production`，在该 Environment 中配置：

| Secret | 内容 |
|---|---|
| `DEPLOY_HOST` | 服务器域名或 IP |
| `DEPLOY_USER` | 专用部署用户 |
| `DEPLOY_PORT` | SSH 端口，通常为 `22` |
| `DEPLOY_SSH_KEY` | Ed25519 私钥完整内容 |
| `DEPLOY_KNOWN_HOSTS` | 经管理员核验指纹后的服务器 `known_hosts` 行 |

可为 `production` 设置 Required reviewers。不得通过关闭 StrictHostKeyChecking 替代 `DEPLOY_KNOWN_HOSTS`，否则会失去服务器身份校验。

## 三、自动发布

合并代码到 `main` 后，在 **Actions → CI and production deployment** 查看：

1. 前端执行 `npm ci` 和 `npm run build`。
2. 后端使用 Dockerfile 执行 Maven 构建与测试。
3. 上传仅包含 Git 跟踪文件的发布包。
4. 服务器备份数据库、构建镜像、切换版本并检查 `/api/health`。
5. 健康失败时自动切回上一应用版本，并保留数据库备份和故障日志。

## 四、回滚操作教程

### 方法 A：GitHub Actions 回滚（推荐）

1. 打开 **Actions → CI and production deployment → Run workflow**。
2. `operation` 选择 `rollback`。
3. `release` 填写目标版本的完整 40 位 Git commit SHA。
4. 点击 **Run workflow**，等待 `Roll back application` 和健康检查通过。

版本 SHA 可从成功部署的 Actions 记录或服务器版本列表取得。


## 生产验证记录

- 2026-07-26：GitHub `production` Environment 已创建，5 个部署 Secret 已配置。
- Commit `f8bbca049bdf1321232a000d79e64c2d98cc3c1e` 的前端构建与后端 Docker/Maven 测试通过。
- GitHub Actions Run `30189367675` 完成首次生产发布，服务器健康接口返回 `ok`。
- 运行镜像以完整 commit SHA 标记，`current` 指向对应 release 目录。
- 发布前 MySQL 备份已生成并保存在 `/opt/backups/supervision/`。
- 第二版 `6f5ad6d87b71b4e2748786e8bd0f6ee08c28c225` 自动发布成功，两个 release 均被保留。
- Actions Run `30189918169` 将生产回滚至首版，`current`、API 镜像标签同步切换，健康接口返回 `ok`。
- Actions Run `30189963282` 使用同一入口切回第二版并通过健康检查。
- 回滚前均自动生成 MySQL 备份；演练未执行高风险数据库覆盖恢复。
### 方法 B：服务器命令行回滚

```bash
# 查看保留版本
/opt/supervision/bin/supervision-deploy list

# 回滚指定版本
/opt/supervision/bin/supervision-deploy rollback <commit-sha>

# 验证
readlink -f /opt/supervision/current
curl -fsS http://127.0.0.1:8002/api/health
docker compose --project-name supervision --env-file /opt/supervision/.env \
  --file /opt/supervision/current/docker-compose.yml ps
```

### 数据库恢复边界

应用回滚默认保留当前数据库。只有迁移不兼容导致旧应用无法运行，并且已进入维护窗口时，才考虑恢复发布前备份。备份位于 `/opt/backups/supervision/<时间-版本>/database.sql.gz`。恢复会覆盖当前数据，必须先再次备份、停止 API 与调度写入，并由运维人员复核目标文件；GitHub Actions 不自动执行数据库恢复。

## 五、安全与运维边界

- `.gitignore` 排除 `.env`、私钥、证书和常见密钥文件；`git archive` 只打包已跟踪文件。
- Actions 使用最小 `contents: read` 权限，生产 Secrets 绑定 `production` Environment。
- 工作流不打印私钥、业务密码或服务器 `.env`。
- 应用保留最近 5 个 release；数据库备份不会自动删除，应另设加密归档和保留策略。
- Flyway 变更应向后兼容；破坏性迁移采用“先扩展、后迁移、再收缩”。

## 六、常见问题

- SSH 失败：核对部署用户、公钥权限、端口、防火墙和 `DEPLOY_KNOWN_HOSTS`。
- `sudo` 要求密码：修正最小 sudoers 规则，GitHub Runner 无法交互输入。
- 首次部署缺少 `.env`：在服务器创建 `/opt/supervision/.env` 并设为 `600`，不要提交。
- `Unknown release`：目标版本已被清理，需从该 commit 重新执行 deploy 或恢复外部归档。

## 本次需求与证据

用户确认 GitHub Actions + SSH，要求开源仓库不包含敏感信息、支持版本回滚并提供教程。仓库此前已有 Compose 和部署脚本，但不存在 Actions，旧脚本仅备份且不会自动回切。本次影响 `.github/workflows/ci-cd.yml`、`scripts/deploy-server.sh`、`scripts/deploy.ps1`、`docker-compose.yml` 和部署文档。

GitHub 授权、Environment Secrets 和服务器 SSH 公钥均已配置完成。用户曾在对话中提供登录口令，该口令未写入项目文件；应立即轮换。

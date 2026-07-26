# 参与贡献

感谢你关注 **Supervision**！本文档说明如何参与本项目。

## 提交流程

1. 先搜索 [Issues](https://github.com/zhangk-miduo/Supervision/issues)，确认问题/建议未被提出。
2. 提 Bug 请用 **Bug 报告**模板，提需求请用 **功能请求**模板。
3. Fork 后从 `main` 切出特性分支（如 `feat/xxx`、`fix/xxx`）；本仓库也使用 `codex/*` 分支。
4. 提交 PR 到 `main`，填写 PR 模板，并在描述中关联相关 Issue。
5. CI 会自动构建前端、构建后端（含 Maven 测试）并验证。

## 开发环境

- 完整搭建与验证见 [AGENTS.md](AGENTS.md) 与 [README.md](README.md) 的「快速开始 / 本地开发与验证」。
- 推荐：用 Docker 方式一条命令起全套环境，无需本地安装 Java / Maven / Node。

## 代码约定

- **后端**：Java 17 + Spring Boot，单模块 DDD 分层（`api` / `application` / `domain` / `infrastructure` / `entity`）。
- **前端**：Vue 3 + TypeScript + `<script setup>`。
- **提交信息**：建议遵循 `type(scope): 简述`（如 `feat(task): 支持 Cron 调度`、`fix(auth): 修正菜单权限刷新`）。
- **敏感文件**：保持 `.env` 等不入库；密钥一律通过环境变量 / GitHub Secrets 管理，切勿提交真实凭据。

## 文档约定（重要）

本仓库有一条强约定：**任何涉及本仓库的对话或任务，在视为完成前，必须将其要点整理进 `doc/` 下的项目文档**，至少包含：用户需求、结论与决策、关键证据、影响模块、风险与开放问题、建议的后续动作。

- 若已有相关文档则更新它；
- 否则按 `doc/rule.supervision.conversation-documentation.md` 的命名与结构新建。

详见 [AGENTS.md](AGENTS.md) 的 Conversation Documentation 小节。

## 发版

- 版本号遵循 [SemVer](https://semver.org/lang/zh-CN/)；发版前在 [CHANGELOG.md](CHANGELOG.md) 汇总变更。
- 打标签并发布：

  ```bash
  git tag v0.1.0
  git push origin v0.1.0
  ```

  随后在 GitHub 创建对应 Release。

## 行为准则

参与本仓库即表示同意 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)。

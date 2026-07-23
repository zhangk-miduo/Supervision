# Supervision 仓库敏感信息审计报告

> **版本**：v1.1 | **日期**：2026-07-23 | **状态**：历史重写与远端强制更新已完成

## 用户要求

检查开源仓库当前内容与 Git 历史是否签入敏感信息；发现后修复，并彻底清理 Git 历史后强制推送。

## 审计结论

- 未发现私钥、AWS/Google/GitHub Token 或真实 OpenAI API Key。
- 高置信规则的两处命中均为误报：测试用带认证 URL，以及变更名称中相邻字符形成的 `sk-` 模式。
- 发现真实公网服务器地址及固定的 MySQL、RabbitMQ、应用加密默认值，均已从当前代码和全部 main 历史提交中替换。
- 实际凭据已迁移到不跟踪的 `.env`；仓库只提供不可用占位符 `.env.example`。

## 已完成修改

- 扩充 `.gitignore`，排除 `.env`、私钥、证书容器及 Java KeyStore。
- Docker Compose 改为要求显式提供数据库、消息队列与应用加密密钥。
- Spring 配置和 `SecretCipher` 移除弱默认值；缺失加密密钥时快速失败。
- 部署脚本改为通过 `-Server` 或 `SUPERVISION_SERVER` 接收目标地址。
- 当前文档中的真实公网地址已脱敏。

## 历史清理结果

- 使用 `git-filter-repo 2.47.0` 对 `main` 全部提交执行精确内容替换。
- 重写后的敏感值历史扫描为 0 命中，Git 完整性检查通过。
- 使用 `--force-with-lease` 将 GitHub `main` 从旧历史安全更新为 `688ceecf63cae7666e8356b90bca0a21edc21fe9`，未覆盖并发远端提交。
- 本地旧引用和 reflog 已清除，并执行立即对象回收。
- 含旧历史的临时 bundle 和替换规则文件已删除。

## 剩余风险与必要行动

- GitHub 缓存、第三方镜像、已有 clone 或 fork 可能仍保留旧对象，历史重写无法保证删除这些外部副本。
- 所有曾提交或固定使用的线上口令都必须视为已泄露并完成轮换，包括服务器登录、MySQL、RabbitMQ 和应用加密密钥。
- 更换 `SUPERVISION_CRYPTO_KEY` 前必须迁移已有密文，否则旧密文无法解密。
- 所有协作者应重新 clone，或显式将本地分支重置到重写后的远端历史；不得把旧分支再次合并回 main。
- 建议在 CI 中加入 Gitleaks，并启用 GitHub secret scanning 与 push protection。

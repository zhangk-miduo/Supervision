# 企业微信机器人（Wechat Robot）

## JTBD（Jobs To Be Done）
管理员需要配置企业微信群机器人 Webhook，并在发送前验证连通性，作为通知出口。

## 范围
提供企业微信机器人的 CRUD 与连通性测试。

## 验收标准
- `GET/POST/PUT/DELETE /api/robots` 机器人增删改查，表 `supervision_wechat_robot`（robot_id/name/webhook_url/template）。
- `POST /api/robots/{id}/test` 调用 Webhook 发送一条测试消息，返回发送结果（成功/失败 + 企微返回码）。
- `WechatClient` 封装 Webhook POST（`Content-Type: application/json`，markdown 或 text 消息体）。

## 约束
- 仅企业微信一种渠道（V1.0）。
- Webhook URL 形如 `https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx`。

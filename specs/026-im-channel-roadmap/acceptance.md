# 026 验收记录

**日期**: 2026-09-08  
**范围**: 波次 0–5 代码 + 契约/归一化单测。真平台按已有凭证推进。

## 单测（本机 JDK 21）

| 模块 | 结果 |
|------|------|
| ChannelConfigLoader extra 回写/解析 | 通过（POSIX 权限用例 Windows 跳过，既有） |
| VendorNotifyAdapter（含 slack/discord/telegram/whatsapp/teams/gchat/mattermost/matrix） | 29 通过 |
| ChannelInboundWebhookController（未知渠道 / 非 webhook → 404；挑战回写） | 3 通过 |
| telegram / whatsapp / teams / gchat / mattermost / matrix 契约 + 归一化 | 全部通过 |
| WhatsApp 24h 窗外硬拒绝 + 验签/挑战 | 4 通过 |

全仓 `mvn test` 另有既有失败：Windows 上 `MasterKeyResolverTest`（POSIX 0600）与 `AgentSkillStartupOrderTest`（symlink），与本变更无关。

## 真平台

本机 `.env` 已有 Slack / Discord / Telegram 凭证（不入库）。2026-09-08 探测：

| 项 | 结果 |
|----|------|
| Telegram `getMe` | 通过。Bot 是提交者**个人测试号**（username=`rchuangbot`），不是仓库官方 Bot，凭证只在本机 `.env` |
| Telegram `getUpdates` | 空（该个人测试 Bot 尚无会话，无法取 `chat_id`） |
| Telegram 私聊往返 / 群 @ / notify | 待该个人测试 Bot 先有一条私聊后，再补 `CONNECTED` + 回写 + notify |
| Slack `auth.test` | 通过（team=`OryxOS`）；`conversations.list` 缺 scope，未打真实频道 |
| Discord `@me` | 通过（username=`OryxOS`）；列频道 403，未打真实频道 |
| WhatsApp / Teams / GChat / Mattermost / Matrix | 无可用凭证，未宣称 COMPLETE |

管理台 Notify 已补齐 026 类型（原先 API 只允许飞书/企微/钉钉/webhook/email，适配器注册了也建不了渠道）。

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
| Telegram 入站 `CONNECTED` | 本机 `oryxos serve` 后 `GET /api/v1/channels/status` 中 `ops-telegram` 为 `CONNECTED` |
| Telegram 私聊往返 | 通过：个人测试 Bot 收到私聊后建了 `telegram:*:demo-agent` 会话（2 条消息），DeepSeek 已回写 |
| Telegram `notify` | 通过：Bot API `sendMessage` 成功（个人测试会话） |
| Telegram 群 `@Bot` | 未测（需要把该个人测试 Bot 拉进群并关 Privacy Mode） |
| Slack 入站 | 主仓已测并合入：[PR #420](https://github.com/oryx-labs/oryxos/pull/420) Socket Mode `CONNECTED` + 文本往返；[PR #421](https://github.com/oryx-labs/oryxos/pull/421) 图片/文件入站。本机 `ops-slack` 仍为 `CONNECTED`，既有 Slack 会话可回放 |
| Discord 入站 | 主仓已测并合入：[PR #422](https://github.com/oryx-labs/oryxos/pull/422) Gateway 文本 DM / 公会 `@Bot`；[#423](https://github.com/oryx-labs/oryxos/pull/423)/[#425](https://github.com/oryx-labs/oryxos/pull/425)/[#426](https://github.com/oryx-labs/oryxos/pull/426)/[#427](https://github.com/oryx-labs/oryxos/pull/427) 图/文件/语音/视频 soak。本机 `ops-discord` 仍为 `CONNECTED`，既有 Discord 会话可回放 |
| Slack / Discord `notify` | 026 [PR #429](https://github.com/oryx-labs/oryxos/pull/429) 已补适配器；出站路径与入站回复同源（Slack `chat.postMessage` / Discord REST）。不要把后续 `conversations.list` / 列频道探测当成「未打真实频道」 |
| WhatsApp / Teams / GChat / Mattermost / Matrix | 无可用凭证，未宣称 COMPLETE |

管理台 Notify 已补齐 026 类型（原先 API 只允许飞书/企微/钉钉/webhook/email，适配器注册了也建不了渠道）。

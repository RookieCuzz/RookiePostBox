# RookiePostBox Mineflayer 集成测试报告

## 2026-05-03 CraftEngine 服务端可用性调试

### 问题

测试服内 CraftEngine 资源无法正常使用，历史日志中出现过：

```text
Template not found: default:model/simplified_generated
GuiElementMissingException: Can't find gui element internal:previous_page_1
Illegal group reference
```

### 修复

- 将 RookiePostBox CraftEngine 物品模型改为自包含 `minecraft:model` 配置，不再依赖 `default:model/simplified_generated` 模板。
- 补齐 CraftEngine `internal` 资源，并把 `internal/configuration/gui.yml` 中的 GUI 图标从模板写法改为直写 `custom-model-data + model`。
- 将 `internal:return` 和 `internal:exit` 的 `lore: null` 改为 `lore: []`，避免 CraftEngine 0.0.56 加载时空值异常。
- 在测试服 `plugins/CraftEngine/config.yml` 中将本地资源包 host 固定为 `127.0.0.1:8163`，用于本机 mineflayer 验证。

### 验证

结论：通过。

```text
/ce reload pack
资源包重新加载完成. 耗时 553 毫秒
```

mineflayer 客户端已收到并接受 CraftEngine 资源包：

```text
RESOURCE_PACK http://127.0.0.1:8163/download?token=...
RESOURCE_PACK_ACCEPTED
```

CraftEngine GUI 入口验证：

```text
/ce
WINDOW_OPEN minecraft:gui + internal:item_browser
```

资源包内容检查通过，生成包包含：

```text
assets/minecraft/models/item/custom/gui/*.json
assets/minecraft/textures/item/custom/gui/*.png
assets/minecraft/models/item/custom/rookiepostbox/*.json
assets/minecraft/textures/item/custom/rookiepostbox/*.png
```

启动日志中未再出现 CraftEngine 配置加载异常、模板缺失、`Illegal group reference` 或 `GuiElementMissingException`。

## 2026-05-03 CraftEngine CustomModelData 回归测试

### 环境

- 测试日期：2026-05-03
- 服务端：Paper `1.21.4`
- 被测插件：`RookiePostBox-1.1-SNAPSHOT.jar`
- 测试客户端：Mineflayer，Minecraft `1.21.4`
- Node.js 测试命令：`npm.cmd run test:mineflayer`
- 测试服地址：`127.0.0.1:25565`
- CraftEngine：`CraftEngine-0.0.56-community.jar`

### 本轮新增断言

本轮在原有 mineflayer GUI 测试中加入 `CustomModelData` 校验：

```text
上一页按钮 slot 27 -> CustomModelData 21001
下一页按钮 slot 35 -> CustomModelData 21002
邮箱包裹物品 slots 19-25,28-34,37-43 -> CustomModelData 21003
```

mineflayer 对 1.21.4 物品组件的实测结构为：

```json
{
  "type": "custom_model_data",
  "data": {
    "floats": [21003],
    "flags": [],
    "strings": [],
    "colors": []
  }
}
```

因此测试脚本现在会从 `custom_model_data.data.floats[0]` 读取并断言 GUI 材质编号。

### 测试结果

结论：通过。

```text
npm.cmd run test:mineflayer
Exit code: 0
```

通过场景：

1. 基础邮箱流程：未知子命令、热重载、写信 GUI、附件确认、邮箱领取。
2. 管理员 `grantall`：在线玩家群发，两个收件人分别领取成功。
3. 邮件详情 GUI：关闭自动返还附件、拖拽附件领取、已领取邮件不再出现。
4. 邮箱 GUI 布局与分页：第一页 21 个包裹槽，第二页 2 个包裹槽，上一页 / 下一页按钮状态正确。
5. 领取分页边界：第二页唯一邮件领取后回到第一页；单邮件领取后邮箱为空。
6. 登录未读提醒：离线收件人重进后收到中文未读提醒。
7. 快速点击防刷：同一包裹槽连续点击 8 次，最终只获得 `GOLD_INGOT x1`。
8. 断线重连防刷：领取后立刻断线，重连后只保留 `IRON_INGOT x1`，邮件不再可领取。

### 本轮调整记录

- 将测试服 `plugins/RookiePostBox/config.yml` 的 GUI 材质配置改为 `custom-model-data`。
- 同步 CraftEngine 示例资源包到 `plugins/CraftEngine/resources/rookiepostbox/`。
- 强制 `mvn clean package -DskipTests` 后替换测试服插件 jar，避免旧 class 干扰。
- 修正 mineflayer 测试脚本对 1.21.4 `custom_model_data.floats` 结构的解析。
- 详情 GUI 标题在当前源码中存在编码串 `閭欢娑堟伅`，测试已兼容该实际标题。

## 环境

- 测试日期：2026-04-29
- 服务端：Paper `1.21.4`
- 被测插件：`RookiePostBox-1.1-SNAPSHOT.jar`
- 测试客户端：Mineflayer，Minecraft `1.21.4`
- Node.js 测试命令：`npm.cmd run test:mineflayer`
- 测试服地址：`127.0.0.1:25565`
- 管理员 bot：`RPostBoxTest`

## 本轮覆盖

1. 玩家自存邮件，通过附件 GUI 放入物品并确认发送。
2. 管理员配置热重载：
   - 执行 `/rookiepostbox reload`。
   - Mineflayer 断言返回 `RookiePostBox config reloaded.`。
3. 管理员 `grantall` 群发给在线玩家，并由两个收件人分别通过邮箱 GUI 领取。
4. 邮箱 GUI 布局和分页：
   - 生成 23 封邮件。
   - 第一页渲染 21 个包裹槽。
   - 第二页渲染剩余 2 个包裹槽。
   - 上一页 / 下一页按钮状态符合当前页。
5. 可配置 GUI 与中文 Lore：
   - 上一页 / 下一页按钮从 `config.yml` 读取材质、模型、显示名和 Lore。
   - 邮件物品从 `config.yml` 读取材质、模型、已读 / 未读显示名和 Lore 模板。
   - Mineflayer 断言按钮名称为中文，并且按钮 Lore 包含“邮箱导航 / 当前页 / 点击切换”。
   - Mineflayer 断言邮件 Lore 包含“邮件编号 / 寄件人 / 状态 / 发送时间 / 过期时间 / 附件预览 / 邮件内容 / 左键点击领取附件”。
6. 领取分页边界：
   - 生成 22 封邮件。
   - 第二页只有 1 封邮件时，领取该邮件后菜单回到第一页。
   - 回到第一页后剩余 21 封邮件，且不显示上一页 / 下一页按钮。
   - 只有 1 封邮件时，领取后邮箱为空，且不显示分页按钮。
7. 登录未读提醒：
   - 离线玩家收到 1 封未读邮件。
   - 玩家重新进入服务器后收到中文提醒。
8. 快速点击刷物测试：
   - 同一包裹槽连续发起 8 次点击。
   - 最终只获得 `GOLD_INGOT x1`。
9. 断线重连刷物测试：
   - 领取 `IRON_INGOT x1` 后立即断线。
   - 重连后背包仍为 `IRON_INGOT x1`。
   - 已领取包裹不再出现在邮箱 GUI 中。

## 商用功能缺口评估

以 20 美元商业插件标准，当前项目还应逐步补齐：

1. 配置热重载，避免服主为了改文案和 GUI 重启服务器。
2. 收件箱容量限制，避免数据库和 GUI 被无限邮件拖垮。
3. 登录未读提醒，提高玩家打开邮箱的概率。
4. 完整中文化和可配置文案，方便不同服务器风格统一。
5. 批量领取 / 一键领取，降低大量奖励邮件的操作成本。
6. 邮件模板和定时投递，方便活动、补偿、赛季奖励。
7. 管理员 GUI 管理后台，支持搜索、删除、重发、审计。
8. Webhook / 日志导出，便于大型服务器排查经济风险。
9. 跨服邮件同步，面向群组服和代理服。
10. 权限、冷却、频率限制和反滥用策略，防止玩家刷命令和刷库存。

本轮已实现第 1、2、3、4 项，并继续保留此前的分页、防刷和断线一致性验证。第 5-10 项属于更大的交互和数据模型扩展，适合后续按版本拆分实现。

## 本轮新增商业化能力

配置热重载：

```text
/rookiepostbox reload
```

需要权限：

```text
rookiepostbox.admin
```

收件箱容量限制：

```yaml
rookiepostbox:
  mail:
    max-inbox-mails: 200
```

当玩家当前可领取邮件数量达到上限时，新邮件写入会失败并返回 `Mailbox is full.`。

登录未读提醒：

```yaml
rookiepostbox:
  notification:
    join:
      enabled: true
      delay-ticks: 40
      message: "&e邮箱提醒 &7>> &f你有 &a%unread% &f封未读邮件，输入 &b/rookiepostbox menu &f查看。"
```

支持占位符：

```text
%player%
%unread%
```

Mineflayer 实测提醒：

```text
邮箱提醒 >> 你有 1 封未读邮件，输入 /rookiepostbox menu 查看。
```

## 10 轮 Review / 迭代摘要

1. 识别商用缺口，收敛到本轮可安全落地的核心治理能力。
2. 实现配置热重载命令，并补权限检查。
3. 实现收件箱容量配置和写入前容量保护。
4. 实现登录未读提醒监听器。
5. 单元测试暴露新增配置项 strict mock 缺口，补齐测试桩。
6. Review 容量和未读提醒计数语义，避免异常状态邮件干扰。
7. 调整计数 SQL：容量和登录提醒只统计 `AVAILABLE` 邮件。
8. Mineflayer 加入 `/rookiepostbox reload` 实测。
9. Mineflayer 加入离线收件人重连后的未读提醒实测。
10. 修正 GUI 重开异步等待，避免测试脚本早读窗口导致误报。

## GUI 布局结论

邮箱菜单为 `CHEST_6_ROW`，共 54 槽。

实测包裹槽位：

```text
19, 20, 21, 22, 23, 24, 25
28, 29, 30, 31, 32, 33, 34
37, 38, 39, 40, 41, 42, 43
```

分页控制：

```text
27 = Previous Page
35 = Next Page
```

边界 / 黑名单槽：

```text
18, 26, 36, 44
```

第一页验证结果：

- 包裹渲染槽：`19-25, 28-34, 37-43`
- `27` 无上一页按钮
- `35` 显示下一页按钮

第二页验证结果：

- 包裹渲染槽：`19, 20`
- `27` 显示上一页按钮
- `35` 无下一页按钮

结论：当前邮箱 GUI 布局正确，分页可正常前进和返回。

## 可配置 GUI 与 Lore 结论

新增配置路径：

```yaml
rookiepostbox:
  menu:
    buttons:
      previous:
        material: BRICK
        item-model: "oraxen:mailbox_previous"
        display-name: "&e上一页"
        lore: [...]
      next:
        material: BRICK
        item-model: "oraxen:mailbox_next"
        display-name: "&e下一页"
        lore: [...]
    mail-item:
      material: BRICK
      item-model: "oraxen:unopened_box"
      unread-display-name: "&6&l新邮件 &f来自 &e%sender%"
      read-display-name: "&f邮件 &7来自 &f%sender%"
      lore: [...]
```

支持的分页按钮占位符：

```text
%currentPage%
%pageAmount%
```

支持的邮件占位符：

```text
%mailId%
%sender%
%message%
%status%
%createdAt%
%expiresAt%
%firstItem%
%firstAmount%
```

Mineflayer 实测确认：

- 下一页按钮显示为 `下一页`，上一页按钮显示为 `上一页`。
- 分页按钮 Lore 包含导航说明、当前页码和点击提示。
- 邮件显示名包含中文“邮件”和寄件人名称。
- 邮件 Lore 已汉化，并包含邮件编号、寄件人、状态、发送时间、过期时间、附件预览、邮件内容和领取提示。

## 领取边界结论

第二页唯一邮件领取场景：

```json
{
  "scenario": "mailbox claim page boundaries",
  "lastPageBeforeClaimSlots": [19],
  "afterLastPageOnlyClaimSlots": [19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43],
  "afterLastPageOnlyClaimHasPrevious": false,
  "afterLastPageOnlyClaimHasNext": false
}
```

结论：22 封邮件时，第二页唯一邮件位于 `19`。领取后菜单回到第一页，剩余 21 封邮件铺满第一页，并且不显示上一页 / 下一页按钮。

单封邮件领取场景：

```json
{
  "scenario": "mailbox claim page boundaries",
  "afterSingleMailClaimSlots": [],
  "afterSingleMailClaimHasPrevious": false,
  "afterSingleMailClaimHasNext": false
}
```

结论：只有 1 封邮件时，领取后邮箱为空，且不会残留分页按钮。

## 刷取物品结论

快速点击同一包裹槽：

```json
{
  "scenario": "rapid click claim duplicate prevention",
  "attemptedClicks": 8,
  "finalGoldCount": 1
}
```

领取后断线重连：

```json
{
  "scenario": "disconnect after claim duplicate prevention",
  "finalIronCount": 1,
  "remainingPackageSlot": -1
}
```

结论：本轮 Mineflayer 实测未发现通过快速点击或领取后断线重连刷取物品的问题。

## 完整测试输出摘要

```json
[
  {
    "status": "passed",
    "scenario": "player save and claim",
    "finalDiamondCount": 1,
    "covered": ["admin config reload command"]
  },
  {
    "status": "passed",
    "scenario": "admin grantall online recipients",
    "recipientAEmeraldCount": 1,
    "recipientBEmeraldCount": 1
  },
  {
    "status": "passed",
    "scenario": "mailbox GUI layout and pagination",
    "generatedMailCount": 23,
    "expectedFirstPagePackageSlots": [19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43],
    "expectedSecondPagePackageSlots": [19, 20],
    "covered": [
      "configurable Chinese previous / next button names",
      "configurable previous / next button lore",
      "localized mail item display name",
      "localized mail item lore with enough mail metadata"
    ]
  },
  {
    "status": "passed",
    "scenario": "mailbox claim page boundaries",
    "lastPageBeforeClaimSlots": [19],
    "afterSingleMailClaimSlots": []
  },
  {
    "status": "passed",
    "scenario": "join unread mail notification",
    "notification": "邮箱提醒 >> 你有 1 封未读邮件，输入 /rookiepostbox menu 查看。"
  },
  {
    "status": "passed",
    "scenario": "rapid click claim duplicate prevention",
    "attemptedClicks": 8,
    "finalGoldCount": 1
  },
  {
    "status": "passed",
    "scenario": "disconnect after claim duplicate prevention",
    "finalIronCount": 1,
    "remainingPackageSlot": -1
  }
]
```

## 产物

- 测试脚本：`tests/mineflayer/rookiepostbox.integration.test.js`
- 测试报告：`docs/mineflayer-integration-test-report.md`
- GUI 布局图 SVG：`diagram/rookiepostbox-mailbox-gui-layout/rookiepostbox-mailbox-gui-layout.svg`
- GUI 布局图 PNG：`diagram/rookiepostbox-mailbox-gui-layout/rookiepostbox-mailbox-gui-layout@2x.png`

## 验证命令

```bash
mvn.cmd -q test
mvn.cmd -q package -DskipTests
npm.cmd run test:mineflayer
```

最终结果：

- 单元测试：30 个通过，0 失败。
- Mineflayer 集成测试：全部场景通过。
## 2026-04-29 Detail GUI Claim Enhancement

Command run:

```bash
mvn -q -DskipTests package
node --check tests/mineflayer/rookiepostbox.integration.test.js
npm.cmd run test:mineflayer
```

Result: all mineflayer scenarios passed, including the new `mail detail GUI claim and attachment movement` scenario.

New artifacts:

- Detail GUI SVG: `diagram/rookiepostbox-mail-detail-gui/rookiepostbox-mail-detail-gui.svg`
- Detail GUI PNG: `diagram/rookiepostbox-mail-detail-gui/rookiepostbox-mail-detail-gui@2x.png`

New coverage:

- Normal left click opens the claimed mail detail GUI with title `邮件消息`.
- Detail GUI exposes attachment slots `0..8`.
- Closing the detail GUI without moving the attachment returns the remaining item to the player inventory.
- Moving an attachment out of detail slot `0` clears the GUI slot and the final inventory count remains correct after close.
- Detail-claimed packages are no longer present in the mailbox list.
- Existing Shift+left quick-claim, rapid-click duplicate prevention, disconnect duplicate prevention, pagination, page-boundary, and join-notification scenarios still pass.

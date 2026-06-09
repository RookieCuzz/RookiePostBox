![1777427756108](image/business-sequence-diagrams/1777427756108.png)# RookiePostBox 业务时序图

## 1. 文档说明

这份文档基于当前项目代码结构和功能盘点文档，按三个阶段组织业务时序图：

1. 原型稳定性阶段
2. 玩家发件功能阶段
3. 正式服能力阶段

文档目标不是只画“理想流程”，而是同时覆盖：

- 当前已实现的核心链路
- 当前半成品链路和脆弱点
- 尚未实现但必须补齐的目标链路

每张时序图后都附带关键问题分析，重点标出：

- 缺失组件
- 事务边界
- 全局静态状态风险
- 并发和幂等问题
- 错误反馈路径

---

## 2. 阶段一：原型稳定性

这一阶段的目标不是继续堆功能，而是把现有闭环梳理清楚，并暴露当前实现中的薄弱点。

---

## 2.1 已实现：玩家打开邮箱菜单

```mermaid
sequenceDiagram
    autonumber
    actor Player as 玩家
    participant Cmd as PostBoxCommandExecutor
    participant Menus as OdalitaMenus
    participant GUI as PostBoxMenu
    participant Cache as Cache.postBoxes
    participant DB as MongoDBManager
    participant Fonts as RookieFonts

    Player->>Cmd: /rookiepostbox menu
    Cmd->>Menus: openMenu(new PostBoxMenu(), player)
    Menus->>GUI: onLoad(player, contents)
    GUI->>Cache: get(player.name)
    alt 缓存命中
        Cache-->>GUI: PostBox
    else 缓存未命中
        GUI->>DB: getPostBoxByPlayer(player)
        alt 邮箱存在
            DB-->>GUI: PostBox
        else 邮箱不存在
            GUI->>DB: createNewPostBox(player.uuid, player.name)
            GUI->>DB: getPostBoxByPlayer(player)
            DB-->>GUI: PostBox
        end
    end
    GUI->>GUI: 构建分页对象与邮件列表项
    GUI->>Fonts: 更新 %currentPage% / %pageAmount%
    Fonts-->>GUI: 标题模板组件
    GUI-->>Menus: setTitle + set pagination items
    Menus-->>Player: 打开邮箱 GUI
```

**问题分析**
- 当前流程没有真正稳定的缓存命中路径，大多数情况下仍然直接回数据库。
- `Cache.postBoxes` 的 key 使用不一致，代码里既有 `player.name`，也有 `uuid` 方向的清理逻辑，缓存语义不稳定。
- `createNewPostBox` 与后续查询没有事务封装。如果未来改成 PostgreSQL，这里应改为 `createIfAbsent` 或 `upsert`。
- 标题刷新耦合 `RookieFonts` 占位符更新，GUI 数据加载和标题系统耦合过深。

---

## 2.2 已实现：玩家保存主手物品到邮箱

```mermaid
sequenceDiagram
    autonumber
    actor Player as 玩家
    participant Cmd as PostBoxCommandExecutor
    participant ItemModel as AdminItem
    participant Package as Package
    participant DB as MongoDBManager
    participant Cache as Cache.postBoxes
    participant Toast as Toast

    Player->>Cmd: /rookiepostbox save <message>
    Cmd->>Cmd: 校验 sender 是否为玩家
    Cmd->>Cmd: 校验 args.length == 2
    Cmd->>Cmd: 读取主手物品
    alt 主手为空
        Cmd-->>Player: 提示“请先选择一个物品”
    else 主手有物品
        Cmd->>Package: new Package(ownerUUID, senderName, message, createTime)
        Cmd->>ItemModel: new AdminItem()
        Cmd->>ItemModel: setAmount / setStoreID / setDisplayName
        Cmd->>ItemModel: serializeItemStackToBase64(item)
        ItemModel-->>Cmd: base64Item
        Cmd->>Package: addItem(adminItem)
        Cmd->>DB: addPackageToPostBox(package, player, true)
        DB->>DB: savePackageToDB(package)
        DB->>DB: getPostBoxByUUID(player.uuid)
        alt 邮箱不存在
            DB->>DB: createNewPostBox(uuid, "测试用户")
        end
        DB->>Cache: remove(player.name)
        DB->>DB: update PostBox.packages addToSet(DBRef)
        DB-->>Cmd: success=true
        Cmd->>Toast: displayTo(player, itemType, "已发送："+message)
        Toast-->>Player: Advancement Toast
        Cmd-->>Player: 命令执行结束
    end
```

**问题分析**
- 当前这个“save”并不是发给别人，而是把邮件存进玩家自己的邮箱，本质上更像“自投递”。
- `args.length == 2` 导致消息不能带空格，命令参数设计不完整。
- 当前没有事务边界。`savePackageToDB` 成功但 `PostBox` 引用更新失败时，会出现孤立包裹。
- 当前使用 Mongo `DBRef + addToSet`，对“单封邮件 + 附件 + 邮箱关系”的一致性没有完整保障。
- `Cache.remove(player.name)` 是写后失效，但没有统一的读写策略。
- 玩家物品不会从背包扣除，因此它更像“复制入邮箱”，不是正式服可接受的发件流程。

---

## 2.3 已实现：玩家领取邮件物品

```mermaid
sequenceDiagram
    autonumber
    actor Player as 玩家
    participant GUI as PostBoxMenu
    participant Cache as Cache.postBoxes
    participant DB as MongoDBManager
    participant Inv as PlayerInventory

    Player->>GUI: 点击邮件物品
    GUI->>GUI: 读取 PersistentDataContainer.packageId
    GUI->>Cache: get(player.name)
    alt 缓存命中
        Cache-->>GUI: PostBox
    else 缓存未命中
        GUI->>DB: getPostBoxByPlayer(player)
        DB-->>GUI: PostBox
    end
    GUI->>GUI: 遍历 packages 匹配 packageId
    loop 遍历附件
        GUI->>Inv: firstEmpty()
        alt 背包已满
            Inv-->>GUI: -1
            GUI-->>Player: 提示“背包已满”
            GUI-->>Player: 终止领取
        else 有空位
            Inv-->>GUI: slot
            GUI->>Inv: addItem(item.getBukkitItem())
            GUI-->>Player: 提示“获得了某物品 x 数量”
        end
    end
    alt 全部附件已发放
        GUI->>DB: deletePackageFromPostBox(packageId, postBox)
        DB->>DB: pull packages by DBRef
        DB->>DB: postBox.deletePackage(packageId)
        DB-->>GUI: success
        GUI->>GUI: 重新计算 currentPageX
        GUI->>GUI: reopen menu builder
        GUI-->>Player: 刷新邮箱界面
    else 中途失败
        GUI-->>Player: 保留邮件，不删除记录
    end
```

**问题分析**
- 当前流程先发物品，再删数据库记录，中间没有事务保护。如果服务器在“发完物品但还没删记录”时崩溃，会导致重复领取。
- 当前删除逻辑是“领完直接删”，没有 `已领取` 状态，也没有审计记录。
- 当前领取动作完全运行在 GUI 回调中，没有服务层，不利于后续迁移 PostgreSQL 和加锁。
- 这里没有全局锁或行级锁概念。两次快速点击、双开窗口、插件异步影响都可能造成重复领取风险。
- `currentPageX` 是静态变量，菜单刷新时可能被其他玩家操作污染。

---

## 2.4 半成品：API 发件功能当前状态

```mermaid
sequenceDiagram
    autonumber
    participant OtherPlugin as 其他插件
    participant API as PackageAPI
    participant Package as Package
    participant ItemModel as AdminItem
    participant DB as MongoDBManager
    actor Receiver as 接收玩家

    OtherPlugin->>API: sendPackage(sender, receiver, message, item)
    API->>Package: new Package(...)
    Note over API,Package: 当前 ownerUUID 被写成 sender.uuid<br/>而不是 receiver.uuid
    API->>ItemModel: serializeItemStackToBase64(item)
    ItemModel-->>API: base64Item
    API->>Package: addItem(adminItem)
    API->>DB: addPackageToPostBox(package, receiver, true)
    DB->>DB: savePackageToDB(package)
    DB->>DB: append package ref into receiver PostBox
    DB-->>API: success
    API-->>OtherPlugin: void
    Note over Receiver: 玩家侧没有对应 send 命令、确认流程、发件失败回滚流程
```

**问题分析**
- API 已经具备“对别人发件”的雏形，但语义有 bug，`ownerUUID` 字段当前构造错误。
- API 返回 `void`，调用方拿不到明确的结果对象、包裹 ID、失败原因。
- 当前没有幂等字段，其他插件重复调用时可能重复投递。
- 玩家侧完全缺少对应命令和确认流程，所以这条链路只适合内部测试，不适合真实业务接入。

---

## 2.5 半成品：GUI 并发脆弱点

```mermaid
sequenceDiagram
    autonumber
    actor P1 as 玩家A
    actor P2 as 玩家B
    participant GUI1 as PostBoxMenu(A)
    participant GUI2 as PostBoxMenu(B)
    participant Static as static currentPageX
    participant Menus as OdalitaMenus

    P1->>GUI1: 打开邮箱并翻到第 3 页
    GUI1->>Static: currentPageX = 2
    P2->>GUI2: 打开邮箱并翻到第 1 页
    GUI2->>Static: currentPageX = 0
    P1->>GUI1: 点击领取当前页邮件
    GUI1->>Static: 读取/覆盖 currentPageX
    GUI1->>Menus: openMenuBuilder(...).pagination("mail_pagination", currentPageX)
    Menus-->>P1: 可能刷新到错误页
    Note over P1,P2: 两个玩家通过同一个静态页码变量产生相互干扰
```

**问题分析**
- `currentPageX` 是全局静态状态，不是会话状态。它天然不适合多人并发。
- 正确做法应该是“每个玩家会话独立页码”，归属到 `MenuSession`、`Pagination` 或服务层状态对象。
- 当前没有“全局锁”的正确使用场景。这里不应该加全局锁，而应该移除全局共享变量。
- 真正需要锁的是“领取同一封邮件”这种业务资源，不是分页 UI。

---

## 3. 阶段二：玩家发件功能

这一阶段的目标是从“自投递原型”升级为真正的玩家间邮件系统。

---

## 3.1 预期：玩家间邮件发送完整流程

```mermaid
sequenceDiagram
    autonumber
    actor Sender as 发件玩家
    participant Cmd as SendCommand
    participant Service as MailService
    participant Serializer as ItemStackSerializer
    participant Repo as MailRepository
    participant DB as PostgreSQL
    participant ReceiverBox as ReceiverMailbox

    Sender->>Cmd: /rookiepostbox send <player> <message...>
    Cmd->>Cmd: 校验权限、参数、目标玩家、主手物品
    alt 校验失败
        Cmd-->>Sender: 返回错误提示
    else 校验通过
        Cmd->>Service: sendMail(sender, receiver, item, message, requestId)
        Service->>Serializer: serialize(item)
        Serializer-->>Service: base64 + metadata
        Service->>Repo: begin create mail package
        Repo->>DB: BEGIN
        Repo->>DB: UPSERT post_boxes(receiver_uuid)
        Repo->>DB: INSERT mail_packages(... request_id ...)
        Repo->>DB: INSERT mail_items(...)
        Repo->>DB: INSERT mail_events(CREATED)
        Repo->>DB: COMMIT
        DB-->>Repo: packageId
        Repo-->>Service: packageId
        Service->>Service: 从发件人背包扣除物品
        alt 扣除失败
            Service->>Repo: mark mail as CLAIM_FAILED or rollback create
            Service-->>Cmd: 发送失败
            Cmd-->>Sender: 提示“发送失败，物品未扣除/已回滚”
        else 扣除成功
            Service-->>Cmd: 发送成功(packageId)
            Cmd-->>Sender: 提示“已发送给目标玩家”
            Note over ReceiverBox: 若接收方离线，邮件仍保存在邮箱
        end
    end

    Note over Cmd,Service: 当前项目缺失：SendCommand、MailService、Serializer 抽象、PostgreSQL Repository、结果对象
```

**问题分析**
- 这条链路必须引入 `requestId`，否则命令重试、插件重复回调、网络抖动都可能造成重复投递。
- “写库”和“扣玩家物品”不在同一个数据库事务里，因为 Bukkit 背包操作不属于数据库资源，所以必须设计补偿逻辑。
- 比较稳妥的方式是：
  - 先校验并锁定发件动作
  - 成功写库
  - 再扣除物品
  - 失败时补偿删除邮件或将邮件标记异常
- 这里不应该用全局锁锁住整个插件，只应该针对“发件玩家背包操作”和“requestId 幂等键”做局部保护。

---

## 4. 阶段三：正式服能力

这一阶段关注管理员、状态系统、审计和可恢复性。

---

## 4.1 预期：管理员管理功能交互流程

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 管理员
    participant Cmd as AdminCommand
    participant Service as AdminMailService
    participant Repo as MailRepository
    participant DB as PostgreSQL
    actor Target as 目标玩家

    Admin->>Cmd: /rookiepostbox admin inbox <player>
    Cmd->>Service: queryInbox(targetUuid, filters)
    Service->>Repo: findInbox(targetUuid, page, status)
    Repo->>DB: SELECT packages + items summary
    DB-->>Repo: inbox data
    Repo-->>Service: inbox data
    Service-->>Cmd: inbox data
    Cmd-->>Admin: 展示查询结果或管理 GUI

    Admin->>Cmd: /rookiepostbox admin delete <packageId> <reason>
    Cmd->>Service: deleteMail(packageId, admin, reason)
    Service->>Repo: begin delete transaction
    Repo->>DB: BEGIN
    Repo->>DB: UPDATE mail_packages SET lifecycle_state='DELETED', deleted_at=now()
    Repo->>DB: INSERT mail_events(DELETED, reason)
    Repo->>DB: COMMIT
    DB-->>Repo: success
    Repo-->>Service: success
    Service-->>Cmd: success
    Cmd-->>Admin: 提示“邮件已删除”

    Admin->>Cmd: /rookiepostbox admin grant <player> <template/item>
    Cmd->>Service: createManualMail(...)
    Service->>Repo: create mail package + event
    Repo->>DB: INSERT mail_packages / mail_items / mail_events
    Cmd-->>Admin: 提示“补偿已发放”
    Note over Cmd,Service: 当前项目缺失：管理员命令、过滤查询、软删除、事件日志、管理端结果展示
```

**问题分析**
- 管理员删除不应物理删除附件和事件，至少首选软删除。
- 管理员查询会推动索引设计，必须支持：
  - `owner_uuid + status + created_at`
  - `sender_uuid + created_at`
  - `package_id + event_time`
- 正式服下，管理员操作必须有事件审计，否则删件和补偿会变成黑盒。

---

## 4.2 预期：邮件状态管理系统工作流程

```mermaid
sequenceDiagram
    autonumber
    actor Player as 玩家
    participant GUI as MailboxGUI
    participant Service as MailStateService
    participant Repo as MailRepository
    participant DB as PostgreSQL
    participant Job as ExpireJob

    Player->>GUI: 打开邮箱
    GUI->>Service: openInbox(ownerUuid)
    Service->>Repo: markUnreadAsViewed(ownerUuid, visiblePackageIds)
    Repo->>DB: UPDATE mail_packages SET read_state='READ', read_at=now()
    DB-->>Repo: updated rows
    Repo-->>Service: success
    Service-->>GUI: inbox entries

    Player->>GUI: 点击领取
    GUI->>Service: claimMail(ownerUuid, packageId)
    Service->>Repo: begin claim transaction
    Repo->>DB: BEGIN
    Repo->>DB: SELECT package FOR UPDATE
    alt 状态为 AVAILABLE
        Repo->>DB: UPDATE lifecycle_state='CLAIMING', claim_started_at=now(), claim_token=uuid
        Repo->>DB: INSERT mail_events(CLAIM_STARTED)
        Repo->>DB: COMMIT
        Service-->>GUI: 返回附件与 claimToken
        GUI-->>Player: 发放物品
        GUI->>Service: completeClaim(packageId, claimToken)
        Service->>Repo: UPDATE lifecycle_state='CLAIMED', claimed_at=now()
        Repo->>DB: INSERT mail_events(CLAIMED)
        DB-->>Service: success
    else 状态非 AVAILABLE
        Repo->>DB: ROLLBACK
        Service-->>GUI: 返回“已被领取/已失效”
        GUI-->>Player: 提示失败原因
    end

    Job->>Service: 定时扫描过期邮件
    Service->>Repo: expireAvailableMails(now)
    Repo->>DB: UPDATE lifecycle_state='EXPIRED' WHERE expires_at < now()
    Repo->>DB: INSERT mail_events(EXPIRED)
    DB-->>Service: expired count
```

**问题分析**
- 这里真正需要的是“行级锁”或同等语义，而不是 JVM 级全局锁。锁的对象应该是单封邮件记录。
- 读取和状态切换必须由服务层驱动，GUI 不能直接访问数据库。
- `CLAIMING -> CLAIMED` 是为了解决“发物品”和“改状态”不在同一事务的问题。
- 如果发物品失败，应有 `CLAIM_FAILED` 或重试恢复逻辑，而不是简单回滚数据库。

---

## 4.3 预期：正式服安全领取流程

```mermaid
sequenceDiagram
    autonumber
    actor Player as 玩家
    participant GUI as MailboxGUI
    participant Service as ClaimService
    participant Repo as MailRepository
    participant DB as PostgreSQL
    participant Inv as PlayerInventory

    Player->>GUI: 点击领取某封邮件
    GUI->>Inv: 检查背包空间
    alt 空间不足
        GUI-->>Player: 提示“请先清理背包”
    else 空间足够
        GUI->>Service: startClaim(ownerUuid, packageId)
        Service->>Repo: SELECT ... FOR UPDATE
        Repo->>DB: 锁定邮件记录
        alt 邮件可领取
            Repo->>DB: UPDATE lifecycle_state='CLAIMING'
            Repo-->>Service: 返回附件列表 + claimToken
            Service-->>GUI: 附件列表 + claimToken
            GUI->>Inv: addItem(attachments...)
            alt 发放全部成功
                GUI->>Service: finishClaim(packageId, claimToken)
                Service->>Repo: UPDATE lifecycle_state='CLAIMED'
                Repo->>DB: INSERT event CLAIMED
                GUI-->>Player: 提示“领取成功”
            else 发放中途失败
                GUI->>Service: failClaim(packageId, claimToken, reason)
                Service->>Repo: UPDATE lifecycle_state='CLAIM_FAILED'
                Repo->>DB: INSERT event CLAIM_FAILED
                GUI-->>Player: 提示“领取失败，请联系管理员”
            end
        else 邮件已过期或已领取
            Repo-->>Service: reject
            Service-->>GUI: reject
            GUI-->>Player: 提示“邮件不可领取”
        end
    end
```

**问题分析**
- 这是 PostgreSQL 版本最核心的事务图。没有这条链路，正式服版本不成立。
- 背包空间校验应该尽量前置，但最终仍要容忍中途失败，因为附件可能多件、Bukkit 行为可能异常。
- 不建议使用插件级全局互斥锁锁整个领取系统，那会放大延迟和阻塞。正确做法是“按邮件 ID 做数据库锁定或细粒度互斥”。

---

## 5. 分阶段缺失组件清单

### 阶段一：原型稳定性

缺失或不稳部分：

- 配置文件化数据库连接
- 统一缓存键策略
- 移除 `currentPageX` 静态页码
- 服务层抽象
- 结果对象和错误码

### 阶段二：玩家发件功能

缺失部分：

- `send` 命令
- 发件确认流程
- 目标玩家解析与离线支持
- 发件成功后的物品扣除逻辑
- 发件幂等键

### 阶段三：正式服能力

缺失部分：

- PostgreSQL repository
- 事务化领取流程
- 邮件状态机
- 管理员命令与查询能力
- 审计事件表
- 过期任务和恢复机制

---

## 6. 一句话结论

当前项目已经有了三个真实存在的时序主干：

- 打开邮箱
- 保存邮件
- 领取附件

但它们都还停留在“GUI 和数据库直接耦合”的原型形态。下一版如果要迁移到 PostgreSQL 并迈向正式服，必须把这些时序重构为：

- 命令 / GUI
- 服务层
- 事务化仓储层
- 状态机与审计层

这样的分层交互。

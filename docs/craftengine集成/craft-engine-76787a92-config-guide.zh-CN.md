# CraftEngine 配置指南（基于提交 `76787a92f6d3511fc913fa02c93671ccc84d953d`）

## 1. 文档目标与范围

这份文档面向“要自己做内容包并稳定上线”的使用者，重点讲：

- 如何拿到固定版本源码
- `config.yml` / `commands.yml` 如何配置
- `resources/<pack>/configuration/*.yml` 的 section 结构与字段
- `items / blocks / furniture / recipes / images / emoji / sounds` 的可落地写法
- 常见坑和排错路径

源码基准：

- 仓库：`https://github.com/Xiao-MoMi/craft-engine`
- 提交：`76787a92f6d3511fc913fa02c93671ccc84d953d`

注：当前环境无法稳定直连 GitHub `git clone`，已通过 codeload 拉取该提交快照并解压到本地 `craft-engine/`。

---

## 2. 获取源码（固定提交）

### 2.1 标准方式（推荐）

```bash
git clone https://github.com/Xiao-MoMi/craft-engine.git
cd craft-engine
git checkout 76787a92f6d3511fc913fa02c93671ccc84d953d
```

### 2.2 备用方式（网络受限时）

```powershell
Invoke-WebRequest `
  -Uri "https://codeload.github.com/Xiao-MoMi/craft-engine/zip/76787a92f6d3511fc913fa02c93671ccc84d953d" `
  -OutFile "craft-engine-76787a9.zip"

tar -xf craft-engine-76787a9.zip
```

---

## 3. 项目结构（学习时先看这个）

```text
craft-engine/
  core/                     # 核心配置解析与跨平台逻辑
  bukkit/                   # Bukkit/Paper 平台实现
  common-files/src/main/resources/
    config.yml              # 主配置模板
    commands.yml            # 命令开关与权限模板
    resources/
      default/              # 默认示例内容包（最重要学习样本）
      internal/             # 内置 GUI / 字体偏移 / 内置资源
      remove_shulker_head/  # 特定版本兼容包（默认禁用）
```

关键源码入口：

- `core/.../pack/AbstractPackManager.java`
- `core/.../plugin/config/Config.java`
- `core/.../plugin/config/template/TemplateManagerImpl.java`
- `core/.../item/AbstractItemManager.java`
- `bukkit/.../block/BukkitBlockManager.java`
- `core/.../entity/furniture/AbstractFurnitureManager.java`

---

## 4. 构建与运行要求

从 Gradle 脚本可确认：

- Java Toolchain：`21`
- Paper API 目标：`1.21.5`

构建：

```bash
cd craft-engine
./gradlew build
```

Bukkit 模块产物命名：

- `craft-engine-bukkit-<project_version>.jar`

---

## 5. 启动后数据目录约定

插件数据目录（通常是 `plugins/CraftEngine/`）下会有：

- `config.yml`
- `commands.yml`
- `resources/`
  - 每个子目录就是一个 pack（比如 `default`）
  - 每个 pack 可含：
    - `pack.yml`
    - `configuration/*.yml`
    - `resourcepack/**`

---

## 6. Pack 机制（核心理解）

### 6.1 pack.yml

`resources/<pack>/pack.yml` 常用字段：

```yaml
author: XiaoMoMi
version: 0.0.1
description: Demo Pack
namespace: default
enable: true
```

说明：

- `namespace` 决定该 pack 的默认命名空间
- `enable: false` 可整体关闭该包

### 6.2 配置文件扫描规则

源码行为（`AbstractPackManager.updateCachedConfigFiles`）：

- 只扫描 `configuration/` 下后缀为 `.yml` 的文件
- 顶层 section 由“配置类型 + 数据集合”组成

例如：

```yaml
items:
  mypack:foo: { ... }

items#weapons:
  mypack:bar: { ... }
```

`items#weapons` 的 `#weapons` 只是分组标签，解析类型仍是 `items`。

### 6.3 section 解析顺序（源码固定）

来自 `LoadingSequence`：

1. `templates` / `template`
2. `global-variables` / `global-variable`
3. `lang` / `language` / `languages`
4. `i18n` / `internationalization` / `translation` / `translations`
5. `blocks` / `block`
6. `items` / `item`
7. `furniture`
8. `images` / `image`
9. `recipes` / `recipe`
10. `categories` / `category`
11. `sounds` / `sound`
12. `jukebox_songs` / `song` / `songs` / `jukebox` / `jukebox_song`
13. `vanilla-loots` / `vanilla-loot` / `loots` / `loot`
14. `emoji` / `emojis`
15. `advancements` / `advancement`

---

## 7. 主配置 `config.yml`

### 7.1 基础开关

- `debug`
- `metrics`
- `update-checker`
- `forced-locale`
- `filter-configuration-phase-disconnect`

### 7.2 资源包系统 `resource-pack.*`

常用：

- `override-uniform-font`
- `generate-mod-assets`
- `supported-version.min / max`
- `merge-external-folders`
- `merge-external-zip-files`
- `exclude-file-extensions`
- `validate.enable`
- `remove-tinted-leaves-particle`

分发：

- `delivery.send-on-join`
- `delivery.kick-if-declined`
- `delivery.prompt`
- `delivery.hosting`（支持 `self/external/s3/alist/dropbox/onedrive/gitlab/none` 等）
- `delivery.auto-upload`
- `delivery.file-to-upload`
- `delivery.resend-on-upload`

混淆与防拆：

- `protection.crash-tools.method-1/2/3`
- `protection.obfuscation.*`

冲突处理（重复文件）：

- `duplicated-files-handler`
  - 匹配器类型：`exact / contains / filename / parent_path_suffix / parent_path_prefix / pattern / any_of / all_of / !...`
  - 处理器类型：`retain_matching / merge_json / merge_atlas / merge_pack_mcmeta / conditional`

### 7.3 其他核心开关

- `item.non-italic-tag`
- `block.sound-system.enable`
- `block.simplify-adventure-break-check`
- `block.simplify-adventure-place-check`
- `block.predict-breaking.*`
- `furniture.hide-base-entity`
- `furniture.collision-entity-type`（`interaction` 或 `boat`）
- `emoji.*`
- `image.illegal-characters-filter.*`
- `image.intercept-packets.*`
- `recipe.enable`
- `recipe.disable-vanilla-recipes.all/list`
- `performance.max-note-block-chain-update-limit`
- `performance.max-emojis-per-parse`
- `light-system.enable`
- `chunk-system.*`

### 7.4 需要重启生效的项（按源码/注释）

- `commands.yml` 修改后
- `chunk-system.injection.target`
- `furniture.collision-entity-type`

---

## 8. 命令配置 `commands.yml`

你可以逐条开关命令：

```yaml
reload:
  enable: true
  permission: ce.command.admin.reload
  usage:
    - /craftengine reload
    - /ce reload
```

常见命令组：

- 管理：`reload`, `upload`, `send_resource_pack`
- 物品：`get_item`, `give_item`, `item_browser_admin`
- 玩家查询：`search_usage_player`, `search_recipe_player`
- 资源包：`enable_resource`, `disable_resource`, `list_resource`
- 调试：`debug_*`

---

## 9. 模板系统（强烈建议先学）

section：

- `templates` / `template`

支持：

- `template`
- `arguments`
- `overrides`
- `merges`

内置参数类型（`TemplateArguments`）：

- `plain`
- `self_increase_int`
- `map`
- `list`
- `null`
- `expression`

典型用法：把重复的 model / recipe / loot / block settings 抽到模板里，然后在业务配置中 `template + arguments`。

---

## 10. 全局变量与翻译

### 10.1 全局变量

section：

- `global-variables` / `global-variable`

用于统一文本片段或参数，按键值对存储。

### 10.2 i18n 与 lang

- `i18n`：服务端渲染文本（MiniMessage）
- `lang`：客户端语言键（常用于资源包内 `block_name:*` 等）

示例：

```yaml
i18n:
  en:
    item.ruby: "Ruby"
  zh_cn:
    item.ruby: "红宝石"
```

---

## 11. Items 配置

section：

- `items` / `item`

### 11.1 常用字段

- `material`（必填，非原版覆写时）
- `client-bound-material`（可选）
- `custom-model-data`（可选）
- `item-model`（1.21.2+）
- `settings`
- `data`
- `client-bound-data`
- `model`
- `legacy-model`
- `behavior` / `behaviors`
- `events` / `event`
- `category`
- `enable`

### 11.2 `data` 可用键（源码注册）

- `external`
- `custom-name`
- `item-name` / `display-name`
- `lore` / `display-lore` / `description`
- `dynamic-lore`
- `dyed-color`
- `tags` / `tag` / `nbt`
- `unbreakable`
- `enchantment` / `enchantments` / `enchant`
- `trim`
- `components` / `component`（1.20.5+）
- `remove-components` / `remove-component`（1.20.5+）
- `food`（1.20.5+）
- `jukebox-playable`（1.21+）
- `tooltip-style`（1.21.2+）
- `equippable`（1.21.2+）
- `args` / `argument` / `arguments`

### 11.3 Item Behavior 类型（Bukkit）

- `block_item`
- `liquid_collision_block_item`
- `furniture_item`
- `water_bucket_item`
- `bucket_item`
- `flint_and_steel_item`
- `compostable_item`

---

## 12. Blocks 配置

section：

- `blocks` / `block`

### 12.1 常用字段

- `settings`
- `state` / `states`
- `behavior` / `behaviors`
- `loot`
- `events` / `event`

### 12.2 `state` 与 `states`

单状态（`state`）常见：

- `id`
- `state`（映射到 vanilla 承载状态，如 `note_block:13`）
- `model` / `models`

多状态（`states`）常见：

- `properties`
- `appearances`
- `variants`

### 12.3 属性类型（`states.properties.*.type`）

- `boolean`
- `int`
- `string`
- `axis`
- `4-direction`
- `6-direction`

### 12.4 Block Settings 常用键（源码工厂）

- `luminance`
- `block-light`
- `hardness`
- `resistance`
- `is-randomly-ticking`
- `propagate-skylight`
- `push-reaction`
- `map-color`
- `burnable`
- `instrument`
- `item`
- `tags`
- `burn-chance`
- `fire-spread-chance`
- `replaceable`
- `is-redstone-conductor`
- `is-suffocating`
- `is-view-blocking`
- `sounds`
- `fluid-state`
- `can-occlude`
- `correct-tools`
- `require-correct-tools`
- `respect-tool-component`
- `incorrect-tool-dig-speed`
- `name`
- `support-shape`

### 12.5 Block Behavior 类型（Bukkit）

- `falling_block`
- `bush_block`
- `hanging_block`
- `leaves_block`
- `strippable_block`
- `sapling_block`
- `on_liquid_block`
- `near_liquid_block`
- `waterlogged_block`
- `concrete_powder_block`
- `vertical_crop_block`
- `crop_block`
- `grass_block`
- `lamp_block`

---

## 13. Furniture 配置

section：

- `furniture`

常用结构：

```yaml
furniture:
  mypack:bench:
    settings: ...
    placement:
      ground:
        rules:
          rotation: FOUR
          alignment: CENTER
        elements: [...]
        hitboxes: [...]
    loot: ...
    events: ...
```

关键点：

- `placement` 必填
- 锚点：`ground / wall / ceiling`
- 旋转规则：`ANY / FOUR / EIGHT / SIXTEEN / NORTH / EAST / WEST / SOUTH`
- 对齐规则：`ANY / CORNER / CENTER / HALF / QUARTER`
- hitbox 类型：`interaction / shulker / happy_ghast / custom`
- 可接外部模型：
  - `model-engine`
  - `better-model`

---

## 14. Recipes 配置

section：

- `recipes` / `recipe`

`type` 支持：

- `minecraft:shaped`
- `minecraft:shapeless`
- `minecraft:smelting`
- `minecraft:blasting`
- `minecraft:smoking`
- `minecraft:campfire_cooking`
- `minecraft:stonecutting`
- `minecraft:smithing_transform`

实践建议：

- 直接复用默认模板：
  - `default:recipe/planks`
  - `default:recipe/log_2_wood`
  - `default:recipe/smelting_ore`
  - `default:recipe/blasting_ore`

---

## 15. Images / Emoji / Categories / Sounds

### 15.1 `images`

section：

- `images` / `image`

关键字段：

- `file`
- `font`
- `char` 或 `chars`
- `height`
- `ascent`

### 15.2 `emoji`

section：

- `emoji` / `emojis`

关键字段：

- `keywords`（必填）
- `permission`
- `content`
- `image`（`namespace:id` 或 `namespace:id:x:y`）

### 15.3 `categories`

section：

- `categories` / `category`

关键字段：

- `name`
- `lore`
- `icon`
- `list`
- `priority`
- `hidden`

### 15.4 `sounds` 与 `jukebox_songs`

sections：

- `sounds` / `sound`
- `jukebox_songs` / `song` / `songs` / `jukebox` / `jukebox_song`

`sounds` 常用字段：

- `replace`
- `subtitle`
- `sounds`（字符串或对象列表）

`jukebox_songs` 常用字段：

- `sound`
- `description`
- `length`
- `comparator-output`
- `range`

### 15.5 `vanilla-loots`

section：

- `vanilla-loots` / `vanilla-loot` / `loots` / `loot`

关键字段：

- `type`: `BLOCK` 或 `ENTITY`
- `target`: 目标列表（方块状态或实体 ID）
- `override`: 是否覆盖原版掉落
- `loot`: 掉落表（同 `LootTable` 结构）

### 15.6 `advancements`

section：

- `advancements` / `advancement`

写法：

- 每个 ID 对应一个 advancement JSON 结构（YAML 方式表达）
- 解析后会直接注册到服务端 advancement 注册表

---

## 16. 事件系统（events）

事件触发器（`EventTrigger`）：

- `attack` / `left_click`
- `right_click` / `use_on` / `use` / `use_item_on`
- `eat` / `consume` / `drink`
- `break` / `dig`
- `place` / `build`
- `step`

函数类型（`functions[].type`）：

- `command`
- `message`
- `actionbar`
- `title`
- `open_window`
- `cancel_event`
- `run`
- `place_block`
- `break_block`
- `update_interaction_tick`
- `set_count`
- `drop_loot`
- `swing_hand`
- `food`
- `saturation`
- `play_sound`
- `particle`
- `potion_effect`
- `remove_potion_effect`
- `leveler_exp`
- `set_cooldown`
- `remove_cooldown`

条件类型（`conditions[].type`）：

- `all_of`
- `any_of`
- `inverted` / `!xxx`
- `match_item`
- `match_block_property`
- `table_bonus`
- `survives_explosion`
- `random`
- `enchantment`
- `falling_block`
- `distance`
- `permission`
- `equals`
- `string_equals`
- `string_contains`
- `regex`
- `expression`
- `is_null`
- `hand`
- `on_cooldown`

---

## 17. 最小自定义包示例

### 17.1 目录

```text
plugins/CraftEngine/resources/my_pack/
  pack.yml
  configuration/
    items.yml
    blocks.yml
  resourcepack/
    assets/minecraft/textures/item/custom/ruby.png
    assets/minecraft/textures/block/custom/ruby_ore.png
```

### 17.2 pack.yml

```yaml
author: You
version: 1.0.0
description: Ruby Demo
namespace: mypack
enable: true
```

### 17.3 items.yml

```yaml
items:
  mypack:ruby:
    material: nether_brick
    custom-model-data: 20001
    data:
      item-name: "<!i><red>Ruby</red>"
    model:
      template: default:model/simplified_generated
      arguments:
        path: "minecraft:item/custom/ruby"
```

### 17.4 blocks.yml

```yaml
blocks:
  mypack:ruby_ore:
    loot:
      template: default:loot_table/self
    settings:
      template: default:settings/ore
      arguments:
        break_power: 2
    state:
      id: 20
      state: note_block:20
      model:
        template: default:model/simplified_cube_all
        arguments:
          path: "minecraft:block/custom/ruby_ore"
```

---

## 18. RookiePostBox GUI 按钮 CustomModelData 配置

本仓库已经提供一个 CraftEngine 示例包：

```text
docs/craftengine集成/rookiepostbox-craftengine/resources/rookiepostbox/
  pack.yml
  configuration/items.yml
  resourcepack/assets/minecraft/textures/item/custom/rookiepostbox/
    mailbox_previous.png
    mailbox_next.png
    unopened_box.png
    ready_box.png
```

部署时把 `docs/craftengine集成/rookiepostbox-craftengine/resources/rookiepostbox/` 复制到服务器的：

```text
plugins/CraftEngine/resources/rookiepostbox/
```

示例包使用 `brick` 作为基础材质，并分配以下 `custom-model-data`：

| CraftEngine item | 材质 | CustomModelData | 纹理 |
| --- | --- | ---: | --- |
| `rookiepostbox:mailbox_previous` | `brick` | `21001` | `mailbox_previous.png` |
| `rookiepostbox:mailbox_next` | `brick` | `21002` | `mailbox_next.png` |
| `rookiepostbox:unopened_box` | `brick` | `21003` | `unopened_box.png` |
| `rookiepostbox:ready_box` | `brick` | `21004` | `ready_box.png` |

`items.yml` 中的模型配置是自包含写法，不依赖 `default:model/simplified_generated` 等外部模板。这样即使 CraftEngine 的 `default` pack 被禁用，也不会出现 `Template not found`。

RookiePostBox 的 `config.yml` 对应写法：

```yaml
rookiepostbox:
  menu:
    buttons:
      previous:
        material: BRICK
        custom-model-data: 21001
      next:
        material: BRICK
        custom-model-data: 21002
    mail-item:
      material: BRICK
      custom-model-data: 21003
```

修改 CraftEngine pack 后，需要执行：

```text
/ce reload pack
```

生成完成后控制台应出现类似日志：

```text
[CraftEngine] Generating resource pack...
[CraftEngine] Finished generating resource pack in ...
[CraftEngine] Complete uploading resource pack
```

并生成：

```text
plugins/CraftEngine/generated/resource_pack.zip
```

如果要让玩家进服自动收到资源包，确认 `plugins/CraftEngine/config.yml`：

```yaml
resource-pack:
  delivery:
    send-on-join: true
    hosting:
      - type: "self"
        ip: "你的服务器公网IP或域名"
        port: 8163
        protocol: "http"
```

注意：`ip: "localhost"` 只适合服务端本机测试。真实玩家收到的下载地址会指向玩家自己电脑的 `localhost`，因此无法下载服务器资源包。公网服必须改成玩家客户端可访问的公网 IP 或域名，并放行 `8163` 端口。

---

## 19. 常见坑与排错

1. `configuration` 里文件后缀必须是 `.yml`（不是 `.yaml`）。
2. section 顶层必须是已注册类型（如 `items/blocks/...`）。
3. 条目默认可写 `enable: false` 来临时禁用。
4. `id` 冲突、`custom-model-data` 冲突会直接报错并跳过。
5. `resource-pack.delivery.hosting` 如果配置错误，会退回 `NoneHost`。
6. `SelfHost` 代码读取限流字段是 `rate-map`，默认配置写的是 `rate-limit`，这是版本内不一致点，实际生效以源码读取键为准。
7. 修改 `commands.yml` 后需要重启，不是热重载。
8. 遇到“配置结构不正确”时，先看控制台报错里 `prefix.key` 定位具体节点。

---

## 20. 推荐学习路径

1. 先完整读 `resources/default/configuration/templates.yml`。
2. 再读 `ores.yml / plants.yml / furniture.yml` 三个示例，理解模板复用。
3. 用最小包先做一个 `items + blocks`，确认资源包生成和下发流程。
4. 最后再接入 `events`、`furniture`、`emoji/images` 这些高级模块。

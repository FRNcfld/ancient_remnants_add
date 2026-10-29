# ancient_remnants_add
扩展模组

面向 Minecraft **1.20.1 / Forge** 的 [远古遗迹（Ancient Remnants）](https://modrinth.com/mod/ancient-remnants)
附属模组，[机械动力（Create）](https://modrinth.com/mod/create) 与
[Explorer's Compass（结构指南针）](https://modrinth.com/mod/explorers-compass) 作为可选联动。

> 当前状态：**已有玩法内容**。已实现多祝福、可配置的祝福数值、前哨站残片、
> 2×2×1 远古之心、结构级祭坛仪式（修复/长按激活/守护/献祭）、祭坛层级祝福加成
> 与机械概率扩散、残缺祭坛世界生成、「远古回响」「过往的幻影」「荣光再现」进度、
> Create 概率产出联动，以及 Explorer's Compass 跳过已激活祭坛。
> 加内容时按下面的「约定」一节往下写即可。

## 当前功能

### 多祝福

远古遗迹原本只允许玩家持有一个祝福；本 mod 改为最多 `maxBlessings`（默认 6）个：

- 每个祝福对应的 MobEffect 都会保持；
- 达到上限后，新获得的祝福替换列表中**最早获得**的那个；
- 列表随玩家 NBT 持久化，死亡/重生与跨维度切换会复制；
- 六个祝福的数值都可在 `[blessings]` 配置段调整（见下方配置表）；
- `/ancient_remnants blessing <player> remove` 会清空全部祝福；
  `/ancient_remnants blessing <player> set <blessing>` 会加入列表（满额时同样替换最早的）。

### 祝福机制调整

- **绝境活力**：生命值低于 50% 时获得临时吸收生命（默认 +50% 最大生命的黄色心），持续 10 秒；
  buff 结束时本祝福提供的吸收心消失，随后进入 5 分钟冷却。不会改变最大生命值。
  冷却、buff 结束时间和触发前的吸收值都会写入 NBT，重登/重生后到期仍会正确清理。
- **哨兵护盾**：每损失 1% 生命值，本次伤害计算使用的护甲值提高 1%（默认倍率 1.0，乘算）；
  满血无加成，损失 50% 生命时护甲 ×1.5。护甲为 0 时乘算仍为 0。
- 旧版的绝境活力「低血量给再生」与哨兵护盾「固定 +20 护甲」已被替换。

### 掠夺者前哨站残片

远古遗迹 1.5.2 只在 Fabric 端把 `ancient_remnants:extensions/pillager_outpost`
挂到前哨站箱子；Forge 端拿不到。本 mod 在 `LootTableLoadEvent` 里补上同样的引用，
每个前哨站箱子有 50% 概率掉一个随机研究残片（与 Fabric 一致）。

### 远古之心与「远古回响」

- 集齐全部研究条目并合并到一本巨石典籍后，获得挑战进度「远古回响」。
- 远古之心以 25% 概率出现在古城箱子中（`minecraft:chests/ancient_city` 与冰窖宝箱）。
- 远古之心是 **2×2×1 多方块**：主方块（ROOT）持有 BlockEntity 作为休眠核心的渲染锚点，
  另外三格是附属格；放置时一次校验并补齐四格，破坏任意一格会移除整组。
- 残缺祭坛：三种巨石与三种纪念碑周围 **8–32 区块**、每座遗迹独立 **5%** 概率生成一处
  （每座最多一处）；海洋/河流附近会放在海床。结构中央的 4 格是**空气**，玩家把 2×2
  远古之心放进去即可修复祭坛；站点记录会保留，可以反复修复。
- 激活：站在祭坛范围内，**Shift + 长按右键 2 秒**，手持一本已补齐全部条目的巨石典籍：
  - Shift 用于避免普通右键打开典籍界面；不按 Shift 时典籍界面照常打开；
  - 激活时典籍从手中消耗，并保存在祭坛记录中；
  - 激活后四格同步发光，休眠核心在**中央尖塔顶部上方**渲染（上游 Elderheart 模型）；
  - 激活瞬间会把整块 **32×25×32** 结构按模板修复：缺失/被替换的结构方块恢复原状，
    模板中为空气的位置（玩家放下的方块、地形、液体等无关方块）清成空气；
    中央 2×2 远古之心所在四格会保留；
  - 同时清除结构范围内的**非生物实体**：箭矢、掉落物、飞行中的末影珍珠、经验球、
    掉落沙、TNT、船/矿车等；玩家和生物（村民、宠物等）保留；
  - 激活后整个 **32×25×32** 祭坛结构不可破坏：玩家挖掘被取消，爆炸不会破坏结构方块，
    活塞不能推动/破坏结构方块；
  - 在祭坛丰收生效的范围内（<3 座仅祭坛范围内；3–8 座本维度；9+ 座源/目标维度），
    所有通过原版战利品掉落的作物破坏/收割（玩家、活塞、水流、爆炸、Create 收割机等）
    都按 `bountifulHarvestChance` 额外掉一份；
  - 作物范围由 `ancient_remnants_add:altar_harvestable` 方块标签控制，默认包含
    CropBlock、下界疣、甘蔗、甜浆果、可可、竹子、仙人掌、海带、西瓜、南瓜；
  - 甜浆果右键收割也覆盖；
  - 玩家自己带丰收祝福且破坏成熟的 CropBlock / NetherWart 时仍走上游原本的
    playerDestroy 逻辑，不会重复触发。
- 献祭：激活后，在祭坛范围内由**玩家**击杀 `ancient_remnants_add:altar_sacrifices`
  实体标签内生物（默认只有村民）→ 远古之心四格被摧毁、祭坛熄灭，典籍掉落在祭坛中心；
  站点保留，可以重新放远古之心再次激活。
- 未激活的远古之心可正常破坏：需要**铁镐及以上** + **精准采集**才会掉落物品；
  激活时不可破坏；献祭销毁时不掉方块物品，只掉典籍。
- 新进度：「过往的幻影」（发现残缺祭坛，探索）与「荣光再现」（激活祭坛，挑战）。
- 旧存档注意：新的 4 格空气模板只影响新生成的区块；旧的 `Altars` 活动祭坛数据不迁移，
  旧祭坛里的典籍也不会自动保留。旧结构可以拆掉远古之心后重新放置，触发新的站点登记。
- 创造模式物品栏：已加入「工具与实用物品」标签（与典籍、残片一致），搜索也能搜到。
- 风弹/旋风人刷怪蛋的中文名由本 mod 的 `zh_cn` 补齐为「风弹」「旋风人刷怪蛋」。

### 祭坛层级祝福加成

- 每座激活祭坛让**数据文件指定的祝福**效果 +100%（`altarBlessingBoostPerAltar`，默认 1.0，加算叠加）；
  每种祝福有独立的叠加上限。
- 倍率 = `1 + altarBlessingBoostPerAltar × min(有效祭坛数, max_stacks)`。
- 范围：
  - **<3 座**：只有站在任一激活祭坛 **32×25×32** 范围内的玩家获得加成；
  - **3–8 座**：该维度所有玩家获得；
  - **9+ 座**：源维度 + 数据文件 `spread_dimensions` 列出的其他维度；多个 9+ 源取最大源计数。
- 数据文件 `data/ancient_remnants_add/altar_boost.json`（数据包可覆盖，`/reload` 生效）：
  ```json
  {
    "blessings": [
      { "blessing": "ancient_remnants:hunter_instincts", "max_stacks": 9 }
    ],
    "spread_dimensions": ["minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"]
  }
  ```
  - 缺省某个祝福 = 不 boost；缺省 `spread_dimensions` = 9+ 只影响源维度自身；
  - 出厂文件包含六种祝福，`max_stacks` 默认 9；可自行调低/删除条目。
- 覆盖的数值：猎人本能/战士之怒伤害、哨兵护甲、丰收概率（封顶 1.0）、绝境活力吸收、建筑师之触。
- Create 机械概率：**1–2 座不启用；3–8 座本维度 ×1.5；9+ 源维度与 `spread_dimensions` 目标维度启用**。
- 祭坛丰收（`altar_harvestable`）也使用同一层级范围：**<3 座仅祭坛范围内；3–8 座本维度；9+ 座源/目标维度**；概率仍是 `bountifulHarvestChance`，不随祭坛数叠加。

### JEI 教程

安装 JEI 后，在 JEI 中查看**远古之心**或**巨石典籍**的物品信息（默认 `R` 键），
会显示祭坛教程页：

- 获取与修复；
- Shift + 长按右键激活；
- 激活后的结构修复/清场/不可破坏与休眠核心；
- 祝福加成层级（<3 / 3–8 / 9+）与数据文件 `altar_boost.json`；
- 祭坛丰收（`altar_harvestable`）的同一层级范围；
- Create 概率的层级启用；
- 献祭与重新激活；
- 进度、精准采集和旧存档说明。

JEI 仍是可选前置；未安装 JEI 时插件类不会被加载，游戏与祭坛机制不受影响。

### 典籍中文显示

上游的典籍界面会把研究标题/正文强制套上 `ancient_remnants:header` 的 TTF 字体，
该字体没有中文字形，中文会显示成方框（missing glyph）。本 mod 新建
`ancient_remnants_add:codex` 字体：provider 先放上游 `header.ttf`，再 reference
`minecraft:default`，所以拉丁字符继续使用装饰字体，中文自动回退到原版字体。
客户端 mixin 只改典籍页面的字体，服务端不受影响。

### Create 联动

- **1–2 座**激活祭坛：不启用 Create 概率加成。
- **3–8 座**：该维度内 Create 的 `ProcessingOutput` 概率产出 chance ×1.5（封顶 100%）。
- **9+ 座**：源维度 + `altar_boost.json` 中 `spread_dimensions` 列出的目标维度都启用。
- 覆盖粉碎轮、磨石、锯、工作盆混合、手动应用、注液/排空以及 Deployer/Fan 使用的
  `RecipeApplier` 路径。
- 没有达到层级的维度、JEI 预览等没有机器上下文的掷骰不会增强。
- Create 未安装时，`mixin/create` 下的 mixin 由 `AncientRemnantsAddMixinPlugin` 跳过，
  不会因为缺少 Create 类而崩溃。

### Explorer's Compass 联动

- 结构指南针搜索「残缺祭坛」（`ancient_remnants_add:ancient_heart_ruin`）时，
  **已激活**的祭坛不会被当作目标；搜索会继续向外找未激活的遗迹。
- 未修复、已修复但未激活的祭坛仍可被正常定位；激活后再次搜索不会把玩家引向该据点。
- 三个搜索 worker（RandomSpread / ConcentricRings / Generic）都经过
  `StructureSearchWorker#getStructureGeneratingAt`；过滤只针对本模组的
  `AncientHeartRuinStructure`，不按坐标一刀切，因此单结构搜索与「全部结构」分组搜索行为一致。
- Explorer's Compass 未安装时，`mixin/explorerscompass` 下的 mixin 由
  `AncientRemnantsAddMixinPlugin` 跳过，不会因为缺少上游类而崩溃。
- 版本以 `gradle.properties` 的 `explorers_compass_version`（1.20.1-1.4.0）为基准；
  注入使用 `require = 0`，上游改版导致注入点消失时退回原版搜索行为，不影响启动。

## 环境要求

| 项目 | 版本 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x（`loaderVersion="[47,)"`，开发用 47.4.10） |
| Java | 17 |
| 映射 | Parchment `2023.08.20-1.20.1`（叠加官方映射；联调 Create 6.0.x 时必须与它一致） |
| Create（可选） | 6.0.8+；未安装也能启动 |
| Explorer's Compass（可选联动） | 1.20.1-1.4.0（`explorerscompass`）；未安装也能启动 |

## 依赖说明

版本号统一放在 `gradle.properties`，声明在 `build.gradle`，`mods.toml` 里同步声明版本范围。

| 依赖 | 坐标 | 声明方式 | 为什么 |
|---|---|---|---|
| Create（机械动力，可选联动） | `com.simibubi.create:create-1.20.1:6.0.8-289:slim` | `compileOnly` + `runtimeOnly`（仅开发环境） | `mods.toml` 里 `mandatory=false`，玩家没装 Create 也能启动；`slim` 分类器不内嵌依赖，所以 Ponder / Registrate / Flywheel 要一并声明。以后新增直接引用 Create 类的代码必须做存在性隔离 |
| Ancient Remnants（远古遗迹） | `maven.modrinth:ancient-remnants:r3G6hu9a`（= 1.5.2 / 1.20.1-forge） | `implementation` | 硬依赖，代码直接引用它的类 |
| Fragmentum | `maven.modrinth:fragmentum:T7QOSm4k`（= 5.4.0 / 1.20.1-forge） | `compileOnly` + `runtimeOnly` | 远古遗迹的硬前置框架（它的 `mods.toml` 里 `mandatory=true`、`[5.2.0,)`）。上游类的公开签名里带有它的类型（如 `Deferred`），编译期需要可见；玩家环境由远古遗迹的 mandatory 依赖提供，不打包 |
| Explorer's Compass（结构指南针，可选联动） | `maven.modrinth:explorers-compass:7ZdJbCOx`（= 1.20.1-1.4.0-forge） | `compileOnly` + `runtimeOnly`（仅开发环境） | `mods.toml` 里 `mandatory=false`，玩家没装也能启动；只用于编译期引用与开发环境联调，不打包 |
| JEI | `mezz.jei:jei-1.20.1-forge-api:15.56.0.205` | `compileOnly`（API）+ `runtimeOnly`（完整 jar） | 只是可选兼容，`mods.toml` 里 `mandatory=false`，不打包、不强制玩家安装 |

**为什么用 Modrinth 的「版本 id」而不是版本号**：Modrinth 的 maven 里同一个版本号会对应多个
加载器/游戏版本的产物（1.5.2 同时有 fabric / forge / neoforge），用版本 id 才能锁定到
`1.20.1-forge` 的那一份。版本 id 与人类可读版本的对应关系写在 `gradle.properties` 的注释里。

### 开发环境补齐的库（不随本 mod 分发）

Fragmentum 用 **jarjar** 内嵌了 Kotlin for Forge、YetAnotherConfigLib 与 MixinExtras。
玩家环境由 Forge 自动解包；但 ForgeGradle 只把依赖 jar 放进开发环境 classpath，
**不会解包 jarjar**，所以这三份要在 `build.gradle` 里显式声明成 `runtimeOnly`，
否则开发环境启动时会在第一个用到它们的类上 `NoClassDefFoundError`
（同仓库 `create_improve` 显式补 Ponder / Flywheel 是同一个原因）。

| 库 | 坐标来源 | 谁需要它 |
|---|---|---|
| Kotlin for Forge 4.11.0 | `maven.modrinth:kotlin-for-forge:hmeyC41q` | 远古遗迹与 Fragmentum 都是 Kotlin 写的 |
| YetAnotherConfigLib 3.6.6+1.20.1-forge | `maven.modrinth:yacl:sCWgXDYQ` | Fragmentum 的游戏内配置界面 |
| MixinExtras 0.5.0 | `io.github.llamalad7:mixinextras-forge`（Maven Central） | Create 6.0 与 Fragmentum 的 mixin 都用到；取 0.5.0 与 Fragmentum 内嵌版本一致，生产环境 jarjar 冲突时也是取高版本 |

## 配置

`config/ancient_remnants_add-common.toml`：

| 选项 | 默认值 | 说明 |
|---|---|---|
| `debugLogging` | `true` | 输出本 mod 的调试日志 |
| `maxBlessings` | `6` | 玩家同时持有的祝福上限（1–64）；满额时新祝福替换最早获得的 |
| `altarBlessingBoostPerAltar` | `1.0` | 每座计数祭坛的祝福加成（1.0 = +100%），0 关闭；按 `altar_boost.json` 的 `max_stacks` 截断 |

`[blessings]` 段（六个祝福的数值）：

| 选项 | 默认值 | 说明 |
|---|---|---|
| `bountifulHarvestChance` | `0.5` | 丰收额外掉一份的概率（0–1） |
| `hunterInstinctsMaxDistance` | `64.0` | 猎人本能达到最大增伤的距离（格） |
| `hunterInstinctsMaxDamageBonus` | `1.0` | 猎人本能最大额外伤害倍率（1.0 = +100%） |
| `architectReachBonus` | `3.0` | 建筑师之触额外方块交互距离（格） |
| `desperateVitalityHealthThreshold` | `0.5` | 生命值低于该比例时触发绝境活力（0–1） |
| `desperateVitalityAbsorptionHealthPercentage` | `0.5` | 临时黄色吸收心占最大生命的比例（0.5 = +50%） |
| `desperateVitalityDuration` | `200` | 临时生命加成持续时间（tick，200 = 10 秒） |
| `desperateVitalityCooldown` | `6000` | buff 结束后的冷却时间（tick，6000 = 5 分钟） |
| `warriorWrathMonsterRadius` | `12.0` | 战士之怒统计怪物用的半径（格） |
| `warriorWrathMaxMonsters` | `12` | 战士之怒达到最大增伤所需的怪物数 |
| `warriorWrathMaxDamageBonus` | `1.0` | 战士之怒最大额外伤害倍率（1.0 = +100%） |
| `sentinelAegisArmorBonusPerHealthPercent` | `1.0` | 每损失 1% 生命，本次伤害护甲值提高的百分比（1.0 = +1%） |

祝福数值在效果触发时实时读取；`architectReachBonus` 会在配置加载后刷新属性修正器，
在线玩家会在下一 tick 重新挂上。

旧版的 `desperateVitalityRegenerationDuration`、`desperateVitalityRegenerationAmplifier`、
`sentinelAegisMaxArmorBonus` 已移除；上一轮的 `desperateVitalityBonusHealthPercentage`
也已改名为 `desperateVitalityAbsorptionHealthPercentage`。已有配置文件里如果还有这些键，
可以手动删除。`desperateVitalityHealthThreshold` 的默认值已从 0.33 改为 0.5，
但已有配置文件不会自动改，需要手动修改或删掉该键让新默认值生效。

## 构建

```bash
./gradlew build       # 产物在 build/libs/
./gradlew runClient   # 开发环境启动客户端
./gradlew runServer   # 开发环境启动服务端
./gradlew runData     # 数据生成（目前没有 DataProvider，仅占位）
```

首次构建要下载并反编译 Minecraft 与上游/联动 jar，耗时较长；后续增量构建约十几秒。

## 目录结构

```
src/main/java/com/frnc/ancient_remnants_add/
├── AncientRemnantsAdd.java   # 主入口（@Mod）：注册事件总线、通用配置，并给出 id() 快捷方法
├── Config.java               # 通用配置（config/ancient_remnants_add-common.toml）
├── altar/                    # 远古之心、站点 SavedData、长按激活、结构保护、献祭、层级加成、进度
├── blessing/                 # 多祝福列表、NBT 编解码、tick 同步与祝福数值后处理
├── client/                   # 远古之心 BlockEntityRenderer（尖塔上方的休眠核心）
├── create/                   # Create 概率产出 +50% 的线程上下文
├── jei/                      # JEI 物品信息页（祭坛教程，可选）
├── loot/                     # 前哨站残片掉落接入
├── worldgen/                 # 残缺祭坛结构、afterPlace 站点登记、遗迹邻近放置、结构模板片段
└── mixin/                    # 多祝福 / 上游 API / 祝福效果 / 祭坛 / 典籍字体；create/、explorerscompass/ 为可选联动，client/ 为客户端修复
src/main/resources/
├── META-INF/mods.toml        # 模组元数据（modId / 版本 / 硬依赖与可选联动）
├── assets/ancient_remnants_add/   # 远古之心模型、中英文 lang（含进度文本/风弹/旋风人译名）
├── data/ancient_remnants_add/     # 战利品、古城注入、作物/献祭实体标签、三个进度、残缺祭坛结构与世界生成、altar_boost.json
├── pack.mcmeta               # 资源包描述
└── ancient_remnants_add.mixins.json   # mixin 清单（11 个通用 + 9 个 Create 可选 + 1 个 Explorer's Compass 可选 + 1 个客户端）
```

## 约定

- **命名空间**：一切注册内容都挂在 `ancient_remnants_add` 下，Java 里用 `AncientRemnantsAdd.id("xxx")` 生成 `ResourceLocation`。
- **注册内容**：每个功能子包自己持有 `DeferredRegister` 并提供一个 `register(IEventBus)`，由主类构造函数逐个调用。
- **事件**：`MinecraftForge.EVENT_BUS.register(this)` 已在主类里接好，直接加 `@SubscribeEvent` 方法即可。
- **配置**：`Config` 的取值只放在访问器方法体里，不要写成 `static final` 字段——配置的加载时机晚于方块/物品的构造。
  说明文本写在 `mods.toml` 的 `description` 里，不要放进 `gradle.properties`（Gradle 以 ISO-8859-1 读取它，中文会乱码）。
- **可选联动（Create）**：`mods.toml` 里 `mandatory=false`；`build.gradle` 里 Create/Ponder/Registrate
  用 `compileOnly` + 仅开发环境的 `runtimeOnly`，不打包。以后新增直接引用 Create 类的代码时，
  必须放在 `ModList.get().isLoaded(CREATE_MOD_ID)` 分支或独立的 `@Mod.EventBusSubscriber` 里，
  否则没装 Create 的整合包会在类加载阶段 `NoClassDefFoundError`。Create 专用 mixin 放在
  `mixin/create`，由 `AncientRemnantsAddMixinPlugin` 在 Create 不存在时跳过。
- **可选依赖（JEI）**：JEI 是**软依赖**——`mods.toml` 里 `mandatory=false`、`side="CLIENT"`，
  `build.gradle` 里只 `compileOnly` API（不打包），完整 jar 只 `runtimeOnly` 进开发环境方便调试兼容。
  因此引用 JEI 的类必须放在**不会被无条件加载**的地方（单独的 `@JeiPlugin` 类，或先
  `ModList.get().isLoaded("jei")` 再进分支），否则没装 JEI 的整合包会在类加载阶段 `NoClassDefFoundError`。
- **可选联动（Explorer's Compass）**：同样是软依赖——`mods.toml` 里 `mandatory=false`，
  `build.gradle` 里 `compileOnly` + 仅开发环境的 `runtimeOnly`，不打包。
  引用其类的普通代码要放在 `ModList.get().isLoaded("explorerscompass")` 分支或单独的类里，
  否则没装它的整合包会在类加载阶段 `NoClassDefFoundError`。Explorer's Compass 专用 mixin
  放在 `mixin/explorerscompass`，由 `AncientRemnantsAddMixinPlugin` 在它不存在时跳过；
  mixin 类本身只把上游类名写在 `@Mixin(targets = ...)` 字符串里，不直接引用上游类型。
- **mixin**：MixinGradle 管线（含 refmap 与开发环境 `mixin.env.remapRefMap`）已接好。
  新增 mixin 类时把它登记进 `ancient_remnants_add.mixins.json` 的 `mixins`（通用）或 `client`（仅客户端）列表
  ——**漏登记的典型症状是「编译进 jar 但运行时不生效」**，不会报错。

## 许可证

[MIT](LICENSE)

# ancient_remnants_add
扩展模组

面向 Minecraft **1.20.1 / Forge** 的 [远古遗迹（Ancient Remnants）](https://modrinth.com/mod/ancient-remnants)
附属模组，[机械动力（Create）](https://modrinth.com/mod/create) 作为可选联动。

> 当前状态：**已有玩法内容**。已实现多祝福、可配置的祝福数值、前哨站残片、
> 巨石祭坛、「远古回响」挑战进度与 Create 概率产出 +50% 联动。
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

### 巨石祭坛与「远古回响」

- 集齐全部研究条目并合并到一本巨石典籍后，获得挑战进度「远古回响」。
- 巨石祭坛以 25% 概率出现在古城箱子中（`minecraft:chests/ancient_city` 与冰窖宝箱）。
- 已获得「远古回响」的玩家手持完整典籍右键祭坛，会把典籍放上去并激活：
  - 激活后祭坛中央悬浮渲染缩小版的「休眠核心」（上游 Elderheart 模型），不再显示典籍；
  - 空手右键已激活的祭坛会取回典籍并停用祭坛；
  - 祭坛所在维度内，所有通过原版战利品掉落的作物破坏/收割（玩家、活塞、水流、
    爆炸、Create 收割机等）都按 `bountifulHarvestChance` 额外掉一份；
  - 作物范围由 `ancient_remnants_add:altar_harvestable` 方块标签控制，默认包含
    CropBlock、下界疣、甘蔗、甜浆果、可可、竹子、仙人掌、海带、西瓜、南瓜；
  - 甜浆果右键收割也覆盖；
  - 玩家自己带丰收祝福且破坏成熟的 CropBlock / NetherWart 时仍走上游原本的
    playerDestroy 逻辑，不会重复触发。
- 祭坛方块需要**铁镐及以上** + **精准采集**才会掉落祭坛物品；否则方块会被破坏，
  但不掉祭坛。祭坛里的典籍始终会掉出，并停用该维度的效果。
- 创造模式物品栏：已加入「工具与实用物品」标签（与典籍、残片一致），搜索也能搜到。
- 风弹/旋风人刷怪蛋的中文名由本 mod 的 `zh_cn` 补齐为「风弹」「旋风人刷怪蛋」。

### 典籍中文显示

上游的典籍界面会把研究标题/正文强制套上 `ancient_remnants:header` 的 TTF 字体，
该字体没有中文字形，中文会显示成方框（missing glyph）。本 mod 新建
`ancient_remnants_add:codex` 字体：provider 先放上游 `header.ttf`，再 reference
`minecraft:default`，所以拉丁字符继续使用装饰字体，中文自动回退到原版字体。
客户端 mixin 只改典籍页面的字体，服务端不受影响。

### Create 联动

- 祭坛激活的维度里，Create 的 `ProcessingOutput` 概率产出 chance ×1.5（封顶 100%）。
- 覆盖粉碎轮、磨石、锯、工作盆混合、手动应用、注液/排空以及 Deployer/Fan 使用的
  `RecipeApplier` 路径。
- 没有祭坛的维度、JEI 预览等没有机器上下文的掷骰不会增强。
- Create 未安装时，`mixin/create` 下的 mixin 由 `AncientRemnantsAddMixinPlugin` 跳过，
  不会因为缺少 Create 类而崩溃。

## 环境要求

| 项目 | 版本 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x（`loaderVersion="[47,)"`，开发用 47.4.10） |
| Java | 17 |
| 映射 | Parchment `2023.08.20-1.20.1`（叠加官方映射；联调 Create 6.0.x 时必须与它一致） |
| Create（可选） | 6.0.8+；未安装也能启动 |

## 依赖说明

版本号统一放在 `gradle.properties`，声明在 `build.gradle`，`mods.toml` 里同步声明版本范围。

| 依赖 | 坐标 | 声明方式 | 为什么 |
|---|---|---|---|
| Create（机械动力，可选联动） | `com.simibubi.create:create-1.20.1:6.0.8-289:slim` | `compileOnly` + `runtimeOnly`（仅开发环境） | `mods.toml` 里 `mandatory=false`，玩家没装 Create 也能启动；`slim` 分类器不内嵌依赖，所以 Ponder / Registrate / Flywheel 要一并声明。以后新增直接引用 Create 类的代码必须做存在性隔离 |
| Ancient Remnants（远古遗迹） | `maven.modrinth:ancient-remnants:r3G6hu9a`（= 1.5.2 / 1.20.1-forge） | `implementation` | 硬依赖，代码直接引用它的类 |
| Fragmentum | `maven.modrinth:fragmentum:T7QOSm4k`（= 5.4.0 / 1.20.1-forge） | `compileOnly` + `runtimeOnly` | 远古遗迹的硬前置框架（它的 `mods.toml` 里 `mandatory=true`、`[5.2.0,)`）。上游类的公开签名里带有它的类型（如 `Deferred`），编译期需要可见；玩家环境由远古遗迹的 mandatory 依赖提供，不打包 |
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
├── altar/                    # 巨石祭坛、远古回响进度、维度级丰收
├── blessing/                 # 多祝福列表、NBT 编解码、tick 同步与祝福数值后处理
├── client/                   # 祭坛 BlockEntityRenderer
├── create/                   # Create 概率产出 +50% 的线程上下文
├── loot/                     # 前哨站残片掉落接入
└── mixin/                    # 多祝福 / 上游 API / 祝福效果 / 祭坛 / 典籍字体；create/ 为可选联动，client/ 为客户端修复
src/main/resources/
├── META-INF/mods.toml        # 模组元数据（modId / 版本 / 硬依赖与可选联动）
├── assets/ancient_remnants_add/   # 祭坛模型、中英文 lang（含风弹/旋风人译名）
├── data/ancient_remnants_add/     # 祭坛战利品、古城注入、作物标签、远古回响进度
├── pack.mcmeta               # 资源包描述
└── ancient_remnants_add.mixins.json   # mixin 清单（11 个通用 + 9 个 Create 可选 + 1 个客户端）
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
- **mixin**：MixinGradle 管线（含 refmap 与开发环境 `mixin.env.remapRefMap`）已接好。
  新增 mixin 类时把它登记进 `ancient_remnants_add.mixins.json` 的 `mixins`（通用）或 `client`（仅客户端）列表
  ——**漏登记的典型症状是「编译进 jar 但运行时不生效」**，不会报错。

## 许可证

[MIT](LICENSE)

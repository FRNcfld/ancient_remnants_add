# ancient_remnants_add
扩展模组

面向 Minecraft **1.20.1 / Forge** 的双上游附属模组：同时依赖 [远古遗迹（Ancient Remnants）](https://modrinth.com/mod/ancient-remnants)
与 [机械动力（Create）](https://modrinth.com/mod/create)，在两者的机制之间搭建联动内容。

> 当前状态：**已初始化**。主入口、通用配置、mixin 基础设施与依赖栈已就位，玩法内容尚未落地。
> 加内容时按下面的「约定」一节往下写即可。

## 环境要求

| 项目 | 版本 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x（`loaderVersion="[47,)"`，开发用 47.4.10） |
| Java | 17 |
| 映射 | Parchment `2023.08.20-1.20.1`（叠加官方映射，必须与 Create 6.0.x 一致） |

## 依赖说明

版本号统一放在 `gradle.properties`，声明在 `build.gradle`，`mods.toml` 里同步声明版本范围。

| 依赖 | 坐标 | 声明方式 | 为什么 |
|---|---|---|---|
| Create（机械动力） | `com.simibubi.create:create-1.20.1:6.0.8-289:slim` | `implementation` + Ponder / Flywheel / Registrate | 上游之一，代码直接引用它的类；`slim` 分类器不内嵌依赖，所以那几个库要一并声明 |
| Ancient Remnants（远古遗迹） | `maven.modrinth:ancient-remnants:r3G6hu9a`（= 1.5.2 / 1.20.1-forge） | `implementation` | 另一个上游，代码直接引用它的类 |
| Fragmentum | `maven.modrinth:fragmentum:T7QOSm4k`（= 5.4.0 / 1.20.1-forge） | `runtimeOnly` | 远古遗迹的硬前置框架（它的 `mods.toml` 里 `mandatory=true`、`[5.2.0,)`），本 mod 不直接引用它的类 |
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

## 构建

```bash
./gradlew build       # 产物在 build/libs/
./gradlew runClient   # 开发环境启动客户端
./gradlew runServer   # 开发环境启动服务端
./gradlew runData     # 数据生成（目前没有 DataProvider，仅占位）
```

首次构建要下载并反编译 Minecraft 与三个上游 jar，耗时较长；后续增量构建约十几秒。

## 目录结构

```
src/main/java/com/frnc/ancient_remnants_add/
├── AncientRemnantsAdd.java   # 主入口（@Mod）：注册事件总线、通用配置，并给出 id() 快捷方法
└── Config.java               # 通用配置（config/ancient_remnants_add-common.toml）
src/main/resources/
├── META-INF/mods.toml        # 模组元数据（modId / 版本 / 作者 / 两个上游依赖）
├── pack.mcmeta               # 资源包描述
└── ancient_remnants_add.mixins.json   # mixin 配置（当前为空清单）
```

## 约定

- **命名空间**：一切注册内容都挂在 `ancient_remnants_add` 下，Java 里用 `AncientRemnantsAdd.id("xxx")` 生成 `ResourceLocation`。
- **注册内容**：每个功能子包自己持有 `DeferredRegister` 并提供一个 `register(IEventBus)`，由主类构造函数逐个调用。
- **事件**：`MinecraftForge.EVENT_BUS.register(this)` 已在主类里接好，直接加 `@SubscribeEvent` 方法即可。
- **配置**：`Config` 的取值只放在访问器方法体里，不要写成 `static final` 字段——配置的加载时机晚于方块/物品的构造。
  说明文本写在 `mods.toml` 的 `description` 里，不要放进 `gradle.properties`（Gradle 以 ISO-8859-1 读取它，中文会乱码）。
- **可选依赖（JEI）**：JEI 是**软依赖**——`mods.toml` 里 `mandatory=false`、`side="CLIENT"`，
  `build.gradle` 里只 `compileOnly` API（不打包），完整 jar 只 `runtimeOnly` 进开发环境方便调试兼容。
  因此引用 JEI 的类必须放在**不会被无条件加载**的地方（单独的 `@JeiPlugin` 类，或先
  `ModList.get().isLoaded("jei")` 再进分支），否则没装 JEI 的整合包会在类加载阶段 `NoClassDefFoundError`。
- **mixin**：MixinGradle 管线（含 refmap 与开发环境 `mixin.env.remapRefMap`）已接好。
  新增 mixin 类时把它登记进 `ancient_remnants_add.mixins.json` 的 `mixins`（通用）或 `client`（仅客户端）列表
  ——**漏登记的典型症状是「编译进 jar 但运行时不生效」**，不会报错。

## 许可证

[MIT](LICENSE)

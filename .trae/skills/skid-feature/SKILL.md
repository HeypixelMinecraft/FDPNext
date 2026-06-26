---
name: "skid-feature"
description: "指导如何将其他 Minecraft 1.8.9 客户端（LiquidBounce/FDPClient/Rise/Novoline 等）的功能快速移植（skid）到 FDPNext 项目中。当用户要求 skid、移植、搬运、抄某个客户端的功能/模块/作弊特性时触发。"
---

# Skid Feature（快速移植客户端功能）

将其他 Minecraft 1.8.9 客户端的功能快速移植到 FDPNext 项目中。FDPNext 基于 FDP v5.3.5 + Forge 1.8.9 + Mixin，架构与 LiquidBounce 系高度兼容，多数功能可低改造度搬入。

## 何时触发

- 用户说"skid / 移植 / 搬运 / 抄 / 抄一下"某个客户端的功能
- 用户给出某客户端源码链接或代码片段，要求加到本项目
- 用户要求"复刻 / 实现"其他客户端已有的某个 module

## 核心流程（5 步）

### 1. 分析源功能
- 识别源客户端类型（LiquidBounce、FDPClient、Rise、Novoline、Delta、Skidder 等）
- 确定功能类型：
  - **Module**（可切换的功能模块）→ 放 `features/module/modules/<category>/`
  - **HUD Element**（界面元素）→ 放 `ui/client/hud/element/elements/`
  - **Command**（命令）→ 放 `features/command/commands/`
  - **Mixin/Transformer**（底层修改）→ 放 `injection/`
- 记录源功能的依赖：事件、工具类、mixin、外部库

### 2. 定位目标位置
FDPNext 模块按 `ModuleCategory` 分类（见 [ModuleCategory.kt](file:///d:/codex/FDPNext/src/main/java/net/ccbluex/liquidbounce/features/module/ModuleCategory.kt)）：

| Category | 路径 | 用途 |
|---|---|---|
| COMBAT | `modules/combat/` | 战斗（KillAura、Criticals、Velocity） |
| PLAYER | `modules/player/` | 玩家动作（AutoEat、FastUse） |
| MOVEMENT | `modules/movement/` | 移动（Speed、Fly、NoFall） |
| RENDER | `modules/render/` | 渲染（ESP、NameTags、Tracers） |
| WORLD | `modules/world/` | 世界（Nuker、FastPlace） |
| EXPLOIT | `modules/exploit/` | 漏洞利用（Disabler、Phase） |
| MISC | `modules/misc/` | 杂项（AntiBot、AutoGG） |
| CLIENT | `modules/client/` | 客户端自身（HUD、CapeManager） |

**关键：模块是自动扫描注册的**。`ModuleManager.registerModules()`（[ModuleManager.kt:36](file:///d:/codex/FDPNext/src/main/java/net/ccbluex/liquidbounce/features/module/ModuleManager.kt#L36)）用 `ClassUtils.resolvePackage` 扫描 `modules` 包下所有 `Module` 子类。**只要类放到正确子包下就会自动加载，无需手动注册。**

### 3. 适配改造（重点）

#### 3.1 模块基类
继承 `Module`（[Module.kt](file:///d:/codex/FDPNext/src/main/java/net/ccbluex/liquidbounce/features/module/Module.kt)）：

```kotlin
// object 单例（推荐，FDPNext 多数模块用这种）
object MyModule : Module(
    name = "MyModule",
    description = "从 XXX 客户端移植",
    category = ModuleCategory.COMBAT,
    defaultOn = false
) {
    // 值
    private val delayValue = IntegerValue("Delay", 100, 0, 1000)
    private val modeValue = ListValue("Mode", arrayOf("A", "B"), "A")

    // 事件监听
    @EventTarget
    fun onUpdate(event: UpdateEvent) { ... }

    @EventTarget
    fun onPacket(event: PacketEvent) { ... }

    override fun onEnable() { ... }
    override fun onDisable() { ... }
}

// class 形式（需要传参时用）
class MyModule : Module(name = "MyModule", category = ModuleCategory.COMBAT) { ... }
```

#### 3.2 包名与导入改造
- 源 `net.ccbluex.liquidbounce.*` → 通常**直接兼容**（FDPNext 用同包名）
- 源 `net.optifine.*` / `net.minecraft.client.*` → 保留，Forge 1.8.9 提供
- 其他客户端专有包（如 `me.rise.*`、`net.novoline.*`）→ 改写为 FDPNext 等价物
- 头部注释统一改为：
  ```kotlin
  /*
   * FDPNext Hacked Client
   * A Super Skid Hacked Client by FDP 5.3.5.
   * https://github.com/HeypixelMinecraft/FDPNext
   */
  ```

#### 3.3 值类型映射
FDPNext 的值类型在 `features/value/`，源客户端的配置通常可这样映射：

| 源客户端 | FDPNext |
|---|---|
| `BoolSetting` / `BooleanSetting` | `BoolValue(name, default)` |
| `FloatSetting` / `DoubleSetting` | `FloatValue(name, default, min, max)` |
| `IntSetting` | `IntegerValue(name, default, min, max)` |
| `ModeSetting` / `EnumSetting` | `ListValue(name, options, default)` |
| `StringSetting` | `TextValue(name, default)` |
| `ColorSetting` | `ColorValue(name, default)` |

构造参数顺序与 LB 系一致：`(name, default, min, max)` 或 `(name, default)`。

#### 3.4 事件系统
- 监听方法用 `@EventTarget` 注解
- 常用事件（都在 `net.ccbluex.liquidbounce.event`）：
  - `UpdateEvent` / `MotionEvent`（pre/post 用 `EventState`）
  - `PacketEvent`（`event.packet` 取包，`event.cancelEvent()` 取消）
  - `MoveEvent`（修改 motionX/Y/Z，或 cancel）
  - `Render2DEvent` / `Render3DEvent`
  - `TickEvent` / `KeyEvent` / `ChatEvent`
- 源客户端的事件名若不同（如 Rise 的 `PreUpdateEvent`），映射到 FDPNext 等价事件

#### 3.5 工具类复用
FDPNext `utils/` 下已有大量现成工具，**优先复用而非搬源客户端的**：

| 功能 | FDPNext 工具类 |
|---|---|
| 移动 | `utils/MovementUtils.kt` |
| 旋转 | `utils/RotationUtils.java` |
| 数据包 | `utils/PacketUtils.kt`、`utils/PacketCounterUtils.kt` |
| 渲染 2D | `utils/render/RenderUtils.java`、`RoundedUtil.java` |
| 渲染 3D | `utils/render/RenderUtils.java`、`Stencil.kt` |
| 模糊/阴影 | `utils/render/BlurUtils.kt`、`ShadowUtils.kt` |
| 计时 | `utils/timer/MSTimer.kt`、`TickTimer.kt` |
| 距离/路径 | `utils/PathUtils.java`、`utils/block/BlockUtils.kt` |
| 实体 | `utils/EntityUtils.kt`、`utils/PlayerUtils.kt` |
| 物品 | `utils/InventoryUtils.kt`、`utils/item/ItemUtils.kt` |

### 4. 子模式（Mode）架构
若源功能有多种模式（如 Criticals 有 Packet/Motion/Hover 等），按 FDPNext 的 Mode 模式拆分：

1. 在模块目录下建子目录（如 `criticals/`）
2. 定义模式基类（继承 `Mode` 或自建接口）
3. 每个模式一个类，放子目录
4. 主模块用 `ClassUtils.resolvePackage` 自动加载：

```kotlin
class MyModule : Module(name = "MyModule", category = ModuleCategory.COMBAT) {
    private val modes = ClassUtils.resolvePackage(
        "${this.javaClass.`package`.name}.mymodes", MyMode::class.java
    ).map { it.newInstance() as MyMode }

    val modeValue: ListValue = object : ListValue("Mode",
        modes.map { it.modeName }.toTypedArray(), modes.first().modeName) {
        override fun onChange(old: String, new: String) { if (state) onDisable() }
        override fun onChanged(old: String, new: String) { if (state) onEnable() }
    }
}
```

参考实现：[Criticals.kt](file:///d:/codex/FDPNext/src/main/java/net/ccbluex/liquidbounce/features/module/modules/combat/Criticals.kt)、[Velocity.kt](file:///d:/codex/FDPNext/src/main/java/net/ccbluex/liquidbounce/features/module/modules/combat/Velocity.kt)。

### 5. 验证清单
移植完成后逐项检查：
- [ ] 类放到正确的 `modules/<category>/` 子包下
- [ ] 继承 `Module`，构造参数含 `name` 和 `category`
- [ ] 包名 `net.ccbluex.liquidbounce.*`，注释头改为 FDPNext
- [ ] 所有值类型用 `features/value/` 下的类
- [ ] 事件方法加 `@EventTarget` 注解
- [ ] `onEnable` / `onDisable` 清理状态（取消发送队列、重置计时器）
- [ ] 无源客户端专有依赖残留（删除 `me.rise.*` 等导入）
- [ ] 用 FDPNext 现有工具类替代源客户端工具
- [ ] 若涉及底层修改，检查是否需要 mixin（`injection/`）

## 常见陷阱

1. **忘记 `object` vs `class`**：FDPNext 的 `object` 模块用 `ClassUtils.getObjectInstance` 加载，`class` 用 `newInstance`。`ModuleManager` 两种都支持，但若构造需要传参必须用 `class`。
2. **事件没有注册**：模块本身是 `Listenable`，事件会在 `registerModule` 时自动注册监听器。但若你创建了额外的 `Listenable` 对象，需手动 `FDPNext.eventManager.registerListener(...)`。
3. **配置不保存**：值的 `onChange` 若改了模块状态，需触发 `FDPNext.configManager.smartSave()`（`Module.tKeyBind` 已自动做）。
4. **混入反作弊检测点**：skid 时注意源功能的反检测逻辑（如包累积、计时），FDPNext 的 `PacketUtils`/`PacketCounterUtils` 可辅助。
5. **i18n**：模块名若要本地化，`ModuleCategory` 用 `%module.category.xxx%` 格式，需在 `ui/i18n/` 资源里加键。模块 `localizedName` 默认回退到 `name`。

## 高效 skid 技巧

- **优先找 LiquidBounce 系源码**：FDPNext 源自 LB，包名/事件/值类型几乎 1:1 兼容，改造成本最低
- **用 SearchCodebase 找 FDPNext 现有等价实现**：搬之前先搜项目里是否已有类似功能或可复用的工具
- **先跑通最小版本**：先实现核心逻辑（一个 `@EventTarget onUpdate`），再加值和模式
- **保留源注释**：源功能的算法注释（如 packet 顺序、时序）应保留，便于后续调优

# 多版本连接（Java 内置 + Bedrock 走 ViaProxy）

| 目标 | 方式 | 说明 |
|---|---|---|
| 连接其它版本的 **Java** 服务器 | **内置 ViaMCP** | 已打包进 FDPNext（ViaVersion + ViaBackwards + ViaRewind 的 Java 8 降级版），无需外部 mod |
| 连接 **Bedrock（基岩版）** 服务器 | **ViaProxy**（本地代理） | 含 ViaBedrock，处理 RakNet 与基岩/Xbox 认证；FDPNext 连本地代理，代理桥接目标 |

> 为什么 Bedrock 不内置：FDPNext / MC 1.8.9 跑在 **Java 8**，而 ViaProxy 是 **Java 21** 独立程序，
> 无法塞进 Java 8 进程；现代 ViaForge/ViaVersion 5.x 也是 Java 17，同样无法内置。Java 多版本用的是
> **字节码降级到 Java 8** 的 Via* 库（随客户端打包）。

游戏内输入 `.via` 可查看当前目标版本与用法。

---

## 一、Java 多版本（内置，开箱即用）

1. 进入 **多人游戏** 界面。
2. 左上角有一个 **版本滑块**（ViaMCP）。拖动它选择你要连接的服务器协议版本（例如 1.20.x、1.9、1.7 等）。
3. 直接连接目标服务器即可。
4. 连接更高版本服务器时，建议开启 **ViaVersionFix** 模块（Exploit 分类）——修正跨版本的梯子/移动判定差异。
5. 把滑块拖回 **1.8.9（native）** 即恢复原版连接。

ViaMCP 配置/缓存目录：游戏运行目录下的 `ViaMCP/`。

---

## 二、Bedrock 基岩版（ViaProxy）

ViaMCP 不含 Bedrock，所以基岩版走本地代理：

1. 下载 **ViaProxy**：https://github.com/ViaVersion/ViaProxy （需要 **Java 21** 运行）。
2. 运行 ViaProxy，设置：
   - **Target / 目标类型** = Bedrock
   - **Server Address** = 目标基岩服务器地址（基岩默认端口 19132）
   - **Auth / 认证** = Offline 或 Microsoft/Xbox（连正版基岩服需要 Xbox 登录）
   - 记下 ViaProxy 本地监听端口（默认 `25568`）
3. 点击 **Start**。
4. 在 FDPNext 多人游戏里把 ViaMCP 版本滑块设到与 ViaProxy 对应的版本（通常保持 1.8.9 native 即可，由代理处理协议），
   添加 Java 服务器 `127.0.0.1:25568` 并连接。

> Bedrock 为 **实验性**：表单/UI、部分实体、资源包等可能不完整。

---

## 维护者注意

- 内置 Via* 库为 `libs/` 下的 **降级（Java 8）** jar：`ViaVersion-5.3.0-downgraded.jar`、
  `ViaBackwards-5.3.0-downgraded.jar`、`ViaRewind-4.0.6-downgraded.jar`（经 `include fileTree(dir:"libs")` 打包）。
  升级时务必使用 ViaVersion CI 的 **-downgraded / Java8** 产物，否则在 Java 8 上会
  `UnsupportedClassVersionError`。
- 集成代码在 `de/florianmichael/`（ViaMCP + ViaLoadingBase，源码内置）。
- 管线注入：`injection/forge/mixins/network/MixinNetworkManager.java`（代理路径 + 压缩重排）与
  `MixinNetworkManagerInitChannel.java`（原版 `NetworkManager$5` 路径）；初始化在
  `FDPNext.startGame()`；版本滑块在 `injection/forge/mixins/gui/MixinGuiMultiplayer.java`。

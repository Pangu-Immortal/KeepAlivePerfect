# KeepAlivePerfect

<div align="center">

![萌萌计数器](https://count.getloli.com/get/@KeepAlivePerfect?theme=rule34)

</div>

<p align="center">
  <b>如果觉得有帮助，请点击 <a href="https://github.com/Pangu-Immortal/KeepAlivePerfect/stargazers">Star</a> 支持一下，关注不迷路！</b>
</p>

## 项目概览

**KeepAlivePerfect** 是一个 Android 应用保活库，通过 JNI 技术与 Binder 框架通信实现进程复活，进一步通过 `ioctl` 提高复活率，最大程度增强应用持久性。

完整代码仓库：https://github.com/Pangu-Immortal/KeepLiveService

**分支说明：**

| 分支 | 说明 |
|------|------|
| `main` | 利用 `libbinder.so` 与 `ActivityManagerService` 通信的版本 |
| `ioctl` | 使用 `ioctl` 与 binder 驱动通信的版本（复活率更高） |

**做什么：**

- 通过多种策略实现应用进程保活（前台服务、无声音乐、一像素 Activity、双进程守护、JobScheduler 定时检测）
- 提供简洁的 API，一行代码接入保活能力
- 适配 Android 7.0 ~ Android 16（API 24 ~ 36）

**不做什么：**

- 不保证在所有国产 ROM 上 100% 有效（各厂商杀后台策略不同）
- 不建议在 C 端产品上使用，可能给用户带来性能损失

## 架构设计

### 保活策略分层

```
┌─────────────────────────────────────────────────────────┐
│                    KeepLive (门面入口)                    │
│              startWork() 初始化所有保活策略                │
├──────────┬──────────┬───────────┬───────────┬────────────┤
│ 前台服务  │ 无声音乐  │ 一像素保活  │ 双进程守护  │ 定时检测   │
│ Foreground│ MediaPlay│ OnePixel   │ AIDL Bind │ JobSchedul│
│ Service   │ er       │ Activity   │           │ er        │
├──────────┴──────────┴───────────┴───────────┴────────────┤
│              LocalService (主进程)                        │
│              RemoteService (:remote 守护进程)              │
└─────────────────────────────────────────────────────────┘
```

### 模块职责

| 模块 | 职责 | 包名 |
|------|------|------|
| `library` | 保活核心库，包含所有保活策略实现 | `com.fanjun.keeplive` |
| `app` | 演示应用，展示如何接入保活库 | `com.boolbird.keepalive.demo` |

### 核心设计决策

| 决策 | 原因 |
|------|------|
| 双进程 AIDL 绑定（BIND_ABOVE_CLIENT） | 一个进程被杀时，另一个立即拉起，最高优先级绑定 |
| 无声音乐循环播放 | 保持 CPU 唤醒，防止深度休眠 |
| 一像素 Activity | 屏幕灭时拉起透明 1x1 Activity，提升进程优先级 |
| JobScheduler 30s 周期 | 系统级定时器，定期检查并重启已死亡的服务 |
| 两种运行模式 | ENERGY（省电，延迟播放）/ ROGUE（激进，立即播放） |

## 工程结构

```
KeepAlivePerfect/
├── app/                                    # 演示应用模块
│   ├── build.gradle                        # 应用构建配置
│   └── src/main/
│       ├── AndroidManifest.xml             # 应用清单
│       ├── java/.../demo/
│       │   ├── MainApplication.kt          # 入口：初始化保活库
│       │   └── MainActivity.kt             # 演示界面
│       └── res/                            # 资源文件
├── library/                                # 保活核心库
│   ├── build.gradle                        # 库构建配置
│   └── src/main/
│       ├── AndroidManifest.xml             # 库清单（Service/Receiver 声明）
│       ├── aidl/.../GuardAidl.aidl         # IPC 通信接口
│       ├── java/com/fanjun/keeplive/
│       │   ├── KeepLive.java               # 库入口门面
│       │   ├── config/                     # 配置类
│       │   │   ├── ForegroundNotification.java
│       │   │   ├── KeepLiveService.java    # 业务回调接口
│       │   │   └── NotificationUtils.java
│       │   ├── service/                    # 核心服务
│       │   │   ├── LocalService.java       # 本地服务（主进程）
│       │   │   ├── RemoteService.java      # 守护服务（:remote 进程）
│       │   │   ├── JobHandlerService.java  # 定时检测（API 21+）
│       │   │   └── HideForegroundService.java
│       │   ├── activity/
│       │   │   └── OnePixelActivity.java   # 一像素保活 Activity
│       │   ├── receiver/                   # 广播接收器
│       │   └── utils/
│       │       └── ServiceUtils.java       # 服务状态检测
│       └── res/raw/novioce.wav             # 无声音乐资源
├── build.gradle                            # 根构建配置
├── settings.gradle                         # 项目设置
├── gradle.properties                       # Gradle 全局配置
└── gradle/wrapper/                         # Gradle Wrapper
```

## 运行环境

| 组件 | 版本 |
|------|------|
| Kotlin | 2.3.10 |
| Android Gradle Plugin (AGP) | 9.0.1 |
| Gradle | 9.3.1 |
| JDK | 17+ |
| compileSdk | 36（Android 16） |
| targetSdk | 36（Android 16） |
| minSdk | 24（Android 7.0） |
| Android Studio | Ladybug 或更高版本 |

## 从零搭建指南

### 环境准备

1. 安装 [Android Studio](https://developer.android.com/studio)（Ladybug 或更高版本）
2. 确保 JDK 17+ 已安装（Android Studio 自带）
3. 安装 Android SDK 36（通过 SDK Manager）

### 依赖安装

1. 克隆仓库：

```bash
git clone https://github.com/Pangu-Immortal/KeepAlivePerfect.git
cd KeepAlivePerfect
```

2. 在 Android Studio 中打开项目，等待 Gradle Sync 完成

3. 如果作为库引用到自己的项目中：

```gradle
// settings.gradle 中添加模块
include ':library'

// app/build.gradle 中添加依赖
dependencies {
    implementation project(path: ':library')
}
```

### 构建命令

```bash
# 编译 Debug 版本
./gradlew assembleDebug

# 编译 Release 版本
./gradlew assembleRelease

# 清理构建
./gradlew clean

# 运行 Lint 检查
./gradlew lint
```

## 快速启动

### 1. 在 Application 中初始化保活服务

```kotlin
class MainApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        // 定义前台通知样式
        val foregroundNotification = ForegroundNotification(
            "应用名称", "保活服务运行中", R.drawable.ic_small_notification
        ) { context, intent ->
            // 通知点击事件
        }

        // 启动保活服务
        KeepLive.startWork(
            this,
            KeepLive.RunMode.ENERGY,        // ENERGY（省电）或 ROGUE（激进）
            foregroundNotification,
            object : KeepLiveService {
                override fun onWorking() {
                    // 保活服务启动时的业务逻辑（如 socket 连接、心跳检测）
                    // 注意：此方法可能被多次调用
                }

                override fun onStop() {
                    // 保活服务停止时的清理逻辑
                    // 注意：此方法可能被多次调用，需与 onWorking 配套
                }
            }
        )
    }
}
```

### 2. AndroidManifest.xml 配置

```xml
<!-- 声明权限 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- 声明 Application -->
<application android:name=".MainApplication">
    <!-- 你的 Activity 等 -->
</application>
```

library 模块已在其 AndroidManifest.xml 中声明了所有必要的 Service、Receiver 和 Activity，会在构建时自动合并，无需手动配置。

### 3. 运行模式说明

| 模式 | 说明 | 适用场景 |
|------|------|---------|
| `ENERGY` | 省电模式，屏幕灭后延迟 5s 播放无声音乐 | 一般保活需求 |
| `ROGUE` | 激进模式，立即播放无声音乐 | 对保活要求极高的场景 |

### 常见问题

**Q: 保活服务的通知如何自定义？**

通过 `ForegroundNotification` 构造函数传入标题、描述和图标即可。

**Q: 如何判断保活是否生效？**

使用 `adb shell` 命令查看进程：

```bash
adb shell ps | grep your.package.name
```

如果看到主进程和 `:remote` 守护进程同时存在，说明保活生效。

**Q: 部分机型无效怎么办？**

国产 ROM 通常有额外的后台管理策略，需要引导用户手动设置：
- 将应用加入电池优化白名单
- 允许应用自启动
- 锁定应用在最近任务列表中

## 核心流程列表

### 主流程：保活服务启动

```
Application.attachBaseContext()
    └── KeepLive.startWork()
        ├── API >= 21: 启动 JobHandlerService
        │   └── onStartCommand()
        │       ├── 调度 JobScheduler（30s 周期）
        │       ├── 启动 LocalService
        │       └── 启动 RemoteService
        └── API < 21: 直接启动双 Service
            ├── LocalService.onStartCommand()
            │   ├── 创建前台通知
            │   ├── 启动无声音乐循环
            │   ├── 注册屏幕状态监听
            │   ├── 绑定 RemoteService（BIND_ABOVE_CLIENT）
            │   └── 回调 KeepLiveService.onWorking()
            └── RemoteService.onStartCommand()
                └── 绑定 LocalService（BIND_ABOVE_CLIENT）
```

### 关键分支：进程被杀后恢复

```
进程 A 被系统杀死
    └── 进程 B 的 ServiceConnection.onServiceDisconnected() 触发
        ├── 检查进程 A 是否存活
        ├── 重新启动进程 A 的 Service
        └── 重新绑定（BIND_ABOVE_CLIENT）
```

### 关键分支：屏幕灭/亮事件

```
屏幕关闭 → OnepxReceiver 接收广播
    ├── 延迟 1s 启动 OnePixelActivity（1x1 透明窗口）
    └── 发送 "_ACTION_SCREEN_OFF" → LocalService 开始播放无声音乐

屏幕点亮 → OnepxReceiver 接收广播
    ├── 关闭 OnePixelActivity
    └── 发送 "_ACTION_SCREEN_ON" → LocalService 暂停无声音乐
```

## 技术债与风险

| 位置 | 问题 | 风险等级 | 说明 |
|------|------|---------|------|
| `ServiceUtils.getRunningServices()` | API 29+ 已废弃 | 高 | 需迁移到 `getRunningAppProcesses()` 或替代方案 |
| `OnepxReceiver` PendingIntent | API 31+ 需 `FLAG_IMMUTABLE` | 高 | 当前传入 0，可能导致崩溃 |
| `KeepLive.isMain()` | `getRunningAppProcesses()` 可能返回 null | 高 | 需添加 null 检查 |
| 通知 ID 硬编码 13691 | 多 Service 共用同一 ID | 中 | 多库共存时可能冲突 |
| `HideForegroundService` 延迟 2s | 通知可能短暂可见 | 低 | 用户可能看到通知闪烁 |

**不建议修改的区域：**

- `library/src/main/aidl/` — AIDL 接口是双进程通信核心，修改会导致保活失效
- `RemoteService` 的 `android:process=":remote"` — 进程名在多处硬编码引用
- `BIND_ABOVE_CLIENT` 绑定标志 — 降级会显著削弱保活效果
- `res/raw/novioce.wav` — 无声音乐资源是保活策略的物理载体

## 声明

- **咨询服务**：关于 Google 上架和封号相关问题，提供按问题收费的咨询服务。
- **深度定制**：提供个性化服务，价格视需求而定。
- **保活服务**：提供 AAB 保活服务和马甲包服务，彻底解决关联问题，价格私聊。
- **优惠政策**：如果您之前有过打赏，享受六折优惠；曾提交过 PR 的用户，免费提供服务。
- **交流与合作**：欢迎通过提 Issue 提出问题，或通过提交 PR 与我们交流合作。

![二维码](https://github.com/Pangu-Immortal/Pangu-Immortal/blob/main/getqrcode.png)

**Telegram 群组**：[点击加群讨论](https://t.me/+V7HSo1YNzkFkY2M1)

## 注意事项

1. 该项目仅供学习和参考，已在三星、摩托罗拉、索尼、Google 真机上全面测试通过，适配所有机型到 Android 16。
2. 资源占用少，用户无感知，成功率高。
3. 不建议在 C 端产品上使用，可能会给用户带来性能损失，像病毒一样存在于用户手机上是不合理的。
4. 可作为学习 Binder 框架的一个案例。

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

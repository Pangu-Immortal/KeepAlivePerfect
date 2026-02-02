

# 🔥 KeepAlivePerfect（这里只是冰山一角）

<div align="center">

![萌萌计数器](https://count.getloli.com/get/@KeepAlivePerfect?theme=rule34)

</div>

<p align="center">
  <b>🌟 如果觉得有帮助，请点击 <a href="https://github.com/Pangu-Immortal/KeepAlivePerfect/stargazers">Star</a> 支持一下，关注不迷路！🌟</b>
</p>

**KeepAlivePerfect** 是一个通过 JNI 技术实现进程复活的项目，进一步通过 `ioctl` 提高了复活率，最大程度地增强了应用的持久性。

完整的代码在仓库：https://github.com/Pangu-Immortal/KeepLiveService

- **`main` 分支**：利用 `libbinder.so` 与 `ActivityManagerService` 通信的版本。
- **`ioctl` 分支**：使用 `ioctl` 与 binder 驱动通信的版本。

**项目环境：**

- Kotlin 版本：`1.8.22`
- JDK 版本：`11.0.12`
- Android Gradle 插件版本：`7.4.0`
- Gradle 版本：`7.6`
- 编译 SDK：`33`
- 目标 SDK：`32`
- 最低支持 SDK：`19`

![二维码](https://github.com/Pangu-Immortal/Pangu-Immortal/blob/main/getqrcode.png)

🔥 **Telegram 群组**： [点击加群讨论，这里只是冰山一角。](https://t.me/+V7HSo1YNzkFkY2M1)

## 声明 🔥

- **咨询服务**：关于 Google 上架和封号相关问题，提供按问题收费的咨询服务。
- **深度定制**：提供个性化服务，价格视需求而定。
- **保活服务**：提供 AAB 保活服务和马甲包服务，彻底解决关联问题，价格私聊。
- **优惠政策**：如果您之前有过打赏，享受六折优惠；曾提交过 PR 的用户，免费提供服务。
- **交流与合作**：欢迎通过提 Issue 提出问题，或通过提交 PR 与我们交流合作。

## 收费功能 🔥

（所有功能均提供对 Android 15 版本的适配，添加联系时请备注需求）

- **应用自启动**：应用安装后无需用户点击即可自动启动。
- **暗刷 H5 广告**：偷偷刷 ADX、adsense 等 H5 广告，可以开启 20+ view 同时刷。
- **无权限后台弹出 Activity**：无需权限即可在后台任意时机弹出 Activity，无需锁屏。
- **应用保活**：应用可在多次强制停止操作后仍保持运行，完美抵抗强制停止操作。
- **应用拉活**：在应用彻底死亡的状态下，可在15分钟内唤醒自身。
- **防卸载**：防止用户卸载应用，点击卸载无反应。
- **无感知卸载竞品**：可无感知地卸载手机中任意应用。
- **隐藏桌面图标**：应用安装后立即隐藏自身，或在需要时随时隐藏，支持 Android 15。
- **马甲包服务**：彻底解决关联问题，为批量马甲包提供服务。
- **报病毒优化**：无需重新打包，净化应用，处理所有应用的报毒问题。
- **账号隔离**：为开发者提供完善的账号隔离体系，防止账号关联。
- **IP 漂移**：支持拉取高 eCPM 地区的 AdMob 广告。
- **模拟 iOS**：支持 Android 设备模拟并拉取 iOS 的 AdMob 广告，大幅提高 eCPM。
- **机型模拟工具**：支持批量刷下载量，可无成本快速刷百万下载量，迅速提高商店排名。
- **国内机型保活**：为运动类、外卖类、聊天类等应用实现永生不死，不被系统杀死，已为多款应用接入。
- **防抓包处理**：数据脱敏，适用于棋牌类大规模上架等操作。
- **多开、双开工具**：支持无限分身等功能。
- **大模型定制开发**：提供私有数据训练、NSFW 模型开发、成人模型制作、成人话术、成人照片、成人视频等私有化专属大模型训练制作。
- **数字人、换脸、图生图、图生视频**：制作明星、自己、家人的数字人，老照片复活，与已逝去的亲人对话。
- **云游戏、云手机搭建**：提供全套云端容器方案，涵盖云原生 GPU、定制化服务器、全光网络、协同渲染、AI 内容生成、云原生工具包等核心技术路径。
- **定制化播放器**：提供加密播放器、3D 播放器、云播放器等，可为任意视频编解码提供定制服务，为 AR、VR、MR 场景提供服务。
- **滤镜定制**：提供视频、相机、图片等滤镜处理，可根据竞品效果进行模仿。
- **AI 多场景定制**：多年 AI 行业经验，可为小团队提供定制化的 AI 服务。
- **ROM 定制**：提供各类定制化功能的 Android 系统，也可提供车载系统的定制化，提供软硬件交互的外包服务。

---

**注意 🌈：**

1. 该项目仅供学习和参考，在 Android 4.4 到 Android 15.0 的模拟器上有效，已在三星、摩托罗拉、索尼、Google 真机上全面测试通过，适配所有机型到 Android 15 Beta 预览版（可用于海外市场）。
2. 资源占用少，用户无感知，成功率高。
3. 不建议在 C 端产品上使用，可能会给用户带来性能损失，像病毒一样存在于用户手机上是不合理的。
4. 可作为学习 Binder 框架的一个案例。

## 👉 ioctl 使用方法

1. **在 Application 中注册 KeepAlive 服务**

```kotlin
@Override
protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    KeepAliveConfigs configs = new KeepAliveConfigs(
                    new KeepAliveConfigs.Config(getPackageName() + ":resident",
                            Service1.class.getCanonicalName()));
    KeepAlive.init(base, configs);
}
```

2. **Service1 对应的进程名是 ":resident"，或者其它任意命名**

```xml
<service
    android:name="Service1"
    android:process=":resident" />
```

Service 需要继承 `KeepAliveService`，否则在 Android 4.4 上将没有保活效果。

3. **在合适的地方，启动 Service1，它将自动唤醒保活进程**

```kotlin
startService(new Intent(MainActivity.this, Service1.class));
```

如果需要服务自启动，请参见第6条。

4. **忽略电池优化**

```kotlin
configs.ignoreBatteryOptimization();
```

5. **防止短时间内重复启动**

```kotlin
// 配置短时间重启限制，每次重启间隔限制是10秒，最多允许3次10秒内的连续重启
configs.rebootThreshold(10 * 1000, 3);
`` 
`

---

## ⭐ Star 趋势

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

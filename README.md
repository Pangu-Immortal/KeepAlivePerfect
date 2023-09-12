# 🔥KeepAlivePerfect
KeepAlivePerfect是通过JNI复活进程的基础上，实现了通过ioctl复活进程，能最大程度提高复活率。

- `main` 分支是`利用 libbinder.so 与 ActivityManagerService 通信`的版本
- `ioctl`  分支是`使用 ioctl 与 binder 驱动通信`的版本。

- kotlin version '1.8.22'
- JDK version '11.0.12'
- AGP version '7.4.0'
- Gradle  version 'gradle-7.6-bin.zip'
- compileSdk 33
- targetSdk 32
- minSdk 19


![avatar](https://github.com/Pangu-Immortal/Pangu-Immortal/blob/main/qrcode_for_gh_5d1938320a76_344.jpg)

## 声明（每天找我的人太多，又太菜，很无奈，请理解）

- 想咨询Google上架，保活，权限，体外，ecpm等相关问题，1000元/小时(不满一小时按一小时算)

- 想深度定制？ok，也提供收费服务，价格视需求而定(8800元起)

- 本人提供aab 保活服务 和 马甲包服务，彻底解决关联问题，价格私聊。

- 如果你之前有打赏过，打6折，提过PR者，免费

- 有问题欢迎提Issue，有想法欢迎提PR或与我交流

- 说话之前先转账，先来一小时的，可以跟你聊一会。




#### QQ 大学生实习群：794834282，877612090 


---

**注🌈**：
1. 该项目仅供学习和参考，在android4.4到android14.0的模拟器上有效，在三星、摩托罗拉、索尼、Google真机上已经全面测试通过，适配所有机型到Android14 bate3预览版。（可用于海外市场）
2. 资源占用少，用户无感知，成功率高。
3. 不建议在C端产品上使用，容易给用户带来性能损失，像病毒一样存在用户手机上是不合理的。
4. 可作为学习binder框架的一个案例。

## 👉 ioctl 使用方法
1. 在Application中注册KeepAlive服务
```
@Override
protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    KeepAliveConfigs configs = new KeepAliveConfigs(
                    new KeepAliveConfigs.Config(getPackageName() + ":resident",
                            Service1.class.getCanonicalName()));
    KeepAlive.init(base, configs);
}
```

2. Service1对应的进程名是":resident"，或者其它任意命名
```
<service
    android:name="Service1"
    android:process=":resident" />
```
Service需要继承KeepAliveService，否则在Android4.4上将没有保活效果。

3. 在合适的地方，启动Service1，它将自动唤醒保活进程
```
startService(new Intent(MainActivity.this, Service1.class));
```
如果需要服务自启动，看第6条。

4. 忽略电池优化
```
configs.ignoreBatteryOptimization();
```

5. 防止短时间内重复启动
```
// 配置短时间重启限制，每次重启间隔限制是10s，最多允许3次10秒内的连续重启
configs.rebootThreshold(10*1000, 3);
```
⚠️注：保活和重启限制相违背，更准确的应该做崩溃重启限制。

6. 设置应用自启执行的操作
```
configs.setOnBootReceivedListener(new KeepAliveConfigs.OnBootReceivedListener() {
    @Override
    public void onReceive(Context context, Intent intent) {
        // 设置服务自启
        context.startService(new Intent(context, Service1.class));
    }
});
```

***

## 欢迎关注公众号，后续技术更新和讲解都发布在公众号文章里。

![avatar](https://github.com/Pangu-Immortal/Pangu-Immortal/blob/main/qrcode_for_gh_5d1938320a76_344.jpg)

## 应对方法

🌴下面是一种简单的方法杀死 KeepAlivePerfect:

```
ps -A | grep `ps -A | grep keepalive | awk '{print $1}' | head -1` | awk '{print $2}' | xargs kill -19 && am force-stop com.boolbird.keepalive
```

对于系统有两种思路可以选择：

1. 加入在 force-stop 期间不允许启动新的进程的逻辑
2. 修改 force-stop 的杀进程逻辑为：预先收集好所有进程再进行 kill（如有必要还可以先发送 SIGSTOP）

## 测试
项目根目录下的kill_alive.sh用于重复杀进程测试。

## 🤔️问题
- 怎么保活多个进程又不额外的耗损电量。
- 避免在Application中初始化第三方库，避免在所有进程都初始化第三方库。

## 感谢🙏Marswin提供的思路，通过逆向破解Google市场的CleanMaster找到了这个库。
https://github.com/Marswin/MarsDaemon

## 许可(LICENSE)✏️

    Copyright 2021 @yugu88, KeepAlivePerfect Open Source Project

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

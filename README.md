# KeepAlive
KeepAlive是在[Leoric](https://github.com/tiann/Leoric)(通过JNI复活进程)的基础上，实现了通过ioctl复活进程，能最大程度提高复活率。

`master`分支是`利用 libbinder.so 与 ActivityManagerService 通信`的版本，`ioctl`分支是`使用 ioctl 与 binder 驱动通信`的版本。

**注**：
1. 该项目仅供学习和参考，在android4.4到android9.0的模拟器上有效，在真机上不能保证保活成功（MIUI等定制系统已封杀了这个方案）。
2. 对于自研轻量定制的 Android系统，对一些系统应用的保活，这个方案还是很有优势的。资源占用少，用户无感知，成功率高。
3. 不建议在C端产品上使用。
4. 可作为学习binder框架的一个案例。

## 使用方法
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
注：保活和重启限制相违背，更准确的应该做崩溃重启限制。

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

## 实现原理

- [Android 黑科技保活实现原理揭秘](http://weishu.me/2020/01/16/a-keep-alive-method-on-android/)
- [深度剖析App保活案例](http://www.52im.net/forum.php?mod=viewthread&tid=2893&highlight=%B1%A3%BB%EE)
- [Android黑科技保活的技术实现](https://juejin.im/post/5e820b61e51d45470652e7b8)

## 应对方法

下面是一种简单的方法杀死 KeepAlive:

```
ps -A | grep `ps -A | grep keepalive | awk '{print $1}' | head -1` | awk '{print $2}' | xargs kill -19 && am force-stop com.boolbird.keepalive
```

对于系统有两种思路可以选择：

1. 加入在 force-stop 期间不允许启动新的进程的逻辑
2. 修改 force-stop 的杀进程逻辑为：预先收集好所有进程再进行 kill（如有必要还可以先发送 SIGSTOP）

## 测试
项目根目录下的kill_alive.sh用于重复杀进程测试。

## 问题
1、怎么保活多个进程
2、避免在Application中初始化第三方库，避免在所有进程都初始化第三方库
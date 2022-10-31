## 安卓后台保活2022新姿势
适配华为大部分系列手机，vivo，OPPO 部分机型，小米的不支持，可见小米在对抗后台自保上做得最好    
本项目原本是给某个公司合作开发的，最后给了对方SDK之后由于付款问题闹得很郁闷，想着这个代码拿在自己手上也没用，就发出来给大家参考参考。目前分析的结果来看，这个是全网目前还能使用的保活方案，曝光之后很有可能活不到明年，如果你的公司恰好使用了这种方案，那么是时候开始研究新的方案了。最后说一下这种想白拿的公司，说实话我不是靠Android技术谋生的，最多就少了点零花钱，但是这个方案被封之后，你还有多少个机会白拿？       

# 原理 

安卓后台保活前前后后网上出了好多公开的方案，但是到目前为止，还能广泛使用的并没有，我通过了研究了一下网上几个大厂的APP（APP名字就不点名了），整理实现了这个方案   
虽然本方案看似集成了好几个保活的方案，比如一像素，JOB，等等，但是对于新版本android真正起作用的还是双进程守护。双进程守护的代码网上一搜一大堆，但是自己试过就知道了，现在的很多ROM已经封杀了这个方案，那些代码只能在原生系统上玩玩。 
这里大概说一下双进程守护的逻辑，同时启动A进程和B进程，相互守护，检测到对方退出就再次启动对方，大部分公开的方案都是使用startservice启动，网上有好几个改进版，甚至有的都在native层自己实现和service manger的通信逻辑来启动服务，为的就是能在被杀时候第一时间再次启动。但是，改到native层也没有用，现在大部分rom已经封杀了startservice，我大概研究了下样本，发现样本使用的是startInstrumentation来启动进程，对于Instrumentation不了解的同学可以自行百度。 所以只需要在MarsDaemon基础上做一下小改动即可： 
```
@Override
    public void onDaemonDead() {
        Log.d(TAG, "on daemon dead!");
        if (startServiceByAmsBinder()) {

            int pid = Process.myPid();
            Log.d(TAG, "mPid: " + mPid + " current pid: " + pid);
            Daemon.context.startInstrumentation(mApp,null,null);
            android.os.Process.killProcess(mPid);
        }
    }
```

# 2步集成使用
1、 打开library的AndroidManifest.xml找 如下位置：
```<instrumentation
        android:name="com.daemon.DInstrumentation"
        android:targetPackage="com.daemonLibrary.demo"
        android:targetProcesses="com.daemonLibrary.demo,com.daemonLibrary.demo:service" />
<application>
```
将包名替换成自己的包名

2、在app的Application中添加启动代码，并实现配置接口和回调接口
 ```
 override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        //DaemonLog.d("Application onCrearte")
        Daemon.startWork(this, DaemonConfigurationImplement(this), DaemonCallbackImplement())
}
```



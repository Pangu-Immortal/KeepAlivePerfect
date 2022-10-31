# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keep public class com.daemon.Daemon{
    boolean isMainProcess(android.app.Application);
    void startActivity(android.content.Context ,android.content.Intent );
    void startWork(android.app.Application,com.daemon.Daemon$DaemonConfiguration,com.daemon.Daemon$DaemonCallback);
}

-keep class com.daemon.process.NativeLoader{
    void onDaemonDead();
}

-keep class com.daemon.Daemon$DaemonCallback{*;
}
-keep class com.daemon.Daemon$DaemonConfiguration{*;
}
-keepclasseswithmembernames class * { # 保持native方法不被混淆
    native <methods>;
}

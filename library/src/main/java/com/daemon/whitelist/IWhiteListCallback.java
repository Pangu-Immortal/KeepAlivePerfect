package com.daemon.whitelist;


import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


/**
 * 白名单跳转回调
 *
 * @author xuexiang
 * @since 2019-09-02 15:01
 */
public interface IWhiteListCallback {

    /**
     * 初始化
     *
     * @param target  需要加入白名单的目标
     * @param appName 需要处理白名单的应用名
     */
    void init(@NonNull String target, @NonNull String appName);

    /**
     * 显示白名单
     *
     * @param activity
     * @param intentWrapper
     */
    void showWhiteList(@NonNull Activity activity, @NonNull WhiteListIntentWrapper intentWrapper);

    /**
     * 显示白名单
     *
     * @param fragment
     * @param intentWrapper
     */
    void showWhiteList(@NonNull Fragment fragment, @NonNull WhiteListIntentWrapper intentWrapper);

}

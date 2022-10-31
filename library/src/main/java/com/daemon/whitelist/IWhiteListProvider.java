package com.daemon.whitelist;


import android.app.Application;

import java.util.List;

/**
 * 白名单跳转意图数据提供者
 *
 * @author xuexiang
 * @since 2019-09-02 21:40
 */
public interface IWhiteListProvider {

    /**
     * 提供白名单跳转意图
     *
     * @param application
     * @return 白名单跳转意图
     */
    List<WhiteListIntentWrapper> getWhiteList(Application application);

}

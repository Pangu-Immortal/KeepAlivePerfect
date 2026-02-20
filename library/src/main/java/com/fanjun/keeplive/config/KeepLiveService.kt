package com.fanjun.keeplive.config

/**
 * 需要保活的服务回调接口
 */
interface KeepLiveService {
    /**
     * 运行中
     * 由于服务可能会多次自动启动，该方法可能重复调用
     */
    fun onWorking()

    /**
     * 服务终止
     * 由于服务可能会被多次终止，该方法可能重复调用，需同 onWorking 配套使用，如注册和注销
     */
    fun onStop()
}

package com.fanjun.keeplive.config

import java.io.Serializable

/**
 * 默认前台服务通知样式配置
 * 支持构造器和链式调用两种初始化方式
 */
class ForegroundNotification : Serializable {

    var title: String = ""
        get() = field.ifEmpty { "" }
        private set

    var description: String = ""
        get() = field.ifEmpty { "" }
        private set

    var iconRes: Int = 0
        private set

    var foregroundNotificationClickListener: ForegroundNotificationClickListener? = null
        private set

    // 私有无参构造，用于链式调用
    private constructor()

    // 带点击事件的构造器
    constructor(
        title: String,
        description: String,
        iconRes: Int,
        foregroundNotificationClickListener: ForegroundNotificationClickListener?
    ) {
        this.title = title
        this.description = description
        this.iconRes = iconRes
        this.foregroundNotificationClickListener = foregroundNotificationClickListener
    }

    // 不带点击事件的构造器
    constructor(title: String, description: String, iconRes: Int) {
        this.title = title
        this.description = description
        this.iconRes = iconRes
    }

    // 链式调用方法
    fun title(title: String): ForegroundNotification = apply { this.title = title }
    fun description(description: String): ForegroundNotification = apply { this.description = description }
    fun icon(iconRes: Int): ForegroundNotification = apply { this.iconRes = iconRes }
    fun foregroundNotificationClickListener(listener: ForegroundNotificationClickListener): ForegroundNotification =
        apply { this.foregroundNotificationClickListener = listener }

    companion object {
        /**
         * 链式调用初始化入口
         */
        @JvmStatic
        fun ini(): ForegroundNotification = ForegroundNotification()
    }
}

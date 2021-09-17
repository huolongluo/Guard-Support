package com.tqxd.guard.support.entity

import com.tqxd.guard.support.callback.GuardCallback
import com.tqxd.guard.support.callback.GuardBackgroundCallback

internal object Constant {
    /**
     * 包名
     */
    internal const val GUARD_PACKAGE = "com.tqxd.guard"

    /**
     * 停止标识符
     */
    internal const val GUARD_FLAG_STOP = "$GUARD_PACKAGE.flag.stop"

    /**
     * tag
     */
    internal const val GUARD_TAG = "guard"

    /**
     * 配置信息
     */
    internal const val GUARD_CONFIG = "guardConfig"

    /**
     * 通知栏配置信息
     */
    internal const val GUARD_NOTIFICATION_CONFIG = "notificationConfig"

    /**
     * 服务ID key
     */
    internal const val GUARD_SERVICE_ID = "serviceId"

    /**
     * JobID key
     */
    internal const val GUARD_JOB_ID = "jobId"

    /**
     * 进程名字
     */
    internal const val GUARD_EMOTE_SERVICE = "guardRemoteService"

    /**
     * 回调集合
     */
    internal val CALLBACKS = arrayListOf<GuardCallback>()

    /**
     * 前后台回调集合
     */
    internal val BACKGROUND_CALLBACKS = arrayListOf<GuardBackgroundCallback>()
}
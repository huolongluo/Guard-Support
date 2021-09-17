package com.tqxd.guard.support.entity

import android.os.Parcel
import android.os.Parcelable

/**
 * 用户配置的信息
 */
data class GuardConfig(
    /**
     * 通知栏信息
     */
    var notificationConfig: NotificationConfig = NotificationConfig(),
    /**
     * 默认配置信息
     */
    val defaultConfig: DefaultConfig = DefaultConfig()
) : Parcelable {
    constructor(source: Parcel) : this(
        source.readParcelable<NotificationConfig>(NotificationConfig::class.java.classLoader)!!,
        source.readParcelable<DefaultConfig>(DefaultConfig::class.java.classLoader)!!
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeParcelable(notificationConfig, 0)
        writeParcelable(defaultConfig, 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<GuardConfig> = object : Parcelable.Creator<GuardConfig> {
            override fun createFromParcel(source: Parcel): GuardConfig =
                GuardConfig(source)

            override fun newArray(size: Int): Array<GuardConfig?> = arrayOfNulls(size)
        }
    }
}
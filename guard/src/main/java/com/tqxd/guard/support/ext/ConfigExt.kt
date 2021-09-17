package com.tqxd.guard.support.ext

import android.content.Context
import com.google.gson.Gson
import com.tqxd.guard.support.entity.Constant
import com.tqxd.guard.support.entity.GuardConfig

/**
 * 配置信息扩展
 */

/**
 * 保存配置信息
 *
 * @receiver Context
 * @param guardConfig GuardConfig
 */
internal fun Context.saveConfig(guardConfig: GuardConfig) {
    sGuardConfig = guardConfig
    val serviceId = getServiceId()
    if (serviceId > 0) {
        guardConfig.notificationConfig.serviceId = serviceId
    }
    getSharedPreferences(Constant.GUARD_TAG, Context.MODE_PRIVATE).edit().apply {
        putString(Constant.GUARD_CONFIG, Gson().toJson(guardConfig))
        if (serviceId <= 0) {
            putInt(Constant.GUARD_SERVICE_ID, guardConfig.notificationConfig.serviceId)
        }
    }.apply()
}

/**
 * 获取配置信息
 *
 * @receiver Context
 * @return GuardConfig
 */
internal fun Context.getConfig() = sGuardConfig ?: getPreviousConfig() ?: GuardConfig()

/**
 * 获取Sp保存的配置信息
 *
 * @receiver Context
 * @return GuardConfig?
 */
internal fun Context.getPreviousConfig() = getSharedPreferences(
    Constant.GUARD_TAG,
    Context.MODE_PRIVATE
).getString(Constant.GUARD_CONFIG, null)?.run {
    Gson().fromJson(this, GuardConfig::class.java)
}

/**
 * 保存JobId
 *
 * @receiver Context
 * @param jobId Int
 */
internal fun Context.saveJobId(jobId: Int) =
    getSharedPreferences(
        Constant.GUARD_TAG,
        Context.MODE_PRIVATE
    ).edit().putInt(Constant.GUARD_JOB_ID, jobId).apply()

/**
 * 获得JobId
 *
 * @receiver Context
 * @return Int
 */
internal fun Context.getJobId() =
    getSharedPreferences(
        Constant.GUARD_TAG,
        Context.MODE_PRIVATE
    ).getInt(Constant.GUARD_JOB_ID, -1)

/**
 * 获得serviceId
 *
 * @receiver Context
 * @return Int
 */
private fun Context.getServiceId() = getSharedPreferences(
    Constant.GUARD_TAG,
    Context.MODE_PRIVATE
).getInt(Constant.GUARD_SERVICE_ID, -1)
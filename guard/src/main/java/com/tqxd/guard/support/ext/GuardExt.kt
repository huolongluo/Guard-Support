package com.tqxd.guard.support.ext

import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.tqxd.guard.support.Guard
import com.tqxd.guard.support.callback.AppBackgroundCallback
import com.tqxd.guard.support.entity.Constant
import com.tqxd.guard.support.entity.GuardConfig
import com.tqxd.guard.support.exception.GuardUncaughtExceptionHandler
import com.tqxd.guard.support.pix.OnePixActivity
import com.tqxd.guard.support.receiver.StopReceiver
import com.tqxd.guard.support.service.GuardJobService
import com.tqxd.guard.support.service.LocalService
import com.tqxd.guard.support.service.RemoteService
import com.tqxd.guard.support.workmanager.GuardWorker
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Guard扩展
 */

/**
 * 用以保存一像素Activity
 */
private var mWeakReference: WeakReference<Activity>? = null

/**
 * 用来表示是前台还是后台
 */
private var mIsForeground = false

/**
 * 是否注册过
 */
private var sRegistered = false

/**
 * 主Handler
 */
internal val sMainHandler by lazy {
    Handler(Looper.getMainLooper())
}

/**
 * 启动次数
 */
internal var sTimes = 0

/**
 * 启动次数，用以判断是否使用奔溃重启
 */
internal var sStartTimes = 0

/**
 * 配置信息
 */
internal var sGuardConfig: GuardConfig? = null

/**
 * 前后台切换监听
 */
private var mAppBackgroundCallback: AppBackgroundCallback? = null

/**
 * kotlin里使用Guard
 *
 * @receiver Context
 * @param block [@kotlin.ExtensionFunctionType] Function1<Guard, Unit>
 */
fun Context.guard(block: Guard.() -> Unit) =
    Guard.instance.apply { block() }.register(this)

/**
 * 注销
 *
 * @receiver Context
 */
fun Context.guardUnregister() = Guard.instance.unregister(this)

/**
 * 重启
 *
 * @receiver Context
 */
fun Context.guardRestart() = Guard.instance.restart(this)

/**
 * 更新通知栏
 *
 * @receiver Context
 * @param block [@kotlin.ExtensionFunctionType] Function1<Guard, Unit>
 */
fun Context.guardUpdateNotification(block: Guard.() -> Unit) =
    Guard.instance.apply { block() }.updateNotification(this)

/**
 * 是否已经停止
 *
 * @receiver Context
 * @return Boolean
 */
val Context.guardIsRunning
    get() = Guard.instance.isRunning(this)

/**
 * kotlin里使用注册Receiver
 *
 * @receiver Context
 * @param block Function0<Unit>
 */
internal fun Context.registerStopReceiver(block: () -> Unit) =
    StopReceiver.newInstance(this).register(block)

/**
 * 注册Guard服务
 *
 * @receiver Context
 * @param guardConfig GuardConfig
 */
internal fun Context.register(guardConfig: GuardConfig) {
    if (isMain) {
        try {
            if (sRegistered && isGuardRunning) {
                log("Guard is running，Please stop Guard before registering!!")
            } else {
                sStartTimes++
                sRegistered = true
                handleRestartIntent(guardConfig)
                saveConfig(guardConfig)
                GuardUncaughtExceptionHandler.instance
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    registerJobGuard(guardConfig)
                } else {
                    registerGuard(guardConfig)
                }
                if (this is Application && mAppBackgroundCallback == null) {
                    mAppBackgroundCallback = AppBackgroundCallback(this)
                    registerActivityLifecycleCallbacks(mAppBackgroundCallback)
                }
                mAppBackgroundCallback?.useCallback(true)
            }
        } catch (e: Exception) {
            log("Unable to open guard service!!")
        }
    }
}

/**
 * 注销Guard
 *
 * @receiver Context
 */
internal fun Context.unregister() {
    try {
        if (isGuardRunning && sRegistered) {
            sRegistered = false
            sGuardConfig?.apply {
                if (defaultConfig.workerEnabled) {
                    unregisterWorker()
                }
            }
            sendBroadcast(Intent("${Constant.GUARD_FLAG_STOP}.$packageName"))
            sMainHandler.postDelayed({
                mAppBackgroundCallback?.also {
                    it.useCallback(false)
                    if (this is Application) {
                        unregisterActivityLifecycleCallbacks(it)
                        mAppBackgroundCallback = null
                    }
                }
            }, 1000)
        } else {
            log("Guard is not running，Please make sure Guard is running!!")
        }
    } catch (e: Exception) {
    }
}

/**
 * 重新启动
 *
 * @receiver Context
 */
internal fun Context.restart() = register(getConfig())

/**
 * 更新通知栏
 *
 * @receiver Context
 * @param guardConfig GuardConfig
 */
internal fun Context.updateNotification(guardConfig: GuardConfig) {
    if (!getConfig().notificationConfig.canUpdate(guardConfig.notificationConfig)) {
        return
    }
    saveConfig(guardConfig)
    val managerCompat = NotificationManagerCompat.from(this)
    val notification = getNotification(guardConfig.notificationConfig)
    managerCompat.notify(guardConfig.notificationConfig.serviceId, notification)
}

/**
 * 最终都将调用此方法，注册Guard服务
 *
 * @receiver Context
 * @param guardConfig GuardConfig
 */
internal fun Context.registerGuard(guardConfig: GuardConfig) {
    val intent = Intent(this, LocalService::class.java)
    intent.putExtra(Constant.GUARD_CONFIG, guardConfig)
    startInternService(intent)
    sMainHandler.postDelayed({
        if (guardConfig.defaultConfig.workerEnabled) {
            registerWorker()
        } else {
            unregisterWorker()
        }
    }, 5000)
}

/**
 * 注册JobService
 *
 * @receiver Context
 * @param guardConfig GuardConfig
 */
internal fun Context.registerJobGuard(guardConfig: GuardConfig) {
    val intent = Intent(this, GuardJobService::class.java)
    intent.putExtra(Constant.GUARD_CONFIG, guardConfig)
    startInternService(intent)
}

/**
 * 开启WorkManager
 *
 * @receiver Context
 */
internal fun Context.registerWorker() {
    if (isGuardRunning && sRegistered) {
        try {
            val workRequest =
                PeriodicWorkRequest.Builder(GuardWorker::class.java, 15, TimeUnit.SECONDS)
                    .build()
            WorkManager.getInstance().enqueueUniquePeriodicWork(
                GuardWorker::class.java.name,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        } catch (e: Exception) {
            unregisterWorker()
            log("WorkManager registration failed")
        }
    }
}

/**
 * 取消WorkManager
 *
 * @receiver Context
 * @return Operation
 */
internal fun Context.unregisterWorker() =
    WorkManager.getInstance().cancelUniqueWork(GuardWorker::class.java.name)

/**
 * 开启远程服务
 *
 * @receiver Service
 * @param serviceConnection ServiceConnection
 * @param guardConfig GuardConfig
 */
internal fun Service.startRemoteService(
    serviceConnection: ServiceConnection,
    guardConfig: GuardConfig
) = startAndBindService(RemoteService::class.java, serviceConnection, guardConfig)

/**
 * 开启本地服务
 *
 * @receiver Service
 * @param serviceConnection ServiceConnection
 * @param guardConfig GuardConfig
 * @param isStart Boolean
 */
internal fun Service.startLocalService(
    serviceConnection: ServiceConnection,
    guardConfig: GuardConfig,
    isStart: Boolean = true
) = startAndBindService(LocalService::class.java, serviceConnection, guardConfig, isStart)

/**
 * 开启并绑定服务
 *
 * @receiver Service
 * @param cls Class<*>
 * @param serviceConnection ServiceConnection
 * @param guardConfig GuardConfig
 * @param isStart Boolean
 * @return Boolean
 */
private fun Service.startAndBindService(
    cls: Class<*>,
    serviceConnection: ServiceConnection,
    guardConfig: GuardConfig,
    isStart: Boolean = true
) = run {
    val intent = Intent(this, cls)
    intent.putExtra(Constant.GUARD_CONFIG, guardConfig)
    if (isStart) {
        startInternService(intent)
    }
    val bindService = bindService(intent, serviceConnection, Context.BIND_IMPORTANT)
    bindService
}

/**
 * 开启Service
 *
 * @receiver Context
 * @param intent Intent
 */
internal fun Context.startInternService(intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}

/**
 * 停止服务
 *
 * @receiver Service
 */
internal fun Service.stopService() {
    sMainHandler.postDelayed({
        try {
            this.stopSelf()
        } catch (e: Exception) {
        }
    }, 1000)
}

/**
 * 设置重启Intent
 *
 * @receiver Context
 * @param guardConfig GuardConfig
 */
private fun Context.handleRestartIntent(guardConfig: GuardConfig) {
    guardConfig.defaultConfig.apply {
        if (crashRestartEnabled) {
            restartIntent = packageManager.getLaunchIntentForPackage(packageName)
            restartIntent?.apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            restartIntent = null
        }
    }
}

/**
 * 开启一像素界面
 *
 * @receiver Context
 */
internal fun Context.startOnePixActivity() {
    if (!isScreenOn && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        mIsForeground = isForeground
        log("isForeground:$mIsForeground")
        val onePixIntent = Intent(this, OnePixActivity::class.java)
        onePixIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        onePixIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(this, 0, onePixIntent, 0)
        try {
            pendingIntent.send()
        } catch (e: Exception) {
        }
    }
}

/**
 * 销毁一像素
 */
internal fun finishOnePix() {
    mWeakReference?.apply {
        get()?.apply {
            finish()
        }
        mWeakReference = null
    }
}

/**
 * 保存一像素，方便销毁
 *
 * @receiver OnePixActivity
 */
internal fun OnePixActivity.setOnePix() {
    if (mWeakReference == null) {
        mWeakReference = WeakReference(this)
    }
}

/**
 * 退到后台
 *
 * @receiver Context
 */
internal fun backBackground() {
    mWeakReference?.apply {
        get()?.apply {
            if (!mIsForeground && isScreenOn) {
                val home = Intent(Intent.ACTION_MAIN)
                home.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                home.addCategory(Intent.CATEGORY_HOME)
                startActivity(home)
            }
        }
    }
}

/**
 * WaterBear是否在运行中
 */
internal val Context.isGuardRunning
    get() = run {
        isServiceRunning(LocalService::class.java.name) and isRunningTaskExist(Constant.GUARD_EMOTE_SERVICE)
    }

/**
 * 获得带id值的字段值
 */
internal val String.fieldById get() = "${Constant.GUARD_PACKAGE}.${this}.$id"

/**
 * 获取id
 */
internal val id get() = if (Process.myUid() <= 0) Process.myPid() else Process.myUid()

/**
 * 解除DeathRecipient绑定
 *
 * @receiver IBinder.DeathRecipient
 * @param iInterface IInterface?
 * @param block Function0<Unit>?
 */
internal fun IBinder.DeathRecipient.unlinkToDeath(
    iInterface: IInterface? = null,
    block: (() -> Unit)? = null
) {
    iInterface?.asBinder()?.unlinkToDeath(this, 0)
    block?.invoke()
}

/**
 * 全局log
 *
 * @param msg String
 */
internal fun log(msg: String) {
    sGuardConfig?.defaultConfig?.apply {
        if (debug) {
            Log.d(Constant.GUARD_TAG, msg)
        }
    } ?: Log.v(Constant.GUARD_TAG, msg)
}
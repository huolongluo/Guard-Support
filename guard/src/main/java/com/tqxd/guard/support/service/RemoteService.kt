package com.tqxd.guard.support.service

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Process
import com.tqxd.guard.support.entity.Constant
import com.tqxd.guard.support.entity.GuardConfig
import com.tqxd.guard.support.entity.IGuardInterface
import com.tqxd.guard.support.exception.GuardUncaughtExceptionHandler
import com.tqxd.guard.support.ext.*
import kotlin.system.exitProcess

/**
 * 远程服务
 */
class RemoteService : Service(), IBinder.DeathRecipient {

    /**
     * 配置信息
     */
    private lateinit var mGuardConfig: GuardConfig

    /**
     * 服务连接次数
     */
    private var mConnectionTimes = sTimes

    /**
     * 停止标识符
     */
    private var mIsStop = false

    /**
     * 是否已经绑定
     */
    private var mIsBind = false

    /**
     * 是否已经注册linkToDeath
     */
    private var mIsDeathBind = false

    private lateinit var remoteBinder: RemoteBinder

    private var mIInterface: IGuardInterface? = null

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            log("onServiceDisconnected")
            if (!mIsStop) {
                mIsBind = startLocalService(this, mGuardConfig)
            }
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            log("onServiceConnected")
            service?.let {
                mIInterface = IGuardInterface.Stub.asInterface(it)
                    ?.apply {
                        if (asBinder().isBinderAlive && asBinder().pingBinder()) {
                            try {
                                ++mConnectionTimes
                                wakeup(mGuardConfig)
                                connectionTimes(mConnectionTimes)
                                if (!mIsDeathBind) {
                                    mIsDeathBind = true
                                    asBinder().linkToDeath(this@RemoteService, 0)
                                }
                            } catch (e: Exception) {
                                --mConnectionTimes
                            }
                        }
                    }
            }
        }
    }

    override fun binderDied() {
        log("binderDied")
        try {
            unlinkToDeath(mIInterface) {
                mIsDeathBind = false
                mIInterface = null
                if (!mIsStop) {
                    mIsBind = startLocalService(mServiceConnection, mGuardConfig)
                }
            }
        } catch (e: Exception) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        GuardUncaughtExceptionHandler.instance
        try {
            log("handleNotification")
            mGuardConfig = getConfig()
            setNotification(mGuardConfig.notificationConfig)
        } catch (e: Exception) {
        }
        registerStopReceiver {
            mIsStop = true
            sTimes = mConnectionTimes
            stopService()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getParcelableExtra<GuardConfig>(Constant.GUARD_CONFIG)?.let {
            sGuardConfig = it
            mGuardConfig = it
        }
        setNotification(mGuardConfig.notificationConfig)
        mIsBind = startLocalService(mServiceConnection, mGuardConfig, false)
        log("RemoteService is running")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        stopBind()
        log("RemoteService has stopped")
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }

    override fun onBind(intent: Intent?): IBinder? {
        remoteBinder = RemoteBinder()
        return remoteBinder
    }

    inner class RemoteBinder : IGuardInterface.Stub() {

        override fun wakeup(config: GuardConfig) {
            mGuardConfig = config
            setNotification(mGuardConfig.notificationConfig)
        }

        override fun connectionTimes(time: Int) {
            mConnectionTimes = time
            if (mConnectionTimes > 4 && mConnectionTimes % 2 == 1) {
                ++mConnectionTimes
            }
            sTimes = mConnectionTimes
        }
    }

    /**
     * 解除相关绑定
     */
    private fun stopBind() {
        try {
            if (mIsDeathBind) {
                mIsDeathBind = false
                unlinkToDeath(mIInterface)
            }
            if (mIsBind) {
                unbindService(mServiceConnection)
                mIsBind = false
            }
        } catch (e: Exception) {
        }
    }
}
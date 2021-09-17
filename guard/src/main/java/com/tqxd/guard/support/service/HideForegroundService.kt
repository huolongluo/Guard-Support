package com.tqxd.guard.support.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.tqxd.guard.support.entity.Constant
import com.tqxd.guard.support.entity.NotificationConfig
import com.tqxd.guard.support.ext.sMainHandler
import com.tqxd.guard.support.ext.setNotification

/**
 * 隐藏前台服务
 */
class HideForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getParcelableExtra<NotificationConfig>(Constant.GUARD_NOTIFICATION_CONFIG)
            ?.let {
                setNotification(it, true)
            }
        sMainHandler.postDelayed({
            stopForeground(true)
            stopSelf()
        }, 2000)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
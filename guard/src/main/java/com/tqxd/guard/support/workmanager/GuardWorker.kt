package com.tqxd.guard.support.workmanager

import android.content.Context
import androidx.work.Worker
import com.tqxd.guard.support.ext.*

/**
 * WorkManager定时器
 */
class GuardWorker(val context: Context) :
    Worker() {

    /**
     * 停止标识符
     */
    private var mIsStop = false

    init {
        context.registerStopReceiver {
            mIsStop = true
        }
    }

    override fun doWork(): Result {
        context.apply {
            val guardConfig = getConfig()
            log("${this@GuardWorker}-doWork")
            if (!isGuardRunning && !mIsStop && !isStopped) {
                register(guardConfig)
            }
        }
        return Result.SUCCESS
    }
}
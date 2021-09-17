package com.tqxd.guard.sample.support

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tqxd.guard.support.Guard

/**
 * 测试Guard广播接受
 */
class MainReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        intent.action?.apply {
            when (this) {
                Guard.GUARD_WORK -> {
                    Log.e(
                        BaseApplication.TAG,
                        this + "--" + intent.getIntExtra(Guard.GUARD_TIMES, 0)
                    )
                }
                Guard.GUARD_STOP -> {
                    Log.e(BaseApplication.TAG, this)
                }
                Guard.GUARD_BACKGROUND -> {
                    Log.e(BaseApplication.TAG, this)
                }
                Guard.GUARD_FOREGROUND -> {
                    Log.e(BaseApplication.TAG, this)
                }
            }
        }
    }
}
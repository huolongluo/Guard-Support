package com.tqxd.guard.sample.support

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import com.tqxd.guard.support.Guard
import com.tqxd.guard.support.callback.GuardCallback
import com.tqxd.guard.support.ext.guard
import io.reactivex.disposables.Disposable
import java.text.SimpleDateFormat
import java.util.*

class BaseApplication : Application(), GuardCallback {

    companion object {
        const val TAG = "guard-sample"

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        /**
         * 结束时间
         */
        val mEndDate = MutableLiveData<String>()

        /**
         * 上次存活时间
         */
        val mLastTimer = MutableLiveData<String>()

        /**
         * 存活时间
         */
        val mTimer = MutableLiveData<String>()

        /**
         * 运行状态
         */
        val mStatus = MutableLiveData<Boolean>().apply { value = true }
    }

    private var mDisposable: Disposable? = null

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        //可选，设置通知栏点击事件
        val pendingIntent =
            PendingIntent.getActivity(this, 0, Intent().apply {
                setClass(this@BaseApplication, MainActivity::class.java)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }, PendingIntent.FLAG_UPDATE_CURRENT)
        //可选，注册广播监听器
        registerReceiver(MainReceiver(), IntentFilter().apply {
            addAction(Guard.GUARD_WORK)
            addAction(Guard.GUARD_STOP)
            addAction(Guard.GUARD_BACKGROUND)
            addAction(Guard.GUARD_FOREGROUND)
        })

        guard {
            //可选，设置通知栏点击事件
            setPendingIntent(pendingIntent)
            //可选，设置音乐
            setMusicId(R.raw.debug)
            //可选，是否是debug模式
            isDebug(true)
            //可选，退到后台是否可以播放音乐
            setBackgroundMusicEnabled(true)
            //可选，设置奔溃可以重启，google原生rom android 10以下可以正常重启
            setCrashRestartUIEnabled(true)
            setWorkerEnabled(true)
            hideNotification(true)
            hideNotificationAfterO(true)
            //可选，运行时回调
            addCallback(this@BaseApplication)
            //可选，切后台切换回调
            addBackgroundCallback {
                Toast.makeText(
                    this@BaseApplication,
                    if (it) "退到后台啦" else "跑到前台啦",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        //或者这样设置前后台监听
//        registerActivityLifecycleCallbacks(AppBackgroundCallback {
//
//        })
    }

    override fun doWork(times: Int) {
        Log.d(TAG, "doWork:$times")
        mStatus.postValue(true)
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+00:00")
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        mStatus.postValue(false)
        mDisposable?.apply {
            if (!isDisposed) {
                dispose()
            }
        }
    }

}
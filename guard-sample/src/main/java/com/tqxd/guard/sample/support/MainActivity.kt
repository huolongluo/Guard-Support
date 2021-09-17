package com.tqxd.guard.sample.support

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.tqxd.guard.support.ext.guardRestart
import com.tqxd.guard.support.ext.guardUnregister
import com.tqxd.guard.support.ext.guardUpdateNotification

@Suppress("DIVISION_BY_ZERO")
@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    private lateinit var tvVersion: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnUpdate: Button
    private lateinit var btnStop: Button
    private lateinit var btnRestart: Button
    private lateinit var btnCrash: Button

    private var times = 0L

    private val list = listOf(
        Pair("火龙裸1", "使命召唤1"),
        Pair("火龙裸2", "使命召唤2"),
        Pair("火龙裸3", "使命召唤3"),
        Pair("火龙裸4", "使命召唤4"),
        Pair("火龙裸5", "使命召唤5"),
        Pair("火龙裸6", "使命召唤6"),
        Pair("火龙裸7", "使命召唤7"),
        Pair("火龙裸8", "使命召唤8"),
        Pair("火龙裸9", "使命召唤9")
    )

    companion object {
        private const val TIME = 4000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvVersion = findViewById(R.id.tvVersion)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnStop = findViewById(R.id.btnStop)
        btnRestart = findViewById(R.id.btnRestart)
        btnCrash = findViewById(R.id.btnCrash)
        tvStatus = findViewById(R.id.tvStatus)
        initData()
        setListener()
    }

    private fun initData() {
        tvVersion.text = "Version(版本)：${BuildConfig.VERSION_NAME}"
        BaseApplication.mStatus.observe(this, Observer {
            tvStatus.text = if (it == true) {
                "Operating status(运行状态):Running(运行中)"
            } else {
                "Operating status(运行状态):Stopped(已停止)"
            }
        })
    }

    private fun setListener() {
        //更新通知栏信息
        btnUpdate.onClick {
            val num = (0..8).random()
            guardUpdateNotification {
                setTitle(list[num].first)
                setContent(list[num].second)
            }
        }
        //停止
        btnStop.onClick {
            guardUnregister()
        }
        //重启
        btnRestart.onClick {
            guardRestart()
        }
        //奔溃
        btnCrash.setOnClickListener {
            Toast.makeText(
                this,
                "The app will crash after three seconds(3s后奔溃)",
                Toast.LENGTH_SHORT
            ).show()
            Handler().postDelayed({
                2 / 0
            }, 3000)
        }
    }

    private inline fun View.onClick(crossinline block: () -> Unit) {
        setOnClickListener {
            val nowTime = System.currentTimeMillis()
            val intervals = nowTime - times
            if (intervals > TIME) {
                times = nowTime
                block()
            } else {
                Toast.makeText(
                    context,
                    ((TIME.toFloat() - intervals) / 1000).toString() + "秒之后再点击",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
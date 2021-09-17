# Guard-Support

## 实现原理参考
* [Android 黑科技保活实现原理揭秘](http://weishu.me/2020/01/16/a-keep-alive-method-on-android/)
* [深度剖析APP保活案例](http://gityuan.com/2018/02/24/process-keep-forever/)


## 用法（具体api请参考api说明）
```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

* ### androidx库的使用
```
dependencies {
    implementation 'com.github.huolongluo:Guard:v1.0'
}
```
* ### android support库的使用
```
dependencies {
    implementation 'com.github.huolongluo:Guard-Support:v1.0'
}
```

### java用法
* 注册（建议在Application的onCreate方法中进行）
```
Guard.getInstance()
         .isDebug(true)
         .setPendingIntent(pendingIntent)//设置通知栏点击事件（非必传）
         .setMusicId(R.raw.main)//设置音乐（非必传）
         .setBackgroundMusicEnabled(true)//退到后台是否可以播放音乐（非必传）
         .setCrashRestartUIEnabled(true)//设置App崩溃可以重启，google原生rom android 10以下可以正常重启（非必传）
         .setWorkerEnabled(true)//是否可以使用WorkManager，默认可以使用（非必传）
//         .addCallback(new GuardCallback())//运行时回调（非必传）
//         .addBackgroundCallback(new GuardBackgroundCallback())//前后台切换回调，用于处理app前后台切换，（非必传）
         .register(this);
```
* 注销
```
Guard.getInstance().unregister(this)
```
* 重启
```
Guard.getInstance().restart(this)
```

### Kotlin用法
* 注册
```
guard {
    setPendingIntent(pendingIntent)
    setMusicId(R.raw.debug)
    isDebug(true)
    ... //其他api等
    ...
    addCallback({
       //onStop回调，可以省略
    }) { 
       //doWork回调
    }
 }
```
* 注销
```
guardUnregister()
```
* 重启
```
guardRestart()
```

## 混淆规则(proguard-rules.pro)
```
-keep class com.tqxd.guard.entity.* {*;} 
```
## Be careful:
当项目在android 8.0以上的设备，隐藏了通知栏信息，app进程一旦崩溃，则自动重启，后会出现invalid channel for service notification异常，而该异常属于系统级别的，无法直接对这种系统的异常进行捕获，假如项目里面用到一些三方异常捕获库，如Bugly之类的，不能完全保证第三方异常监控也能捕获它。 可通过调用hideNotificationAfterO(false)方法，打开通知栏信息，这样就可以查看App的存活状态了。

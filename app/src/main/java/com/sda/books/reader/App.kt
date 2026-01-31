package com.sda.books.reader

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import com.sda.books.reader.util.SPUtils
import com.sda.books.reader.security.NetworkSecurityInterceptor
import es.dmoral.toasty.Toasty
import okhttp3.OkHttpClient
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class App : Application() {
    companion object {
        lateinit var context: Context
        val weakActivity = mutableListOf<WeakReference<Activity>>()

        private const val TAG = "App"

        // 全局安全 OkHttpClient（供其他模块使用）
        lateinit var secureOkHttpClient: OkHttpClient
            private set

        fun clear(){
            weakActivity.forEach {
                it.get()?.finish()
            }
            weakActivity.clear()
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        SPUtils.init(this)

        // ===== 配置安全的 OkHttpClient（带白名单拦截器）=====
        secureOkHttpClient = OkHttpClient.Builder()
            .addInterceptor(NetworkSecurityInterceptor()) // 添加安全拦截器（第一道防线）
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)  // 允许重定向（会被拦截器二次校验）
            .followSslRedirects(true)
            .build()

        Log.i(TAG, "✅ 网络安全拦截器已启用")

        // FileDownloader 和 OkGo 已移除，统一使用 OkHttp
        Log.i(TAG, "✅ 网络层统一使用 OkHttp + 白名单拦截")

        Toasty.Config.getInstance()
            .setGravity(Gravity.CENTER, 0, 0)
            .apply()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                weakActivity.add(WeakReference(activity))
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        Log.i(TAG, "========================================")
        Log.i(TAG, "🔒 网络安全加固已完成")
        Log.i(TAG, "📋 允许的域名：sdattg.com, sdacn.cn（及子域名）")
        Log.i(TAG, "🚫 其他域名、IP、localhost 均被拦截")
        Log.i(TAG, "========================================")
    }
}

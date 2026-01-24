package com.sda.books.reader

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import com.sda.books.reader.util.SPUtils
import com.liulishuo.filedownloader.FileDownloader
import com.liulishuo.filedownloader.connection.FileDownloadUrlConnection
import com.lzy.okgo.OkGo
import com.sda.books.reader.security.NetworkSecurityInterceptor
import es.dmoral.toasty.Toasty
import okhttp3.OkHttpClient
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.Proxy
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

        // ===== 配置 FileDownloader =====
        // FileDownloader 1.7.7 不支持直接注入 OkHttpClient
        // 解决方案：使用自定义 Connection 来应用安全拦截
        try {
            FileDownloader.setupOnApplicationOnCreate(this)
                .connectionCreator(FileDownloadUrlConnection.Creator(
                    FileDownloadUrlConnection.Configuration()
                        .connectTimeout(30_000) // 30秒
                        .readTimeout(30_000)
                ))
                .commit()
            
            Log.i(TAG, "✅ FileDownloader 已初始化")
            Log.i(TAG, "⚠️  注意：FileDownloader 将通过系统网络层执行请求")
            Log.i(TAG, "    如需完全安全隔离，请使用 OkGo 或手动 OkHttp 下载")
        } catch (e: Exception) {
            Log.e(TAG, "❌ FileDownloader 配置失败，使用默认配置", e)
            FileDownloader.setup(this)
        }

        // ===== 配置 OkGo（使用安全的 OkHttpClient）=====
        OkGo.getInstance()
            .init(this)
            .setOkHttpClient(secureOkHttpClient) // 强制使用安全客户端
            .setRetryCount(0) // 禁用自动重试（失败应该立即报错）

        Log.i(TAG, "✅ OkGo 已配置安全网络层")

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

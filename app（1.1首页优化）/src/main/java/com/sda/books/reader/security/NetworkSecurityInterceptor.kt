package com.sda.books.reader.security

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 网络安全拦截器
 * 在所有网络请求发出前强制校验域名白名单，无法绕过
 * 
 * 功能：
 * 1. 请求前校验：拦截非白名单域名的请求
 * 2. 响应后校验：防止重定向到非白名单域名
 * 3. 禁止 IP/localhost/file 协议
 */
class NetworkSecurityInterceptor : Interceptor {

    companion object {
        private const val TAG = "NetworkSecurity"
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val host = request.url.host

        // ===== 第一道防线：请求前校验 =====
        if (!WhitelistConfig.isHostAllowed(host)) {
            val errorMsg = WhitelistConfig.getBlockedMessage(url)
            Log.w(TAG, "❌ 请求被拦截: $errorMsg")
            
            // 抛出异常阻止请求
            throw SecurityException(errorMsg)
        }

        Log.d(TAG, "✅ 请求通过白名单校验: $url")

        // 继续执行请求
        val response = chain.proceed(request)

        // ===== 第二道防线：响应后校验重定向 =====
        // 检查是否发生了重定向（3xx 状态码）
        if (response.isRedirect) {
            val redirectUrl = response.header("Location")
            
            if (redirectUrl != null && !WhitelistConfig.isUrlAllowed(redirectUrl)) {
                val errorMsg = "安全策略：禁止重定向到非白名单域名 ($redirectUrl)"
                Log.w(TAG, "❌ 重定向被拦截: $errorMsg")
                
                // 关闭响应体
                response.close()
                
                // 抛出异常阻止重定向
                throw SecurityException(errorMsg)
            }

            if (redirectUrl != null) {
                Log.d(TAG, "✅ 重定向通过白名单校验: $redirectUrl")
            }
        }

        return response
    }
}

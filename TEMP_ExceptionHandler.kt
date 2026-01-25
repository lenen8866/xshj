package com.sda.books.reader.util

import android.util.Log
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 全局异常处理工具
 */
object ExceptionHandler {
    
    private const val TAG = "ExceptionHandler"
    
    /**
     * 创建协程异常处理器
     */
    fun createCoroutineExceptionHandler(
        onError: ((Throwable) -> Unit)? = null
    ): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Coroutine exception: ${throwable.message}", throwable)
            handleException(throwable)
            onError?.invoke(throwable)
        }
    }
    
    /**
     * 处理异常并显示友好提示
     */
    fun handleException(throwable: Throwable) {
        val message = when (throwable) {
            is UnknownHostException -> "网络连接失败，请检查网络设置"
            is SocketTimeoutException -> "网络请求超时，请稍后重试"
            is IOException -> "网络错误：${throwable.message}"
            is NullPointerException -> "数据异常，请稍后重试"
            else -> "操作失败：${throwable.message ?: "未知错误"}"
        }
        
        ToastUtils.showShort(message)
        Log.e(TAG, "Exception handled: $message", throwable)
    }
    
    /**
     * 安全执行代码块
     */
    inline fun <T> safeLet(
        block: () -> T,
        onError: ((Throwable) -> T)? = null
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "SafeLet exception: ${e.message}", e)
            handleException(e)
            onError?.invoke(e)
        }
    }
    
    /**
     * 处理 Result 类型
     */
    fun <T> Result<T>.handleResult(
        onSuccess: (T) -> Unit,
        onFailure: ((Throwable) -> Unit)? = null
    ) {
        fold(
            onSuccess = onSuccess,
            onFailure = { throwable ->
                handleException(throwable)
                onFailure?.invoke(throwable)
            }
        )
    }
}

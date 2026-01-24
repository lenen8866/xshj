package com.sda.books.reader.store

import android.util.Log
import com.sda.books.reader.App
import com.sda.books.reader.security.SafeFileDownloader
import com.sda.books.reader.security.WhitelistConfig
import com.sda.books.reader.util.SPUtils
import com.sda.books.reader.util.md5
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * 文件下载管理器（已加固：使用安全的 OkHttpClient）
 * 所有下载都经过白名单校验和安全文件名处理
 */
object StoreManager {

    private const val TAG = "StoreManager"

    fun checkFileIsDownLoadFinish(url: String): Boolean {
        // ===== 安全校验：只允许白名单URL =====
        if (!SafeFileDownloader.isDownloadUrlSafe(url)) {
            Log.w(TAG, "❌ URL 不在白名单内: $url")
            return false
        }

        val cacheDir = App.context.cacheDir.absolutePath + File.separator + "/dlFile"
        val safeFileName = SafeFileDownloader.getSafeFileName(url)
        val desFile = File(cacheDir, safeFileName)
        val key = url.md5()
        val isFinish = SPUtils.get().get(key, false)
        return isFinish && desFile.exists()
    }

    fun getDesFile(url: String): File? {
        // ===== 安全校验：只允许白名单URL =====
        if (!SafeFileDownloader.isDownloadUrlSafe(url)) {
            Log.w(TAG, "❌ URL 不在白名单内: $url")
            return null
        }

        val cacheDir = App.context.cacheDir.absolutePath + File.separator + "/dlFile"
        val safeFileName = SafeFileDownloader.getSafeFileName(url)
        return File(cacheDir, safeFileName)
    }

    /**
     * 开始下载文件（使用安全的 OkHttpClient）
     * 替代原有的 FileDownloader，确保所有请求都经过白名单拦截
     */
    fun startDownLoad(url: String, onProgress: ((Int) -> Unit)? = null, onComplete: (() -> Unit)? = null, onError: ((Throwable) -> Unit)? = null) {
        // ===== 安全校验：只允许白名单URL =====
        if (!SafeFileDownloader.isDownloadUrlSafe(url)) {
            val errorMsg = WhitelistConfig.getBlockedMessage(url)
            Log.e(TAG, "❌ 下载被拦截: $errorMsg")
            onError?.invoke(SecurityException(errorMsg))
            return
        }

        val cacheDir = App.context.cacheDir.absolutePath + File.separator + "/dlFile"
        val cacheDirFile = File(cacheDir)
        if (!cacheDirFile.exists()) {
            cacheDirFile.mkdir()
        }

        val key = url.md5()
        val isFinish = SPUtils.get().get(key, false)
        
        // 使用安全文件名
        val safeFileName = SafeFileDownloader.getSafeFileName(url)
        val desFile = File(cacheDir, safeFileName)

        // 验证文件路径安全性
        try {
            SafeFileDownloader.validateFilePath(cacheDirFile, safeFileName)
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ 文件路径校验失败", e)
            onError?.invoke(e)
            return
        }

        if (isFinish) {
            // 下载完成并且文件存在
            if (desFile.exists()) {
                Log.i(TAG, "✅ 文件已存在: ${desFile.name}")
                onComplete?.invoke()
                return
            }
            // 文件不存在，设置成未完成
            SPUtils.get().put(key, false)
        } else {
            // 没下载完成但是文件存在，直接删除
            if (desFile.exists()) {
                desFile.delete()
                Log.i(TAG, "删除未完成的文件: ${desFile.name}")
            }
        }

        Log.i(TAG, "✅ 开始下载: $url")
        Log.i(TAG, "安全文件名: $safeFileName")

        // ===== 使用安全的 OkHttpClient 下载 =====
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder().url(url).build()
                val response = App.secureOkHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("下载失败: HTTP ${response.code}")
                }

                val contentLength = response.body?.contentLength() ?: -1L
                val outputStream = FileOutputStream(desFile)

                response.body?.byteStream()?.use { inputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // 更新进度
                        if (contentLength > 0) {
                            val progress = (totalBytesRead * 100 / contentLength).toInt()
                            withContext(Dispatchers.Main) {
                                onProgress?.invoke(progress)
                            }
                            Log.d(TAG, "下载进度: $progress% ($totalBytesRead/$contentLength)")
                        }
                    }
                }
                outputStream.close()

                // 验证文件完整性
                if (contentLength > 0 && desFile.length() != contentLength) {
                    desFile.delete()
                    throw Exception("文件大小不匹配")
                }

                Log.i(TAG, "✅ 下载完成: ${desFile.name}")
                SPUtils.get().put(key, true)
                
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 下载失败", e)
                SPUtils.get().put(key, false)
                
                // 删除损坏的文件
                if (desFile.exists()) {
                    desFile.delete()
                }

                withContext(Dispatchers.Main) {
                    onError?.invoke(e)
                }
            }
        }
    }
}

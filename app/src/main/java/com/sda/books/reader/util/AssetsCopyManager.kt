package com.sda.books.reader.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*

/**
 * Assets 文件复制工具类
 * @param context 上下文（建议用 Application Context 避免内存泄漏）
 */
class AssetsCopyManager(private val context: Context) {

    // 进度回调接口
    interface CopyProgressCallback {
        // 进度值：0.0 ~ 1.0
        fun onProgress(progress: Float)
        // 复制完成
        fun onComplete()
        // 复制失败
        fun onError(e: Exception)
    }

    /**
     * 复制 assets 下的文件到指定路径
     * @param assetsFilePath assets 内的文件路径（如 "test.txt"、"files/app.apk"）
     * @param targetFilePath 目标文件完整路径（如 "${context.externalCacheDir}/test.txt"）
     * @param callback 进度回调
     */
    suspend fun copyFile(
        assetsFilePath: String,
        targetFilePath: String,
        callback: CopyProgressCallback
    ) = withContext(Dispatchers.IO) {
        try {
            // 1. 打开 assets 输入流
            val inputStream = context.assets.open(assetsFilePath)
            // 2. 创建目标文件（自动创建父目录）
            val targetFile = File(targetFilePath)
            if (!targetFile.parentFile?.exists()!!) {
                targetFile.parentFile?.mkdirs()
            }
            if (!targetFile.exists()) {
                targetFile.createNewFile()
            }
            // 3. 打开输出流
            val outputStream = FileOutputStream(targetFile)

            // 4. 获取文件总大小
            val totalSize = inputStream.available().toFloat()
            var copiedSize = 0L

            // 5. 分块读写（8KB 缓冲区，可根据需求调整）
            val buffer = ByteArray(20480)
            var length: Int
            while (inputStream.read(buffer).also { length = it } != -1) {
                outputStream.write(buffer, 0, length)
//                /*copiedSize += length
//
//                // 6. 计算并回调进度（切换到主线程更新 UI）
//                val progress = copiedSize / totalSize
//                withContext(Dispatchers.Main) {
//                    callback.onProgress(progress)
//                }*/
            }

            // 7. 关闭流
            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // 8. 回调完成
            withContext(Dispatchers.Main) {
                callback.onComplete()
            }
        } catch (e: Exception) {
            // 9. 回调错误
            withContext(Dispatchers.Main) {
                callback.onError(e)
            }
        }
    }
}
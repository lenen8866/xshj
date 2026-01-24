package com.sda.books.reader.security

import java.io.File

/**
 * 安全文件下载工具类
 * 防止路径穿越攻击和文件名注入
 */
object SafeFileDownloader {

    /**
     * 从 URL 中安全提取文件名
     * @param url 下载地址
     * @return 安全的文件名，如果无法提取则返回默认名称
     */
    fun getSafeFileName(url: String): String {
        try {
            // 提取 URL 路径部分的文件名
            val rawFileName = url
                .substringAfterLast("/")
                .substringBefore("?")  // 移除查询参数
                .substringBefore("#")  // 移除锚点

            // 如果提取失败，使用时间戳作为文件名
            if (rawFileName.isBlank()) {
                return "download_${System.currentTimeMillis()}.tmp"
            }

            // 清理文件名中的危险字符
            return sanitizeFileName(rawFileName)
        } catch (e: Exception) {
            // 任何异常都返回安全的默认文件名
            return "download_${System.currentTimeMillis()}.tmp"
        }
    }

    /**
     * 清理文件名，移除危险字符
     * @param fileName 原始文件名
     * @return 清理后的安全文件名
     */
    private fun sanitizeFileName(fileName: String): String {
        // 1. 移除路径穿越字符
        var safe = fileName
            .replace("..", "")
            .replace("/", "")
            .replace("\\", "")
            .replace(":", "")
            .replace("*", "")
            .replace("?", "")
            .replace("\"", "")
            .replace("<", "")
            .replace(">", "")
            .replace("|", "")

        // 2. 移除开头的点（隐藏文件）
        safe = safe.trimStart('.')

        // 3. 限制文件名长度（防止超长文件名）
        if (safe.length > 100) {
            val extension = safe.substringAfterLast(".", "")
            safe = if (extension.isNotBlank()) {
                "${safe.substring(0, 90)}.${extension}"
            } else {
                safe.substring(0, 100)
            }
        }

        // 4. 如果清理后为空，返回默认文件名
        if (safe.isBlank()) {
            safe = "download_${System.currentTimeMillis()}.tmp"
        }

        return safe
    }

    /**
     * 验证目标文件路径是否安全
     * @param targetDir 目标目录
     * @param fileName 文件名
     * @return 安全的完整文件路径
     * @throws SecurityException 如果路径不安全
     */
    @Throws(SecurityException::class)
    fun validateFilePath(targetDir: File, fileName: String): File {
        // 1. 清理文件名
        val safeFileName = sanitizeFileName(fileName)

        // 2. 构建目标文件
        val targetFile = File(targetDir, safeFileName)

        // 3. 验证规范路径（防止路径穿越）
        val canonicalTarget = targetFile.canonicalPath
        val canonicalDir = targetDir.canonicalPath

        if (!canonicalTarget.startsWith(canonicalDir)) {
            throw SecurityException("路径穿越攻击检测：文件路径超出允许目录")
        }

        return targetFile
    }

    /**
     * 校验下载 URL 是否安全
     * @param url 下载地址
     * @return true: 安全, false: 不安全
     */
    fun isDownloadUrlSafe(url: String): Boolean {
        // 1. 校验白名单
        if (!WhitelistConfig.isUrlAllowed(url)) {
            return false
        }

        // 2. 校验协议（只允许 http/https）
        val lowerUrl = url.lowercase()
        if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
            return false
        }

        return true
    }

    /**
     * 添加时间戳前缀（防止文件覆盖）
     * @param fileName 原始文件名
     * @return 带时间戳的文件名
     */
    fun addTimestampPrefix(fileName: String): String {
        return "${System.currentTimeMillis()}_$fileName"
    }

    /**
     * 校验文件扩展名是否在允许列表中
     * @param fileName 文件名
     * @param allowedExtensions 允许的扩展名集合（如 setOf("zip", "db", "json")）
     * @return true: 允许, false: 禁止
     */
    fun isExtensionAllowed(fileName: String, allowedExtensions: Set<String>): Boolean {
        val extension = fileName.substringAfterLast(".", "").lowercase()
        return extension.isNotBlank() && extension in allowedExtensions
    }
}

package com.sda.books.reader.security

import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * 安全文件下载工具测试用例
 * 验证文件名清理和路径穿越防护
 */
class SafeFileDownloaderTest {

    @Test
    fun `test 正常文件名提取`() {
        assertEquals(
            "data.zip",
            SafeFileDownloader.getSafeFileName("https://sdacn.cn/downloads/data.zip")
        )
        
        assertEquals(
            "中文文件.db",
            SafeFileDownloader.getSafeFileName("https://sdattg.com/files/中文文件.db")
        )
    }

    @Test
    fun `test 移除查询参数`() {
        val result = SafeFileDownloader.getSafeFileName(
            "https://sdacn.cn/file.zip?token=abc123&id=456"
        )
        assertEquals("file.zip", result)
    }

    @Test
    fun `test 移除锚点`() {
        val result = SafeFileDownloader.getSafeFileName(
            "https://sdattg.com/download.zip#section"
        )
        assertEquals("download.zip", result)
    }

    @Test
    fun `test 路径穿越攻击防护`() {
        // 包含 ../ 的文件名应被清理
        val maliciousName = "../../../etc/passwd"
        val safe = SafeFileDownloader.getSafeFileName("https://sdacn.cn/$maliciousName")
        
        assertFalse(safe.contains(".."))
        assertFalse(safe.contains("/"))
    }

    @Test
    fun `test 危险字符清理`() {
        val dangerous = "file:name?.txt"
        val url = "https://sdacn.cn/$dangerous"
        val safe = SafeFileDownloader.getSafeFileName(url)
        
        // 应该移除所有危险字符
        assertFalse(safe.contains(":"))
        assertFalse(safe.contains("?"))
        assertFalse(safe.contains("*"))
    }

    @Test
    fun `test 隐藏文件防护`() {
        val hidden = ".hidden_file"
        val url = "https://sdacn.cn/$hidden"
        val safe = SafeFileDownloader.getSafeFileName(url)
        
        // 不应该以 . 开头
        assertFalse(safe.startsWith("."))
    }

    @Test
    fun `test 超长文件名截断`() {
        val longName = "a".repeat(150) + ".zip"
        val url = "https://sdacn.cn/$longName"
        val safe = SafeFileDownloader.getSafeFileName(url)
        
        // 应该被截断到合理长度
        assertTrue(safe.length <= 100)
        assertTrue(safe.endsWith(".zip"))  // 保留扩展名
    }

    @Test
    fun `test 空文件名降级处理`() {
        // 无法提取文件名时应返回默认名称
        val safe1 = SafeFileDownloader.getSafeFileName("https://sdacn.cn/")
        assertTrue(safe1.startsWith("download_"))
        assertTrue(safe1.endsWith(".tmp"))
        
        val safe2 = SafeFileDownloader.getSafeFileName("https://sdacn.cn")
        assertTrue(safe2.startsWith("download_"))
    }

    @Test
    fun `test URL 安全性校验 - 白名单`() {
        // 白名单域名
        assertTrue(SafeFileDownloader.isDownloadUrlSafe("https://sdacn.cn/file.zip"))
        assertTrue(SafeFileDownloader.isDownloadUrlSafe("http://static.sdattg.com/data.db"))
        
        // 非白名单域名
        assertFalse(SafeFileDownloader.isDownloadUrlSafe("https://evil.com/malware.apk"))
        assertFalse(SafeFileDownloader.isDownloadUrlSafe("http://192.168.1.1/file.zip"))
    }

    @Test
    fun `test URL 安全性校验 - 协议限制`() {
        // 只允许 http/https
        assertTrue(SafeFileDownloader.isDownloadUrlSafe("http://sdacn.cn/file.zip"))
        assertTrue(SafeFileDownloader.isDownloadUrlSafe("https://sdacn.cn/file.zip"))
        
        // 禁止其他协议
        assertFalse(SafeFileDownloader.isDownloadUrlSafe("ftp://sdacn.cn/file.zip"))
        assertFalse(SafeFileDownloader.isDownloadUrlSafe("file:///sdcard/file.zip"))
    }

    @Test
    fun `test 时间戳前缀`() {
        val original = "data.zip"
        val withTimestamp = SafeFileDownloader.addTimestampPrefix(original)
        
        assertTrue(withTimestamp.endsWith("_data.zip"))
        assertTrue(withTimestamp.length > original.length)
        
        // 验证时间戳是数字
        val timestamp = withTimestamp.substringBefore("_")
        assertTrue(timestamp.all { it.isDigit() })
    }

    @Test
    fun `test 文件扩展名校验`() {
        val allowed = setOf("zip", "db", "json")
        
        assertTrue(SafeFileDownloader.isExtensionAllowed("file.zip", allowed))
        assertTrue(SafeFileDownloader.isExtensionAllowed("data.db", allowed))
        assertTrue(SafeFileDownloader.isExtensionAllowed("config.JSON", allowed))  // 大小写不敏感
        
        assertFalse(SafeFileDownloader.isExtensionAllowed("malware.apk", allowed))
        assertFalse(SafeFileDownloader.isExtensionAllowed("script.sh", allowed))
        assertFalse(SafeFileDownloader.isExtensionAllowed("noextension", allowed))
    }

    @Test
    fun `test 路径规范化校验`() {
        val tempDir = createTempDir("test_dir")
        
        try {
            // 正常文件名
            val safe = SafeFileDownloader.validateFilePath(tempDir, "normal.zip")
            assertTrue(safe.canonicalPath.startsWith(tempDir.canonicalPath))
            
            // 尝试路径穿越（应抛出异常）
            try {
                SafeFileDownloader.validateFilePath(tempDir, "../../../etc/passwd")
                fail("应该抛出 SecurityException")
            } catch (e: SecurityException) {
                assertTrue(e.message?.contains("路径穿越") == true)
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }
}

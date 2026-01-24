package com.sda.books.reader.security

import org.junit.Test
import org.junit.Assert.*

/**
 * 网络安全白名单测试用例
 * 验证 URL 拦截机制的正确性
 */
class WhitelistConfigTest {

    @Test
    fun `test 允许的域名 - sdattg_com`() {
        assertTrue(WhitelistConfig.isUrlAllowed("https://sdattg.com"))
        assertTrue(WhitelistConfig.isUrlAllowed("http://sdattg.com"))
        assertTrue(WhitelistConfig.isUrlAllowed("https://sdattg.com/path/to/file.zip"))
        assertTrue(WhitelistConfig.isUrlAllowed("http://sdattg.com:8080/download"))
    }

    @Test
    fun `test 允许的域名 - sdacn_cn`() {
        assertTrue(WhitelistConfig.isUrlAllowed("https://sdacn.cn"))
        assertTrue(WhitelistConfig.isUrlAllowed("http://sdacn.cn"))
        assertTrue(WhitelistConfig.isUrlAllowed("https://static.sdacn.cn/files/data.zip"))
    }

    @Test
    fun `test 允许的子域名`() {
        assertTrue(WhitelistConfig.isUrlAllowed("https://www.sdattg.com"))
        assertTrue(WhitelistConfig.isUrlAllowed("https://api.sdattg.com"))
        assertTrue(WhitelistConfig.isUrlAllowed("https://static.sdacn.cn"))
        assertTrue(WhitelistConfig.isUrlAllowed("https://cdn.api.sdacn.cn"))
    }

    @Test
    fun `test 拒绝非白名单域名`() {
        assertFalse(WhitelistConfig.isUrlAllowed("https://google.com"))
        assertFalse(WhitelistConfig.isUrlAllowed("http://example.com"))
        assertFalse(WhitelistConfig.isUrlAllowed("https://evil.com/malware.apk"))
        assertFalse(WhitelistConfig.isUrlAllowed("https://sdattg.org"))  // 不同后缀
        assertFalse(WhitelistConfig.isUrlAllowed("https://fakesdattg.com"))
    }

    @Test
    fun `test 拒绝 IP 地址`() {
        assertFalse(WhitelistConfig.isUrlAllowed("http://192.168.1.1"))
        assertFalse(WhitelistConfig.isUrlAllowed("https://8.8.8.8:443"))
        assertFalse(WhitelistConfig.isUrlAllowed("http://127.0.0.1:8080"))
        assertFalse(WhitelistConfig.isUrlAllowed("http://[2001:db8::1]"))  // IPv6
    }

    @Test
    fun `test 拒绝 localhost`() {
        assertFalse(WhitelistConfig.isUrlAllowed("http://localhost"))
        assertFalse(WhitelistConfig.isUrlAllowed("https://localhost:8080"))
        assertFalse(WhitelistConfig.isUrlAllowed("http://127.0.0.1"))
    }

    @Test
    fun `test 拒绝 file 协议`() {
        assertFalse(WhitelistConfig.isUrlAllowed("file:///sdcard/malware.apk"))
        assertFalse(WhitelistConfig.isUrlAllowed("file:///data/data/com.app/files"))
    }

    @Test
    fun `test 拒绝空值和非法输入`() {
        assertFalse(WhitelistConfig.isUrlAllowed(null))
        assertFalse(WhitelistConfig.isUrlAllowed(""))
        assertFalse(WhitelistConfig.isUrlAllowed("   "))
        assertFalse(WhitelistConfig.isUrlAllowed("not-a-url"))
    }

    @Test
    fun `test Host 校验`() {
        assertTrue(WhitelistConfig.isHostAllowed("sdattg.com"))
        assertTrue(WhitelistConfig.isHostAllowed("www.sdattg.com"))
        assertTrue(WhitelistConfig.isHostAllowed("static.sdacn.cn"))
        
        assertFalse(WhitelistConfig.isHostAllowed("google.com"))
        assertFalse(WhitelistConfig.isHostAllowed("192.168.1.1"))
        assertFalse(WhitelistConfig.isHostAllowed("localhost"))
    }

    @Test
    fun `test 大小写不敏感`() {
        assertTrue(WhitelistConfig.isUrlAllowed("https://SDATTG.COM"))
        assertTrue(WhitelistConfig.isUrlAllowed("https://SdAtTg.CoM"))
        assertTrue(WhitelistConfig.isUrlAllowed("HTTPS://WWW.SDACN.CN"))
    }

    @Test
    fun `test 复杂 URL 解析`() {
        // 带端口
        assertTrue(WhitelistConfig.isUrlAllowed("https://sdattg.com:443/path"))
        
        // 带查询参数
        assertTrue(WhitelistConfig.isUrlAllowed("https://sdacn.cn/file?id=123&name=test"))
        
        // 带锚点
        assertTrue(WhitelistConfig.isUrlAllowed("https://sdattg.com/page#section"))
        
        // 完整复杂 URL
        assertTrue(WhitelistConfig.isUrlAllowed("https://api.sdacn.cn:8080/v2/download?file=data.zip&token=abc123#result"))
    }

    @Test
    fun `test 错误提示信息`() {
        val blockedUrl = "https://evil.com/malware"
        val message = WhitelistConfig.getBlockedMessage(blockedUrl)
        
        assertTrue(message.contains("安全策略"))
        assertTrue(message.contains("evil.com"))
    }
}

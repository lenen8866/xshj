package com.sda.books.reader.security

/**
 * 网络安全白名单配置
 * 严格限制只允许访问指定域名，防止数据泄露和恶意请求
 */
object WhitelistConfig {
    
    /**
     * 允许访问的域名白名单（不含协议和端口）
     * 支持主域名及所有子域名
     */
    private val ALLOWED_DOMAINS = setOf(
        "sdattg.com",
        "sdacn.cn"
    )

    /**
     * 校验 URL 是否在白名单内
     * @param url 完整的 URL 字符串
     * @return true: 允许访问, false: 禁止访问
     */
    fun isUrlAllowed(url: String?): Boolean {
        if (url.isNullOrBlank()) {
            return false
        }

        return try {
            val host = extractHost(url) ?: return false
            isHostAllowed(host)
        } catch (e: Exception) {
            // 任何解析异常都认为不安全
            false
        }
    }

    /**
     * 校验 Host 是否在白名单内
     * @param host 域名（不含协议和端口）
     * @return true: 允许访问, false: 禁止访问
     */
    fun isHostAllowed(host: String?): Boolean {
        if (host.isNullOrBlank()) {
            return false
        }

        val normalizedHost = host.lowercase().trim()

        // 禁止 IP 地址（包括 IPv4 和 IPv6）
        if (isIpAddress(normalizedHost)) {
            return false
        }

        // 禁止 localhost 和特殊域名
        if (normalizedHost in listOf("localhost", "127.0.0.1", "0.0.0.0", "::1")) {
            return false
        }

        // 检查是否匹配白名单域名或其子域名
        return ALLOWED_DOMAINS.any { allowedDomain ->
            normalizedHost == allowedDomain || normalizedHost.endsWith(".$allowedDomain")
        }
    }

    /**
     * 从 URL 中提取 Host
     * 支持标准 URL 格式和非标准格式
     */
    private fun extractHost(url: String): String? {
        return try {
            // 移除协议前缀
            val withoutProtocol = url
                .replace(Regex("^https?://", RegexOption.IGNORE_CASE), "")
                .replace(Regex("^ftp://", RegexOption.IGNORE_CASE), "")
                .replace(Regex("^file://", RegexOption.IGNORE_CASE), "")

            // 提取 host 部分（移除路径、端口、查询参数）
            val host = withoutProtocol
                .split("/")[0]  // 移除路径
                .split("?")[0]  // 移除查询参数
                .split("#")[0]  // 移除锚点
                .split(":")[0]  // 移除端口

            if (host.isBlank()) null else host
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 判断是否为 IP 地址
     */
    private fun isIpAddress(host: String): Boolean {
        // IPv4 正则
        val ipv4Pattern = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")
        // IPv6 简化判断（包含冒号）
        val ipv6Pattern = Regex("""^[0-9a-fA-F:]+$""")
        
        return ipv4Pattern.matches(host) || 
               (host.contains(":") && ipv6Pattern.matches(host))
    }

    /**
     * 获取友好的错误提示信息
     */
    fun getBlockedMessage(url: String): String {
        val host = extractHost(url) ?: "未知域名"
        return "安全策略：禁止访问非白名单域名 ($host)"
    }

    /**
     * 获取白名单域名列表（仅用于调试）
     */
    fun getAllowedDomains(): Set<String> = ALLOWED_DOMAINS.toSet()
}

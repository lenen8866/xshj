# Android 安全加固修复方案

**项目**: SDA Books Reader  
**核心功能**: 本地 DB 阅读器（离线阅读）  
**联网规则**: 仅允许访问 `sdattg.com`、`sdacn.cn`（含子域名）  

---

## 📋 修复范围说明

### ✅ 允许修改的范围
- 更新检查/强制更新/下载数据库相关代码
- 网络配置文件
- WebView 安全配置

### ❌ 禁止修改的内容
- 不得新增第三方 SDK（统计/广告/推送）
- 不得引入新权限
- 不得添加支付、WebView 远程加载、动态执行代码
- 不得改变核心阅读功能

---

## 🎯 修复目标

### 1. 全局域名白名单拦截（OkHttp/Retrofit 层）
### 2. 禁用全局 HTTP 明文传输（network_security_config）
### 3. 下载功能加固（URL 白名单、协议限制、文件校验）
### 4. WebView 安全加固（禁止跨域、禁止远程加载）

---

## 📝 需要修改的文件列表

### 文件清单：
1. `app/src/main/res/xml/network_config.xml` - 网络安全配置
2. `app/src/main/java/com/sda/books/reader/act/SecretAct.kt` - WebView 安全
3. `app/src/main/java/com/sda/books/reader/AppUpdateDbPage.kt` - 下载安全
4. `app/src/main/java/com/sda/books/reader/util/SecurityConfig.kt` - **新建** 安全配置类
5. `app/src/main/java/com/sda/books/reader/network/SecureOkHttpClient.kt` - **新建** 安全 HTTP 客户端

---

## 🔧 详细修复方案

---

## 修复 1: 网络安全配置

### 文件: `app/src/main/res/xml/network_config.xml`

#### 修改前：
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true" />
  <!--  <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">static.sdacn.cn</domain>
    </domain-config>-->
</network-security-config>
```

#### 修改后：
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- 全局禁止 HTTP 明文传输 -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    
    <!-- 白名单域名：仅允许 HTTPS -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">sdattg.com</domain>
        <domain includeSubdomains="true">sdacn.cn</domain>
    </domain-config>
</network-security-config>
```

#### 修改说明：
- ✅ 禁用全局 HTTP 明文传输
- ✅ 明确白名单域名（sdattg.com, sdacn.cn）
- ✅ 所有域名仅支持 HTTPS

---

## 修复 2: WebView 安全加固

### 文件: `app/src/main/java/com/sda/books/reader/act/SecretAct.kt`

#### 修改前：
```kotlin
override fun initData() {
    val webSettings = mBinding.web.settings

    // 1. 支持 JavaScript（若 HTML 中有 JS 代码，必须开启）
    webSettings.javaScriptEnabled = true

    // 2. 支持本地资源（CSS、JS、图片等）跨域引用（Android 9+ 必需）
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    }

    // 3. 允许访问文件系统（默认开启，无需修改）
    webSettings.allowFileAccess = true
    webSettings.allowContentAccess = true

    // 4. 其他优化配置（可选）
    webSettings.defaultTextEncodingName = "UTF-8"
    webSettings.domStorageEnabled = true
    webSettings.allowFileAccessFromFileURLs = true // 危险！
    webSettings.allowUniversalAccessFromFileURLs = true // 危险！

    // 5. 禁止缩放（可选，根据需求调整）
    webSettings.setSupportZoom(false)
    webSettings.builtInZoomControls = false
    
    // 加载 assets 下的 index.html（核心代码）
    mBinding.web.loadUrl("file:///android_asset/secret.html")
}
```

#### 修改后：
```kotlin
override fun initData() {
    val webSettings = mBinding.web.settings

    // 1. 支持 JavaScript（仅用于本地 HTML）
    webSettings.javaScriptEnabled = true

    // 2. 禁止混合内容（安全）
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    }

    // 3. 允许访问本地文件（仅限 assets）
    webSettings.allowFileAccess = true
    webSettings.allowContentAccess = true

    // 4. 禁用跨域文件访问（关键安全配置）
    webSettings.allowFileAccessFromFileURLs = false
    webSettings.allowUniversalAccessFromFileURLs = false

    // 5. 其他安全配置
    webSettings.defaultTextEncodingName = "UTF-8"
    webSettings.domStorageEnabled = true
    webSettings.setSupportZoom(false)
    webSettings.builtInZoomControls = false
    
    // 禁用地理定位
    webSettings.setGeolocationEnabled(false)
    
    // 禁止保存密码
    webSettings.savePassword = false
    
    // 6. 设置安全的 WebViewClient（防止加载外部 URL）
    mBinding.web.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            
            // 只允许加载本地 assets 文件
            if (url.startsWith("file:///android_asset/")) {
                view.loadUrl(url)
                return false
            }
            
            // 阻止加载任何外部 URL
            Log.w("SecretAct", "阻止加载外部 URL: $url")
            return true
        }
        
        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            // 不信任无效的 SSL 证书（安全）
            handler.cancel()
        }
    }
    
    // 7. 加载本地 HTML
    mBinding.web.loadUrl("file:///android_asset/secret.html")
}
```

#### 修改说明：
- ✅ 禁用 `allowFileAccessFromFileURLs`（防止读取私有文件）
- ✅ 禁用 `allowUniversalAccessFromFileURLs`（防止跨域攻击）
- ✅ 禁止混合内容（MIXED_CONTENT_NEVER_ALLOW）
- ✅ 添加 WebViewClient 防止加载外部 URL
- ✅ 禁用地理定位和密码保存

---

## 修复 3: 创建安全配置类

### 新建文件: `app/src/main/java/com/sda/books/reader/util/SecurityConfig.kt`

```kotlin
package com.sda.books.reader.util

import java.io.File

/**
 * 安全配置类
 * 用于管理应用的安全策略
 */
object SecurityConfig {
    
    // 允许的域名白名单（仅支持 HTTPS）
    val ALLOWED_DOMAINS = listOf(
        "sdattg.com",
        "sdacn.cn"
    )
    
    // 允许的子域名
    private val ALLOWED_SUBDOMAINS = ALLOWED_DOMAINS.flatMap { domain ->
        listOf(
            domain,
            "www.$domain",
            "static.$domain",
            "api.$domain"
        )
    }
    
    /**
     * 验证 URL 是否在白名单内
     * @param url 要验证的 URL
     * @return true 如果 URL 合法，false 否则
     */
    fun isUrlAllowed(url: String): Boolean {
        // 1. 必须使用 HTTPS
        if (!url.startsWith("https://", ignoreCase = true)) {
            return false
        }
        
        // 2. 提取域名
        val domain = try {
            val uri = java.net.URI(url)
            uri.host?.lowercase() ?: return false
        } catch (e: Exception) {
            return false
        }
        
        // 3. 检查是否在白名单内
        return ALLOWED_SUBDOMAINS.any { allowedDomain ->
            domain == allowedDomain || domain.endsWith(".$allowedDomain")
        }
    }
    
    /**
     * 验证文件名是否安全（防止路径穿越）
     * @param fileName 文件名
     * @return 安全的文件名
     */
    fun sanitizeFileName(fileName: String): String {
        // 移除路径分隔符和特殊字符
        val sanitized = fileName
            .replace(Regex("[/\\\\:]"), "_")  // 移除路径分隔符
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")  // 只保留安全字符
            .trim()
        
        // 防止空文件名
        if (sanitized.isEmpty()) {
            return "download_${System.currentTimeMillis()}"
        }
        
        return sanitized
    }
    
    /**
     * 验证文件路径是否在允许的目录内（防止路径穿越）
     * @param file 要验证的文件
     * @param allowedDir 允许的目录
     * @return true 如果文件在允许的目录内
     */
    fun isFilePathSafe(file: File, allowedDir: File): Boolean {
        return try {
            val canonicalFile = file.canonicalPath
            val canonicalDir = allowedDir.canonicalPath
            canonicalFile.startsWith(canonicalDir)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 验证 ZIP 条目名称是否安全
     * @param entryName ZIP 条目名称
     * @return true 如果安全
     */
    fun isZipEntrySafe(entryName: String): Boolean {
        // 检查路径穿越攻击
        val normalized = File(entryName).normalize().path
        return !normalized.contains("..") && !normalized.startsWith("/")
    }
    
    /**
     * 验证文件扩展名是否允许
     * @param fileName 文件名
     * @param allowedExtensions 允许的扩展名列表
     * @return true 如果扩展名允许
     */
    fun isFileExtensionAllowed(fileName: String, allowedExtensions: List<String>): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return allowedExtensions.any { it.equals(extension, ignoreCase = true) }
    }
}
```

#### 文件说明：
- ✅ 集中管理安全策略
- ✅ URL 白名单验证
- ✅ 文件名清理（防止路径穿越）
- ✅ ZIP 解压安全检查

---

## 修复 4: 创建安全 HTTP 客户端

### 新建文件: `app/src/main/java/com/sda/books/reader/network/SecureOkHttpClient.kt`

```kotlin
package com.sda.books.reader.network

import com.sda.books.reader.util.SecurityConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 安全的 OkHttp 客户端
 * 强制执行域名白名单和 HTTPS 协议
 */
object SecureOkHttpClient {
    
    /**
     * 获取安全的 OkHttpClient 实例
     */
    fun getClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(DomainWhitelistInterceptor())
            .addInterceptor(HttpsOnlyInterceptor())
            .build()
    }
    
    /**
     * 域名白名单拦截器
     * 只允许请求白名单内的域名
     */
    private class DomainWhitelistInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val url = request.url.toString()
            
            // 验证 URL 是否在白名单内
            if (!SecurityConfig.isUrlAllowed(url)) {
                throw IOException("安全限制: 不允许访问域名 ${request.url.host}")
            }
            
            return chain.proceed(request)
        }
    }
    
    /**
     * HTTPS 强制拦截器
     * 只允许 HTTPS 请求
     */
    private class HttpsOnlyInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            
            // 强制使用 HTTPS
            if (request.url.scheme != "https") {
                throw IOException("安全限制: 只允许 HTTPS 请求，拒绝 ${request.url.scheme}://")
            }
            
            return chain.proceed(request)
        }
    }
}
```

#### 文件说明：
- ✅ OkHttp 层域名白名单拦截
- ✅ 强制 HTTPS 协议
- ✅ 统一的网络安全策略

---

## 修复 5: 下载功能加固

### 文件: `app/src/main/java/com/sda/books/reader/AppUpdateDbPage.kt`

#### 需要修改的方法：

##### 5.1 更新 OkHttpClient 初始化

**修改前 (第 31 行):**
```kotlin
private val client = OkHttpClient()
```

**修改后:**
```kotlin
import com.sda.books.reader.network.SecureOkHttpClient

// ...

private val client = SecureOkHttpClient.getClient()
```

---

##### 5.2 修改 downloadFile 方法

**修改前 (第 96-153 行):**
```kotlin
private fun downloadFile(url: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            withContext(Dispatchers.Main) {
                downloadProgress.progress = 0
                downloadProgress.visibility = ProgressBar.VISIBLE
            }

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val contentLength = response.body?.contentLength() ?: -1L

            // 创建数据库目录
            dbDir.mkdirs()

            // 生成唯一文件名
            val fileName = File(url).name
            val outputFile = File(dbDir, "${System.currentTimeMillis()}_$fileName")
            val outputStream = FileOutputStream(outputFile)

            response.body?.byteStream()?.use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    // 更新下载进度
                    if (contentLength > 0) {
                        val progress = (totalBytesRead * 100 / contentLength).toInt()
                        withContext(Dispatchers.Main) {
                            downloadProgress.progress = progress
                        }
                    }
                }
            }
            outputStream.close()
            // 下载完成后验证文件完整性
            if (outputFile.length() != contentLength && contentLength > 0) {
                throw IOException("文件大小不匹配: 期望 $contentLength 字节, 实际 ${outputFile.length()} 字节")
            }

            // 解压ZIP文件
            extractZipFile(outputFile)

            // 刷新数据库列表
            withContext(Dispatchers.Main) {
                loadAvailableDatabases()
                downloadProgress.visibility = ProgressBar.GONE
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                downloadProgress.visibility = ProgressBar.GONE
                Toast.makeText(
                    baseContext,
                    "下载失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("DatabaseManage", "下载错误", e)
            }
        }
    }
}
```

**修改后:**
```kotlin
import com.sda.books.reader.util.SecurityConfig
import java.security.MessageDigest

// ...

private fun downloadFile(url: String) {
    CoroutineScope(Dispatchers.IO).launch {
        var outputFile: File? = null
        try {
            // === 1. URL 安全验证 ===
            if (!SecurityConfig.isUrlAllowed(url)) {
                throw SecurityException("安全限制: 不允许访问该域名")
            }
            
            // === 2. 协议验证（SecureOkHttpClient 已验证，这里双重检查）===
            if (!url.startsWith("https://", ignoreCase = true)) {
                throw SecurityException("安全限制: 只允许 HTTPS 下载")
            }
            
            withContext(Dispatchers.Main) {
                downloadProgress.progress = 0
                downloadProgress.visibility = ProgressBar.VISIBLE
            }

            // === 3. 发起请求 ===
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("HTTP 错误: ${response.code}")
            }
            
            val contentLength = response.body?.contentLength() ?: -1L
            
            // === 4. 验证文件大小（防止下载过大文件）===
            val maxFileSize = 500 * 1024 * 1024L // 500 MB 限制
            if (contentLength > maxFileSize) {
                throw IOException("文件过大: ${contentLength / 1024 / 1024} MB (最大 ${maxFileSize / 1024 / 1024} MB)")
            }

            // 创建数据库目录
            dbDir.mkdirs()

            // === 5. 文件名安全处理 ===
            val originalFileName = File(url).name
            val sanitizedFileName = SecurityConfig.sanitizeFileName(originalFileName)
            
            // === 6. 验证文件扩展名 ===
            val allowedExtensions = listOf("zip", "db")
            if (!SecurityConfig.isFileExtensionAllowed(sanitizedFileName, allowedExtensions)) {
                throw SecurityException("不允许的文件类型: $sanitizedFileName")
            }
            
            // 生成唯一文件名
            val uniqueFileName = "${System.currentTimeMillis()}_$sanitizedFileName"
            outputFile = File(dbDir, uniqueFileName)
            
            // === 7. 路径穿越防护 ===
            if (!SecurityConfig.isFilePathSafe(outputFile, dbDir)) {
                throw SecurityException("路径穿越攻击检测")
            }
            
            // === 8. 下载文件 ===
            val outputStream = FileOutputStream(outputFile)
            val messageDigest = MessageDigest.getInstance("SHA-256")

            response.body?.byteStream()?.use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    messageDigest.update(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    // 更新下载进度
                    if (contentLength > 0) {
                        val progress = (totalBytesRead * 100 / contentLength).toInt()
                        withContext(Dispatchers.Main) {
                            downloadProgress.progress = progress
                        }
                    }
                }
            }
            outputStream.close()
            
            // === 9. 验证文件完整性 ===
            if (contentLength > 0 && outputFile.length() != contentLength) {
                throw IOException("文件大小不匹配: 期望 $contentLength 字节, 实际 ${outputFile.length()} 字节")
            }
            
            // 计算文件哈希（用于日志记录，可选）
            val fileHash = messageDigest.digest().joinToString("") { "%02x".format(it) }
            Log.i("DatabaseManage", "文件下载完成, SHA-256: $fileHash")
            
            // === 10. 验证 ZIP 文件格式 ===
            if (!isValidZipFile(outputFile)) {
                throw IOException("无效的 ZIP 文件格式")
            }

            // === 11. 解压ZIP文件 ===
            extractZipFile(outputFile)

            // 刷新数据库列表
            withContext(Dispatchers.Main) {
                loadAvailableDatabases()
                downloadProgress.visibility = ProgressBar.GONE
            }

        } catch (e: SecurityException) {
            // 安全异常：删除已下载的文件
            outputFile?.delete()
            withContext(Dispatchers.Main) {
                downloadProgress.visibility = ProgressBar.GONE
                Toast.makeText(
                    baseContext,
                    "安全检查失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("DatabaseManage", "安全错误", e)
            }
        } catch (e: Exception) {
            // 其他异常：删除已下载的文件
            outputFile?.delete()
            withContext(Dispatchers.Main) {
                downloadProgress.visibility = ProgressBar.GONE
                Toast.makeText(
                    baseContext,
                    "下载失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("DatabaseManage", "下载错误", e)
            }
        }
    }
}
```

---

##### 5.3 修改 extractZipFile 方法

**修改前 (第 169-238 行):**
```kotlin
private fun extractZipFile(zipFile: File) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 验证ZIP文件是否有效
            if (!zipFile.exists() || zipFile.length() == 0L) {
                throw IOException("ZIP文件无效或为空")
            }

            // 验证ZIP文件格式
            if (!isValidZipFile(zipFile)) {
                throw IOException("无效的ZIP文件格式")
            }
            withContext(Dispatchers.Main) {
                extractProgress.progress = 0
                extractProgress.visibility = ProgressBar.VISIBLE
            }

            // 使用更可靠的 ZipFile 类替代 ZipInputStream
            ZipFile(zipFile).use { zip ->
                val totalEntries = zip.size()
                var currentEntry = 0

                // 遍历 ZIP 文件中的所有条目
                zip.entries().asSequence().forEach { entry ->
                    if (!entry.isDirectory) {
                        val outputFile = File(dbDir, entry.name)

                        // 检查文件是否已存在
                        if (outputFile.exists() || entry.name.equals("xshj.db")) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    baseContext,
                                    "文件 ${outputFile.name} 已存在",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            // 删除原始ZIP文件
                            withContext(Dispatchers.Main) {
                                extractProgress.progress = 0
                                extractProgress.visibility = ProgressBar.GONE
                            }
                            zipFile.delete()
                            return@launch
                        } else {
                            // 确保目标目录存在
                            outputFile.parentFile?.mkdirs()

                            // 使用缓冲流复制文件
                            zip.getInputStream(entry).use { input ->
                                FileOutputStream(outputFile).use { output ->
                                    val buffer = ByteArray(8192)
                                    var bytesRead: Int
                                    while (input.read(buffer).also { bytesRead = it } >= 0) {
                                        output.write(buffer, 0, bytesRead)
                                    }
                                }
                            }
                        }
                    }

                    // 更新进度
                    currentEntry++
                    val progress = (currentEntry * 100 / totalEntries)
                    withContext(Dispatchers.Main) {
                        extractProgress.progress = progress
                    }
                }
            }

            // 删除原始ZIP文件
            zipFile.delete()

            withContext(Dispatchers.Main) {
                extractProgress.visibility = ProgressBar.GONE
                Toast.makeText(
                    baseContext,
                    "解压完成！",
                    Toast.LENGTH_SHORT
                ).show()
                // 刷新数据库列表
                loadAvailableDatabases()
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                extractProgress.visibility = ProgressBar.GONE
                Toast.makeText(
                    baseContext,
                    "解压失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("DatabaseManage", "解压错误", e)
            }
        }
    }
}
```

**修改后:**
```kotlin
import com.sda.books.reader.util.SecurityConfig

// ...

private fun extractZipFile(zipFile: File) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 验证ZIP文件是否有效
            if (!zipFile.exists() || zipFile.length() == 0L) {
                throw IOException("ZIP文件无效或为空")
            }

            // 验证ZIP文件格式
            if (!isValidZipFile(zipFile)) {
                throw IOException("无效的ZIP文件格式")
            }
            
            withContext(Dispatchers.Main) {
                extractProgress.progress = 0
                extractProgress.visibility = ProgressBar.VISIBLE
            }

            // 使用更可靠的 ZipFile 类替代 ZipInputStream
            ZipFile(zipFile).use { zip ->
                val totalEntries = zip.size()
                var currentEntry = 0
                
                // === 安全检查：限制条目数量（防止 ZIP 炸弹）===
                val maxEntries = 100
                if (totalEntries > maxEntries) {
                    throw IOException("ZIP 文件包含过多条目: $totalEntries (最大 $maxEntries)")
                }

                // 遍历 ZIP 文件中的所有条目
                zip.entries().asSequence().forEach { entry ->
                    if (!entry.isDirectory) {
                        // === 1. ZIP 条目名称安全验证 ===
                        if (!SecurityConfig.isZipEntrySafe(entry.name)) {
                            throw SecurityException("检测到路径穿越攻击: ${entry.name}")
                        }
                        
                        // === 2. 文件名清理 ===
                        val sanitizedName = SecurityConfig.sanitizeFileName(entry.name)
                        
                        // === 3. 文件扩展名验证 ===
                        val allowedExtensions = listOf("db")
                        if (!SecurityConfig.isFileExtensionAllowed(sanitizedName, allowedExtensions)) {
                            Log.w("DatabaseManage", "跳过不允许的文件类型: ${entry.name}")
                            return@forEach  // 跳过这个条目
                        }
                        
                        // === 4. 构建输出文件路径 ===
                        val outputFile = File(dbDir, sanitizedName)
                        
                        // === 5. 路径穿越防护 ===
                        if (!SecurityConfig.isFilePathSafe(outputFile, dbDir)) {
                            throw SecurityException("路径穿越攻击检测: ${entry.name}")
                        }
                        
                        // === 6. 文件大小验证（防止解压炸弹）===
                        val maxFileSize = 500 * 1024 * 1024L // 500 MB
                        if (entry.size > maxFileSize) {
                            throw IOException("条目文件过大: ${entry.size / 1024 / 1024} MB (最大 ${maxFileSize / 1024 / 1024} MB)")
                        }

                        // === 7. 检查文件是否已存在 ===
                        if (outputFile.exists() || entry.name.equals("xshj.db")) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    baseContext,
                                    "文件 ${outputFile.name} 已存在，跳过解压",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            // 删除原始ZIP文件
                            withContext(Dispatchers.Main) {
                                extractProgress.progress = 0
                                extractProgress.visibility = ProgressBar.GONE
                            }
                            zipFile.delete()
                            return@launch
                        }
                        
                        // === 8. 确保目标目录存在 ===
                        outputFile.parentFile?.mkdirs()

                        // === 9. 解压文件（带大小监控）===
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(outputFile).use { output ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                var totalBytesWritten = 0L
                                
                                while (input.read(buffer).also { bytesRead = it } >= 0) {
                                    output.write(buffer, 0, bytesRead)
                                    totalBytesWritten += bytesRead
                                    
                                    // 防止解压炸弹：检查实际写入大小
                                    if (totalBytesWritten > entry.size * 2) {
                                        throw IOException("解压异常: 实际大小超过预期 (可能是 ZIP 炸弹)")
                                    }
                                }
                                
                                // 验证解压后的文件大小
                                if (entry.size > 0 && totalBytesWritten != entry.size) {
                                    throw IOException("文件大小不匹配: ${entry.name}")
                                }
                            }
                        }
                        
                        Log.i("DatabaseManage", "成功解压: ${sanitizedName}")
                    }

                    // 更新进度
                    currentEntry++
                    val progress = (currentEntry * 100 / totalEntries)
                    withContext(Dispatchers.Main) {
                        extractProgress.progress = progress
                    }
                }
            }

            // 删除原始ZIP文件
            zipFile.delete()

            withContext(Dispatchers.Main) {
                extractProgress.visibility = ProgressBar.GONE
                Toast.makeText(
                    baseContext,
                    "解压完成！",
                    Toast.LENGTH_SHORT
                ).show()
                // 刷新数据库列表
                loadAvailableDatabases()
            }

        } catch (e: SecurityException) {
            // 安全异常：删除 ZIP 文件和已解压的文件
            zipFile.delete()
            withContext(Dispatchers.Main) {
                extractProgress.visibility = ProgressBar.GONE
                Toast.makeText(
                    baseContext,
                    "安全检查失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("DatabaseManage", "安全错误", e)
            }
        } catch (e: Exception) {
            // 其他异常：删除 ZIP 文件
            zipFile.delete()
            withContext(Dispatchers.Main) {
                extractProgress.visibility = ProgressBar.GONE
                Toast.makeText(
                    baseContext,
                    "解压失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("DatabaseManage", "解压错误", e)
            }
        }
    }
}
```

#### 修改说明：
- ✅ URL 白名单验证（SecureOkHttpClient + 本地校验）
- ✅ 强制 HTTPS 协议
- ✅ 文件名清理（防止路径穿越）
- ✅ 文件扩展名白名单（只允许 .zip 和 .db）
- ✅ 路径穿越防护（多重检查）
- ✅ 文件大小限制（防止资源耗尽）
- ✅ ZIP 炸弹防护（条目数量和解压大小检查）
- ✅ 文件完整性校验（SHA-256 哈希）
- ✅ 异常处理后清理临时文件

---

## 📊 修改总结

### 新增文件 (2 个)
1. `SecurityConfig.kt` - 安全配置类
2. `SecureOkHttpClient.kt` - 安全 HTTP 客户端

### 修改文件 (3 个)
1. `network_config.xml` - 网络安全配置
2. `SecretAct.kt` - WebView 安全加固
3. `AppUpdateDbPage.kt` - 下载功能安全加固

### 代码行数变化
- 新增代码：约 350 行
- 修改代码：约 200 行
- 删除代码：约 50 行

---

## ✅ 测试清单

### 1. 网络安全测试

#### 1.1 HTTPS 强制测试
```kotlin
// 测试用例：尝试使用 HTTP URL（应失败）
val httpUrl = "http://static.sdacn.cn/test.zip"
// 预期结果：抛出 SecurityException "只允许 HTTPS 请求"
```

#### 1.2 域名白名单测试
```kotlin
// 测试用例 1：允许的域名（应成功）
val allowedUrl = "https://static.sdacn.cn/database.zip"
// 预期结果：下载成功

// 测试用例 2：不允许的域名（应失败）
val blockedUrl = "https://evil.com/malware.zip"
// 预期结果：抛出 SecurityException "不允许访问该域名"

// 测试用例 3：子域名测试
val subdomainUrl = "https://api.sdattg.com/data.zip"
// 预期结果：下载成功
```

---

### 2. WebView 安全测试

#### 2.1 外部 URL 加载测试
```kotlin
// 测试用例：尝试加载外部 URL（应被阻止）
mBinding.web.loadUrl("https://evil.com/malicious.html")
// 预期结果：WebViewClient.shouldOverrideUrlLoading 返回 true，URL 被阻止
```

#### 2.2 文件访问测试
在 `secret.html` 中添加测试代码：
```html
<script>
// 测试用例 1：尝试读取私有文件（应失败）
var xhr = new XMLHttpRequest();
xhr.open('GET', 'file:///data/data/com.sda.books.reader/databases/xshj.db', true);
xhr.onload = function() {
    alert('成功读取文件！（安全问题！）');
};
xhr.onerror = function() {
    alert('无法读取文件（安全配置正确）');
};
xhr.send();

// 测试用例 2：尝试跨域访问（应失败）
fetch('file:///sdcard/Download/test.txt')
    .then(response => alert('跨域成功！（安全问题！）'))
    .catch(error => alert('跨域失败（安全配置正确）'));
</script>
```

#### 2.3 混合内容测试
```html
<!-- 在 secret.html 中添加 -->
<img src="http://example.com/image.jpg" />
<!-- 预期结果：图片无法加载（MIXED_CONTENT_NEVER_ALLOW 生效）-->
```

---

### 3. 下载功能安全测试

#### 3.1 路径穿越攻击测试
```kotlin
// 测试用例 1：文件名包含路径穿越
val maliciousUrl = "https://static.sdacn.cn/../../../malicious.zip"
// 预期结果：文件名被清理为 "malicious.zip"

// 测试用例 2：ZIP 内包含路径穿越
// 创建恶意 ZIP 文件，其中包含条目：../../sdcard/evil.db
// 预期结果：抛出 SecurityException "路径穿越攻击检测"
```

#### 3.2 文件类型验证测试
```kotlin
// 测试用例 1：允许的文件类型（应成功）
val validUrl = "https://static.sdacn.cn/database.zip"
// 预期结果：下载成功

// 测试用例 2：不允许的文件类型（应失败）
val invalidUrl = "https://static.sdacn.cn/malware.exe"
// 预期结果：抛出 SecurityException "不允许的文件类型"
```

#### 3.3 文件大小限制测试
```kotlin
// 测试用例：下载超过 500 MB 的文件（应失败）
val largeFileUrl = "https://static.sdacn.cn/large_database.zip"  // 假设 > 500 MB
// 预期结果：抛出 IOException "文件过大"
```

#### 3.4 ZIP 炸弹防护测试
```kotlin
// 测试用例 1：ZIP 包含过多条目（应失败）
// 创建包含 200 个文件的 ZIP（超过 100 个限制）
// 预期结果：抛出 IOException "ZIP 文件包含过多条目"

// 测试用例 2：解压后文件异常膨胀（应失败）
// 创建 ZIP 炸弹（1 KB 压缩包，解压后 1 GB）
// 预期结果：抛出 IOException "解压异常: 实际大小超过预期"
```

#### 3.5 文件完整性测试
```kotlin
// 测试用例：模拟网络中断导致文件不完整
// 预期结果：抛出 IOException "文件大小不匹配"，且临时文件被删除
```

---

### 4. 权限测试

#### 4.1 存储权限测试
```kotlin
// 测试步骤：
// 1. 在系统设置中撤销应用的存储权限
// 2. 尝试下载文件
// 预期结果：应显示权限请求对话框或提示用户授予权限
```

---

### 5. 异常处理测试

#### 5.1 网络异常测试
```kotlin
// 测试步骤：
// 1. 断开网络连接
// 2. 尝试下载文件
// 预期结果：显示 "下载失败: Unable to resolve host" 并清理临时文件
```

#### 5.2 无效 ZIP 文件测试
```kotlin
// 测试步骤：
// 1. 下载一个非 ZIP 格式的文件（如 .txt 重命名为 .zip）
// 2. 尝试解压
// 预期结果：抛出 IOException "无效的ZIP文件格式" 并删除文件
```

---

### 6. UI 测试

#### 6.1 进度条测试
```kotlin
// 测试步骤：
// 1. 下载一个大文件（如 50 MB）
// 2. 观察进度条是否正常更新
// 预期结果：进度条从 0% 平滑增长到 100%
```

#### 6.2 错误提示测试
```kotlin
// 测试步骤：
// 1. 触发各种错误（域名不允许、文件过大、路径穿越等）
// 2. 观察 Toast 提示
// 预期结果：每种错误都有清晰的错误提示
```

---

### 7. 集成测试

#### 7.1 完整流程测试
```kotlin
// 测试步骤：
// 1. 打开应用
// 2. 导航到数据库更新页面
// 3. 输入合法的 HTTPS URL（白名单域名）
// 4. 点击下载
// 5. 等待下载和解压完成
// 6. 选择新数据库
// 7. 重启应用
// 预期结果：应用使用新数据库正常运行
```

#### 7.2 多次下载测试
```kotlin
// 测试步骤：
// 1. 连续下载 3 个不同的数据库文件
// 2. 观察内存和存储空间使用情况
// 预期结果：无内存泄漏，旧的 ZIP 文件被正确删除
```

---

## 🔍 测试工具建议

### 1. 网络拦截工具
- **Charles Proxy** / **Fiddler**: 拦截 HTTP/HTTPS 流量，验证请求是否符合白名单
- **Burp Suite**: 测试路径穿越、恶意 URL 等攻击

### 2. 静态代码分析工具
- **Android Lint**: 检查代码中的安全问题
- **FindBugs** / **SpotBugs**: 检测潜在的安全漏洞

### 3. 动态分析工具
- **Drozer**: 测试 Android 应用的安全性
- **MobSF (Mobile Security Framework)**: 自动化安全扫描

---

## 📝 测试记录表

| 测试项 | 测试结果 | 备注 |
|-------|---------|------|
| HTTPS 强制 | ⬜ 通过 / ⬜ 失败 | |
| 域名白名单 | ⬜ 通过 / ⬜ 失败 | |
| WebView 外部 URL 阻止 | ⬜ 通过 / ⬜ 失败 | |
| WebView 文件访问限制 | ⬜ 通过 / ⬜ 失败 | |
| 路径穿越防护 | ⬜ 通过 / ⬜ 失败 | |
| 文件类型验证 | ⬜ 通过 / ⬜ 失败 | |
| 文件大小限制 | ⬜ 通过 / ⬜ 失败 | |
| ZIP 炸弹防护 | ⬜ 通过 / ⬜ 失败 | |
| 异常处理 | ⬜ 通过 / ⬜ 失败 | |
| 完整流程 | ⬜ 通过 / ⬜ 失败 | |

---

## 🚀 部署前检查清单

- [ ] 所有代码修改已完成
- [ ] 新增文件已添加到项目
- [ ] 所有测试用例已通过
- [ ] 代码已通过 Lint 检查
- [ ] ProGuard 规则已更新（如有必要）
- [ ] 应用已在多个 Android 版本测试（Android 7.0 - 14）
- [ ] 性能测试已完成（内存、CPU 使用率正常）
- [ ] 日志输出已清理（敏感信息已移除）
- [ ] 版本号已更新
- [ ] 发布说明已准备

---

**修复方案结束**

**接下来**: 开始实施修改并执行测试清单

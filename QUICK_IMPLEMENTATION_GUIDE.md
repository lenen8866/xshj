# 安全加固快速实施指南

本指南提供快速实施步骤，帮助您在 30 分钟内完成所有安全加固。

---

## 📋 实施前检查清单

- [ ] 备份当前代码（Git commit 或创建副本）
- [ ] 确保 Android Studio 已打开项目
- [ ] 确保项目可以正常编译运行
- [ ] 记录当前测试的下载 URL（用于测试）

---

## 🚀 实施步骤

### 步骤 1: 新增安全配置类（5 分钟）

#### 1.1 创建 SecurityConfig.kt

**位置**: `app/src/main/java/com/sda/books/reader/util/SecurityConfig.kt`

**操作**:
1. 在 Android Studio 中，右键点击 `util` 包
2. 选择 New → Kotlin File/Class
3. 命名为 `SecurityConfig`
4. 复制以下代码：

```kotlin
package com.sda.books.reader.util

import java.io.File

object SecurityConfig {
    val ALLOWED_DOMAINS = listOf("sdattg.com", "sdacn.cn")
    
    private val ALLOWED_SUBDOMAINS = ALLOWED_DOMAINS.flatMap { domain ->
        listOf(domain, "www.$domain", "static.$domain", "api.$domain")
    }
    
    fun isUrlAllowed(url: String): Boolean {
        if (!url.startsWith("https://", ignoreCase = true)) return false
        val domain = try {
            java.net.URI(url).host?.lowercase() ?: return false
        } catch (e: Exception) {
            return false
        }
        return ALLOWED_SUBDOMAINS.any { domain == it || domain.endsWith(".$it") }
    }
    
    fun sanitizeFileName(fileName: String): String {
        val sanitized = fileName
            .replace(Regex("[/\\\\:]"), "_")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .trim()
        return if (sanitized.isEmpty()) "download_${System.currentTimeMillis()}" else sanitized
    }
    
    fun isFilePathSafe(file: File, allowedDir: File): Boolean {
        return try {
            file.canonicalPath.startsWith(allowedDir.canonicalPath)
        } catch (e: Exception) {
            false
        }
    }
    
    fun isZipEntrySafe(entryName: String): Boolean {
        val normalized = File(entryName).normalize().path
        return !normalized.contains("..") && !normalized.startsWith("/")
    }
    
    fun isFileExtensionAllowed(fileName: String, allowedExtensions: List<String>): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return allowedExtensions.any { it.equals(extension, ignoreCase = true) }
    }
}
```

---

### 步骤 2: 创建安全 HTTP 客户端（5 分钟）

#### 2.1 创建 network 包

**操作**:
1. 右键点击 `com.sda.books.reader` 包
2. 选择 New → Package
3. 命名为 `network`

#### 2.2 创建 SecureOkHttpClient.kt

**位置**: `app/src/main/java/com/sda/books/reader/network/SecureOkHttpClient.kt`

**操作**:
1. 右键点击 `network` 包
2. 选择 New → Kotlin File/Class
3. 命名为 `SecureOkHttpClient`
4. 复制以下代码：

```kotlin
package com.sda.books.reader.network

import com.sda.books.reader.util.SecurityConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

object SecureOkHttpClient {
    fun getClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(DomainWhitelistInterceptor())
            .addInterceptor(HttpsOnlyInterceptor())
            .build()
    }
    
    private class DomainWhitelistInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val url = request.url.toString()
            if (!SecurityConfig.isUrlAllowed(url)) {
                throw IOException("安全限制: 不允许访问域名 ${request.url.host}")
            }
            return chain.proceed(request)
        }
    }
    
    private class HttpsOnlyInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            if (request.url.scheme != "https") {
                throw IOException("安全限制: 只允许 HTTPS 请求")
            }
            return chain.proceed(request)
        }
    }
}
```

---

### 步骤 3: 修改网络安全配置（2 分钟）

#### 3.1 修改 network_config.xml

**位置**: `app/src/main/res/xml/network_config.xml`

**操作**:
1. 在 Android Studio 中打开该文件
2. **完全替换**为以下内容：

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

---

### 步骤 4: 修改 SecretAct.kt（8 分钟）

**位置**: `app/src/main/java/com/sda/books/reader/act/SecretAct.kt`

#### 4.1 添加 import

在文件顶部添加：

```kotlin
import android.util.Log
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import android.net.http.SslError
```

#### 4.2 修改 initData() 方法

找到以下行并修改：

**修改 1**: 混合内容模式（约第 22 行）

❌ 删除：
```kotlin
webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
```

✅ 替换为：
```kotlin
webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
```

---

**修改 2**: 跨域文件访问（约第 30-31 行）

❌ 删除：
```kotlin
webSettings.allowFileAccessFromFileURLs = true
webSettings.allowUniversalAccessFromFileURLs = true
```

✅ 替换为：
```kotlin
webSettings.allowFileAccessFromFileURLs = false
webSettings.allowUniversalAccessFromFileURLs = false
```

---

**修改 3**: 添加安全配置（在 `builtInZoomControls = false` 之后）

✅ 添加：
```kotlin
// 禁用地理定位
webSettings.setGeolocationEnabled(false)

// 禁止保存密码
webSettings.savePassword = false

// 设置安全的 WebViewClient（防止加载外部 URL）
mBinding.web.webViewClient = object : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        if (url.startsWith("file:///android_asset/")) {
            view.loadUrl(url)
            return false
        }
        Log.w("SecretAct", "阻止加载外部 URL: $url")
        return true
    }
    
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()
    }
}
```

---

### 步骤 5: 修改 AppUpdateDbPage.kt（10 分钟）

**位置**: `app/src/main/java/com/sda/books/reader/AppUpdateDbPage.kt`

#### 5.1 添加 import

在文件顶部添加：

```kotlin
import com.sda.books.reader.network.SecureOkHttpClient
import com.sda.books.reader.util.SecurityConfig
import java.security.MessageDigest
```

#### 5.2 修改 OkHttpClient 初始化（约第 41 行）

❌ 删除：
```kotlin
private val client = OkHttpClient()
```

✅ 替换为：
```kotlin
private val client = SecureOkHttpClient.getClient()
```

#### 5.3 修改 downloadFile() 方法

这是最复杂的修改。找到 `downloadFile()` 方法（约第 86 行开始）。

**关键修改点**:

1. **在方法开头添加变量**:
```kotlin
var outputFile: File? = null
```

2. **在 try 块开头添加安全验证**:
```kotlin
// URL 安全验证
if (!SecurityConfig.isUrlAllowed(url)) {
    throw SecurityException("安全限制: 不允许访问该域名")
}

if (!url.startsWith("https://", ignoreCase = true)) {
    throw SecurityException("安全限制: 只允许 HTTPS 下载")
}
```

3. **在下载前添加文件大小验证**:
```kotlin
val contentLength = response.body?.contentLength() ?: -1L

// 验证文件大小
val maxFileSize = 500 * 1024 * 1024L // 500 MB
if (contentLength > maxFileSize) {
    throw IOException("文件过大: ${contentLength / 1024 / 1024} MB")
}
```

4. **修改文件名处理**:

❌ 删除：
```kotlin
val fileName = File(url).name
val outputFile = File(dbDir, "${System.currentTimeMillis()}_$fileName")
```

✅ 替换为：
```kotlin
val originalFileName = File(url).name
val sanitizedFileName = SecurityConfig.sanitizeFileName(originalFileName)

// 验证文件扩展名
val allowedExtensions = listOf("zip", "db")
if (!SecurityConfig.isFileExtensionAllowed(sanitizedFileName, allowedExtensions)) {
    throw SecurityException("不允许的文件类型: $sanitizedFileName")
}

val uniqueFileName = "${System.currentTimeMillis()}_$sanitizedFileName"
outputFile = File(dbDir, uniqueFileName)

// 路径穿越防护
if (!SecurityConfig.isFilePathSafe(outputFile, dbDir)) {
    throw SecurityException("路径穿越攻击检测")
}
```

5. **添加文件哈希计算**:

在 `FileOutputStream(outputFile)` 之后添加：
```kotlin
val messageDigest = MessageDigest.getInstance("SHA-256")
```

在写入数据的循环中，添加哈希更新：
```kotlin
while (inputStream.read(buffer).also { bytesRead = it } != -1) {
    outputStream.write(buffer, 0, bytesRead)
    messageDigest.update(buffer, 0, bytesRead)  // 新增这行
    totalBytesRead += bytesRead
    // ...
}
```

在文件下载完成后添加：
```kotlin
val fileHash = messageDigest.digest().joinToString("") { "%02x".format(it) }
Log.i("DatabaseManage", "文件下载完成, SHA-256: $fileHash")
```

6. **添加 SecurityException 处理**:

在现有的 `catch (e: Exception)` 之前添加：
```kotlin
catch (e: SecurityException) {
    outputFile?.delete()
    withContext(Dispatchers.Main) {
        downloadProgress.visibility = ProgressBar.GONE
        Toast.makeText(baseContext, "安全检查失败: ${e.message}", Toast.LENGTH_LONG).show()
        Log.e("DatabaseManage", "安全错误", e)
    }
}
```

同时修改现有的异常处理，在开头添加：
```kotlin
catch (e: Exception) {
    outputFile?.delete()  // 新增这行
    // ... 其余代码不变
}
```

#### 5.4 修改 extractZipFile() 方法

找到 `extractZipFile()` 方法（约第 169 行开始）。

**关键修改点**:

1. **在 `ZipFile(zipFile).use { zip ->` 之后立即添加**:
```kotlin
val totalEntries = zip.size()
val maxEntries = 100
if (totalEntries > maxEntries) {
    throw IOException("ZIP 文件包含过多条目: $totalEntries")
}
```

2. **在 `forEach { entry ->` 循环内，`if (!entry.isDirectory)` 之后添加安全检查**:
```kotlin
if (!entry.isDirectory) {
    // ZIP 条目名称安全验证
    if (!SecurityConfig.isZipEntrySafe(entry.name)) {
        throw SecurityException("检测到路径穿越攻击: ${entry.name}")
    }
    
    // 文件名清理
    val sanitizedName = SecurityConfig.sanitizeFileName(entry.name)
    
    // 文件扩展名验证
    val allowedExtensions = listOf("db")
    if (!SecurityConfig.isFileExtensionAllowed(sanitizedName, allowedExtensions)) {
        Log.w("DatabaseManage", "跳过不允许的文件类型: ${entry.name}")
        return@forEach
    }
    
    // 构建输出文件路径
    val outputFile = File(dbDir, sanitizedName)
    
    // 路径穿越防护
    if (!SecurityConfig.isFilePathSafe(outputFile, dbDir)) {
        throw SecurityException("路径穿越攻击检测: ${entry.name}")
    }
    
    // 文件大小验证
    val maxFileSize = 500 * 1024 * 1024L
    if (entry.size > maxFileSize) {
        throw IOException("条目文件过大: ${entry.size / 1024 / 1024} MB")
    }
    
    // ... 继续原有的解压逻辑
}
```

3. **修改解压逻辑，添加大小监控**:

❌ 替换：
```kotlin
zip.getInputStream(entry).use { input ->
    FileOutputStream(outputFile).use { output ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } >= 0) {
            output.write(buffer, 0, bytesRead)
        }
    }
}
```

✅ 替换为：
```kotlin
zip.getInputStream(entry).use { input ->
    FileOutputStream(outputFile).use { output ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        var totalBytesWritten = 0L
        
        while (input.read(buffer).also { bytesRead = it } >= 0) {
            output.write(buffer, 0, bytesRead)
            totalBytesWritten += bytesRead
            
            // 防止解压炸弹
            if (totalBytesWritten > entry.size * 2) {
                throw IOException("解压异常: 实际大小超过预期")
            }
        }
        
        if (entry.size > 0 && totalBytesWritten != entry.size) {
            throw IOException("文件大小不匹配: ${entry.name}")
        }
    }
}
```

4. **添加 SecurityException 处理**:

在现有的 `catch (e: Exception)` 之前添加：
```kotlin
catch (e: SecurityException) {
    zipFile.delete()
    withContext(Dispatchers.Main) {
        extractProgress.visibility = ProgressBar.GONE
        Toast.makeText(baseContext, "安全检查失败: ${e.message}", Toast.LENGTH_LONG).show()
        Log.e("DatabaseManage", "安全错误", e)
    }
}
```

同时修改现有的异常处理，在开头添加：
```kotlin
catch (e: Exception) {
    zipFile.delete()  // 确保这行存在
    // ... 其余代码不变
}
```

---

## ✅ 验证步骤

### 1. 编译项目
1. 点击 Build → Rebuild Project
2. 确保没有编译错误
3. 如有错误，检查 import 语句和包名

### 2. 运行应用
1. 连接测试设备或启动模拟器
2. 点击 Run → Run 'app'
3. 确保应用正常启动

### 3. 测试基本功能
1. 测试 WebView 页面加载
2. 测试数据库更新功能

---

## 🐛 常见问题

### 问题 1: 编译错误 "Unresolved reference"

**原因**: import 语句缺失或包名错误

**解决**:
1. 检查新建文件的包名是否正确
2. 使用 Alt+Enter 自动导入缺失的类
3. 确保 SecurityConfig 和 SecureOkHttpClient 文件已创建

---

### 问题 2: 网络请求失败 "安全限制: 不允许访问域名"

**原因**: 测试 URL 不在白名单内

**解决**:
1. 确保 URL 使用 HTTPS
2. 确保域名是 sdattg.com 或 sdacn.cn
3. 如需添加其他域名，修改 `SecurityConfig.ALLOWED_DOMAINS`

---

### 问题 3: 下载失败 "不允许的文件类型"

**原因**: 文件扩展名不在白名单内

**解决**:
1. 确保下载的文件是 .zip 或 .db
2. 如需支持其他类型，修改 `allowedExtensions` 列表

---

## 📝 实施后检查清单

- [ ] 所有文件已创建/修改
- [ ] 项目可以成功编译
- [ ] 应用可以正常运行
- [ ] WebView 页面正常显示
- [ ] 使用 HTTPS URL 测试下载功能
- [ ] 确认 HTTP URL 被正确拒绝
- [ ] 确认非白名单域名被拒绝

---

## 🎯 下一步

完成实施后，请参考 `SECURITY_FIX_PLAN.md` 中的完整测试清单进行全面测试。

---

**预计总耗时**: 30 分钟  
**难度**: 中等  
**风险**: 低（已有备份）

如遇到问题，请参考 `DIFF_SUMMARY.md` 查看详细的修改对比。

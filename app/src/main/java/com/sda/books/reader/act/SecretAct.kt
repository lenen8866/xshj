package com.sda.books.reader.act

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.aleyn.mvvm.base.BaseVMActivity
import com.book.reader.R
import com.book.reader.databinding.ActSecretInfoBinding
import com.sda.books.reader.model.MainViewModel

class SecretAct: BaseVMActivity<MainViewModel, ActSecretInfoBinding>() {
    override fun initData() {
        val webSettings = mBinding.web.settings

        // ===== 安全配置：最小权限模式 =====
        
        // 1. 禁用 JavaScript（静态 HTML 不需要）
        webSettings.javaScriptEnabled = false

        // 2. 禁止混合内容（仅允许 HTTPS 或本地文件）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        // 3. 允许访问本地文件（仅 assets 目录）
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true

        // 4. 禁止文件跨域访问（防止任意文件读取）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webSettings.allowFileAccessFromFileURLs = false
            webSettings.allowUniversalAccessFromFileURLs = false
        }

        // 5. 禁用 DOM 存储（静态页面不需要）
        webSettings.domStorageEnabled = false
        webSettings.databaseEnabled = false

        // 6. 禁用地理定位
        webSettings.setGeolocationEnabled(false)

        // 7. 禁止保存表单数据和密码
        webSettings.saveFormData = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            webSettings.savePassword = false
        }

        // 8. 基础配置
        webSettings.defaultTextEncodingName = "UTF-8"
        webSettings.setSupportZoom(false)
        webSettings.builtInZoomControls = false
        webSettings.displayZoomControls = false

        // 9. 禁用缓存（防止敏感信息残留）
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE

        // ===== 设置安全的 WebViewClient =====
        mBinding.web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return true
                
                // 只允许加载 assets 目录下的文件
                return if (url.startsWith("file:///android_asset/")) {
                    false // 允许加载
                } else if (url.startsWith("http://") || url.startsWith("https://")) {
                    // HTTPS 链接用系统浏览器打开
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true // 拦截，不在 WebView 中加载
                } else {
                    // 拦截所有其他协议（tel:, mailto:, file:// 等）
                    true
                }
            }

            // ✅ 添加 @Suppress 抑制 Deprecated 警告（API < 24 仍需此方法）
            @Deprecated("Deprecated in Java", ReplaceWith("shouldOverrideUrlLoading(view, request)"))
            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url ?: return true
                
                return if (url.startsWith("file:///android_asset/")) {
                    false
                } else if (url.startsWith("http://") || url.startsWith("https://")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                } else {
                    true
                }
            }
        }

        // 加载本地隐私政策页面
        mBinding.web.loadUrl("file:///android_asset/secret.html")
    }

    override fun initView(savedInstanceState: Bundle?) {
        mBinding.ivBack.setOnClickListener {
            finish()
        }
    }

    override val layoutId: Int
        get() = R.layout.act_secret_info

    override fun onDestroy() {
        super.onDestroy()
        // ✅ 清理 WebView 资源
        mBinding.web.apply {
            stopLoading()
            clearHistory()
            clearCache(true)
            loadUrl("about:blank")
            removeAllViews()
            destroy()
        }
    }
}

package com.sda.books.reader

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.book.reader.R
import com.book.reader.databinding.ActivityAppUpdateDbBinding
import com.sda.books.reader.db.DatabaseHelper
import com.sda.books.reader.security.SafeFileDownloader
import com.sda.books.reader.security.WhitelistConfig
import com.sda.books.reader.util.PreferenceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipFile

class AppUpdateDbPage : BaseActivity() {

    lateinit var binding: ActivityAppUpdateDbBinding
    private lateinit var etUrl: EditText
    private lateinit var btnDownload: TextView
    private lateinit var downloadProgress: ProgressBar
    private lateinit var extractProgress: ProgressBar
    private lateinit var btnSelectDb: TextView
    private lateinit var tvDbPath: TextView
    private lateinit var spinnerDbFiles: Spinner

    // 使用全局配置的安全 OkHttpClient（已在 App.kt 中配置拦截器）
    private val client by lazy { App.secureOkHttpClient }
    private val dbDir by lazy { filesDir.resolve("databases") }
    private val prefs by lazy { PreferenceHelper(this) }

    override fun getRootView(): View {
        return binding.root
    }

    override fun isApplyTheme(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppUpdateDbBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ivBack.setOnClickListener {
            finish()
        }
        etUrl = findViewById(R.id.etUrl)
        btnDownload = findViewById(R.id.btnDownload)
        downloadProgress = findViewById(R.id.downloadProgress)
        extractProgress = findViewById(R.id.extractProgress)
        btnSelectDb = findViewById(R.id.btnSelectDb)
        tvDbPath = findViewById(R.id.tvDbPath)
        spinnerDbFiles = findViewById(R.id.spinnerDbFiles)

        // 设置默认URL（已在白名单内）
        // etUrl.setText("http://static.sdacn.cn/%E7%A0%94%E7%BB%8F%E5%B7%A5%E5%85%B7/%E4%B8%8B%E8%BD%BD/中文书籍.zip")
        // etUrl.setText("http://static.sdacn.cn/%E7%A0%94%E7%BB%8F%E5%B7%A5%E5%85%B7/%E4%B8%8B%E8%BD%BD/xshj.zip")

        // 显示当前选择的数据库
        prefs.currentDbPath?.let { path ->
            tvDbPath.text = "当前数据库: ${File(path).name}"
        }

        btnDownload.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "请输入下载URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ===== 安全校验：白名单验证 =====
            if (!SafeFileDownloader.isDownloadUrlSafe(url)) {
                val errorMsg = WhitelistConfig.getBlockedMessage(url)
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                Log.w("AppUpdateDb", "❌ 下载被拦截: $errorMsg")
                return@setOnClickListener
            }

            Log.i("AppUpdateDb", "✅ URL 白名单校验通过: $url")
            downloadFile(url)
        }

        btnSelectDb.setOnClickListener {
            showDbSelectionDialog()
        }

        // 加载可用数据库文件
        loadAvailableDatabases()
    }

    private fun downloadFile(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    downloadProgress.progress = 0
                    downloadProgress.visibility = ProgressBar.VISIBLE
                }

                Log.i("AppUpdateDb", "开始下载: $url")

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw IOException("下载失败: HTTP ${response.code}")
                }

                val contentLength = response.body?.contentLength() ?: -1L

                // 创建数据库目录
                dbDir.mkdirs()

                // ===== 安全文件名处理 =====
                val safeFileName = SafeFileDownloader.getSafeFileName(url)
                val timestampFileName = SafeFileDownloader.addTimestampPrefix(safeFileName)
                
                // 验证文件路径安全性（防止路径穿越）
                val outputFile = try {
                    SafeFileDownloader.validateFilePath(dbDir, timestampFileName)
                } catch (e: SecurityException) {
                    Log.e("AppUpdateDb", "❌ 路径校验失败", e)
                    throw e
                }

                Log.i("AppUpdateDb", "安全文件名: ${outputFile.name}")

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
                    outputFile.delete()
                    throw IOException("文件大小不匹配: 期望 $contentLength 字节, 实际 ${outputFile.length()} 字节")
                }

                Log.i("AppUpdateDb", "下载完成，文件大小: ${outputFile.length()} 字节")

                // 解压ZIP文件
                extractZipFile(outputFile)

                // 刷新数据库列表
                withContext(Dispatchers.Main) {
                    loadAvailableDatabases()
                    downloadProgress.visibility = ProgressBar.GONE
                }

            } catch (e: SecurityException) {
                // 安全异常（白名单拦截）
                withContext(Dispatchers.Main) {
                    downloadProgress.visibility = ProgressBar.GONE
                    Toast.makeText(
                        baseContext,
                        "安全拦截: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("AppUpdateDb", "❌ 安全拦截", e)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    downloadProgress.visibility = ProgressBar.GONE
                    Toast.makeText(
                        baseContext,
                        "下载失败: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("AppUpdateDb", "❌ 下载错误", e)
                }
            }
        }
    }

    private fun isValidZipFile(file: File): Boolean {
        try {
            ZipFile(file).use { zip ->
                return true // 如果能打开，则认为是有效的ZIP文件
            }
        } catch (e: Exception) {
            return false
        }
    }

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

                Log.i("AppUpdateDb", "开始解压: ${zipFile.name}")

                // 使用更可靠的 ZipFile 类替代 ZipInputStream
                ZipFile(zipFile).use { zip ->
                    val totalEntries = zip.size()
                    var currentEntry = 0

                    // 遍历 ZIP 文件中的所有条目
                    zip.entries().asSequence().forEach { entry ->
                        if (!entry.isDirectory) {
                            // ===== 安全文件名处理 =====
                            val safeEntryName = SafeFileDownloader.getSafeFileName(entry.name)
                            
                            // 验证输出路径安全性
                            val outputFile = try {
                                SafeFileDownloader.validateFilePath(dbDir, safeEntryName)
                            } catch (e: SecurityException) {
                                Log.w("AppUpdateDb", "跳过不安全的条目: ${entry.name}")
                                return@forEach
                            }

                            // 检查文件是否已存在
                            if (outputFile.exists() || safeEntryName.equals("xshj.db", ignoreCase = true)) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        baseContext,
                                        "文件 ${outputFile.name} 已存在，跳过",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                Log.i("AppUpdateDb", "文件已存在，跳过: ${outputFile.name}")
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
                                Log.i("AppUpdateDb", "解压完成: ${outputFile.name}")
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
                Log.i("AppUpdateDb", "ZIP文件已删除: ${zipFile.name}")

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
                    Log.e("AppUpdateDb", "❌ 解压错误", e)
                }
            }
        }
    }

    private fun loadAvailableDatabases() {
        val dbFiles = mutableListOf<File>()

        val internalDbPath = applicationContext.getDatabasePath(DatabaseHelper.DB_NAME)

        // 添加内置数据库
        dbFiles.add(internalDbPath)

        // 添加下载的数据库
        dbDir.listFiles()?.filter { it.extension == "db" }?.forEach {
            dbFiles.add(it)
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            dbFiles.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDbFiles.adapter = adapter
    }

    private fun showDbSelectionDialog() {
        val dbFiles =
            dbDir.listFiles()?.filter { it.extension == "db" }?.toMutableList() ?: mutableListOf()
        // 添加内置数据库
        val internalDbPath = applicationContext.getDatabasePath(DatabaseHelper.DB_NAME)
        dbFiles.add(internalDbPath)

        val dbNames = dbFiles.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("选择数据库")
            .setItems(dbNames) { _, which ->
                val selectedFile = dbFiles[which]
                prefs.currentDbPath = selectedFile.absolutePath
                tvDbPath.text = "当前数据库: ${selectedFile.name}"

                // 通知应用使用新数据库
                DatabaseHelper.setDatabasePath(selectedFile.absolutePath)
                Toast.makeText(this, "已选择: ${selectedFile.name}", Toast.LENGTH_SHORT).show()
                safeRestartApp()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    fun safeRestartApp() {
        val intent = Intent(this, AppCategoryPage::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}

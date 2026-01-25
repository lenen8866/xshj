# ========================================
# 文件创建脚本 (PowerShell)
# ========================================

Write-Host "开始创建缺失文件..." -ForegroundColor Green

# 确保目录存在
$apiDir = "E:\xshjAS\app\src\main\java\com\sda\books\reader\api"
$eventDir = "E:\xshjAS\app\src\main\java\com\sda\books\reader\event"

if (-not (Test-Path $apiDir)) {
    New-Item -ItemType Directory -Path $apiDir -Force | Out-Null
    Write-Host "创建目录: $apiDir" -ForegroundColor Yellow
}

if (-not (Test-Path $eventDir)) {
    New-Item -ItemType Directory -Path $eventDir -Force | Out-Null
    Write-Host "创建目录: $eventDir" -ForegroundColor Yellow
}

# ========================================
# 1. 创建 EventBus.kt
# ========================================
$eventBusContent = @'
package com.sda.books.reader.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus {
    private val _chapterHeightFlow = MutableSharedFlow<Int>(replay = 0)
    val chapterHeightFlow = _chapterHeightFlow.asSharedFlow()

    suspend fun sendChapterHeight(height: Int) {
        _chapterHeightFlow.emit(height)
    }
}
'@

$eventBusPath = Join-Path $eventDir "EventBus.kt"
[System.IO.File]::WriteAllText($eventBusPath, $eventBusContent, [System.Text.Encoding]::UTF8)
Write-Host "✓ 已创建: EventBus.kt" -ForegroundColor Green

# ========================================
# 2. 创建 VersionApi.kt
# ========================================
$versionApiContent = @'
package com.sda.books.reader.api

import com.sda.books.reader.entity.UpdateDio
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface VersionApi {
    @GET("version/version.php")
    suspend fun checkVersion(@QueryMap params: Map<String, String>): Response<UpdateDio>
}
'@

$versionApiPath = Join-Path $apiDir "VersionApi.kt"
[System.IO.File]::WriteAllText($versionApiPath, $versionApiContent, [System.Text.Encoding]::UTF8)
Write-Host "✓ 已创建: VersionApi.kt" -ForegroundColor Green

# ========================================
# 3. 创建 RetrofitClient.kt
# ========================================
$retrofitClientContent = @'
package com.sda.books.reader.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://xshj.version.sdattg.com/BanBen/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
        .build()

    val versionApi: VersionApi = retrofit.create(VersionApi::class.java)
}
'@

$retrofitClientPath = Join-Path $apiDir "RetrofitClient.kt"
[System.IO.File]::WriteAllText($retrofitClientPath, $retrofitClientContent, [System.Text.Encoding]::UTF8)
Write-Host "✓ 已创建: RetrofitClient.kt" -ForegroundColor Green

# ========================================
# 验证文件
# ========================================
Write-Host "`n验证创建的文件:" -ForegroundColor Cyan
Get-ChildItem -Path $apiDir | ForEach-Object { Write-Host "  - $($_.Name)" }
Get-ChildItem -Path $eventDir | ForEach-Object { Write-Host "  - $($_.Name)" }

Write-Host "`n所有文件创建完成！" -ForegroundColor Green
Write-Host "现在请运行: .\gradlew clean assembleDebug" -ForegroundColor Yellow

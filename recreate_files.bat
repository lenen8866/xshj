@echo off
echo 正在重新创建 API 和 Event 文件...

REM 创建 EventBus.kt
(
echo package com.sda.books.reader.event
echo.
echo import kotlinx.coroutines.flow.MutableSharedFlow
echo import kotlinx.coroutines.flow.asSharedFlow
echo.
echo object EventBus {
echo     private val _chapterHeightFlow = MutableSharedFlow^<Int^>(replay = 0^)
echo     val chapterHeightFlow = _chapterHeightFlow.asSharedFlow(^)
echo.
echo     suspend fun sendChapterHeight(height: Int^) {
echo         _chapterHeightFlow.emit(height^)
echo     }
echo }
) > "E:\xshjAS\app\src\main\java\com\sda\books\reader\event\EventBus.kt"

echo EventBus.kt 已创建

REM 创建 VersionApi.kt
(
echo package com.sda.books.reader.api
echo.
echo import com.sda.books.reader.entity.UpdateDio
echo import retrofit2.Response
echo import retrofit2.http.GET
echo import retrofit2.http.QueryMap
echo.
echo interface VersionApi {
echo     @GET("version/version.php"^)
echo     suspend fun checkVersion(@QueryMap params: Map^<String, String^>^): Response^<UpdateDio^>
echo }
) > "E:\xshjAS\app\src\main\java\com\sda\books\reader\api\VersionApi.kt"

echo VersionApi.kt 已创建

REM 创建 RetrofitClient.kt
(
echo package com.sda.books.reader.api
echo.
echo import com.google.gson.GsonBuilder
echo import okhttp3.OkHttpClient
echo import retrofit2.Retrofit
echo import retrofit2.converter.gson.GsonConverterFactory
echo import java.util.concurrent.TimeUnit
echo.
echo object RetrofitClient {
echo     private const val BASE_URL = "http://xshj.version.sdattg.com/BanBen/"
echo.
echo     private val okHttpClient = OkHttpClient.Builder(^)
echo         .connectTimeout(30, TimeUnit.SECONDS^)
echo         .readTimeout(30, TimeUnit.SECONDS^)
echo         .writeTimeout(30, TimeUnit.SECONDS^)
echo         .build(^)
echo.
echo     private val retrofit = Retrofit.Builder(^)
echo         .baseUrl(BASE_URL^)
echo         .client(okHttpClient^)
echo         .addConverterFactory(GsonConverterFactory.create(GsonBuilder(^).setLenient(^).create(^)^)^)
echo         .build(^)
echo.
echo     val versionApi: VersionApi = retrofit.create(VersionApi::class.java^)
echo }
) > "E:\xshjAS\app\src\main\java\com\sda\books\reader\api\RetrofitClient.kt"

echo RetrofitClient.kt 已创建

echo.
echo 所有文件已重新创建！
echo 现在请运行: .\gradlew clean assembleDebug
pause

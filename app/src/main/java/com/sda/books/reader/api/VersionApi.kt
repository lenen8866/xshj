package com.sda.books.reader.api

import com.sda.books.reader.entity.UpdateDio
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface VersionApi {
    @GET("version/version.php")
    suspend fun checkVersion(@QueryMap params: Map<String, String>): Response<UpdateDio>
}

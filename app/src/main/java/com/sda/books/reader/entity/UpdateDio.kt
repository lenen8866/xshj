package com.sda.books.reader.entity

import com.google.gson.annotations.SerializedName

/**
 * 更新信息数据类
 * 使用 Kotlin data class 提供更安全的字段设计和空值处理
 */
data class UpdateDio(
    @SerializedName("platform")
    val platform: String? = null,
    
    @SerializedName("channel")
    val channel: String? = null,
    
    @SerializedName("current_version")
    val currentVersion: String? = null,
    
    @SerializedName("latest_version")
    val latestVersion: String? = null,
    
    @SerializedName("update_available")
    val updateAvailable: Boolean? = null,
    
    @SerializedName("mandatory")
    val mandatory: Boolean? = null,
    
    @SerializedName("assets")
    val assets: Assets? = null,
    
    @SerializedName("release_notes")
    val releaseNotes: List<String>? = null,
    
    @SerializedName("released_at")
    val releasedAt: String? = null,
    
    @SerializedName("server_time")
    val serverTime: String? = null
) {
    /**
     * 检查是否有更新可用（安全访问）
     */
    fun hasUpdate(): Boolean = updateAvailable == true
    
    /**
     * 检查是否为强制更新（安全访问）
     */
    fun isMandatory(): Boolean = mandatory == true
    
    /**
     * 获取下载 URL（安全访问）
     */
    fun getDownloadUrl(): String? = assets?.url
    
    /**
     * 获取文件大小（安全访问）
     */
    fun getFileSize(): String? = assets?.size
    
    /**
     * 获取 SHA256 校验值（安全访问）
     */
    fun getSha256(): String? = assets?.sha256
    
    /**
     * 获取发布说明（安全访问，返回空列表而不是 null）
     */
    fun getReleaseNotesSafe(): List<String> = releaseNotes ?: emptyList()
    
    /**
     * 获取发布说明文本（安全访问）
     */
    fun getReleaseNotesText(): String = getReleaseNotesSafe().joinToString("\n")
    
    /**
     * 资源信息数据类
     */
    data class Assets(
        @SerializedName("url")
        val url: String? = null,
        
        @SerializedName("size")
        val size: String? = null,
        
        @SerializedName("sha256")
        val sha256: String? = null
    ) {
        /**
         * 检查资源信息是否完整
         */
        fun isValid(): Boolean = !url.isNullOrBlank() && !size.isNullOrBlank()
    }
}

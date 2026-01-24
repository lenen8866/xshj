package com.sda.books.reader.entity

data class updateEntity(
    var assets: Assets,
    var channel: String,
    var current_version: String,
    var latest_version: String,
    var mandatory: Boolean,
    var platform: String,
    var release_notes: List<String>,
    var released_at: String,
    var server_time: String,
    var update_available: Boolean
)

data class Assets(
    var sha256: String,
    var size: String,
    var url: String
)
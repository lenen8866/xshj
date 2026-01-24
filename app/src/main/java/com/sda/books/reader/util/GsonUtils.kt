package com.sda.books.reader.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object GsonUtils {
    val gson: Gson by lazy {
        GsonBuilder()
            .setPrettyPrinting() // 格式化输出
            .serializeNulls() // 序列化 null 值
            .disableHtmlEscaping() // 禁用 HTML 转义
            .create()
    }

    // 将对象转换为格式化的 JSON 字符串
    fun toJsonPretty(obj: Any?): String {
        return try {
            gson.toJson(obj)
        } catch (e: Exception) {
            "{}" // 返回空对象以防出错
        }
    }

    // 将对象转换为紧凑的 JSON 字符串
    fun toJsonCompact(obj: Any?): String {
        return try {
            Gson().toJson(obj)
        } catch (e: Exception) {
            "{}"
        }
    }

    // 将 JSON 字符串解析为对象
    inline fun <reified T> fromJson(json: String): T? {
        return try {
            gson.fromJson(json, T::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
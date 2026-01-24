package com.sda.books.reader

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguageUtils {
    // 设置应用语言（通常在启动页或基类 Activity 调用）
    fun setAppLanguage(language: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }

    // 缓存不同语言的 Resources 对象，避免重复创建
    private val resourcesCache = mutableMapOf<String, Resources>()

    /**
     * 获取指定语言的字符串资源
     * @param context 上下文
     * @param resId 字符串资源ID
     * @param language 目标语言代码（如"en", "zh"）
     * @return 指定语言的字符串
     */
    fun getStringByLanguage(context: Context, resId: Int, language: String): String {
        val resources = getResourcesForLanguage(context, language)
        return resources.getString(resId)
    }

    /**
     * 获取指定语言和区域的字符串资源
     * @param context 上下文
     * @param resId 字符串资源ID
     * @param language 语言代码（如"en", "zh"）
     * @param country 区域代码（如"US", "CN"）
     * @return 指定语言的字符串
     */
    fun getStringByLocale(context: Context, resId: Int, language: String, country: String?): String {
        val locale = if (country.isNullOrEmpty()) {
            Locale(language)
        } else {
            Locale(language, country)
        }

        val resources = getResourcesForLocale(context, locale)
        return resources.getString(resId)
    }

    /**
     * 批量获取指定语言的字符串资源
     * @param context 上下文
     * @param resIds 字符串资源ID列表
     * @param language 目标语言代码
     * @return 资源ID到字符串的映射
     */
    fun getStringsByLanguage(context: Context, resIds: List<Int>, language: String): Map<Int, String> {
        val resources = getResourcesForLanguage(context, language)
        return resIds.associateWith { resources.getString(it) }
    }

    // 获取指定语言的Resources对象
    private fun getResourcesForLanguage(context: Context, language: String): Resources {
        return resourcesCache.getOrPut(language) {
            createResourcesForLocale(context, Locale(language))
        }
    }

    // 获取指定Locale的Resources对象
    private fun getResourcesForLocale(context: Context, locale: Locale): Resources {
        val localeKey = "${locale.language}-${locale.country}"
        return resourcesCache.getOrPut(localeKey) {
            createResourcesForLocale(context, locale)
        }
    }

    // 创建指定Locale的Resources对象
    private fun createResourcesForLocale(context: Context, locale: Locale): Resources {
        val config = Configuration(context.resources.configuration)
        // 针对不同API级别处理
        config.setLocale(locale)

        // 创建新的Context和Resources
        val newContext = context.createConfigurationContext(config)
        return newContext.resources
    }

    // 清除缓存
    fun clearCache() {
        resourcesCache.clear()
    }
}

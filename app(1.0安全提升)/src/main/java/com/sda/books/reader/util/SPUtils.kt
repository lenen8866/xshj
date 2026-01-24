package com.sda.books.reader.util

import android.content.Context
import android.content.SharedPreferences
import kotlin.reflect.KProperty

/**
 * SharedPreferences 工具类
 * 使用委托模式实现，支持泛型和默认值
 */
class SPUtils private constructor(context: Context, name: String = "app_prefs") {
    companion object {
        @Volatile
        private var instance: SPUtils? = null

        /**
         * 初始化工具类，建议在 Application 中调用
         */
        fun init(context: Context, name: String = "app_prefs"): SPUtils {
            return instance ?: synchronized(this) {
                instance ?: SPUtils(context.applicationContext, name).also { instance = it }
            }
        }

        /**
         * 获取工具类实例
         */
        fun get(): SPUtils {
            return instance ?: throw IllegalStateException("请先调用 init() 方法初始化")
        }
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    // 保存数据的方法
    fun put(key: String, value: Any?) {
        with(prefs.edit()) {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Long -> putLong(key, value)
                else -> throw IllegalArgumentException("不支持的数据类型: ${value?.javaClass}")
            }
            apply() // 使用 apply() 异步提交，比 commit() 更高效
        }
    }

    // 获取数据的方法（带默认值）
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, defaultValue: T): T {
        return when (defaultValue) {
            is String -> prefs.getString(key, defaultValue) as T
            is Int -> prefs.getInt(key, defaultValue) as T
            is Boolean -> prefs.getBoolean(key, defaultValue) as T
            is Float -> prefs.getFloat(key, defaultValue) as T
            is Long -> prefs.getLong(key, defaultValue) as T
            else -> throw IllegalArgumentException("不支持的数据类型")
        }
    }

    // 移除某个 key
    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    // 清除所有数据
    fun clear() {
        prefs.edit().clear().apply()
    }

    // 判断是否包含某个 key
    fun contains(key: String): Boolean {
        return prefs.contains(key)
    }

    // 获取所有键值对
    fun getAll(): Map<String, *> {
        return prefs.all
    }

    // 委托属性实现，简化使用
    class PreferenceDelegate<T>(
        private val key: String,
        private val defaultValue: T
    ) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return SPUtils.get().get(key, defaultValue)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            SPUtils.get().put(key, value)
        }
    }
}

// 扩展属性，方便使用
inline fun <reified T> preference(key: String, defaultValue: T) =
    SPUtils.PreferenceDelegate(key, defaultValue)
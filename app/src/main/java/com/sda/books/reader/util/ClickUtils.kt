package com.sda.books.reader.util

import android.view.View

/**
 * 点击防抖工具类
 * 用于防止短时间内重复点击
 */
object ClickUtils {
    
    // 默认防抖间隔：500ms
    private const val DEFAULT_DEBOUNCE_INTERVAL = 500L
    
    // 记录上次点击时间的 Map
    private val lastClickTimeMap = mutableMapOf<Int, Long>()
    
    /**
     * 单次调用防抖检查
     * @return true 允许执行，false 被防抖拦截
     */
    fun canClick(key: String, debounceInterval: Long = DEFAULT_DEBOUNCE_INTERVAL): Boolean {
        val currentTime = System.currentTimeMillis()
        val hashCode = key.hashCode()
        val lastClickTime = lastClickTimeMap[hashCode] ?: 0L
        
        return if (currentTime - lastClickTime >= debounceInterval) {
            lastClickTimeMap[hashCode] = currentTime
            true
        } else {
            false
        }
    }
    
    /**
     * 检查 View 是否可以点击
     * @return true 允许执行，false 被防抖拦截
     */
    fun canClickView(viewId: Int, debounceInterval: Long = DEFAULT_DEBOUNCE_INTERVAL): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastClickTime = lastClickTimeMap[viewId] ?: 0L
        
        return if (currentTime - lastClickTime >= debounceInterval) {
            lastClickTimeMap[viewId] = currentTime
            true
        } else {
            false
        }
    }
    
    /**
     * 清空某个 key 的点击记录
     */
    fun clearClickRecord(key: String) {
        lastClickTimeMap.remove(key.hashCode())
    }
    
    /**
     * 清空所有点击记录
     */
    fun clearAll() {
        lastClickTimeMap.clear()
    }
}

/**
 * View 扩展函数：设置防抖点击
 * @param debounceInterval 防抖间隔（毫秒），默认 500ms
 * @param onClick 点击事件回调
 */
fun View.onClickWithDebounce(
    debounceInterval: Long = 500L,
    onClick: (View) -> Unit
) {
    setOnClickListener { view ->
        if (ClickUtils.canClickView(view.id, debounceInterval)) {
            onClick(view)
        }
    }
}

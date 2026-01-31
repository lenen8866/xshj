package com.sda.books.reader.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
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

/**
 * View 扩展函数：设置带缩放动画的点击事件
 * @param scale 缩放比例，默认 0.9（缩小到90%）
 * @param duration 动画持续时间（毫秒），默认 150ms
 * @param onClick 点击事件回调
 */
fun View.onClickWithScaleAnimation(
    scale: Float = 0.9f,
    duration: Long = 150L,
    onClick: (View) -> Unit
) {
    // 确保View可点击
    isClickable = true
    isFocusable = true
    
    setOnClickListener { view ->
        // 清除之前的动画，避免冲突
        view.clearAnimation()
        view.animate().cancel()
        
        // 确保初始缩放值为1
        view.scaleX = 1f
        view.scaleY = 1f
        
        // 创建缩放动画：从1缩小到scale，再恢复到1
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, scale, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, scale, 1f)
        
        scaleX.duration = duration
        scaleY.duration = duration
        
        // 启动动画
        scaleX.start()
        scaleY.start()
        
        // 执行点击回调（在动画开始后执行，确保用户能看到动画效果）
        onClick(view)
    }
}

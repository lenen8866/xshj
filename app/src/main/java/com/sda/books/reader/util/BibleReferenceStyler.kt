package com.sda.books.reader.util

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LineHeightSpan
import com.sda.books.reader.App

/**
 * 圣经引用样式处理工具
 */
object BibleReferenceStyler {
    // 正则：匹配圣经引用（整行）
    private val bibleReferenceRegex = Regex("""^\s*[A-Za-z\u4e00-\u9fa5]+\s*\d+(?:\s*[:：]\s*\d+)?\s*$""")
    
    // 缓存：避免重复正则匹配
    private val matchCache = mutableMapOf<String, Boolean>()
    
    /**
     * 检查文本是否为圣经引用
     */
    fun isBibleReference(text: String): Boolean {
        return matchCache.getOrPut(text) {
            bibleReferenceRegex.matches(text.trim())
        }
    }
    
    /**
     * 应用圣经引用样式
     * @param text 原始文本
     * @param normalTextSize 正常文本大小（sp）
     * @return 处理后的 SpannableStringBuilder
     */
    fun applyStyle(text: CharSequence, normalTextSize: Float): CharSequence {
        val lines = text.split("\n")
        val builder = SpannableStringBuilder()
        
        lines.forEachIndexed { index, line ->
            val start = builder.length
            builder.append(line)
            val end = builder.length
            
            // 检查是否为圣经引用
            if (isBibleReference(line)) {
                // 1. 灰色文字
                builder.setSpan(
                    ForegroundColorSpan(Color.parseColor("#999999")),
                    start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                
                // 2. 缩小字体（比正文小 3sp）
                val smallerSize = (normalTextSize - 3f) * App.context.resources.displayMetrics.scaledDensity
                builder.setSpan(
                    AbsoluteSizeSpan(smallerSize.toInt()),
                    start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                
                // 3. 缩小行距（行高 × 0.85）
                // ✅ 添加 @Suppress 抑制未使用参数警告
                builder.setSpan(
                    @Suppress("UNUSED_PARAMETER")
                    object : LineHeightSpan {
                        override fun chooseHeight(
                            text: CharSequence?, 
                            start: Int, 
                            end: Int,
                            spanstartv: Int, 
                            lineHeight: Int, 
                            fm: android.graphics.Paint.FontMetricsInt?
                        ) {
                            fm?.let {
                                val originalHeight = it.descent - it.ascent
                                val reducedHeight = (originalHeight * 0.85f).toInt()
                                val diff = originalHeight - reducedHeight
                                it.ascent += diff / 2
                                it.descent -= diff / 2
                            }
                        }
                    },
                    start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            
            // 添加换行符（除了最后一行）
            if (index < lines.size - 1) {
                builder.append("\n")
            }
        }
        
        return builder
    }
    
    /**
     * 清空缓存（可在适当时机调用）
     */
    fun clearCache() {
        matchCache.clear()
    }
}

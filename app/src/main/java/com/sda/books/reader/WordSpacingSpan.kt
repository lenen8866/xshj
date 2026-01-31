package com.sda.books.reader

import android.graphics.Canvas
import android.graphics.Paint
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ReplacementSpan

class WordSpacingSpan(private val wordSpacing: Float) : ReplacementSpan() {
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        // 计算空格宽度 + 额外间距
        return paint.measureText(" ").toInt() + wordSpacing.toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        // 绘制空格字符
        canvas.drawText(" ", x, y.toFloat(), paint)
    }

    companion object {
        // 应用单词间距到文本
        fun applyToText(text: String, spacing: Float): SpannableString {
            val spannable = SpannableString(text)
            val words = text.split(" ")

            if (words.size <= 1) return spannable // 没有空格则直接返回

            var currentIndex = 0

            // 为每个空格添加间距
            for (i in 0 until words.size - 1) {
                currentIndex += words[i].length
                spannable.setSpan(
                    WordSpacingSpan(spacing),
                    currentIndex,
                    currentIndex + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                currentIndex += 1 // 移动到下一个字符
            }

            return spannable
        }
    }
}
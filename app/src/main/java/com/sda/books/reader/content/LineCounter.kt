package com.sda.books.reader.content

import com.sda.books.reader.content.TextHighlighter.isAllEnglishAndSymbols

/**
 * 行数计算工具
 * 负责计算文本占用的行数（用于滚动定位）
 */
object LineCounter {
    
    /**
     * 计算文本行数
     * @param text 文本内容
     * @param lineCountPerScreen 每屏行数
     * @return 行数
     */
    fun calculateLineCount(text: String, lineCountPerScreen: Int): Int {
        return if (text.isAllEnglishAndSymbols()) {
            // 英文：字符数 / 2 / 每屏行数 + 1
            text.length / 2 / lineCountPerScreen + 1
        } else {
            // 中文：字符数 / 每屏行数
            text.length / lineCountPerScreen
        }
    }
}

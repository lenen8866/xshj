package com.sda.books.reader.content

import java.util.regex.Pattern

/**
 * 文本高亮工具类
 * 负责对中英文文本进行关键词高亮处理
 */
object TextHighlighter {
    
    /**
     * 判断文本是否为纯英文
     */
    fun String.isAllEnglishAndSymbols(): Boolean {
        return matches(Regex("^[a-zA-Z\\x20-\\x7E]+$"))
    }
    
    /**
     * 通用高亮函数
     * @param text 原始文本
     * @param keywords 关键词列表
     * @param indent 缩进（全角空格数量）
     * @param removeAsterisk 是否移除 * 号
     * @return 处理后的 HTML 文本
     */
    fun highlightKeywords(
        text: String,
        keywords: List<String>,
        indent: Int = 0,
        removeAsterisk: Boolean = true
    ): String {
        if (keywords.isEmpty()) {
            val indentSpaces = if (text.startsWith("*")) "" else "\u3000".repeat(indent)
            return "$indentSpaces${if (removeAsterisk) text.replace("*", "") else text}"
        }
        
        var result = text
        val isEnglish = text.isAllEnglishAndSymbols()
        
        // 按关键词长度降序排序，避免短词匹配到长词内部
        val sortedKeywords = keywords.sortedByDescending { it.length }
        
        sortedKeywords.forEach { keyword ->
            if (isEnglish) {
                // 英文：单词边界匹配，忽略大小写
                val pattern = Regex("\\b${Pattern.quote(keyword)}\\b", RegexOption.IGNORE_CASE)
                result = result.replace(pattern) { matchResult ->
                    "<font color='red'>${matchResult.value}</font>"
                }
            } else {
                // 中文：直接包含匹配
                if (result.contains(keyword)) {
                    result = result.replace(keyword, "<font color='red'>$keyword</font>")
                }
            }
        }
        
        // 处理缩进和 * 号
        val indentSpaces = if (text.startsWith("*")) "" else "\u3000".repeat(indent)
        val cleanedResult = if (removeAsterisk) result.replace("*", "") else result
        
        return "$indentSpaces$cleanedResult"
    }
    
    /**
     * 检查文本是否包含关键词（忽略大小写）
     * @param text 原始文本
     * @param word 关键词
     * @return 是否包含
     */
    fun containsWordIgnoreCase(text: String, word: String): Boolean {
        val regex = Regex("\\b${Pattern.quote(word)}\\b", RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(text)
    }
}

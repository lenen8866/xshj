package com.sda.books.reader.content

import com.blankj.utilcode.util.SPUtils
import com.sda.books.reader.entity.ChapterContentItem
import com.sda.books.reader.util.Constant

/**
 * 内容处理器
 * 负责章节内容的构建、文本高亮、行数计算等所有内容处理逻辑
 */
object ContentProcessor {
    
    /**
     * 构建结果数据类
     */
    data class BuildResult(
        val items: List<ChapterContentItem>,
        val stopLineCount: Int,
        val totalLineCount: Int
    )
    
    /**
     * 构建章节内容列表
     * @param contentItems 原始内容列表
     * @param matchContent 关键词列表
     * @param scrollIndex 滚动索引
     * @param sectionSpacing 段落间距
     * @param firstLetterSpacing 首行缩进
     * @return 构建结果
     */
    fun buildChapterItems(
        contentItems: List<ChapterContentItem>,
        matchContent: List<String>,
        scrollIndex: Int,
        sectionSpacing: Int,
        firstLetterSpacing: Int
    ): BuildResult {
        val processedData = mutableListOf<ChapterContentItem>()
        var contentBuffer = StringBuilder()
        var stopLineCount = 0
        var totalLineCount = 0
        
        // 段落间距 HTML
        val pHeight = "<br>".repeat(sectionSpacing + 1)
        val lineCountPerScreen = SPUtils.getInstance().getInt(Constant.LineCount, 30)
        
        contentItems.forEachIndexed { index, item ->
            when {
                // 图片 item
                item.isImg() -> {
                    if (contentBuffer.isNotEmpty()) {
                        processedData.add(ChapterContentItem().apply {
                            setUserContent(contentBuffer.toString())
                        })
                        contentBuffer = StringBuilder()
                    }
                    processedData.add(item)
                }
                
                // 音频文件（跳过）
                item.getShowContent().contains(".mp3") -> {
                    // 不处理
                }
                
                // 文本内容
                else -> {
                    val text = item.getShowContent()
                    
                    // 高亮处理
                    val highlightedText = if (matchContent.isNotEmpty()) {
                        TextHighlighter.highlightKeywords(
                            text = text,
                            keywords = matchContent,
                            indent = firstLetterSpacing,
                            removeAsterisk = true
                        )
                    } else {
                        val indent = if (text.startsWith("*")) "" else "\u3000".repeat(firstLetterSpacing)
                        "$indent${text.replace("*", "")}"
                    }
                    
                    contentBuffer.append("$highlightedText$pHeight")
                    
                    // 计算行数（用于滚动定位）
                    if (matchContent.isNotEmpty()) {
                        val lineCount = LineCounter.calculateLineCount(text, lineCountPerScreen)
                        totalLineCount += lineCount
                        if (index <= scrollIndex) {
                            stopLineCount += lineCount
                        }
                    }
                }
            }
        }
        
        // 添加最后的文本块
        if (contentBuffer.isNotEmpty()) {
            processedData.add(ChapterContentItem().apply {
                setUserContent(contentBuffer.toString())
            })
        }
        
        return BuildResult(
            items = processedData,
            stopLineCount = stopLineCount,
            totalLineCount = totalLineCount
        )
    }
}

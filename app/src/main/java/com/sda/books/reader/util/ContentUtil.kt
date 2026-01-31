package com.sda.books.reader.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.widget.Toast
import com.sda.books.reader.App
import com.sda.books.reader.entity.ChapterContentItem
import java.security.MessageDigest


/**
 * 根据内容显示模式过滤文本，处理 〖en〗...〖/en〗 标签
 * @param raw 原始文本（可能包含 〖en〗...〖/en〗 标签，可能跨行）
 * @param mode 内容模式：0=中，1=双，2=EN
 * @return 过滤后的文本（不包含标签）
 */
fun filterContentByMode(raw: String, mode: Int): String {
    if (raw.isBlank()) return raw
    
    // 匹配 〖en〗...〖/en〗 标签（支持跨行，非贪婪匹配）
    val enTagRegex = Regex("〖en〗([\\s\\S]*?)〖/en〗")
    
    return when (mode) {
        0 -> { // 中：删除所有 〖en〗...〖/en〗，保留其它内容
            enTagRegex.replace(raw, "")
        }
        2 -> { // EN：只保留 〖en〗...〖/en〗 的内容，删除其它所有内容
            val matches = enTagRegex.findAll(raw)
            val enContents = matches.map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.toList()
            if (enContents.count() > 0) enContents.joinToString("\n") else ""
        }
        else -> { // 1=双：保留中文，同时显示 〖en〗...〖/en〗 的内容（去掉标签，英文紧贴中文下一行显示）
            enTagRegex.replace(raw) { matchResult ->
                val enContent = matchResult.groupValues[1].trim()
                // 不额外插入换行，保留原始的换行结构：
                // 原始一般是：中文\n〖en〗英文〖/en〗\n
                // 替换后变成：中文\n英文\n —— 英文紧贴在中文下一行，看起来是“对照”而不是独立一块
                if (enContent.isNotBlank()) enContent else ""
            }
        }
    }
}

fun getChapterContentShowList(content:String):List<ChapterContentItem>{
    val contentMode = AppSettingUtil.getContentMode() // 0=中，1=双，2=EN
    
    // 先在整个内容上过滤 〖en〗...〖/en〗 标签（支持跨行）
    val filteredContent = filterContentByMode(content, contentMode)
    
    // 然后按行分割
    val contentList = filteredContent.split("\n")
    
    // 根据内容显示模式决定显示哪些内容
    val isShowChina = contentMode != 2 // 中或双模式显示中文
    val isShowEn = contentMode != 0    // 双或EN模式显示英文

    val chapterContentItemList = contentList.map {
        ChapterContentItem().init(it)
    }.filter {
        if(it.isNoFilter())return@filter false
        if(it.isCn()) return@filter isShowChina
        if(it.isEng()) return@filter isShowEn
        return@filter true
    }
    return chapterContentItemList
}

fun getUrlName(url:String):String{
    return  url.split("/").last()
}

fun String.md5(): String {
    val digest = MessageDigest.getInstance("MD5")
    val hashBytes = digest.digest(this.toByteArray(Charsets.UTF_8))

    // 将字节数组转换为十六进制字符串
    return hashBytes.joinToString("") {
        "%02x".format(it)
    }
}

val rippleColor = Color.RED

fun copyText(text:String){

    // 获取剪贴板管理器
    val clipboard = App.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

// 创建剪贴板数据
    val clip = ClipData.newPlainText("label", text)

// 设置到剪贴板
    clipboard.setPrimaryClip(clip)

// 可选：显示复制成功提示
    Toast.makeText(App.context, "文本已复制到剪贴板", Toast.LENGTH_SHORT).show()
}
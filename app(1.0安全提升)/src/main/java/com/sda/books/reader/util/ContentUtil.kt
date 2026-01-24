package com.sda.books.reader.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.widget.Toast
import com.sda.books.reader.App
import com.sda.books.reader.entity.ChapterContentItem
import java.security.MessageDigest


fun getChapterContentShowList(content:String):List<ChapterContentItem>{
    val contentList =  content.split("\n")
    var isShowChina = true
    var isShowEn = false
    val isOpenChinaAndEn = AppSettingUtil.getIsOpenChinaAndEn()
    if(isOpenChinaAndEn){
        isShowChina = true
        isShowEn = true
    }

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
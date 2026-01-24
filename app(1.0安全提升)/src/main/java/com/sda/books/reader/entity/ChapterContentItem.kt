package com.sda.books.reader.entity

import android.graphics.Typeface
import android.widget.TextView

class ChapterContentItem {
    private var tag = ""
    private var content = ""
    private var userContent = ""

    fun getUserContent():String{
        return userContent
    }
    fun setUserContent (showContent:String){
        this.userContent = showContent
    }
    private var contentList = mutableListOf<String>()
    fun init(content:String):ChapterContentItem{
        this.content = content
        contentList.clear()
        contentList.addAll(contentChange())
        return this
    }

    fun updateTextView(textView: TextView){
        if(tag == "h3"){
            textView.typeface = Typeface.DEFAULT_BOLD
        }
    }
    private val noFilterTag = listOf("img","h3")
    fun isNoFilter():Boolean{
        return noFilterTag.contains(tag)
    }

    var engTag = listOf("KJV","English","en")
    var cnTag = listOf("和合本","中文","cn")
    fun isEng():Boolean{
       return engTag.contains(tag)
    }

    fun isCn():Boolean{
        return cnTag.contains(tag)
    }

    fun isImg():Boolean{
        return tag == "img";
    }


    fun getShowContent():String{
        return contentList.joinToString(separator = "")
    }
    private fun contentChange():List<String>{
        val contentList = mutableListOf<String>()
        val regex = Regex("(.*?)〖([^〗]+)〗([\\s\\S]*?)〖/\\2〗(.*?)")
        val matches = regex.findAll(content).toList()
        if(matches.isNotEmpty()){
            matches.forEach { match ->
                val beforeText = match.groupValues[1].trim()  // 标签前内容
                val tagName = match.groupValues[2]           // 标签名称（如 "和合本"）
                val tagContent = match.groupValues[3].trim() // 标签内内容
                val afterText = match.groupValues[4].trim()  // 标签后内容
                tag = tagName
                contentList.add(beforeText)
                contentList.add(tagContent)
                contentList.add(afterText)
            }
        }else {
            contentList.add(content)
        }
        return contentList
    }
}
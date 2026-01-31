package com.sda.books.reader.entity

class SearchResultEntity {
    lateinit var chapter: Chapter
    lateinit var content : ChapterContentItem
    var scrollIndex = 0
    var tabs: String = ""
    var isLoader: Boolean = false
    var isEndData:Boolean = false
}
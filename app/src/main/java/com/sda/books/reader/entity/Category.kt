package com.sda.books.reader.entity

class Category {
    var id = 0
    var cateName = ""

    fun getShowName():String{
        if(id == -1)return cateName
        return if(cateName.contains("-")) cateName.split("-", limit = 2)[1] else cateName
    }
}
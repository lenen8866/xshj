package com.sda.books.reader.entity

class Volume {
    var id = 0
    var categoryId = 0
    var volName = ""
    var isEndData = false // 添加标记是否是底线数据的属性

}

fun getShowVolumeName(content:String):String{
    try {
        content.split("-", limit = 2)
        val regex = Regex("^\\d{1,3}-")

        val matchResult = regex.find(content)
        val remainingPart = if (matchResult != null) {
            content.substring(matchResult.range.last + 1)  // 从匹配结束位置的下一个字符开始截取
        } else {
            content  // 如果没有匹配到，返回原字符串
        }
        return remainingPart
    }catch (e:Exception){}
    return content
}

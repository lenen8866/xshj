package com.sda.books.reader.util


object AppSettingUtil {

    val textSizeKey = "textSizeKey"
    fun updateTextSize(textSize:Int){
        SPUtils.Companion.get().put(textSizeKey,textSize)
    }

    fun getTextSize():Int{
        return SPUtils.Companion.get().get(textSizeKey, defaultValue = 19)
    }

    val letterSpacingKey = "letterSpacingKey"
    fun updateTextLetterSpacing(letterSpacing:Int){
        SPUtils.Companion.get().put(letterSpacingKey,letterSpacing)
    }

    fun getTextLetterSpacing():Int{
        return SPUtils.Companion.get().get(letterSpacingKey,9)
    }

    val lineSpacingMultiplierKey = "lineSpacingMultiplierKey"
    fun updateTextLineSpacingMultiplier(lineSpacingMultiplier:Int){
        SPUtils.Companion.get().put(lineSpacingMultiplierKey,lineSpacingMultiplier)
    }

    fun getTextLineSpacingMultiplier():Int{
        return SPUtils.Companion.get().get(lineSpacingMultiplierKey,7)
    }

    val screenPaddingKey = "screenPaddingKey"
    fun updateScreenPadding(screenPadding:Int){
        SPUtils.Companion.get().put(screenPaddingKey,screenPadding)
    }

    fun getScreenPadding():Int{
        return SPUtils.Companion.get().get(screenPaddingKey,12)
    }

    val themeIndexKey = "themeIndexKey"
    fun updateThemeIndex(themeIndex:Int){
        SPUtils.Companion.get().put(themeIndexKey,themeIndex)
    }

    fun getThemeIndex():Int{
        return SPUtils.Companion.get().get(themeIndexKey,0)
    }

    val openChinaAndEnKey = "openChinaAndEnKey"
    fun updateIsOpenChinaAndEn(isOpen:Boolean){
        SPUtils.Companion.get().put(openChinaAndEnKey,isOpen)
    }

    fun getIsOpenChinaAndEn():Boolean{
        return SPUtils.Companion.get().get(openChinaAndEnKey,false)
    }

    val openEnKey = "openEnKey"
    fun updateIsOpenEn(isOpen:Boolean){
        SPUtils.Companion.get().put(openEnKey,isOpen)
    }

    fun getIsOpenEn():Boolean{
        return SPUtils.Companion.get().get(openEnKey,false)
    }


    val tv_text_frist_spacing = "tv_text_frist_spacing"
    fun updateTextFristLetterSpacing(letterSpacing: Int) {
        SPUtils.Companion.get().put(tv_text_frist_spacing, letterSpacing)
    }

    fun getTextFristLetterSpacing(): Int {
        return SPUtils.Companion.get().get(tv_text_frist_spacing, 2)
    }


    val tv_text_section_h_spacing = "tv_text_section_h_spacing"
    fun updateTextSectionHLetterSpacing(letterSpacing: Int) {
        SPUtils.Companion.get().put(tv_text_section_h_spacing, letterSpacing)
    }

    fun getTextSectionHLetterSpacing(): Int {
        return SPUtils.Companion.get().get(tv_text_section_h_spacing, 1)
    }

}
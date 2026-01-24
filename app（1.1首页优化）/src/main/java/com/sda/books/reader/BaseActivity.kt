package com.sda.books.reader

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.sda.books.reader.util.AppSettingUtil


abstract class BaseActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()
        if(isApplyTheme()){
            val themeColor = getThemeList()[AppSettingUtil.getThemeIndex()].color.toColorInt()
            getRootView().setBackgroundColor(getThemeList()[AppSettingUtil.getThemeIndex()].color.toColorInt())
           //window.statusBarColor = themeColor
        }
    }

    abstract fun getRootView():View

    open fun isApplyTheme() = true

}
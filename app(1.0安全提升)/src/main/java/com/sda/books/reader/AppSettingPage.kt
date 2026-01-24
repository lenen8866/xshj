package com.sda.books.reader

import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Layout
import android.text.StaticLayout
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.book.reader.R
import com.sda.books.reader.act.AboutUsAct
import com.sda.books.reader.adapter.ThemeListAdapter
import com.book.reader.databinding.ActivityAppSettingBinding
import com.sda.books.reader.entity.ThemeEntity
import com.sda.books.reader.util.AppSettingUtil
import com.sda.books.reader.util.Constant



fun getThemeList():List<ThemeEntity>{
    return listOf(
        ThemeEntity().apply {
            color = "#FFF6F5F3"
            borderColor = "#FF393938"
        },
        ThemeEntity().apply {
            color = "#FFE0D5C3"
            borderColor = "#FF706B71"
        },
        ThemeEntity().apply {
            color = "#FFCFDAC1"
            borderColor = "#FF81897B"
        },
        ThemeEntity().apply {
            color = "#FF96938F"
            borderColor = "#FF383435"
        },
    )
}

var maxScreenPadding = 30

fun dpToPx(dpValue: Float): Int {
    val scale = Resources.getSystem().displayMetrics.density
    return (dpValue * scale + 0.5f).toInt()
}

fun getActualLineSpacing(textView: TextView): Float {
    // 行高（每行的总高度）
    val lineHeight = textView.lineHeight.toFloat()

    // 字体的最大高度（Ascent + |Descent|）
    val paint = textView.paint
    val fontHeight = paint.fontMetrics.run { descent - ascent }

    // 实际行间距 = 行高 - 字体高度 + 额外间距
    // （额外间距通过setLineSpacing(addSpacing, multiplier)设置）
    return lineHeight - fontHeight
}
class AppSettingPage : BaseActivity() {
    lateinit var binding : ActivityAppSettingBinding
    var fontSize = 0
    var maxFontSize = 40
    var minFontSize = 18
    var fontSizePercent = 0f
    var hSpace = 0
    var vSpace = 0
    var vFristSpace = 0
    var vHSpace = 0
    var screenPadding = 0
    var isOpenChinaAndEn = false
    var isOpenEn = false
    var themeIndex = 0
    var themeList = listOf<ThemeEntity>()
    override fun getRootView(): View {
        return binding.root
    }

    override fun isApplyTheme(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("TAG","==========0004")
        binding = ActivityAppSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ivBack.setOnClickListener {
            finish()
        }
        binding.upDateDb.setOnClickListener {
            startActivity(Intent(this, AppUpdateDbPage::class.java))
            finish()
        }
        Log.i("cccccc","oncreate====${resources.configuration.locale.language}")
        fontSize = AppSettingUtil.getTextSize()
        hSpace = AppSettingUtil.getTextLetterSpacing()
        vSpace = AppSettingUtil.getTextLineSpacingMultiplier()
        vFristSpace = AppSettingUtil.getTextFristLetterSpacing()
        vHSpace = AppSettingUtil.getTextSectionHLetterSpacing()
        screenPadding = AppSettingUtil.getScreenPadding()
        themeIndex = AppSettingUtil.getThemeIndex()
        isOpenChinaAndEn = AppSettingUtil.getIsOpenChinaAndEn()
        isOpenEn = AppSettingUtil.getIsOpenEn()



        themeList = getThemeList()
        themeList[SPUtils.getInstance().getInt(Constant.ThemeColorIndex,0)].isSelect = true
        //themeList[themeIndex].isSelect = true

        binding.tvTextSpace.text = "$hSpace"
        binding.tvTextVSpace.text = "$vSpace"
        binding.tvTextFirstSpace.text = "$vFristSpace"
        binding.tvTextSectionHSpace.text = "$vHSpace"
        binding.tvScreenPadding.text = "$screenPadding"

        updateTvShow()
        updateFontSize()

        binding.textSectionHSpaceBar.progress = vHSpace
        binding.textSectionHSpaceBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                /*val percent = progress / 60f
                vHSpace = (percent * 60).toInt()
                AppSettingUtil.updateTextSectionHLetterSpacing(vHSpace)
                binding.tvTextSectionHSpace.text = "$vHSpace"*/

                val percent = progress / 5f
                vHSpace = (percent * 5).toInt()
                AppSettingUtil.updateTextSectionHLetterSpacing(vHSpace)
                binding.tvTextSectionHSpace.text = "$vHSpace"
                updateTvShow()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        binding.textFirstSpaceBar.progress = vFristSpace
        binding.textFirstSpaceBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val percent = progress / 3f
                vFristSpace = (percent * 3).toInt()
                AppSettingUtil.updateTextFristLetterSpacing(vFristSpace)
                binding.tvTextFirstSpace.text = "$vFristSpace"
                updateTvShow()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })


        binding.textSpaceBar.progress = hSpace
        binding.textSpaceBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val percent = progress / 30f
                hSpace = (percent * 30).toInt()
                AppSettingUtil.updateTextLetterSpacing(hSpace)
                binding.tvTextSpace.text = "$hSpace"
                updateTvShow()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        binding.tvAboutUs.setOnClickListener {
            //Log.e("tag","=======")
            var intent = Intent(this@AppSettingPage,AboutUsAct::class.java)
            startActivity(intent)
        }

        binding.textVSpaceBar.progress = vSpace
        binding.textVSpaceBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val percent = progress / 60f
                vSpace = (percent * 60).toInt()
                AppSettingUtil.updateTextLineSpacingMultiplier(vSpace)
                binding.tvTextVSpace.text = "$vSpace"
                updateTvShow()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        binding.textScreenPaddingBar.progress = screenPadding
        binding.textScreenPaddingBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val percent = progress / 30f
                screenPadding = (percent * 30).toInt()
                binding.tvScreenPadding.text = "$screenPadding"
                AppSettingUtil.updateScreenPadding(screenPadding)
                updateTvShow()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        binding.switchChinaAndEn.isChecked = AppSettingUtil.getIsOpenChinaAndEn()
        binding.switchChinaAndEn.setOnCheckedChangeListener { view, isChecked ->
            isOpenChinaAndEn = isChecked
            AppSettingUtil.updateIsOpenChinaAndEn(isOpenChinaAndEn)
        }

        binding.switchEn.isChecked = AppSettingUtil.getIsOpenEn()
        binding.switchEn.setOnCheckedChangeListener { view, isChecked ->
            isOpenEn = isChecked
            updateTvLanguage()
            AppSettingUtil.updateIsOpenEn(isOpenEn)
        }
        binding.switchSearch.setOnCheckedChangeListener { view, isChecked ->
            if(isChecked){
                SPUtils.getInstance().put(Constant.SearchAllFile,1)
            }else{
                SPUtils.getInstance().put(Constant.SearchAllFile,2)
            }
            updateTvLanguage()
        }






//        binding.ivAddFontSize.setOnClickListener {
//            fontSize =  min(fontSize+1,maxFontSize)
//            updateFontSize()
//            AppSettingUtil.updateTextSize(fontSize)
//        }

//        binding.ivReduceFontSize.setOnClickListener {
//            fontSize =  max(minFontSize,fontSize-1)
//            updateFontSize()
//            AppSettingUtil.updateTextSize(fontSize)
//        }

        //字体大小
        binding.fontSizeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val  percent =  progress / 100f
                fontSize = ((maxFontSize - minFontSize) * percent + minFontSize).toInt()
                binding.tvFontSize.text = "$fontSize"
                updateTvShow()
                AppSettingUtil.updateTextSize(fontSize)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })


//        NumberRangeInputFilter(
//            binding.etHSpace,
//            minValue = 0
//        ){
//            hSpace = it
//            updateTvShow()
//        }

//        NumberRangeInputFilter(
//            binding.etVSpace,
//            minValue = 0,
//        ){
//            vSpace = it
//            updateTvShow()
//        }


//        NumberRangeInputFilter(
//            binding.etScreenPadding,
//            maxValue = 100
//        ){
//            screenPadding = it
//            updateTvShow()
//        }


        binding.themeList.layoutManager = LinearLayoutManager(this,RecyclerView.HORIZONTAL,false)
        binding.themeList.adapter = ThemeListAdapter(themeList, onItemClick = object : ThemeListAdapter.OnItemClickListener{
            override fun onItemClick(position: Int, theme: ThemeEntity) {
                themeIndex = position
                SPUtils.getInstance().put(Constant.Background_Color,themeList[themeIndex].color)
                SPUtils.getInstance().put(Constant.Border_Color,themeList[themeIndex].borderColor)
                SPUtils.getInstance().put(Constant.ThemeColorIndex,themeIndex)

                updateTvShow()
            }
        })


//        binding.tvReset.setOnClickListener {
//            reset()
//        }

        updateTvLanguage()
    }

    //字体大小
    fun updateFontSize(){
        fontSizePercent = (fontSize - minFontSize) / ((maxFontSize - minFontSize) *1f)
        binding.fontSizeBar.progress = (fontSizePercent * 100).toInt()
        binding.tvFontSize.text = "$fontSize"
        updateTvShow()
    }

    fun updateTvShow(){
        //vHSpace = 1000
        var divStyle = "<div \"style\"=\"height : 500px\">ssss</div>"
        var pHeight = "<br>";
        for (index in 0 until vHSpace){
            pHeight = pHeight +"<br>"
        }



        // 应用首行缩进
        val indentSpaces = "\u3000".repeat(vFristSpace) // 全角空格
       // val previewText = "$indentSpaces${resources.getString(R.string.pre)}"
        //"<div margin-bottom: ${vHSpace*100}px;/>"
        val previewText = "$indentSpaces"+"这是字体预览区域，您可以在此直观查看字体在阅读时的呈现效果。无论是字体类型、字号，还是行距、字间距的微调，预览区都会实时反映您的设置。${pHeight}" +
                "$indentSpaces This is the font preview area, where you can visually check how the text will appear during reading. Whether you're adjusting the font type, size, line spacing, or letter spacing, changes will be reflected here in real time.${pHeight}" +
        "$indentSpaces 通过右侧的设置项，您可以根据阅读需求灵活调整样式。选择不同字体和排版方式，可以帮助您找到最适合自己阅读习惯的显示效果，提升阅读的舒适度与效率。${pHeight}" +
        "$indentSpaces Use the settings on the right to customize the style according to your reading preferences. Trying different fonts and layout options can help you discover the most comfortable and efficient reading experience.${pHeight}"+
        "$indentSpaces 请注意，预览区域仅用于展示样式效果，实际阅读内容请以正式界面为准。建议在做出调整后，多次切换预览内容进行比对，以获得最佳阅读体验。${pHeight}"+
        "$indentSpaces Please note that this preview is for display purposes only—the actual reading interface may vary. We recommend switching between different preview texts after making adjustments to find the most suitable configuration.${pHeight}"
        // 应用段落间距
        //binding.tv1.text = previewText
        LogUtils.e("======================$previewText")

        // 设置段落间距为24dp
        /*val paragraphSpacing = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            vHSpace.toFloat(),
            resources.displayMetrics
        ).toInt()*/
        var paragraphSpacing = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            vHSpace.toFloat(),
            resources.displayMetrics
        ).toInt()
        //LogUtils.e("=====================${paragraphSpacing}")


        binding.tv1.setLineSpacing(0f, 1f) // 重置行高
        //binding.tv1.setPadding(0, vHSpace.dpToPx(), 0, 0)

        binding.tv1.setTextSize(TypedValue.COMPLEX_UNIT_SP,fontSize * 1f)
        binding.tv1.apply {
            //默认是1
            setLineSpacing(0f, vSpace / 30f * 1 + 1)
            letterSpacing = hSpace / 30f * 2
            val params = layoutParams as MarginLayoutParams
            Log.i("cccccccccc", "${dpToPx(((screenPadding / 30f) * maxScreenPadding))}")
            params.marginStart = dpToPx(((screenPadding / 30f) * maxScreenPadding))
            params.marginEnd = dpToPx(((screenPadding / 30f) * maxScreenPadding))
            layoutParams = params


           // LogUtils.e("==============>>>"+getMaxCharsPerLine(binding.tv1))


            binding.tv1.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener{
                override fun onGlobalLayout() {

                    // 移除监听器，避免重复调用
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        binding.tv1.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                    } else {
                        binding.tv1.getViewTreeObserver().removeGlobalOnLayoutListener(this)
                    }


                    // 获取一行可显示的字符数
                    val charsPerLine = getMaxCharsPerLine(binding.tv1)
                    //LogUtils.e("==============>>>==="+charsPerLine)
                    SPUtils.getInstance().put(Constant.LineCount,charsPerLine+2)
                    /*if(charsPerLine < 10){
                        SPUtils.getInstance().put(Constant.LineCount,10)
                    }else{

                    }*/
                }

            })

        }

        binding.previewContainer.updatePadding(left =  dpToPx(screenPadding * 1f), right =  dpToPx(screenPadding * 1f))

        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE  // 矩形形状
        shape.setColor(themeList[SPUtils.getInstance().getInt(Constant.ThemeColorIndex,0)].color.toColorInt())
        shape.setStroke(3, "#FFC3C3C3".toColorInt())
        shape.cornerRadius = 18f
        binding.previewContainer.background = shape
        runOnUiThread {

            binding.tv1.text = Html.fromHtml(previewText,Html.FROM_HTML_MODE_LEGACY)
            LogUtils.e("===========>>>>>>${binding.tv1.lineHeight}")
            SPUtils.getInstance().put(Constant.TextViewLineHeight,binding.tv1.lineHeight)
            //binding.web.loadDataWithBaseURL(null, htmlString, "text/html", "UTF-8", null)
        }


        //ppSettingUtil.updateThemeIndex(themeIndex)
    }

    private fun reset(){


        vSpace = 0
        hSpace = 0
        screenPadding = 22
        themeIndex = 0
        ( binding.themeList.adapter as ThemeListAdapter).reset()

        fontSize = 20
        updateFontSize()

        isOpenChinaAndEn = false
        binding.switchChinaAndEn.isChecked = isOpenChinaAndEn

        binding.tvTextSpace.text = "$hSpace"
        binding.tvTextVSpace.text = "$vSpace"
        binding.tvScreenPadding.text = "$screenPadding"

        binding.textSpaceBar.progress = hSpace
        binding.textVSpaceBar.progress = vSpace
        binding.textScreenPaddingBar.progress = screenPadding

        updateTvShow()

        AppSettingUtil.updateThemeIndex(themeIndex)
        AppSettingUtil.updateTextSize(fontSize)
        AppSettingUtil.updateScreenPadding(screenPadding)
        AppSettingUtil.updateIsOpenChinaAndEn(isOpenChinaAndEn)
        AppSettingUtil.updateTextLetterSpacing(hSpace)
        AppSettingUtil.updateTextLineSpacingMultiplier(vSpace)
    }

    private fun updateTvLanguage(){
        val language = if(isOpenEn) "en" else "zh"
        binding.tvSetting.text = LanguageUtils.getStringByLanguage(this, R.string.setting,language)
        binding.tvLanguage.text = LanguageUtils.getStringByLanguage(this, R.string.language,language)
        binding.tvChineseEnglishComparison.text = LanguageUtils.getStringByLanguage(this,
            R.string.chinese_english_comparison,language)
        binding.tvAboutUs.text = LanguageUtils.getStringByLanguage(this, R.string.about_us,language)
        binding.tvContentSetting.text = LanguageUtils.getStringByLanguage(this,
            R.string.content_setting,language)
        binding.tvSize.text = LanguageUtils.getStringByLanguage(this, R.string.Size,language)
        binding.tvTextSpacing.text = LanguageUtils.getStringByLanguage(this,
            R.string.text_spacing,language)
        binding.tvTextVSpacing.text = LanguageUtils.getStringByLanguage(this,
            R.string.row_height_spacing,language)
        binding.tvTextFristSpacing.text =
            LanguageUtils.getStringByLanguage(this, R.string.text_frist_spacing, language)
        binding.tvTextSectionHSpacing.text =
            LanguageUtils.getStringByLanguage(this, R.string.text_section_height_spacing, language)
        binding.tvScreenPitch.text = LanguageUtils.getStringByLanguage(this,
            R.string.screen_pitch,language)
        binding.tvTheme.text = LanguageUtils.getStringByLanguage(this, R.string.theme,language)
        binding.previewEffect.text = LanguageUtils.getStringByLanguage(this,
            R.string.preview_effect,language)
//        binding.tvReset.text = LanguageUtils.getStringByLanguage(this,R.string.restore_the_default,language)

        binding.switchSetttingLanguage.text = if(isOpenEn) "切换中文" else "Switch to English"

        if(SPUtils.getInstance().getInt(Constant.SearchAllFile,0) == 1){
            binding.switchSearch.isChecked = true
            if(isOpenEn){
                binding.switchSetttingSearch.text = "Global Search"
            }else{
                binding.switchSetttingSearch.text = "全局搜索"
            }

        }else{
            if (isOpenEn){
                binding.switchSetttingSearch.text = "Fast Search"
            }else{
                binding.switchSetttingSearch.text = "极速搜索"
            }

        }
    }

    fun getMaxCharsPerLine(textView: TextView): Int {
        // 获取文本、字体和宽度
        val text = textView.text
        val paint = textView.paint
        val width = textView.width - textView.paddingLeft - textView.paddingRight

        // 创建 StaticLayout 实例来测量文本
        val layout = StaticLayout(
            text,
            paint,
            width,
            Layout.Alignment.ALIGN_NORMAL,
            1.0f,  // 行间距倍数
            0.0f,  // 额外行间距
            false // 是否包含额外空间
        )

        // 返回第一行的字符数
        return layout.getLineEnd(0) - layout.getLineStart(0)
    }
    fun estimateMaxCharsPerLine(textView: TextView): Int {
        val paint = textView.paint
        val width = textView.width - textView.paddingLeft - textView.paddingRight

        // 计算单个字符的平均宽度（以 "A" 为例）
        val charWidth = paint.measureText("A")

        // 估算一行可显示的字符数
        return (width / charWidth).toInt()
    }
}
package com.sda.books.reader

import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.Layout
import android.text.StaticLayout
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.SeekBar
import android.view.MotionEvent
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.SPUtils
import com.book.reader.R
import com.book.reader.databinding.ActivityAppSettingBinding
import com.sda.books.reader.act.AboutUsAct
import com.sda.books.reader.adapter.ThemeListAdapter
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
    val lineHeight = textView.lineHeight.toFloat()
    val paint = textView.paint
    val fontHeight = paint.fontMetrics.run { descent - ascent }
    return lineHeight - fontHeight
}

class AppSettingPage : BaseActivity() {

    companion object {
        const val SEEKBAR_SECTION_H_MAX = 5
        const val SEEKBAR_FIRST_SPACE_MAX = 3
        const val SEEKBAR_LETTER_SPACE_MAX = 30
        const val SEEKBAR_LINE_SPACE_MAX = 60
        const val SEEKBAR_SCREEN_PADDING_MAX = 30
        const val SEEKBAR_FONT_PROGRESS_MAX = 100
        const val FONT_SIZE_MIN = 18
        const val FONT_SIZE_MAX = 40
        const val PREVIEW_SPACE_SCALE = 30f
        const val LINE_COUNT_EXTRA = 2
        const val PREVIEW_STROKE_WIDTH = 3
        const val PREVIEW_CORNER_RADIUS = 18f
        const val PREVIEW_STROKE_COLOR = 0xFFC3C3C3.toInt()
        private const val PREVIEW_DEBOUNCE_MS = 120L

        // 推荐默认值（内容设置）
        const val DEFAULT_FONT_SIZE = 19
        const val DEFAULT_LETTER_SPACE = 9
        const val DEFAULT_LINE_SPACE = 7
        const val DEFAULT_FIRST_SPACE = 2
        const val DEFAULT_SECTION_H_SPACE = 1
        const val DEFAULT_SCREEN_PADDING = 12
    }

    lateinit var binding: ActivityAppSettingBinding
    var fontSize = 0
    var maxFontSize = FONT_SIZE_MAX
    var minFontSize = FONT_SIZE_MIN
    var fontSizePercent = 0f
    var hSpace = 0
    var vSpace = 0
    var vFristSpace = 0
    var vHSpace = 0
    var screenPadding = 0
    var contentMode = 0 // 0=中，1=双，2=EN（内容显示模式）
    var isOpenEn = false // 界面语言开关（保留，不影响内容显示）
    var themeIndex = 0
    var themeList = listOf<ThemeEntity>()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var previewUpdateRunnable: Runnable? = null

    override fun getRootView(): View {
        return binding.root
    }

    override fun isApplyTheme(): Boolean = false

    override fun onDestroy() {
        super.onDestroy()
        previewUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
        previewUpdateRunnable = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ivBack.setOnClickListener {
            finish()
        }
        binding.upDateDb.setOnClickListener {
            startActivity(Intent(this, AppUpdateDbPage::class.java))
            finish()
        }
        fontSize = AppSettingUtil.getTextSize()
        hSpace = AppSettingUtil.getTextLetterSpacing()
        vSpace = AppSettingUtil.getTextLineSpacingMultiplier()
        vFristSpace = AppSettingUtil.getTextFristLetterSpacing()
        vHSpace = AppSettingUtil.getTextSectionHLetterSpacing()
        screenPadding = AppSettingUtil.getScreenPadding()
        themeIndex = AppSettingUtil.getThemeIndex()
        contentMode = AppSettingUtil.getContentMode() // 内容显示模式（兼容旧数据迁移）
        isOpenEn = AppSettingUtil.getIsOpenEn() // 界面语言开关（保留）



        themeList = getThemeList()
        themeList[SPUtils.getInstance().getInt(Constant.ThemeColorIndex,0)].isSelect = true

        binding.tvTextSpace.text = "$hSpace"
        binding.tvTextVSpace.text = "$vSpace"
        binding.tvTextFirstSpace.text = "$vFristSpace"
        binding.tvTextSectionHSpace.text = "$vHSpace"
        binding.tvScreenPadding.text = "$screenPadding"

        updateTvShow()
        updateFontSize()

        binding.textSectionHSpaceBar.progress = vHSpace
        binding.textSectionHSpaceBar.setOnSeekBarChangeListener(createDebouncedSeekBarListener(
            setValue = { vHSpace = it },
            textView = binding.tvTextSectionHSpace,
            persist = { AppSettingUtil.updateTextSectionHLetterSpacing(vHSpace) },
            progressToValue = { it.coerceIn(0, SEEKBAR_SECTION_H_MAX) }
        ))
        enableSeekBarTap(binding.textSectionHSpaceBar)

        binding.textFirstSpaceBar.progress = vFristSpace
        binding.textFirstSpaceBar.setOnSeekBarChangeListener(createDebouncedSeekBarListener(
            setValue = { vFristSpace = it },
            textView = binding.tvTextFirstSpace,
            persist = { AppSettingUtil.updateTextFristLetterSpacing(vFristSpace) },
            progressToValue = { it.coerceIn(0, SEEKBAR_FIRST_SPACE_MAX) }
        ))
        enableSeekBarTap(binding.textFirstSpaceBar)

        binding.textSpaceBar.progress = hSpace
        binding.textSpaceBar.setOnSeekBarChangeListener(createDebouncedSeekBarListener(
            setValue = { hSpace = it },
            textView = binding.tvTextSpace,
            persist = { AppSettingUtil.updateTextLetterSpacing(hSpace) },
            progressToValue = { it.coerceIn(0, SEEKBAR_LETTER_SPACE_MAX) }
        ))

        binding.tvAboutUs.setOnClickListener {
            startActivity(Intent(this@AppSettingPage, AboutUsAct::class.java))
        }

        binding.textVSpaceBar.progress = vSpace
        binding.textVSpaceBar.setOnSeekBarChangeListener(createDebouncedSeekBarListener(
            setValue = { vSpace = it },
            textView = binding.tvTextVSpace,
            persist = { AppSettingUtil.updateTextLineSpacingMultiplier(vSpace) },
            progressToValue = { it.coerceIn(0, SEEKBAR_LINE_SPACE_MAX) }
        ))

        binding.textScreenPaddingBar.progress = screenPadding
        binding.textScreenPaddingBar.setOnSeekBarChangeListener(createDebouncedSeekBarListener(
            setValue = { screenPadding = it },
            textView = binding.tvScreenPadding,
            persist = { AppSettingUtil.updateScreenPadding(screenPadding) },
            progressToValue = { it.coerceIn(0, SEEKBAR_SCREEN_PADDING_MAX) }
        ))

        // 内容显示模式：三态选择（中/双/EN）
        setupContentModeButtons()

        binding.switchEn.isChecked = AppSettingUtil.getIsOpenEn()
        binding.switchEn.setOnCheckedChangeListener { view, isChecked ->
            isOpenEn = isChecked
            updateTvLanguage() // 只影响界面语言，不影响内容显示
            AppSettingUtil.updateIsOpenEn(isOpenEn)
        }
        binding.switchSearch.setOnCheckedChangeListener { view, isChecked ->
            SPUtils.getInstance().put(Constant.SearchAllFile, if (isChecked) 1 else 2)
            updateTvLanguage()
        }



        binding.fontSizeBar.setOnSeekBarChangeListener(createDebouncedSeekBarListener(
            setValue = { fontSize = it },
            textView = binding.tvFontSize,
            persist = { AppSettingUtil.updateTextSize(fontSize) },
            progressToValue = { p ->
                ((p / SEEKBAR_FONT_PROGRESS_MAX.toFloat()) * (FONT_SIZE_MAX - FONT_SIZE_MIN) + FONT_SIZE_MIN).toInt()
                    .coerceIn(FONT_SIZE_MIN, FONT_SIZE_MAX)
            }
        ))
        enableSeekBarTap(binding.fontSizeBar)

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

        // 一键恢复推荐默认内容设置
        binding.tvRestoreDefault.setOnClickListener {
            restoreDefaultSettings()
        }

        updateTvLanguage()
    }
    
    private fun restoreDefaultSettings() {
        fontSize = DEFAULT_FONT_SIZE
        hSpace = DEFAULT_LETTER_SPACE
        vSpace = DEFAULT_LINE_SPACE
        vFristSpace = DEFAULT_FIRST_SPACE
        vHSpace = DEFAULT_SECTION_H_SPACE
        screenPadding = DEFAULT_SCREEN_PADDING

        AppSettingUtil.updateTextSize(fontSize)
        AppSettingUtil.updateTextLetterSpacing(hSpace)
        AppSettingUtil.updateTextLineSpacingMultiplier(vSpace)
        AppSettingUtil.updateTextFristLetterSpacing(vFristSpace)
        AppSettingUtil.updateTextSectionHLetterSpacing(vHSpace)
        AppSettingUtil.updateScreenPadding(screenPadding)

        binding.tvFontSize.text = "$fontSize"
        binding.tvTextSpace.text = "$hSpace"
        binding.tvTextVSpace.text = "$vSpace"
        binding.tvTextFirstSpace.text = "$vFristSpace"
        binding.tvTextSectionHSpace.text = "$vHSpace"
        binding.tvScreenPadding.text = "$screenPadding"

        updateFontSize()
        binding.textSpaceBar.progress = hSpace
        binding.textVSpaceBar.progress = vSpace
        binding.textFirstSpaceBar.progress = vFristSpace
        binding.textSectionHSpaceBar.progress = vHSpace
        binding.textScreenPaddingBar.progress = screenPadding
    }

    fun updateFontSize() {
        fontSizePercent = (fontSize - minFontSize) / ((maxFontSize - minFontSize) * 1f)
        binding.fontSizeBar.progress = (fontSizePercent * SEEKBAR_FONT_PROGRESS_MAX).toInt()
        binding.tvFontSize.text = "$fontSize"
        updateTvShow()
    }

    fun updateTvShow(){
        val brCount = if (contentMode == 0) 1 else vHSpace.coerceAtLeast(1)
        val pHeight = "<br>".repeat(brCount)
        val indentSpaces = "\u3000".repeat(vFristSpace)
        
        val previewText = buildPreviewText(contentMode, indentSpaces, pHeight)
        binding.tv1.setLineSpacing(0f, 1f)
        binding.tv1.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize * 1f)
        binding.tv1.apply {
            setLineSpacing(0f, vSpace / PREVIEW_SPACE_SCALE * 1 + 1)
            letterSpacing = hSpace / PREVIEW_SPACE_SCALE * 2
            val params = layoutParams as MarginLayoutParams
            params.marginStart = dpToPx(((screenPadding / PREVIEW_SPACE_SCALE) * maxScreenPadding))
            params.marginEnd = dpToPx(((screenPadding / PREVIEW_SPACE_SCALE) * maxScreenPadding))
            layoutParams = params
            viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val charsPerLine = getMaxCharsPerLine(binding.tv1)
                    SPUtils.getInstance().put(Constant.LineCount, charsPerLine + LINE_COUNT_EXTRA)
                }
            })
        }

        binding.previewContainer.updatePadding(left =  dpToPx(screenPadding * 1f), right =  dpToPx(screenPadding * 1f))

        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.setColor(themeList[SPUtils.getInstance().getInt(Constant.ThemeColorIndex, 0)].color.toColorInt())
        shape.setStroke(PREVIEW_STROKE_WIDTH, PREVIEW_STROKE_COLOR)
        shape.cornerRadius = PREVIEW_CORNER_RADIUS
        binding.previewContainer.background = shape
        runOnUiThread {
            binding.tv1.text = Html.fromHtml(previewText, Html.FROM_HTML_MODE_LEGACY)
            SPUtils.getInstance().put(Constant.TextViewLineHeight, binding.tv1.lineHeight)
        }
    }

    private fun updateTvLanguage(){
        val language = if(isOpenEn) "en" else "zh"
        binding.tvSetting.text = LanguageUtils.getStringByLanguage(this, R.string.setting,language)
        binding.tvLanguage.text = LanguageUtils.getStringByLanguage(this, R.string.language, language)
        binding.tvContentMode.text = LanguageUtils.getStringByLanguage(this,
            R.string.chinese_english_comparison, language)
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
            R.string.preview_effect, language)

        binding.switchSetttingLanguage.text = if (isOpenEn) "切换中文" else "Switch to English"
        
        val isGlobalSearch = SPUtils.getInstance().getInt(Constant.SearchAllFile, 0) == 1
        binding.switchSearch.isChecked = isGlobalSearch
        binding.switchSetttingSearch.text = when {
            isGlobalSearch && isOpenEn -> "Global Search"
            isGlobalSearch -> "全局搜索"
            isOpenEn -> "Fast Search"
            else -> "极速搜索"
        }
    }

    private fun schedulePreviewUpdate() {
        previewUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
        previewUpdateRunnable = Runnable {
            updateTvShow()
            previewUpdateRunnable = null
        }.also {
            previewUpdateRunnable = it
            mainHandler.postDelayed(it, PREVIEW_DEBOUNCE_MS)
        }
    }

    private fun commitPreviewUpdate() {
        previewUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
        previewUpdateRunnable = null
        updateTvShow()
    }

    private fun createDebouncedSeekBarListener(
        setValue: (Int) -> Unit,
        textView: TextView,
        persist: () -> Unit,
        progressToValue: (Int) -> Int
    ): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val v = progressToValue(progress)
                setValue(v)
                textView.text = "$v"
                persist()
                schedulePreviewUpdate()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                commitPreviewUpdate()
            }
        }
    }

    /**
     * 允许在 SeekBar 任意位置点击/拖动来直接设置进度，
     * 不需要非要点中间那个小圆点，操作更省力。
     */
    private fun enableSeekBarTap(seekBar: SeekBar) {
        seekBar.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                    // 告诉父级不要拦截事件（NestedScrollView 等）
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    val sb = v as SeekBar
                    val width = sb.width
                    if (width > 0) {
                        val x = event.x.coerceIn(0f, width.toFloat())
                        val proportion = x / width
                        val newProgress = (proportion * sb.max).toInt()
                        sb.progress = newProgress
                    }
                    // 事件自己消费掉，触发 OnSeekBarChangeListener 即可
                    true
                }
                else -> false
            }
        }
    }

    fun getMaxCharsPerLine(textView: TextView): Int {
        // 获取文本、字体和宽度
        val text = textView.text
        val paint = textView.paint
        val width = textView.width - textView.paddingLeft - textView.paddingRight

        // 使用 StaticLayout.Builder 创建实例（Android 6.0+ 的新写法）
        val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0.0f, 1.0f)
                .setIncludePad(false)
                .build()
        } else {
            // Android 6.0 以下使用旧构造函数
            @Suppress("DEPRECATION")
            StaticLayout(
                text,
                paint,
                width,
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                0.0f,
                false
            )
        }

        return layout.getLineEnd(0) - layout.getLineStart(0)
    }
    
    private fun buildPreviewText(mode: Int, indent: String, pHeight: String): String {
        val cn1 = "这是字体预览区域，您可以在此直观查看字体在阅读时的呈现效果。无论是字体类型、字号，还是行距、字间距的微调，预览区都会实时反映您的设置。"
        val cn2 = "通过右侧的设置项，您可以根据阅读需求灵活调整样式。选择不同字体和排版方式，可以帮助您找到最适合自己阅读习惯的显示效果，提升阅读的舒适度与效率。"
        val cn3 = "请注意，预览区域仅用于展示样式效果，实际阅读内容请以正式界面为准。建议在做出调整后，多次切换预览内容进行比对，以获得最佳阅读体验。"
        val en1 = "This is the font preview area, where you can visually check how the text will appear during reading. Whether you're adjusting the font type, size, line spacing, or letter spacing, changes will be reflected here in real time."
        val en2 = "Use the settings on the right to customize the style according to your reading preferences. Trying different fonts and layout options can help you discover the most comfortable and efficient reading experience."
        val en3 = "Please note that this preview is for display purposes only—the actual reading interface may vary. We recommend switching between different preview texts after making adjustments to find the most suitable configuration."
        
        return when (mode) {
            0 -> "$indent$cn1$pHeight$indent $cn2$pHeight$indent $cn3$pHeight"
            2 -> "$indent$en1$pHeight$indent $en2$pHeight$indent $en3$pHeight"
            else -> "$indent$cn1$pHeight$indent $en1$pHeight$indent $cn2$pHeight$indent $en2$pHeight$indent $cn3$pHeight$indent $en3$pHeight"
        }
    }
    
    private fun setupContentModeButtons() {
        // 初始化按钮状态
        updateContentModeButtons()
        
        binding.btnContentChinese.setOnClickListener {
            contentMode = 0
            AppSettingUtil.updateContentMode(contentMode)
            updateContentModeButtons()
            updateTvShow() // 只更新预览，不影响界面语言
        }
        
        binding.btnContentDual.setOnClickListener {
            contentMode = 1
            AppSettingUtil.updateContentMode(contentMode)
            updateContentModeButtons()
            updateTvShow()
        }
        
        binding.btnContentEnglish.setOnClickListener {
            contentMode = 2
            AppSettingUtil.updateContentMode(contentMode)
            updateContentModeButtons()
            updateTvShow()
        }
    }
    
    private fun updateContentModeButtons() {
        updateButtonState(binding.btnContentChinese, contentMode == 0)
        updateButtonState(binding.btnContentDual, contentMode == 1)
        updateButtonState(binding.btnContentEnglish, contentMode == 2)
    }
    
    private fun updateButtonState(button: TextView, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundResource(R.drawable.btn_content_mode_selected)
            button.setTextColor(android.graphics.Color.RED)
            button.setTypeface(button.typeface, android.graphics.Typeface.BOLD)
        } else {
            button.setBackgroundResource(R.drawable.btn_content_mode_unselected)
            button.setTextColor(0xFF666666.toInt())
            button.setTypeface(button.typeface, android.graphics.Typeface.NORMAL)
        }
    }
}
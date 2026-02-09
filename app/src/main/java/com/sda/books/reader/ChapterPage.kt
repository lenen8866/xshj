package com.sda.books.reader

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.EdgeEffect
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.book.reader.R
import com.sda.books.reader.adapter.ChapterContentAdapter
import com.book.reader.databinding.ActivityChapterBinding
import com.sda.books.reader.db.DatabaseHelper
import com.sda.books.reader.entity.ChapterContentItem
import com.sda.books.reader.store.StoreManager
import com.sda.books.reader.util.AppSettingUtil
import com.sda.books.reader.util.Constant
import com.sda.books.reader.util.getChapterContentShowList
import com.sda.books.reader.event.EventBus
import kotlinx.coroutines.flow.collectLatest
import com.gyf.immersionbar.ImmersionBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.reflect.Field


class ChapterPage : BaseActivity() {

    companion object {
        private const val EXTRA_VOLUME_TITLE = "volumeTitle"
        private const val EXTRA_CHAPTER_TITLE = "chapterTitle"
        private const val EXTRA_CHAPTER_ID = "chapterId"
        private const val EXTRA_SCROLL_INDEX = "scrollIndex"
        private const val EXTRA_TABS = "tabs"
        private const val EXTRA_MATCH_CONTENT = "matchContent"
        private const val EXTRA_TARGET_LINE_TEXT = "targetLineText"
        private const val EXTRA_TEMP_CONTENT_MODE = "tempContentMode"

        fun start(
            activity: Activity,
            volumeTitle: String,
            chapterTitle: String,
            chapterId: Int,
            scrollIndex: Int = 0,
            tabs: String,
            matchContent: ArrayList<String> = arrayListOf(),
            targetLineText: String = "",
            tempContentMode: Int = -1,
        ) {
            activity.startActivity(Intent(activity, ChapterPage::class.java).apply {
                putExtra(EXTRA_VOLUME_TITLE, volumeTitle)
                putExtra(EXTRA_CHAPTER_TITLE, chapterTitle)
                putExtra(EXTRA_CHAPTER_ID, chapterId)
                putExtra(EXTRA_SCROLL_INDEX, scrollIndex)
                putExtra(EXTRA_TABS, tabs)
                putStringArrayListExtra(EXTRA_MATCH_CONTENT, matchContent)
                putExtra(EXTRA_TARGET_LINE_TEXT, targetLineText)
                putExtra(EXTRA_TEMP_CONTENT_MODE, tempContentMode)
            })
        }
    }

    lateinit var binding: ActivityChapterBinding
    var volumeTitle = ""
    var chapterTitle = ""
    var tabs = ""
    var chapterId = 0
    lateinit var matchContent: ArrayList<String>
    lateinit var adapter: ChapterContentAdapter
    var scrollIndex = 0
    var stopLineCount = 0
    var totalLineCount = 0
    private var targetLineText: String = ""
    private var tempContentMode: Int = -1
    override fun getRootView(): View {
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeTitle = intent.getStringExtra(EXTRA_VOLUME_TITLE) ?: ""
        chapterTitle = intent.getStringExtra(EXTRA_CHAPTER_TITLE) ?: ""
        tabs = intent.getStringExtra(EXTRA_TABS) ?: ""
        chapterId = intent.getIntExtra(EXTRA_CHAPTER_ID, -1)
        scrollIndex = intent.getIntExtra(EXTRA_SCROLL_INDEX, 0)
        targetLineText = intent.getStringExtra(EXTRA_TARGET_LINE_TEXT) ?: ""
        tempContentMode = intent.getIntExtra(EXTRA_TEMP_CONTENT_MODE, -1)

        // 日志：记录接收到的参数
        LogUtils.e("ChapterPage: onCreate接收参数, chapterId=$chapterId, chapterTitle=$chapterTitle, intentTabs=$tabs")
        // 根据内容显示模式调整 scrollIndex
        // contentMode: 0=中, 1=双, 2=EN
        val contentMode = if (tempContentMode >= 0) tempContentMode else AppSettingUtil.getContentMode()
        if (contentMode == 0) {
            // 中文模式：双语内容会被过滤，需要调整索引
            scrollIndex = (scrollIndex + 1) / 2
        }
        // 双模式和EN模式不需要调整，因为内容行数对应关系一致

        matchContent = intent.getStringArrayListExtra(EXTRA_MATCH_CONTENT) ?: arrayListOf()



        binding = ActivityChapterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 处理卷标题显示
        binding.ivCe.visibility = if (volumeTitle.endsWith("E")) View.VISIBLE else View.GONE
        volumeTitle = volumeTitle.replace("E", "")
        binding.tvVolumeTitle.text = volumeTitle.split("(")[0]
        volumeTitle.split("(").getOrNull(1)?.replace(")", "")?.let {
            binding.tvAuthor.text = it
        }

        // 先不设置分类标题，等待数据库查询结果
        binding.tvVolumeTitles.text = ""
        binding.tvChapterTitle.text = chapterTitle
        binding.tvZj.text = volumeTitle
        binding.tvTitle.text = chapterTitle



        adapter = ChapterContentAdapter(matchContent)
        binding.chapterContentList.adapter = adapter
        binding.chapterContentList.layoutManager = LinearLayoutManager(this)
        setupEdgeEffect()
        
        // 查询并更新分类信息（优先使用数据库反查）
        queryAndUpdateCategoryTabs()
        
        queryItemContent()
        
        // 统一处理返回按钮点击
        listOf(binding.ivBack, binding.tvVolumeTitle, binding.tvVolumeTitles).forEach {
            it.setOnClickListener { finish() }
        }




        binding.audioSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 0
                player?.seekTo((progress / 100f * durationLenght).toLong())
            }
        })

        binding.ivControl.setOnClickListener {
            player?.let {
                if (it.isPlaying) {
                    it.pause()
                    binding.ivControl.setImageResource(R.drawable.play1)
                } else {
                    it.play()
                    binding.ivControl.setImageResource(R.drawable.pause1)
                }
            }
        }


        lifecycleScope.launch {
            EventBus.chapterHeightFlow.collectLatest { height ->
                // 搜索模式下不使用 EventBus 滚动，避免与 scrollToSearchResult 冲突导致光标乱跑
                if (matchContent.isEmpty() && scrollIndex > 0 && totalLineCount > 0) {
                    // 让目标尽量落在屏幕中间：在原有滚动距离基础上减去半屏高度
                    val viewportHalf = (binding.chapterContentList.height / 2f).toInt()
                    val dy = (stopLineCount * height / totalLineCount) - viewportHalf
                    binding.chapterContentList.smoothScrollBy(0, dy.coerceAtLeast(0))
                }
            }
        }

    }

    /**
     * 查询并更新分类信息（优先使用数据库反查，不依赖传入的tabs）
     */
    private fun queryAndUpdateCategoryTabs() {
        if (chapterId <= 0) {
            LogUtils.e("ChapterPage: chapterId无效，跳过分类查询, chapterId=$chapterId")
            // chapterId无效时，使用fallback逻辑
            handleCategoryTabsFallback()
            return
        }
        
        LogUtils.e("ChapterPage: 开始查询分类路径, chapterId=$chapterId")
        
        lifecycleScope.launch {
            try {
                DatabaseHelper.getInstance().openDatabase()
                
                // 优先使用数据库反查分类路径（优先使用parentId）
                val categoryPath = DatabaseHelper.getInstance().queryCategoryPathByChapterId(chapterId) ?: ""
                
                if (categoryPath.isNotBlank() || tabs.isNotBlank()) {
                    // 查询成功或 Intent 里本身就带有分类信息：
                    // - 优先用“带有二级分类(包含 - )”的那一个
                    //   比如：tabs = "圣经-旧约"，categoryPath = "圣经" => 选 "圣经-旧约"
                    // - 都没有二级分类时，就用不为空的那个
                    val bestTabs = chooseBestTabs(tabs, categoryPath)
                    tabs = bestTabs
                    binding.tvVolumeTitles.text = formatTabsForDisplay(bestTabs)
                    LogUtils.e("ChapterPage: 更新分类标题成功, intentTabs=$tabs, dbPath=$categoryPath, use=$bestTabs")
                } else {
                    // 查询失败，使用fallback逻辑
                    LogUtils.e("ChapterPage: 数据库查询失败，使用fallback逻辑")
                    handleCategoryTabsFallback()
                }
            } catch (e: Exception) {
                LogUtils.e("ChapterPage: 查询分类信息异常: ${e.message}", e)
                // 查询异常，使用fallback逻辑
                handleCategoryTabsFallback()
            }
        }
    }
    
    /**
     * Fallback逻辑：
     *  - 优先避免展示“全部”作为子分类
     *  - 如果 tabs 为 "父-全部" 或 "全部"，则只展示父分类
     *  - 其它情况直接使用传入的 tabs
     */
    private fun handleCategoryTabsFallback() {
        val intentTabs = intent.getStringExtra(EXTRA_TABS) ?: ""
        
        if (intentTabs.isBlank()) {
            // 传入的tabs为空，保持为空
            LogUtils.e("ChapterPage: fallback时传入tabs为空，不显示")
            binding.tvVolumeTitles.text = ""
            tabs = ""
            return
        }

        // 如果是 "父-全部" 或只包含 "全部"，则只展示父分类，避免误导
        if (intentTabs.contains("全部")) {
            // 尝试按照 "-" 拆分，例如 "圣经-全部"
            val parts = intentTabs.split("-", limit = 2)
            val parent = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: ""
            if (parent.isNotBlank()) {
                tabs = parent
                // 这里只展示父分类本身，例如 “圣经”
                binding.tvVolumeTitles.text = formatTabsForDisplay(tabs)
                LogUtils.e("ChapterPage: fallback检测到'全部'，仅展示父分类, parent=$parent, rawTabs=$intentTabs")
            } else {
                // 拆不出父分类时，不显示
                binding.tvVolumeTitles.text = ""
                tabs = ""
                LogUtils.e("ChapterPage: fallback检测到'全部'但无法解析父分类, rawTabs=$intentTabs")
            }
            return
        }

        // 传入的tabs有效且不包含"全部"，直接使用（只调整展示格式：父-子 => 父 · 子）
        tabs = intentTabs
        binding.tvVolumeTitles.text = formatTabsForDisplay(tabs)
        LogUtils.e("ChapterPage: fallback使用传入的tabs, tabs=$tabs")
    }

    /**
     * 小小的展示格式处理：
     *  原始 tabs 一般是 "父-子" 这样的字符串
     *  这里把它变成 "父 · 子"，只影响显示，不改动原始数据
     */
    private fun formatTabsForDisplay(tabs: String): String {
        if (tabs.isBlank()) return ""
        // 按 "-" 拆分，只关心前两个层级：例如 "圣经-旧约-创世记" 也只取前两个
        val parts = tabs.split("-", limit = 3)
        val first = parts.getOrNull(0)?.trim().orEmpty()
        val second = parts.getOrNull(1)?.trim()
        return if (!second.isNullOrEmpty()) {
            "$first · $second"
        } else {
            first
        }
    }

    /**
     * 从 Intent 自带的 tabs 和 数据库反查的 categoryPath 里，选一个“信息更全的”：
     * - 谁包含 "-"（说明有“一级-二级”），就优先用谁
     * - 都有 "-" 时，优先用 Intent 传进来的（和搜索结果页保持一致）
     * - 都没有 "-" 时，谁不为空就用谁
     */
    private fun chooseBestTabs(intentTabs: String, categoryPath: String): String {
        val hasIntentSecond = intentTabs.contains("-")
        val hasDbSecond = categoryPath.contains("-")

        return when {
            hasIntentSecond && hasDbSecond -> intentTabs
            hasIntentSecond -> intentTabs
            hasDbSecond -> categoryPath
            intentTabs.isNotBlank() -> intentTabs
            else -> categoryPath
        }
    }

    /**
     * 设置 RecyclerView 边缘效果颜色
     */
    private fun setupEdgeEffect() {
        val edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(recyclerView: RecyclerView, direction: Int): EdgeEffect {
                val edgeEffect = super.createEdgeEffect(recyclerView, direction)
                edgeEffect.color = ContextCompat.getColor(recyclerView.context, R.color.text_dark)
                return edgeEffect
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.chapterContentList.edgeEffectFactory = edgeEffectFactory
        } else {
            try {
                val field = RecyclerView::class.java.getDeclaredField("mEdgeEffectFactory")
                field.isAccessible = true
                field.set(binding.chapterContentList, edgeEffectFactory)
            } catch (e: Exception) {
                Log.e("EdgeEffectFactory", "Failed to set edge effect color on older API", e)
            }
        }
    }
    
    private fun queryItemContent(){
        lifecycleScope.launch {
            DatabaseHelper.getInstance().queryItemChapterContent(chapterId, matchContent,
                onSuccess = { content, music ->
                    if (music.isNotEmpty()) {
                        initializePlayer(music)
                    }
                    val modeToUse = if (tempContentMode >= 0) tempContentMode else AppSettingUtil.getContentMode()
                    val chapterContentItemList = getChapterContentShowList(content, modeToUse)
                    // 精准定位：
                    // - 优先使用"点击的那一行原文(targetLineText)"在章节内容中定位
                    // - 找不到时才退回使用 scrollIndex（老逻辑）
                    fun normalizeForMatch(s: String): String {
                        return s.replace("*", "").trim()
                    }

                    val targetIndexForScroll = run {
                        if (chapterContentItemList.isEmpty()) {
                            0
                        } else {
                            val hint = scrollIndex.coerceIn(0, chapterContentItemList.lastIndex)
                            val target = normalizeForMatch(targetLineText)

                            if (target.isNotBlank()) {
                                // 先精确匹配整行，再用 contains 做弱匹配
                                val exact = chapterContentItemList.indices.filter { i ->
                                    normalizeForMatch(chapterContentItemList[i].getShowContent()) == target
                                }
                                val candidates = if (exact.isNotEmpty()) exact else chapterContentItemList.indices.filter { i ->
                                    normalizeForMatch(chapterContentItemList[i].getShowContent()).contains(target)
                                }

                                if (candidates.isNotEmpty()) {
                                    candidates.minBy { i -> kotlin.math.abs(i - hint) }
                                } else {
                                    hint
                                }
                            } else {
                                hint
                            }
                        }
                    }

                    // 只高亮"本次点击"对应的命中点，并以其作为滚动基准
                    val highlightIndex = targetIndexForScroll
                    scrollIndex = targetIndexForScroll
                    val nData = mutableListOf<ChapterContentItem>()

                    // 段落间距：使用 <br> 标签实现
                    val contentMode = modeToUse
                    val sectionSpace = AppSettingUtil.getTextSectionHLetterSpacing()
                    val brCount = if (contentMode == 0) 1 else sectionSpace.coerceAtLeast(1)
                    val pHeight = "<br>".repeat(brCount)

                    // === 全文拼接方案：将所有文本段落拼接到单个 Item 中，支持跨段落自由选择复制 ===
                    var insertContent = ""
                    var insertChapterContentItem = ChapterContentItem()

                    for (index in 0 until chapterContentItemList.size) {
                        val item = chapterContentItemList[index]
                        if (item.isImg()) {
                            // 遇到图片时，先保存之前积累的文本为一个 Item
                            if (insertContent.isNotEmpty()) {
                                val textItem = ChapterContentItem()
                                textItem.setUserContent(insertContent)
                                nData.add(textItem)
                                insertContent = ""
                            }
                            nData.add(item)
                        } else {
                            val strTxt = item.getShowContent()
                            if (strTxt.contains(".mp3")) {
                                continue
                            }

                            val vFristSpace = AppSettingUtil.getTextFristLetterSpacing()
                            var indentSpaces = "\u3000".repeat(vFristSpace) // 全角空格
                            if (strTxt.startsWith("*")) {
                                indentSpaces = ""
                            }

                            // 搜索结果跳转到内容页：只展示"本次点击"对应的命中点高亮
                            val processedText = if (matchContent.isNotEmpty() && index == highlightIndex) {
                                processTextHighlight(strTxt, matchContent, true)
                            } else {
                                strTxt.replace("*", "")
                            }

                            // 拼接到 insertContent 中，用 <br> 分隔段落
                            insertContent += "$indentSpaces$processedText$pHeight"

                            // 计算行数（用于滚动定位）
                            if (matchContent.isNotEmpty()) {
                                val lineCount = calculateLineCount(strTxt)
                                if (index <= scrollIndex) {
                                    stopLineCount += lineCount
                                }
                                totalLineCount += lineCount
                            }
                        }

                        // 最后一项：将剩余的拼接文本存为一个 Item
                        if (index == chapterContentItemList.size - 1 && insertContent.isNotEmpty()) {
                            insertChapterContentItem.setUserContent(insertContent)
                            nData.add(insertChapterContentItem)
                        }
                    }

                    // 更新适配器数据
                    adapter.updateData(nData)

                    // 滚动到目标位置
                    if (matchContent.isNotEmpty()) {
                        scrollToSearchResult(nData)
                    } else if (scrollIndex > 0) {
                        lifecycleScope.launch {
                            delay(500)
                            scrollLineHeight(scrollIndex * AppSettingUtil.getTextSectionHLetterSpacing(), stopLineCount)
                        }
                    }
                },
                onFail = { } )
        }
    }

    /**
     * 滚动到搜索结果位置
     * 由于采用全文拼接方案（所有段落拼接到单个 Item），需要精确定位到 Item 内关键词的 Y 坐标
     */
    private fun scrollToSearchResult(nData: List<ChapterContentItem>) {
        binding.chapterContentList.post {
            lifecycleScope.launch {
                delay(300)

                // 优先找包含高亮标记 <font color='red'> 的 Item，其次找包含所有关键词的 Item
                val targetIndex = nData.indexOfFirst { item ->
                    val txt = item.getUserContent()
                    txt.isNotBlank() && txt.contains("<font color='red'>", ignoreCase = true)
                }.takeIf { it >= 0 }
                    ?: nData.indexOfFirst { item ->
                        val txt = item.getUserContent()
                        txt.isNotBlank() && matchContent.all { key ->
                            val k = key.trim()
                            k.isEmpty() || txt.contains(k, ignoreCase = true)
                        }
                    }.takeIf { it >= 0 }
                    ?: 0

                val layoutManager = binding.chapterContentList.layoutManager as? LinearLayoutManager ?: return@launch
                if (targetIndex !in 0 until adapter.itemCount) return@launch

                // Step 1: 先将目标 Item 滚动到屏幕顶部
                layoutManager.scrollToPositionWithOffset(targetIndex, 0)

                // Step 2: 等待 layout 完成
                delay(500)

                // Step 3: 在渲染后的 TextView 中精确定位关键词的 Y 坐标
                val viewHolder = binding.chapterContentList.findViewHolderForAdapterPosition(targetIndex)
                val tvContent = viewHolder?.itemView?.findViewById<TextView>(R.id.tv_content)

                if (tvContent != null && tvContent.layout != null) {
                    val viewportHeight = binding.chapterContentList.height
                    var keywordLineTop = -1

                    // 方法1：通过 ForegroundColorSpan 查找高亮位置（最精确，只定位被高亮的那个关键词）
                    val text = tvContent.text
                    if (text is Spanned) {
                        val spans = text.getSpans(0, text.length, ForegroundColorSpan::class.java)
                        if (spans.isNotEmpty()) {
                            val spanStart = text.getSpanStart(spans[0])
                            val line = tvContent.layout.getLineForOffset(spanStart)
                            keywordLineTop = tvContent.layout.getLineTop(line)
                        }
                    }

                    // 方法2：如果方法1未找到，使用关键词文本搜索（兜底）
                    if (keywordLineTop < 0) {
                        val plainText = tvContent.text.toString()
                        val keyword = matchContent.firstOrNull()?.trim() ?: ""
                        if (keyword.isNotEmpty()) {
                            val keywordPos = plainText.indexOf(keyword, ignoreCase = true)
                            if (keywordPos >= 0) {
                                val line = tvContent.layout.getLineForOffset(keywordPos)
                                keywordLineTop = tvContent.layout.getLineTop(line)
                            }
                        }
                    }

                    // 滚动到关键词位置（关键词居于屏幕约 1/3 处）
                    if (keywordLineTop > 0) {
                        val scrollY = keywordLineTop - viewportHeight / 3
                        if (scrollY > 0) {
                            binding.chapterContentList.scrollBy(0, scrollY)
                        }
                    }
                }

                adapter.highlightPosition(targetIndex)
            }
        }
    }
    
    private fun scrollLineHeight(index: Int, length: Int) {
        val height = (index + length) * SPUtils.getInstance().getInt(Constant.TextViewLineHeight, 0)
        // 预留接口，当前不执行实际滚动
    }


    private fun String.isAllEnglishAndSymbols(): Boolean {
        return matches(Regex("^[a-zA-Z\\x20-\\x7E]+$"))
    }


    // 媒体播放器
    var isPrepare = false
    var durationLenght = 1L
    private val handler = Handler(Looper.getMainLooper())
    private val run = Runnable {
        getAudioProgress()
    }

    private fun getAudioProgress() {
        val currentPosition = player?.currentPosition ?: 0
        binding.current.text = formatMillisecondsToHMS(currentPosition)
        handler.postDelayed(run, 1000)
        binding.audioSeekBar.progress = (currentPosition / (durationLenght * 1f) * 100).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(run)
        player?.release()
    }


    private fun formatMillisecondsToHMS(milliseconds: Long): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        val hours = (milliseconds / (1000 * 60 * 60)) % 24
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }


    var player: ExoPlayer? = null

    /**
     * 初始化播放器
     */
    private fun initializePlayer(music: String) {
        val isDownload = StoreManager.checkFileIsDownLoadFinish(music)
        
        if (!isDownload) {
            StoreManager.startDownLoad(music)
        }
        
        player = ExoPlayer.Builder(this).build().apply {
            val mediaItem = if (isDownload) {
                MediaItem.fromUri(Uri.fromFile(StoreManager.getDesFile(music)))
            } else {
                MediaItem.fromUri(music)
            }
            setMediaItem(mediaItem)
            playWhenReady = false
            
            if (music.endsWith("mp4")) {
                binding.playerView.player = this
            }
            prepare()
            
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isPrepare = true
                            durationLenght = duration
                            
                            if (music.endsWith("mp4")) {
                                binding.playerView.visibility = View.VISIBLE
                            } else {
                                binding.audioContainer.visibility = View.VISIBLE
                                binding.duration.text = formatMillisecondsToHMS(durationLenght)
                                getAudioProgress()
                            }
                        }
                        Player.STATE_ENDED -> {
                            binding.ivControl.setImageResource(R.drawable.play1)
                            binding.audioSeekBar.progress = 0
                        }
                    }
                }
            })
        }
    }


    override fun onResume() {
        super.onResume()
        binding.main.setBackgroundColor(Color.parseColor(SPUtils.getInstance().getString(Constant.Background_Color)))
        
        val statusBarColor = when (SPUtils.getInstance().getInt(Constant.ThemeColorIndex, 0)) {
            0 -> R.color.title_1
            1 -> R.color.title_2
            2 -> R.color.title_3
            3 -> R.color.title_4
            else -> R.color.title_1
        }
        
        ImmersionBar.with(this)
            .transparentStatusBar()
            .transparentBar()
            .transparentNavigationBar()
            .statusBarColor(statusBarColor)
            .statusBarDarkFont(true)
            .navigationBarDarkIcon(true)
            .autoDarkModeEnable(true)
            .init()
    }
    
    /**
     * 检查文本中是否包含独立的单个字母（不在单词内部）
     * @param text 要检查的文本
     * @param letter 要查找的字母（会同时匹配大小写）
     * @return 如果找到独立的字母则返回 true
     */
    private fun containsStandaloneLetter(text: String, letter: Char): Boolean {
        val lowerLetter = letter.lowercaseChar()
        val upperLetter = letter.uppercaseChar()
        
        for (i in text.indices) {
            val c = text[i]
            if (c == lowerLetter || c == upperLetter) {
                val prevChar = if (i > 0) text[i - 1] else ' '
                val prevIsEnglishLetter = prevChar in 'A'..'Z' || prevChar in 'a'..'z'
                val nextChar = if (i < text.length - 1) text[i + 1] else ' '
                val nextIsEnglishLetter = nextChar in 'A'..'Z' || nextChar in 'a'..'z'
                
                if (!prevIsEnglishLetter && !nextIsEnglishLetter) {
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * 检查文本是否包含关键词（区分大小写，单个字母启用单词边界）
     */
    private fun containsWordIgnoreCase(text: String, word: String): Boolean {
        if (word.length == 1 && (word[0] in 'a'..'z' || word[0] in 'A'..'Z')) {
            return containsStandaloneLetter(text, word[0])
        }
        return text.contains(word)
    }
    
    /**
     * 高亮独立的单个字母（不在单词内部的字母，大小写都高亮）
     */
    private fun highlightStandaloneLetter(text: String, letter: Char): String {
        val lowerLetter = letter.lowercaseChar()
        val upperLetter = letter.uppercaseChar()
        val result = StringBuilder()
        
        var i = 0
        while (i < text.length) {
            // 跳过已有的 HTML 标签
            if (text.startsWith("<font", i)) {
                val endIdx = text.indexOf("</font>", i)
                if (endIdx != -1) {
                    result.append(text.substring(i, endIdx + 7))
                    i = endIdx + 7
                    continue
                }
            }
            
            val c = text[i]
            if (c == lowerLetter || c == upperLetter) {
                val prevChar = if (i > 0) text[i - 1] else ' '
                val prevIsEnglishLetter = prevChar in 'A'..'Z' || prevChar in 'a'..'z'
                val nextChar = if (i < text.length - 1) text[i + 1] else ' '
                val nextIsEnglishLetter = nextChar in 'A'..'Z' || nextChar in 'a'..'z'
                
                if (!prevIsEnglishLetter && !nextIsEnglishLetter) {
                    result.append("<font color='red'>$c</font>")
                } else {
                    result.append(c)
                }
            } else {
                result.append(c)
            }
            i++
        }
        return result.toString()
    }

    /**
     * 高亮完整英文单词（忽略大小写，必须是完整单词）
     */
    private fun highlightWholeWordIgnoreCase(text: String, word: String): String {
        if (word.isBlank()) return text
        
        val lowerWord = word.lowercase()
        val result = StringBuilder()
        var i = 0
        
        while (i < text.length) {
            // 跳过已有的 HTML 标签
            if (text.startsWith("<font", i)) {
                val endIdx = text.indexOf("</font>", i)
                if (endIdx != -1) {
                    result.append(text.substring(i, endIdx + 7))
                    i = endIdx + 7
                    continue
                }
            }
            
            // 动态获取当前位置的小写文本（不包含已处理部分）
            val remainingText = text.substring(i)
            val lowerRemainingText = remainingText.lowercase()
            val idx = lowerRemainingText.indexOf(lowerWord)
            
            if (idx == -1) {
                result.append(remainingText)
                break
            }
            
            // 计算在原文本中的实际位置
            val actualIdx = i + idx
            val before = if (actualIdx > 0) text[actualIdx - 1] else ' '
            val afterIndex = actualIdx + word.length
            val after = if (afterIndex < text.length) text[afterIndex] else ' '
            val beforeIsLetter = before in 'A'..'Z' || before in 'a'..'z'
            val afterIsLetter = after in 'A'..'Z' || after in 'a'..'z'
            
            if (!beforeIsLetter && !afterIsLetter) {
                // 找到完整单词，高亮
                result.append(remainingText.substring(0, idx))
                result.append("<font color='red'>")
                result.append(text.substring(actualIdx, actualIdx + word.length))
                result.append("</font>")
                i = actualIdx + word.length
            } else {
                // 不是完整单词，继续查找
                result.append(remainingText.substring(0, idx + 1))
                i = actualIdx + 1
            }
        }
        return result.toString()
    }

    /**
     * 检查是否包含完整英文单词（忽略大小写，必须是完整单词）
     */
    private fun containsWholeWordIgnoreCase(text: String, word: String): Boolean {
        if (word.isBlank()) return false
        val lowerText = text.lowercase()
        val lowerWord = word.lowercase()
        var start = 0
        while (true) {
            val idx = lowerText.indexOf(lowerWord, start)
            if (idx == -1) return false
            val before = if (idx > 0) text[idx - 1] else ' '
            val afterIndex = idx + word.length
            val after = if (afterIndex < text.length) text[afterIndex] else ' '
            val beforeIsLetter = before in 'A'..'Z' || before in 'a'..'z'
            val afterIsLetter = after in 'A'..'Z' || after in 'a'..'z'
            if (!beforeIsLetter && !afterIsLetter) return true
            start = idx + word.length
        }
    }
    
    /**
     * 高亮关键词（区分大小写，单个字母启用单词边界）
     */
    private fun highlightWordIgnoreCase(originalText: String, targetWord: String): String {
        if (targetWord.length == 1 && (targetWord[0] in 'a'..'z' || targetWord[0] in 'A'..'Z')) {
            return highlightStandaloneLetter(originalText, targetWord[0])
        }
        val pattern = Regex(Regex.escape(targetWord))
        return originalText.replace(pattern) { matchResult ->
            "<font color='red'>${matchResult.value}</font>"
        }
    }
    
    /**
     * 统一的高亮处理函数
     * @param text 原始文本
     * @param keywords 关键词列表
     * @param isHighlightIndex 是否为需要高亮的目标索引
     * @return 处理后的文本
     */
    private fun processTextHighlight(
        text: String,
        keywords: List<String>,
        isHighlightIndex: Boolean
    ): String {
        if (keywords.isEmpty() || !isHighlightIndex) {
            return text.replace("*", "")
        }
        
        var result = text
        
        // 分离单个英文字母和其他关键词
        val singleLetters = keywords.map { it.trim() }.filter { it.length == 1 && (it[0] in 'a'..'z' || it[0] in 'A'..'Z') }
        val otherKeywords = keywords.map { it.trim() }.filter { it.isNotEmpty() && (it.length != 1 || (it[0] !in 'a'..'z' && it[0] !in 'A'..'Z')) }
        
        // 检查是否有任何匹配（用于决定是否需要高亮）
        val hasAnyHit = singleLetters.any { containsStandaloneLetter(text, it[0]) } ||
                        otherKeywords.any { keyword ->
                            if (hasEnglishChars(keyword)) {
                                containsWholeWordIgnoreCase(text, keyword)
                            } else {
                                text.contains(keyword)
                            }
                        }
        
        if (!hasAnyHit) return text.replace("*", "")

        // 先处理单个英文字母（无论内容是中文还是英文，都需要单词边界过滤）
        singleLetters.forEach { kw ->
            result = highlightStandaloneLetter(result, kw[0])
        }
        
        // 再处理其他关键词（直接匹配）
        otherKeywords.forEach { keyword ->
            result = if (hasEnglishChars(keyword)) {
                highlightWholeWordIgnoreCase(result, keyword)
            } else {
                result.replace(keyword, "<font color='red'>$keyword</font>")
            }
        }
        
        return result.replace("*", "")
    }

    private fun hasEnglishChars(text: String): Boolean {
        return text.any { it in 'A'..'Z' || it in 'a'..'z' }
    }
    
    /**
     * 构建章节内容数据
     */
    private fun buildChapterContentData(
        chapterContentItemList: List<ChapterContentItem>,
        highlightIndex: Int,
        pHeight: String
    ): MutableList<ChapterContentItem> {
        val nData = mutableListOf<ChapterContentItem>()
        var insertContent = ""
        var insertChapterContentItem = ChapterContentItem()
        
        chapterContentItemList.forEachIndexed { index, item ->
            when {
                item.isImg() -> {
                    if (insertContent.isNotEmpty()) {
                        nData.add(ChapterContentItem().apply { setUserContent(insertContent) })
                    }
                    nData.add(item)
                    insertContent = ""
                }
                !item.getShowContent().contains(".mp3") -> {
                    val strTxt = item.getShowContent()
                    val indentSpaces = if (strTxt.startsWith("*")) "" 
                        else "\u3000".repeat(AppSettingUtil.getTextFristLetterSpacing())
                    
                    val processedText = if (matchContent.isNotEmpty() && index == highlightIndex) {
                        processTextHighlight(strTxt, matchContent, true)
                    } else {
                        strTxt.replace("*", "")
                    }
                    insertContent += "$indentSpaces$processedText$pHeight"

                    // 计算行数（用于滚动定位）
                    if (matchContent.isNotEmpty()) {
                        val lineCount = calculateLineCount(strTxt)
                        if (index <= scrollIndex) {
                            stopLineCount += lineCount
                        }
                        totalLineCount += lineCount
                    }
                }
            }
        }
        
        // 添加最后一段内容
        if (insertContent.isNotEmpty()) {
            insertChapterContentItem.setUserContent(insertContent)
            nData.add(insertChapterContentItem)
        }
        
        return nData
    }
    
    /**
     * 查找高亮索引（在 scrollIndex 附近找包含所有关键词的段落）
     */
    private fun findHighlightIndex(chapterContentItemList: List<ChapterContentItem>): Int {
        if (matchContent.isEmpty() || chapterContentItemList.isEmpty()) {
            return scrollIndex
        }
        
        val window = 20
        val start = (scrollIndex - window).coerceAtLeast(0)
        val end = (scrollIndex + window).coerceAtMost(chapterContentItemList.lastIndex)
        
        var bestIndex = scrollIndex.coerceIn(0, chapterContentItemList.lastIndex)
        var bestDistance = Int.MAX_VALUE
        
        for (i in start..end) {
            val txt = chapterContentItemList[i].getShowContent()
            if (txt.isBlank()) continue
            
            val hit = matchContent.all { key ->
                val k = key.trim()
                k.isEmpty() || txt.contains(k, ignoreCase = true)
            }
            
            if (hit) {
                val d = kotlin.math.abs(i - scrollIndex)
                if (d < bestDistance) {
                    bestDistance = d
                    bestIndex = i
                    if (d == 0) break
                }
            }
        }
        return bestIndex
    }
    
    /**
     * 计算文本行数（用于滚动定位）
     */
    private fun calculateLineCount(content: String): Int {
        val lineCountPerScreen = SPUtils.getInstance().getInt(Constant.LineCount)
        return if (content.isAllEnglishAndSymbols()) {
            content.length / 2 / lineCountPerScreen + 1
        } else {
            content.length / lineCountPerScreen
        }
    }

}
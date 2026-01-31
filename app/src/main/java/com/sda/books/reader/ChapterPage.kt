package com.sda.books.reader

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.EdgeEffect
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
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
        
        fun start(
            activity: Activity,
            volumeTitle: String,
            chapterTitle: String,
            chapterId: Int,
            scrollIndex: Int = 0,
            tabs: String,
            matchContent: ArrayList<String> = arrayListOf(),
        ) {
            activity.startActivity(Intent(activity, ChapterPage::class.java).apply {
                putExtra(EXTRA_VOLUME_TITLE, volumeTitle)
                putExtra(EXTRA_CHAPTER_TITLE, chapterTitle)
                putExtra(EXTRA_CHAPTER_ID, chapterId)
                putExtra(EXTRA_SCROLL_INDEX, scrollIndex)
                putExtra(EXTRA_TABS, tabs)
                putStringArrayListExtra(EXTRA_MATCH_CONTENT, matchContent)
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
        
        // 日志：记录接收到的参数
        LogUtils.e("ChapterPage: onCreate接收参数, chapterId=$chapterId, chapterTitle=$chapterTitle, intentTabs=$tabs")
        // 根据内容显示模式调整 scrollIndex
        // contentMode: 0=中, 1=双, 2=EN
        val contentMode = AppSettingUtil.getContentMode()
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
                if (scrollIndex > 0 && totalLineCount > 0) {
                    binding.chapterContentList.smoothScrollBy(0, (stopLineCount * height / totalLineCount))
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
                    val chapterContentItemList = getChapterContentShowList(content)
                    // 纠偏：由于数据源拼接/双语过滤等原因，scrollIndex 可能和实际段落 index 存在偏移
                    // 这里在 scrollIndex 附近找“真正包含关键词”的段落作为唯一高亮目标
                    val highlightIndex = run {
                        if (matchContent.isEmpty() || chapterContentItemList.isEmpty()) {
                            scrollIndex
                        } else {
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
                                    if (k.isEmpty()) true else txt.contains(k, ignoreCase = true)
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
                            bestIndex
                        }
                    }
                    val nData = mutableListOf<ChapterContentItem>()
                    // 段落间距：中文模式下只保留 1 行空行，其它模式按设置值
                    val contentMode = AppSettingUtil.getContentMode()
                    val sectionSpace = AppSettingUtil.getTextSectionHLetterSpacing()
                    val brCount = if (contentMode == 0) 1 else sectionSpace.coerceAtLeast(1)
                    val pHeight = "<br>".repeat(brCount)
                    
                    var insertContent = ""
                    var insertChapterContentItem = ChapterContentItem()
                    for (index in 0 until chapterContentItemList.size) {
                        if (chapterContentItemList[index].isImg()) {
                            if (insertContent != "") {
                                var insertChapterContentItem1 = ChapterContentItem()
                                insertChapterContentItem1.setUserContent(insertContent)
                                //insertChapterContentItem1.isCn() = true

                                nData.add(insertChapterContentItem1)
                            }

                            nData.add(chapterContentItemList[index])
                            insertContent = ""
                        } else {

                            if (chapterContentItemList[index].getShowContent()
                                    .contains(".mp3")
                            ) {

                            } else {
                                val  strTxt = chapterContentItemList[index].getShowContent()


                                var   vFristSpace = AppSettingUtil.getTextFristLetterSpacing()
                                var indentSpaces = "\u3000".repeat(vFristSpace) // 全角空格
                                if(strTxt.startsWith("*")){
                                    indentSpaces = ""
                                }

                                // 搜索结果跳转到内容页：只展示“本次点击”对应的命中点高亮
                                // 其它命中结果不再显示（仅保留 highlightIndex 这一条的高亮/提示）
                                // 搜索结果跳转到内容页：只展示"本次点击"对应的命中点高亮
                                val processedText = if (matchContent.isNotEmpty() && index == highlightIndex) {
                                    processTextHighlight(strTxt, matchContent, true)
                                } else {
                                    strTxt.replace("*", "")
                                }
                                insertContent += "$indentSpaces$processedText$pHeight"

                                //insertContent += "$indentSpaces${chapterContentItemList[index].getShowContent()}<br>"

                                // 计算行数（用于滚动定位）
                                if (matchContent.isNotEmpty()) {
                                    val content = chapterContentItemList[index].getShowContent()
                                    val lineCount = calculateLineCount(content)
                                    
                                    if (index <= scrollIndex) {
                                        stopLineCount += lineCount
                                    }
                                    totalLineCount += lineCount
                                }
                            }

                        }

                        if (index == chapterContentItemList.size - 1){
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
     */
    private fun scrollToSearchResult(nData: List<ChapterContentItem>) {
        binding.chapterContentList.post {
            lifecycleScope.launch {
                delay(300)
                
                val targetIndex = nData.indexOfFirst { item ->
                    val txt = item.getUserContent()
                    txt.isNotBlank() && matchContent.all { key ->
                        val k = key.trim()
                        k.isEmpty() || txt.contains(k, ignoreCase = true)
                    }
                }.takeIf { it >= 0 } ?: 0
                
                val layoutManager = binding.chapterContentList.layoutManager as? LinearLayoutManager
                if (layoutManager != null && targetIndex in 0 until adapter.itemCount) {
                    layoutManager.scrollToPositionWithOffset(targetIndex, binding.root.height / 4)
                    adapter.highlightPosition(targetIndex)
                }
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
     * 检查文本是否包含单词（忽略大小写，单词边界）
     */
    private fun containsWordIgnoreCase(text: String, word: String): Boolean {
        val regex = Regex("""\b${Regex.escape(word)}\b""", RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(text)
    }
    
    /**
     * 高亮单词（忽略大小写，单词边界）
     */
    private fun highlightWordIgnoreCase(originalText: String, targetWord: String): String {
        val pattern = Regex("\\b${Regex.escape(targetWord)}\\b", RegexOption.IGNORE_CASE)
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
        
        val isEnglish = text.isAllEnglishAndSymbols()
        var result = text
        
        // 检查是否包含所有关键词
        val containsAllKeywords = if (isEnglish) {
            // 英文：检查单词边界匹配
            keywords.all { keyword ->
                val trimmed = keyword.trim()
                trimmed.isEmpty() || containsWordIgnoreCase(text, trimmed)
            }
        } else {
            // 中文：直接匹配
            keywords.all { keyword ->
                val trimmed = keyword.trim()
                trimmed.isEmpty() || text.contains(trimmed, ignoreCase = true)
            }
        }
        
        if (!containsAllKeywords) {
            return text.replace("*", "")
        }
        
        // 高亮所有关键词（英文使用单词边界，中文直接匹配）
        keywords.forEach { keyword ->
            val trimmed = keyword.trim()
            if (trimmed.isNotEmpty()) {
                if (isEnglish) {
                    // 英文：使用单词边界匹配，忽略大小写
                    result = highlightWordIgnoreCase(result, trimmed)
                } else {
                    // 中文：直接匹配并高亮
                    result = result.replace(trimmed, "<font color='red'>$trimmed</font>", ignoreCase = true)
                }
            }
        }
        
        return result.replace("*", "")
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
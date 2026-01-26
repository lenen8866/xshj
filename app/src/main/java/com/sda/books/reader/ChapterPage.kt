package com.sda.books.reader

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.SPUtils
import com.book.reader.R
import com.book.reader.databinding.ActivityChapterBinding
import com.gyf.immersionbar.ImmersionBar
import com.sda.books.reader.adapter.ChapterContentAdapter
import com.sda.books.reader.content.ContentProcessor
import com.sda.books.reader.db.DatabaseHelper
import com.sda.books.reader.event.EventBus
import com.sda.books.reader.util.AppSettingUtil
import com.sda.books.reader.util.AudioController
import com.sda.books.reader.util.Constant
import com.sda.books.reader.util.getChapterContentShowList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChapterPage : BaseActivity() {

    companion object {
        fun start(
            activity: Activity,
            volumeTitle: String,
            chapterTitle: String,
            chapterId: Int,
            scrollIndex: Int = 0,
            tabs: String,
            matchContent: ArrayList<String> = arrayListOf()
        ) {
            activity.startActivity(Intent(activity, ChapterPage::class.java).apply {
                putExtra("volumeTitle", volumeTitle)
                putExtra("chapterTitle", chapterTitle)
                putExtra("chapterId", chapterId)
                putExtra("scrollIndex", scrollIndex)
                putExtra("tabs", tabs)
                putStringArrayListExtra("matchContent", matchContent)
            })
        }
    }

    private lateinit var binding: ActivityChapterBinding
    private lateinit var adapter: ChapterContentAdapter
    private lateinit var audioController: AudioController
    
    // Intent 数据
    private val chapterId by lazy { intent.getIntExtra("chapterId", -1) }
    private val matchContent by lazy { intent.getStringArrayListExtra("matchContent") ?: arrayListOf<String>() }
    private val scrollIndex by lazy { 
        var index = intent.getIntExtra("scrollIndex", 0)
        if (!AppSettingUtil.getIsOpenEn()) {
            index = (index + 1) / 2
        }
        index
    }
    
    private var stopLineCount = 0
    private var totalLineCount = 0

    override fun getRootView(): View = binding.root

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChapterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        audioController = AudioController(this)
        
        setupUI()
        setupRecyclerView()
        setupAudioControls()
        loadContent()
        observeHeightEvents()
    }

    private fun setupUI() {
        val volumeTitle = intent.getStringExtra("volumeTitle")?.replace("E", "") ?: ""
        val chapterTitle = intent.getStringExtra("chapterTitle") ?: ""
        val tabs = intent.getStringExtra("tabs") ?: ""
        
        binding.apply {
            // 显示/隐藏 CE 标识
            ivCe.visibility = if (intent.getStringExtra("volumeTitle")?.endsWith("E") == true) {
                View.VISIBLE
            } else {
                View.GONE
            }
            
            // 标题处理
            tvVolumeTitle.text = volumeTitle.split("(")[0]
            tvAuthor.text = if (volumeTitle.contains("(")) {
                volumeTitle.split("(")[1].replace(")", "")
            } else {
                ""
            }
            tvVolumeTitles.text = tabs
            tvChapterTitle.text = chapterTitle
            tvZj.text = volumeTitle
            tvTitle.text = chapterTitle
            
            // 返回按钮
            ivBack.setOnClickListener { finish() }
            tvVolumeTitle.setOnClickListener { finish() }
            tvVolumeTitles.setOnClickListener { finish() }
        }
    }

    private fun setupRecyclerView() {
        adapter = ChapterContentAdapter(matchContent, lifecycleScope)
        
        binding.chapterContentList.apply {
            adapter = this@ChapterPage.adapter
            layoutManager = LinearLayoutManager(this@ChapterPage)
            
            // 设置边缘效果颜色
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
                    override fun createEdgeEffect(recyclerView: RecyclerView, direction: Int) =
                        super.createEdgeEffect(recyclerView, direction).apply {
                            color = ContextCompat.getColor(context, R.color.text_dark)
                        }
                }
            }
        }
    }

    private fun setupAudioControls() {
        audioController.bindViews(
            playerView = binding.playerView,
            audioContainer = binding.audioContainer,
            seekBar = binding.audioSeekBar,
            currentTimeView = binding.current,
            durationView = binding.duration,
            controlButton = binding.ivControl
        )
    }

    private fun loadContent() {
        lifecycleScope.launch {
            DatabaseHelper.getInstance().queryItemChapterContent(
                chapterId, matchContent,
                onSuccess = { content, music ->
                    // 初始化音频播放器
                    if (music.isNotEmpty()) {
                        audioController.initializePlayer(music)
                    }
                    
                    // ✅ 修复：在协程中调用 suspend 函数
                    lifecycleScope.launch {
                        processContentAsync(content)
                    }
                },
                onFail = {
                    Log.e("ChapterPage", "加载内容失败")
                }
            )
        }
    }

    private suspend fun processContentAsync(content: String) {
        // 后台线程处理内容
        val result = withContext(Dispatchers.Default) {
            val contentItems = getChapterContentShowList(content)
            ContentProcessor.buildChapterItems(
                contentItems = contentItems,
                matchContent = matchContent,
                scrollIndex = scrollIndex,
                sectionSpacing = AppSettingUtil.getTextSectionHLetterSpacing(),
                firstLetterSpacing = AppSettingUtil.getTextFristLetterSpacing()
            )
        }
        
        // 主线程更新 UI
        withContext(Dispatchers.Main) {
            stopLineCount = result.stopLineCount
            totalLineCount = result.totalLineCount
            adapter.updateData(result.items)
            
            // 延迟滚动
            if (scrollIndex > 0) {
                delay(500)
                // 滚动逻辑保持不变（等待高度事件触发）
            }
        }
    }

    private fun observeHeightEvents() {
        lifecycleScope.launch {
            EventBus.chapterHeightFlow.collectLatest { height ->
                if (scrollIndex > 0 && totalLineCount > 0) {
                    binding.chapterContentList.smoothScrollBy(
                        0,
                        stopLineCount * height / totalLineCount
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        
        // 设置背景色
        binding.main.setBackgroundColor(
            Color.parseColor(SPUtils.getInstance().getString(Constant.Background_Color))
        )
        
        // 设置状态栏主题
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

    override fun onDestroy() {
        super.onDestroy()
        audioController.release()
    }
}

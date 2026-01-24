package com.sda.books.reader

import android.app.Activity
import android.content.Context
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
import com.eightbitlab.rxbus.Bus
import com.eightbitlab.rxbus.registerInBus
import com.gyf.immersionbar.ImmersionBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.reflect.Field


class ChapterPage : BaseActivity() {

    companion object {
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
                putExtra("volumeTitle", volumeTitle)
                putExtra("chapterTitle", chapterTitle)
                putExtra("chapterId", chapterId)
                putExtra("scrollIndex", scrollIndex)
                putExtra("tabs", tabs)
                putStringArrayListExtra("matchContent", matchContent)
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
    var engTag = listOf("KJV","English","en")
    var cnTag = listOf("和合本","中文","cn")
    /*fun isEng():Boolean{
        return engTag.contains(tag)
    }

    fun isCn():Boolean{
        return cnTag.contains(tag)
    }*/
    var stopLineCount = 0
    var totalLineCount = 0
    override fun getRootView(): View {
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Log.e("TAG", "==========0008")
        volumeTitle = intent.getStringExtra("volumeTitle") ?: ""
        chapterTitle = intent.getStringExtra("chapterTitle") ?: ""
        tabs = intent.getStringExtra("tabs") ?: ""
        chapterId = intent.getIntExtra("chapterId", -1)
        scrollIndex = intent.getIntExtra("scrollIndex", 0)
        //判断是否开启中英文
        if(AppSettingUtil.getIsOpenEn()){

        }else{
            scrollIndex = (scrollIndex+1)/2
        }

        matchContent = intent.getStringArrayListExtra("matchContent") ?: arrayListOf()



        binding = ActivityChapterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //LogUtils.e("=================>>${volumeTitle}")
        if(volumeTitle.endsWith("E")){
            binding.ivCe.visibility = View.VISIBLE
        }else{
            binding.ivCe.visibility = View.GONE
        }
        volumeTitle = volumeTitle.replace("E","")

        //binding.tvVolumeTitle.text = volumeTitle
        binding.tvVolumeTitle.text = volumeTitle.split("(")[0]
        if(volumeTitle.contains("(")){
            binding.tvAuthor.text = volumeTitle.split("(")[1].replace(")","")
        }else{

        }


        binding.tvVolumeTitles.text = tabs
        binding.tvChapterTitle.text = chapterTitle

        binding.tvZj.text = volumeTitle
        binding.tvTitle.text = chapterTitle

        //LogUtils.e("==================>>>>>${matchContent}")



        adapter = ChapterContentAdapter(matchContent)
        binding.chapterContentList.adapter = adapter
        binding.chapterContentList.layoutManager = LinearLayoutManager(this)
        // ========== 修改点：兼容各版本的 EdgeEffectFactory ==========
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 使用 API 21+ 的官方方法
            binding.chapterContentList.edgeEffectFactory =
                object : RecyclerView.EdgeEffectFactory() {
                    override fun createEdgeEffect(
                        recyclerView: RecyclerView,
                        direction: Int
                    ): EdgeEffect {
                        val edgeEffect = super.createEdgeEffect(recyclerView, direction)
                        edgeEffect.color =
                            ContextCompat.getColor(recyclerView.context, R.color.text_dark)
                        return edgeEffect
                    }
                }
        } else {
            // 对于 API < 21 的兼容方案
            try {
                val field: Field = RecyclerView::class.java.getDeclaredField("mEdgeEffectFactory")
                field.isAccessible = true
                field.set(binding.chapterContentList, object : RecyclerView.EdgeEffectFactory() {
                    override fun createEdgeEffect(
                        recyclerView: RecyclerView,
                        direction: Int
                    ): EdgeEffect {
                        val edgeEffect = super.createEdgeEffect(recyclerView, direction)
                        edgeEffect.color =
                            ContextCompat.getColor(recyclerView.context, R.color.text_dark)
                        return edgeEffect
                    }
                })
            } catch (e: Exception) {
                Log.e("EdgeEffectFactory", "Failed to set edge effect color on older API", e)
            }
        }
        // ======================================================
        queryItemContent()
        //queryContent()
        binding.ivBack.setOnClickListener {
            finish()
        }
        binding.tvVolumeTitle.setOnClickListener {
            finish()
        }
        binding.tvVolumeTitles.setOnClickListener {
            finish()
        }




        binding.audioSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 0
                val audioProgress = progress / 100f * durationLenght
                player?.seekTo(audioProgress.toLong())
            }

        })

        binding.ivControl.setOnClickListener {
            if (player?.isPlaying == true) {
                player?.pause()
                binding.ivControl.setImageResource(R.drawable.play1)
            } else {
                player?.play()
                binding.ivControl.setImageResource(R.drawable.pause1)
            }
        }

        // 添加适配器监听器
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                //scrollToHighlightPosition()
            }
        })
        //binding.vs.attachToRecyclerView(binding.chapterContentList)

        Bus.observe<Int>()
            .subscribe {
                if(scrollIndex > 0){
                    /*lifecycleScope.launch {
                        delay(500)
                        scrollLineHeight(scrollIndex*AppSettingUtil.getTextSectionHLetterSpacing(),stopLineCount)
                    }*/
                   // binding.chapterContentList.smoothScrollBy(0,height.toInt()-3000)
                    //stopLineCount*it/totalLineCount
                    binding.chapterContentList.smoothScrollBy(0,(stopLineCount*it/totalLineCount))
                }

            }.registerInBus(this)

    }


    private fun scrollToHighlightPosition() {
        Log.d("ScrollDebug", "Attempting scroll to $scrollIndex")
        Log.d("ScrollDebug", "Adapter item count: ${adapter.itemCount}")
        if (scrollIndex > 0 && scrollIndex < adapter.itemCount) {
            binding.chapterContentList.post {
                // 确保布局完成
                if (binding.chapterContentList.layoutManager != null) {
                    val layoutManager =
                        binding.chapterContentList.layoutManager as LinearLayoutManager

                    // 滚动到位置并添加偏移量确保在屏幕中央
                    layoutManager.scrollToPositionWithOffset(scrollIndex, binding.root.height / 3)

                    // 设置高亮
                    adapter.highlightPosition(scrollIndex)

                    // 添加滚动动画
                    binding.chapterContentList.smoothScrollBy(0, 0)
                }
            }
        } else {
            Log.e("ScrollDebug", "Invalid scroll index: $scrollIndex")
        }
    }
    private fun queryItemContent(){
        lifecycleScope.launch {
            DatabaseHelper.getInstance().queryItemChapterContent(chapterId,matchContent,
                onSuccess = { content,music ->
                    try {
                        Log.e("TAg", "=======>>>" + music)
                        LogUtils.e("001=========${music}")
                        if (music.isNotEmpty()) {
                            initializePlayer(music)
                        }
                    } catch (e: Exception) {

                    }
                   // LogUtils.e("================${content}")
                    val chapterContentItemList = getChapterContentShowList(content)
                    val nData = mutableListOf<ChapterContentItem>()
                    // LogUtils.e("==========${chapterContentItemList.size}")
                    var pHeight = "<br>";
                    for (index in 0 until AppSettingUtil.getTextSectionHLetterSpacing()){
                        pHeight = pHeight +"<br>"
                    }


                    // nData.addAll(chapterContentItemList)
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

                                if(matchContent.size > 0){
                                     LogUtils.e("${index}=============>>${strTxt}")
                                    LogUtils.e("${index}=============>>${scrollIndex}")

                                    when(matchContent.size){
                                        1 -> {
                                            if(strTxt.isAllEnglishAndSymbols()){
                                                //if(strTxt.contains(matchContent[0]+"")){
                                                if(containsWordIgnoreCase(strTxt,matchContent[0])){
                                                    if(scrollIndex == index){
                                                        //LogUtils.e("=============>>${matchContent[0]}")
                                                        //val newTxt = strTxt.replace(matchContent[0]+"","<font color='red'>${matchContent[0]+""} </font>").replace("*","")

                                                        val newTxt = highlightWordIgnoreCase(strTxt,matchContent[0]).replace("*","")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }

                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }

                                            }else{
                                                if(strTxt.contains(matchContent[0])){
                                                    /*if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0],"<font color='red'>${matchContent[0]} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }*/
                                                    val newTxt = strTxt.replace(matchContent[0],"<font color='red'>${matchContent[0]} </font>")
                                                    insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"

                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }


                                            }


                                        }
                                        2 -> {

                                            if(strTxt.isAllEnglishAndSymbols()){

                                                if(strTxt.contains(matchContent[0]+" ") &&
                                                    strTxt.contains(matchContent[1]+ " ")){
                                                    /*if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0]+" ","<font color='red'>${matchContent[0]+" "} </font>")
                                                            .replace(matchContent[1]+" ","<font color='red'>${matchContent[1]+" "} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }*/
                                                    val newTxt = strTxt.replace(matchContent[0]+" ","<font color='red'>${matchContent[0]+" "} </font>")
                                                        .replace(matchContent[1]+" ","<font color='red'>${matchContent[1]+" "} </font>")
                                                    insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"


                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }



                                            }else{
                                                if(strTxt.contains(matchContent[0]) &&
                                                    strTxt.contains(matchContent[1])){
                                                   /* if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0],"<font color='red'>${matchContent[0]} </font>")
                                                            .replace(matchContent[1],"<font color='red'>${matchContent[1]} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }*/

                                                    val newTxt = strTxt.replace(matchContent[0],"<font color='red'>${matchContent[0]} </font>")
                                                        .replace(matchContent[1],"<font color='red'>${matchContent[1]} </font>")
                                                    insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"


                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }

                                            }
                                        }
                                        3 -> {
                                            if(strTxt.isAllEnglishAndSymbols()){

                                                if(strTxt.contains(matchContent[0]+" ") &&
                                                    strTxt.contains(matchContent[1]+" ") &&
                                                    strTxt.contains(matchContent[2]+" ")){
                                                    /*if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0]+" ","<font color='red'>${matchContent[0]+" "} </font>")
                                                            .replace(matchContent[1]+" ","<font color='red'>${matchContent[1]+" "} </font>")
                                                            .replace(matchContent[2]+" ","<font color='red'>${matchContent[2]+" "} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"

                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }*/
                                                    val newTxt = strTxt.replace(matchContent[0]+" ","<font color='red'>${matchContent[0]+" "} </font>")
                                                        .replace(matchContent[1]+" ","<font color='red'>${matchContent[1]+" "} </font>")
                                                        .replace(matchContent[2]+" ","<font color='red'>${matchContent[2]+" "} </font>")
                                                    insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }

                                            }else{
                                                if(strTxt.contains(matchContent[0]) &&
                                                    strTxt.contains(matchContent[1]) &&
                                                    strTxt.contains(matchContent[2])){
                                                    /*if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0],"<font color='red'>${matchContent[0]} </font>")
                                                            .replace(matchContent[1],"<font color='red'>${matchContent[1]} </font>")
                                                            .replace(matchContent[2],"<font color='red'>${matchContent[2]} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }*/
                                                    val newTxt = strTxt.replace(matchContent[0],"<font color='red'>${matchContent[0]} </font>")
                                                        .replace(matchContent[1],"<font color='red'>${matchContent[1]} </font>")
                                                        .replace(matchContent[2],"<font color='red'>${matchContent[2]} </font>")
                                                    insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"

                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }
                                            }

                                        }
                                        4 -> {
                                            if(strTxt.isAllEnglishAndSymbols()){
                                                if(strTxt.contains(matchContent[0]+" ") &&
                                                    strTxt.contains(matchContent[1]+" ") &&
                                                    strTxt.contains(matchContent[2]+" ") &&
                                                    strTxt.contains(matchContent[3]+" ") ){

                                                    /*if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0]+" ","<font color='red'>${matchContent[0]+" "} </font>")
                                                            .replace(matchContent[1]+" ","<font color='red'>${matchContent[1]+" "} </font>")
                                                            .replace(matchContent[2]+" ","<font color='red'>${matchContent[2]+" "} </font>")
                                                            .replace(matchContent[3]+" ","<font color='red'>${matchContent[3]+" "} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }*/

                                                    val newTxt = strTxt.replace(matchContent[0]+" ","<font color='red'>${matchContent[0]+" "} </font>")
                                                        .replace(matchContent[1]+" ","<font color='red'>${matchContent[1]+" "} </font>")
                                                        .replace(matchContent[2]+" ","<font color='red'>${matchContent[2]+" "} </font>")
                                                        .replace(matchContent[3]+" ","<font color='red'>${matchContent[3]+" "} </font>")
                                                    insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"


                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }


                                            }else{
                                                if(strTxt.contains(matchContent[0]) &&
                                                    strTxt.contains(matchContent[1]) &&
                                                    strTxt.contains(matchContent[2]) &&
                                                    strTxt.contains(matchContent[3]) ){
                                                    /*if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0],"<font color='red'>${matchContent[0]} </font>")
                                                            .replace(matchContent[1],"<font color='red'>${matchContent[1]} </font>")
                                                            .replace(matchContent[2],"<font color='red'>${matchContent[2]} </font>")
                                                            .replace(matchContent[3],"<font color='red'>${matchContent[3]} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }*/
                                                    val newTxt = strTxt.replace(matchContent[0],"<font color='red'>${matchContent[0]} </font>")
                                                        .replace(matchContent[1],"<font color='red'>${matchContent[1]} </font>")
                                                        .replace(matchContent[2],"<font color='red'>${matchContent[2]} </font>")
                                                        .replace(matchContent[3],"<font color='red'>${matchContent[3]} </font>")
                                                    insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"



                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }
                                            }

                                        }
                                        5 -> {
                                            if(strTxt.isAllEnglishAndSymbols()){

                                                if(strTxt.contains(matchContent[0]+" ") &&
                                                    strTxt.contains(matchContent[1]+" ") &&
                                                    strTxt.contains(matchContent[2]+" ") &&
                                                    strTxt.contains(matchContent[3]+" ") &&
                                                    strTxt.contains(matchContent[4]+" ")){
                                                    if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0]+" ","<font color='red'>${matchContent[0]+" "} </font>")
                                                            .replace(matchContent[1]+" ","<font color='red'>${matchContent[1]+" "} </font>")
                                                            .replace(matchContent[2]+" ","<font color='red'>${matchContent[2]+" "} </font>")
                                                            .replace(matchContent[3]+" ","<font color='red'>${matchContent[3]+" "} </font>")
                                                            .replace(matchContent[4]+" ","<font color='red'>${matchContent[4]+" "} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }



                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }

                                            }else{
                                                if(strTxt.contains(matchContent[0]) &&
                                                    strTxt.contains(matchContent[1]) &&
                                                    strTxt.contains(matchContent[2]) &&
                                                    strTxt.contains(matchContent[3]) &&
                                                    strTxt.contains(matchContent[4])){
                                                    if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0],"<font color='red'>${matchContent[0]} </font>")
                                                            .replace(matchContent[1],"<font color='red'>${matchContent[1]} </font>")
                                                            .replace(matchContent[2],"<font color='red'>${matchContent[2]} </font>")
                                                            .replace(matchContent[3],"<font color='red'>${matchContent[3]} </font>")
                                                            .replace(matchContent[4],"<font color='red'>${matchContent[4]} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }




                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }
                                            }

                                        }
                                        6 -> {
                                            if(strTxt.isAllEnglishAndSymbols()){

                                                if(strTxt.contains(matchContent[0]+" ") &&
                                                    strTxt.contains(matchContent[1]+" ") &&
                                                    strTxt.contains(matchContent[2]+" ") &&
                                                    strTxt.contains(matchContent[3]+" ") &&
                                                    strTxt.contains(matchContent[4]+" ") &&
                                                    strTxt.contains(matchContent[5]+" ")){

                                                    if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0]+" ","<font color='red'>${matchContent[0]+" "} </font>")
                                                            .replace(matchContent[1]+" ","<font color='red'>${matchContent[1]+" "} </font>")
                                                            .replace(matchContent[2]+" ","<font color='red'>${matchContent[2]+" "} </font>")
                                                            .replace(matchContent[3]+" ","<font color='red'>${matchContent[3]+" "} </font>")
                                                            .replace(matchContent[4]+" ","<font color='red'>${matchContent[4]+" "} </font>")
                                                            .replace(matchContent[5]+" ","<font color='red'>${matchContent[5]+" "} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }


                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }

                                            }else{
                                                if(strTxt.contains(matchContent[0]) &&
                                                    strTxt.contains(matchContent[1]) &&
                                                    strTxt.contains(matchContent[2]) &&
                                                    strTxt.contains(matchContent[3]) &&
                                                    strTxt.contains(matchContent[4]) &&
                                                    strTxt.contains(matchContent[5]) ){
                                                    if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0],"<font color='red'>${matchContent[0]} </font>")
                                                            .replace(matchContent[1],"<font color='red'>${matchContent[1]} </font>")
                                                            .replace(matchContent[2],"<font color='red'>${matchContent[2]} </font>")
                                                            .replace(matchContent[3],"<font color='red'>${matchContent[3]} </font>")
                                                            .replace(matchContent[4],"<font color='red'>${matchContent[4]} </font>")
                                                            .replace(matchContent[5],"<font color='red'>${matchContent[5]} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }
                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }
                                            }

                                        }
                                        else -> {
                                            insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                        }
                                    }
                                }else{
                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                }

                                //insertContent += "$indentSpaces${chapterContentItemList[index].getShowContent()}<br>"

                                if(matchContent.size > 0){

                                    if(index <= scrollIndex){

                                        if(chapterContentItemList[index].getShowContent().isAllEnglishAndSymbols()){
                                            //LogUtils.e("英文行数==============>>>>>>>>>${chapterContentItemList[index].getShowContent().length}")
                                            //LogUtils.e("英文行数==============>>>>>>>>>${SPUtils.getInstance().getInt(Constant.LineCount)}")
                                            //LogUtils.e("英文行数==============>>>>>>>>>${chapterContentItemList[index].getShowContent().length/2/SPUtils.getInstance().getInt(Constant.LineCount)}")
                                            stopLineCount = stopLineCount +  chapterContentItemList[index].getShowContent().length/2/SPUtils.getInstance().getInt(Constant.LineCount) +1
                                        }else{
                                            //LogUtils.e("中文行数==============>>>>>>>>>${chapterContentItemList[index].getShowContent().length/SPUtils.getInstance().getInt(Constant.LineCount)}")
                                            stopLineCount = stopLineCount + chapterContentItemList[index].getShowContent().length/SPUtils.getInstance().getInt(Constant.LineCount)
                                            //LogUtils.e("中文行数==============>>>>>>>>>---${stopLineCount}")
                                            //LogUtils.e("中文行数==============>>>>>>>>>---${SPUtils.getInstance().getInt(Constant.LineCount)}")
                                        }
                                        if(chapterContentItemList[index].getShowContent().isAllEnglishAndSymbols()){
                                            //LogUtils.e("英文行数==============>>>>>>>>>${chapterContentItemList[index].getShowContent().length}")
                                            //LogUtils.e("英文行数==============>>>>>>>>>${SPUtils.getInstance().getInt(Constant.LineCount)}")
                                            //LogUtils.e("英文行数==============>>>>>>>>>${chapterContentItemList[index].getShowContent().length/2/SPUtils.getInstance().getInt(Constant.LineCount)}")
                                            totalLineCount = totalLineCount +  chapterContentItemList[index].getShowContent().length/2/SPUtils.getInstance().getInt(Constant.LineCount) +1
                                        }else{
                                           // LogUtils.e("中文行数==============>>>>>>>>>${chapterContentItemList[index].getShowContent().length/SPUtils.getInstance().getInt(Constant.LineCount)}")
                                            totalLineCount = totalLineCount + chapterContentItemList[index].getShowContent().length/SPUtils.getInstance().getInt(Constant.LineCount)
                                            //LogUtils.e("中文行数==============>>>>>>>>>---${stopLineCount}")
                                            //LogUtils.e("中文行数==============>>>>>>>>>---${SPUtils.getInstance().getInt(Constant.LineCount)}")
                                        }
                                    }else{
                                        if(chapterContentItemList[index].getShowContent().isAllEnglishAndSymbols()){
                                            //LogUtils.e("英文行数==============>>>>>>>>>${chapterContentItemList[index].getShowContent().length}")
                                            //LogUtils.e("英文行数==============>>>>>>>>>${SPUtils.getInstance().getInt(Constant.LineCount)}")
                                            //LogUtils.e("英文行数==============>>>>>>>>>${chapterContentItemList[index].getShowContent().length/2/SPUtils.getInstance().getInt(Constant.LineCount)}")
                                            totalLineCount = totalLineCount +  chapterContentItemList[index].getShowContent().length/2/SPUtils.getInstance().getInt(Constant.LineCount) +1
                                        }else{
                                            //LogUtils.e("中文行数==============>>>>>>>>>${chapterContentItemList[index].getShowContent().length/SPUtils.getInstance().getInt(Constant.LineCount)}")
                                            totalLineCount = totalLineCount + chapterContentItemList[index].getShowContent().length/SPUtils.getInstance().getInt(Constant.LineCount)
                                            //LogUtils.e("中文行数==============>>>>>>>>>---${stopLineCount}")
                                            //LogUtils.e("中文行数==============>>>>>>>>>---${SPUtils.getInstance().getInt(Constant.LineCount)}")
                                        }
                                    }
                                }


                                //LogUtils.e("=============>>>003"+insertContent)

                                // LogUtils.e("=============>>>001${insertContent}")
                                //LogUtils.e("===================${insertContent}")
                                /*insertContent =
                                    insertContent + chapterContentItemList[index].getShowContent();

                                var insertChapterContentItem1 = ChapterContentItem()
                                insertChapterContentItem1.setUserContent(insertContent)
                                nData.add(insertChapterContentItem1)
                                insertContent = ""*/
                            }

                        }

                        if (index == chapterContentItemList.size - 1){
                            insertChapterContentItem.setUserContent(insertContent)
                            nData.add(insertChapterContentItem)
                        }

                    }
                    // 更新适配器数据
                    adapter.updateData(nData)
                    //LogUtils.e("===================${scrollIndex}")
                    /*if(scrollIndex > 0){
                        binding.chapterContentList.scrollBy(0,100 * scrollIndex)
                    }*/
                    //LogUtils.e("==================>>>>>${scrollIndex}")
                    if(scrollIndex > 0){
                        lifecycleScope.launch {
                            delay(500)
                            //stopLineCount = insertContent.length
                            //binding.chapterContentList.scrollBy(0,300)

                            scrollLineHeight(scrollIndex*AppSettingUtil.getTextSectionHLetterSpacing(),stopLineCount)
                        }
                    }
                },
                onFail ={

                } )
        }
    }

    private fun queryContent() {
        lifecycleScope.launch {
            DatabaseHelper.getInstance().queryChapterContent(chapterId,
                onSuccess = { content, music ->

                    try {
                        Log.e("TAg", "=======>>>" + music)
                        if (music.isNotEmpty()) {
                            initializePlayer(music)
                        }
                    } catch (e: Exception) {

                    }

                    val chapterContentItemList = getChapterContentShowList(content)
                    val nData = mutableListOf<ChapterContentItem>()
                    // LogUtils.e("==========${chapterContentItemList.size}")
                    var pHeight = "<br>";
                    for (index in 0 until AppSettingUtil.getTextSectionHLetterSpacing()){
                        pHeight = pHeight +"<br>"
                    }


                    // nData.addAll(chapterContentItemList)
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

                                if(matchContent.size > 0){
                                   // LogUtils.e("=============>>${matchContent[0]}")

                                    when(matchContent.size){
                                        1 -> {
                                            if(strTxt.isAllEnglishAndSymbols()){
                                                if(strTxt.contains(matchContent[0]+"")){
                                                    if(scrollIndex == index){
                                                        LogUtils.e("=============>>${matchContent[0]}")
                                                        val newTxt = strTxt.replace(matchContent[0]+"","<font color='red'>${matchContent[0]+""} </font>").replace("*","")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }

                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }

                                            }else{
                                                if(strTxt.contains(matchContent[0])){
                                                    LogUtils.e("${scrollIndex}==============>>>>>>>${index}")
                                                    if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0],"<font color='red'>${matchContent[0]} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }

                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }


                                            }


                                        }
                                        2 -> {

                                            if(strTxt.isAllEnglishAndSymbols()){

                                                if(strTxt.contains(matchContent[0]+" ") &&
                                                    strTxt.contains(matchContent[1]+ " ")){
                                                    if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0]+" ","<font color='red'>${matchContent[0]+" "} </font>")
                                                            .replace(matchContent[1]+" ","<font color='red'>${matchContent[1]+" "} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }


                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }



                                            }else{
                                                if(strTxt.contains(matchContent[0]) &&
                                                    strTxt.contains(matchContent[1])){
                                                    if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0],"<font color='red'>${matchContent[0]} </font>")
                                                            .replace(matchContent[1],"<font color='red'>${matchContent[1]} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }




                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }

                                            }
                                        }
                                        3 -> {
                                            if(strTxt.isAllEnglishAndSymbols()){

                                                if(strTxt.contains(matchContent[0]+" ") &&
                                                    strTxt.contains(matchContent[1]+" ") &&
                                                    strTxt.contains(matchContent[2]+" ")){
                                                    if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0]+" ","<font color='red'>${matchContent[0]+" "} </font>")
                                                            .replace(matchContent[1]+" ","<font color='red'>${matchContent[1]+" "} </font>")
                                                            .replace(matchContent[2]+" ","<font color='red'>${matchContent[2]+" "} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"

                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }
                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }

                                            }else{
                                                if(strTxt.contains(matchContent[0]) &&
                                                    strTxt.contains(matchContent[1]) &&
                                                    strTxt.contains(matchContent[2])){
                                                    if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0],"<font color='red'>${matchContent[0]} </font>")
                                                            .replace(matchContent[1],"<font color='red'>${matchContent[1]} </font>")
                                                            .replace(matchContent[2],"<font color='red'>${matchContent[2]} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }

                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }
                                            }

                                        }
                                        4 -> {
                                            if(strTxt.isAllEnglishAndSymbols()){
                                                if(strTxt.contains(matchContent[0]+" ") &&
                                                    strTxt.contains(matchContent[1]+" ") &&
                                                    strTxt.contains(matchContent[2]+" ") &&
                                                    strTxt.contains(matchContent[3]+" ") ){

                                                    if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0]+" ","<font color='red'>${matchContent[0]+" "} </font>")
                                                            .replace(matchContent[1]+" ","<font color='red'>${matchContent[1]+" "} </font>")
                                                            .replace(matchContent[2]+" ","<font color='red'>${matchContent[2]+" "} </font>")
                                                            .replace(matchContent[3]+" ","<font color='red'>${matchContent[3]+" "} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }


                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }


                                            }else{
                                                if(strTxt.contains(matchContent[0]) &&
                                                    strTxt.contains(matchContent[1]) &&
                                                    strTxt.contains(matchContent[2]) &&
                                                    strTxt.contains(matchContent[3]) ){
                                                    if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0],"<font color='red'>${matchContent[0]} </font>")
                                                            .replace(matchContent[1],"<font color='red'>${matchContent[1]} </font>")
                                                            .replace(matchContent[2],"<font color='red'>${matchContent[2]} </font>")
                                                            .replace(matchContent[3],"<font color='red'>${matchContent[3]} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }

                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }
                                            }

                                        }
                                        5 -> {
                                            if(strTxt.isAllEnglishAndSymbols()){

                                                if(strTxt.contains(matchContent[0]+" ") &&
                                                    strTxt.contains(matchContent[1]+" ") &&
                                                    strTxt.contains(matchContent[2]+" ") &&
                                                    strTxt.contains(matchContent[3]+" ") &&
                                                    strTxt.contains(matchContent[4]+" ")){
                                                    if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0]+" ","<font color='red'>${matchContent[0]+" "} </font>")
                                                            .replace(matchContent[1]+" ","<font color='red'>${matchContent[1]+" "} </font>")
                                                            .replace(matchContent[2]+" ","<font color='red'>${matchContent[2]+" "} </font>")
                                                            .replace(matchContent[3]+" ","<font color='red'>${matchContent[3]+" "} </font>")
                                                            .replace(matchContent[4]+" ","<font color='red'>${matchContent[4]+" "} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }



                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }

                                            }else{
                                                if(strTxt.contains(matchContent[0]) &&
                                                    strTxt.contains(matchContent[1]) &&
                                                    strTxt.contains(matchContent[2]) &&
                                                    strTxt.contains(matchContent[3]) &&
                                                    strTxt.contains(matchContent[4])){
                                                    if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0],"<font color='red'>${matchContent[0]} </font>")
                                                            .replace(matchContent[1],"<font color='red'>${matchContent[1]} </font>")
                                                            .replace(matchContent[2],"<font color='red'>${matchContent[2]} </font>")
                                                            .replace(matchContent[3],"<font color='red'>${matchContent[3]} </font>")
                                                            .replace(matchContent[4],"<font color='red'>${matchContent[4]} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }




                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }
                                            }

                                        }
                                        6 -> {
                                            if(strTxt.isAllEnglishAndSymbols()){

                                                if(strTxt.contains(matchContent[0]+" ") &&
                                                    strTxt.contains(matchContent[1]+" ") &&
                                                    strTxt.contains(matchContent[2]+" ") &&
                                                    strTxt.contains(matchContent[3]+" ") &&
                                                    strTxt.contains(matchContent[4]+" ") &&
                                                    strTxt.contains(matchContent[5]+" ")){

                                                    if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0]+" ","<font color='red'>${matchContent[0]+" "} </font>")
                                                            .replace(matchContent[1]+" ","<font color='red'>${matchContent[1]+" "} </font>")
                                                            .replace(matchContent[2]+" ","<font color='red'>${matchContent[2]+" "} </font>")
                                                            .replace(matchContent[3]+" ","<font color='red'>${matchContent[3]+" "} </font>")
                                                            .replace(matchContent[4]+" ","<font color='red'>${matchContent[4]+" "} </font>")
                                                            .replace(matchContent[5]+" ","<font color='red'>${matchContent[5]+" "} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }


                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }

                                            }else{
                                                if(strTxt.contains(matchContent[0]) &&
                                                    strTxt.contains(matchContent[1]) &&
                                                    strTxt.contains(matchContent[2]) &&
                                                    strTxt.contains(matchContent[3]) &&
                                                    strTxt.contains(matchContent[4]) &&
                                                    strTxt.contains(matchContent[5]) ){
                                                    if(scrollIndex == index){
                                                        val newTxt = strTxt.replace(matchContent[0],"<font color='red'>${matchContent[0]} </font>")
                                                            .replace(matchContent[1],"<font color='red'>${matchContent[1]} </font>")
                                                            .replace(matchContent[2],"<font color='red'>${matchContent[2]} </font>")
                                                            .replace(matchContent[3],"<font color='red'>${matchContent[3]} </font>")
                                                            .replace(matchContent[4],"<font color='red'>${matchContent[4]} </font>")
                                                            .replace(matchContent[5],"<font color='red'>${matchContent[5]} </font>")
                                                        insertContent += "$indentSpaces${newTxt.replace("*","")}${pHeight}"
                                                    }else{
                                                        insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                    }
                                                }else{
                                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                                }
                                            }

                                        }
                                        else -> {
                                            insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                        }
                                    }
                                }else{
                                    insertContent += "$indentSpaces${strTxt.replace("*","")}${pHeight}"
                                }

                                //insertContent += "$indentSpaces${chapterContentItemList[index].getShowContent()}<br>"

                                if(matchContent.size > 0){

                                    if(index <= scrollIndex){

                                        if(chapterContentItemList[index].getShowContent().isAllEnglishAndSymbols()){
                                            LogUtils.e("英文===========>>>>>>${chapterContentItemList[index].getShowContent().length/SPUtils.getInstance().getInt(Constant.LineCount)}");

                                            stopLineCount = stopLineCount +  chapterContentItemList[index].getShowContent().length/2/SPUtils.getInstance().getInt(Constant.LineCount) +1
                                        }else{
                                            LogUtils.e(" 中文===========>>>>>>${chapterContentItemList[index].getShowContent().length/SPUtils.getInstance().getInt(Constant.LineCount)}");
                                            stopLineCount = stopLineCount + chapterContentItemList[index].getShowContent().length/SPUtils.getInstance().getInt(Constant.LineCount) +1
                                        }
                                        if(chapterContentItemList[index].getShowContent().isAllEnglishAndSymbols()){
                                            //LogUtils.e("英文行数==============>>>>>>>>>${chapterContentItemList[index].getShowContent().length}")
                                            //LogUtils.e("英文行数==============>>>>>>>>>${SPUtils.getInstance().getInt(Constant.LineCount)}")
                                            LogUtils.e("英文行数==============>>>>>>>>>${chapterContentItemList[index].getShowContent().length/2/SPUtils.getInstance().getInt(Constant.LineCount)}")
                                            totalLineCount = totalLineCount +  chapterContentItemList[index].getShowContent().length/2/SPUtils.getInstance().getInt(Constant.LineCount) +1
                                        }else{
                                            LogUtils.e("中文行数==============>>>>>>>>>${chapterContentItemList[index].getShowContent().length/SPUtils.getInstance().getInt(Constant.LineCount)}")
                                            totalLineCount = totalLineCount + chapterContentItemList[index].getShowContent().length/SPUtils.getInstance().getInt(Constant.LineCount)
                                            //LogUtils.e("中文行数==============>>>>>>>>>---${stopLineCount}")
                                            //LogUtils.e("中文行数==============>>>>>>>>>---${SPUtils.getInstance().getInt(Constant.LineCount)}")
                                        }
                                    }else{
                                        if(chapterContentItemList[index].getShowContent().isAllEnglishAndSymbols()){
                                            //LogUtils.e("英文行数==============>>>>>>>>>${chapterContentItemList[index].getShowContent().length}")
                                            //LogUtils.e("英文行数==============>>>>>>>>>${SPUtils.getInstance().getInt(Constant.LineCount)}")
                                            LogUtils.e("英文行数==============>>>>>>>>>${chapterContentItemList[index].getShowContent().length/2/SPUtils.getInstance().getInt(Constant.LineCount)}")
                                            totalLineCount = totalLineCount +  chapterContentItemList[index].getShowContent().length/2/SPUtils.getInstance().getInt(Constant.LineCount) +1
                                        }else{
                                            LogUtils.e("中文行数==============>>>>>>>>>${chapterContentItemList[index].getShowContent().length/SPUtils.getInstance().getInt(Constant.LineCount)}")
                                            totalLineCount = totalLineCount + chapterContentItemList[index].getShowContent().length/SPUtils.getInstance().getInt(Constant.LineCount)
                                            //LogUtils.e("中文行数==============>>>>>>>>>---${stopLineCount}")
                                            //LogUtils.e("中文行数==============>>>>>>>>>---${SPUtils.getInstance().getInt(Constant.LineCount)}")
                                        }
                                    }
                                }


                                //LogUtils.e("=============>>>003"+insertContent)

                                // LogUtils.e("=============>>>001${insertContent}")
                                //LogUtils.e("===================${insertContent}")
                                /*insertContent =
                                    insertContent + chapterContentItemList[index].getShowContent();

                                var insertChapterContentItem1 = ChapterContentItem()
                                insertChapterContentItem1.setUserContent(insertContent)
                                nData.add(insertChapterContentItem1)
                                insertContent = ""*/
                            }

                        }

                        if (index == chapterContentItemList.size - 1){
                            insertChapterContentItem.setUserContent(insertContent)
                            nData.add(insertChapterContentItem)
                        }

                    }
                    // 更新适配器数据
                    adapter.updateData(nData)
                    //LogUtils.e("===================${scrollIndex}")
                    /*if(scrollIndex > 0){
                        binding.chapterContentList.scrollBy(0,100 * scrollIndex)
                    }*/
                    //LogUtils.e("==================>>>>>${scrollIndex}")
                    /*if(scrollIndex > 0){
                        lifecycleScope.launch {
                            delay(500)
                            scrollLineHeight(scrollIndex*AppSettingUtil.getTextSectionHLetterSpacing(),stopLineCount)
                        }
                    }*/



//                    binding.root.post {
//                        if(scrollIndex != 0){
//                            ( binding.chapterContentList.layoutManager!! as LinearLayoutManager).scrollToPositionWithOffset(scrollIndex,0)
//                        }
//                    }
                    // 在数据更新后触发滚动
                    //scrollToHighlightPosition()
                    //binding.chapterContentList.scrollBy(0,500)
                }, onFail = {

                }
            )
        }
    }

    private fun scrollLineHeight(index: Int, length: Int) {
        //LogUtils.e("段落数===========>>>>>>${index}")
        //LogUtils.e("行数===========>>>>>>${length}")




        var height = (index + length) * SPUtils.getInstance().getInt(Constant.TextViewLineHeight,0)
        LogUtils.e("行高==========----${height}")
        LogUtils.e("行高==========${binding.chapterContentList.measuredHeight}")
        /*if(height < 1000){
            binding.chapterContentList.smoothScrollBy(0,height.toInt())
        }else{
            binding.chapterContentList.smoothScrollBy(0,height.toInt()-3000)
        }*/
        //binding.chapterContentList.smoothScrollBy(0,height.toInt())


    }


    fun String.isAllEnglishAndSymbols(): Boolean {
        // 正则表达式说明：
        // ^ 表示字符串开头
        // $ 表示字符串结尾
        // [a-zA-Z\\x20-\\x7E]+ 表示匹配大小写字母及ASCII码32-126的可打印符号
        // + 表示至少包含一个字符（空字符串返回false）
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
        Bus.unregister(this)

    }


    fun formatMillisecondsToHMS(milliseconds: Long): String {
        // 计算时、分、秒
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / (1000 * 60)) % 60
        val hours = (milliseconds / (1000 * 60 * 60)) % 24

        // 使用String.format进行格式化，确保每位数字两位显示
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            return String.format("%02d:%02d", minutes, seconds)
        }

        //return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }


    var player: ExoPlayer? = null

    // 初始化播放器
    private fun initializePlayer(music: String) {

        val isDownload = StoreManager.checkFileIsDownLoadFinish(music)

        Log.i("ccccccc", "music===$music")
        if (isDownload) {
            player = ExoPlayer.Builder(this)
                .build()
                .also { exoPlayer ->
                    Log.i("ccccccc", "播放本地音乐")
                    // 设置音频源
                    val mediaItem = MediaItem.fromUri(Uri.fromFile(StoreManager.getDesFile(music)))
                    exoPlayer.setMediaItem(mediaItem)
                    // 配置播放参数
                    exoPlayer.playWhenReady = false
                    if (music.endsWith("mp4")) {
                        binding.playerView.player = exoPlayer
                    }
                    // 准备播放（异步）
                    exoPlayer.prepare()
                    Log.i("ccccccc", "prepare22")
                }
        } else {
            StoreManager.startDownLoad(music)
            player = ExoPlayer.Builder(this)
                .build()
                .also { exoPlayer ->
                    // 设置音频源
                    val mediaItem = MediaItem.fromUri(music)
                    exoPlayer.setMediaItem(mediaItem)

                    // 配置播放参数
                    exoPlayer.playWhenReady = false
                    if (music.endsWith("mp4")) {
                        binding.playerView.player = exoPlayer
                    }
                    // 准备播放（异步）
                    exoPlayer.prepare()
                    Log.i("ccccccc", "prepare11")
                }
        }


        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.i("ccccccccc", "onPlaybackStateChanged====" + playbackState)
                when (playbackState) {
                    Player.STATE_IDLE -> Log.d("Media3", "播放器空闲")
                    Player.STATE_BUFFERING -> Log.d("Media3", "缓冲中...")
                    Player.STATE_READY -> {
                        // 播放器准备完成，可以开始播放
                        Log.d("Media3", "准备就绪，总时长: ${player?.duration ?: 0}ms")

                        isPrepare = true
                        durationLenght = player?.duration ?: 0

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


    override fun onResume() {
        super.onResume()

        binding.main.setBackgroundColor(Color.parseColor(SPUtils.getInstance().getString(Constant.Background_Color)))
        when(SPUtils.getInstance().getInt(Constant.ThemeColorIndex,0)){
            0 -> {
                ImmersionBar.with(this@ChapterPage).transparentStatusBar()
                    .transparentBar()
                    .transparentNavigationBar()
                    //.statusBarColor(Color.parseColor(SPUtils.getInstance().getString(Constant.Background_Color)))
                    .statusBarColor(R.color.title_1)
                    //.fullScreen(true)
                    .statusBarDarkFont(true)
                    .navigationBarDarkIcon(true)
                    .autoDarkModeEnable(true)
                    //.reset()
                    .init()
            }
            1 -> {
                ImmersionBar.with(this@ChapterPage).transparentStatusBar()
                    .transparentBar()
                    .transparentNavigationBar()
                    //.statusBarColor(Color.parseColor(SPUtils.getInstance().getString(Constant.Background_Color)))
                    .statusBarColor(R.color.title_2)
                    //.fullScreen(true)
                    .statusBarDarkFont(true)
                    .navigationBarDarkIcon(true)
                    .autoDarkModeEnable(true)
                    //.reset()
                    .init()
            }
            2 -> {
                ImmersionBar.with(this@ChapterPage).transparentStatusBar()
                    .transparentBar()
                    .transparentNavigationBar()
                    //.statusBarColor(Color.parseColor(SPUtils.getInstance().getString(Constant.Background_Color)))
                    .statusBarColor(R.color.title_3)
                    //.fullScreen(true)
                    .statusBarDarkFont(true)
                    .navigationBarDarkIcon(true)
                    .autoDarkModeEnable(true)
                    //.reset()
                    .init()
            }
            3 -> {
                ImmersionBar.with(this@ChapterPage).transparentStatusBar()
                    .transparentBar()
                    .transparentNavigationBar()
                    //.statusBarColor(Color.parseColor(SPUtils.getInstance().getString(Constant.Background_Color)))
                    .statusBarColor(R.color.title_4)
                    //.fullScreen(true)
                    .statusBarDarkFont(true)
                    .navigationBarDarkIcon(true)
                    .autoDarkModeEnable(true)
                    //.reset()
                    .init()
            }
        }
    }
    /**
     * 将 px 值转换为 dp 值
     */
    fun px2dp(context: Context, pxValue: Float): Float {
        val scale = context.resources.displayMetrics.density
        return pxValue / scale
    }
    fun containsWordIgnoreCase(text: String, word: String): Boolean {
        val regex = Regex("""\b$word\b""", RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(text)
    }
    fun highlightWordIgnoreCase(originalText: String, targetWord: String): String {
        // 检查目标单词是否为空
        /*if (targetWord.isBlank()) {
            return Pair(false, originalText)
        }*/

        // 创建不区分大小写的正则表达式，确保匹配整个单词
        val pattern = Regex("\\b$targetWord\\b", RegexOption.IGNORE_CASE)

        // 检查是否包含目标单词
        val containsWord = pattern.containsMatchIn(originalText)

        // 如果不包含，直接返回原文本
        /*if (!containsWord) {
            return Pair(false, originalText)
        }*/

        // 替换匹配的单词，加上红色标记
        // 这里使用HTML的span标签示例，实际使用中可根据需要修改
        val highlightedText = originalText.replace(pattern) { matchResult ->
            "<span style=\"color: red;\">${matchResult.value}</span>"
        }

        return highlightedText;
        //return Pair(true, highlightedText)
    }


}
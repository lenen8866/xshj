package com.sda.books.reader

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.book.reader.R

import com.sda.books.reader.adapter.ChapterListAdapter
import com.sda.books.reader.adapter.ChapterListAdapter.OnItemClickListener
import com.book.reader.databinding.ActivityVolumeBinding
import com.sda.books.reader.db.DatabaseHelper
import com.sda.books.reader.entity.Chapter
import com.sda.books.reader.loading.LoadingDialog
import com.sda.books.reader.util.Constant
import com.gyf.immersionbar.ImmersionBar
import kotlinx.coroutines.launch

class AppVolumePage : BaseActivity() {

    var volumeId = -1
    var volumeTabs: String = ""

    var volumeTitle = ""
    lateinit var loadingDialog : LoadingDialog
    lateinit var adapter: ChapterListAdapter
    lateinit var binding:ActivityVolumeBinding
    override fun getRootView(): View {

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        Log.e("TAG","==========0001")
        volumeTitle = intent.getStringExtra("volumeTitle") ?:""
        volumeId = intent.getIntExtra("volumeId",-1)
        volumeTabs = intent.getStringExtra("volumeTabs") ?: ""
        binding = ActivityVolumeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ivBack.setOnClickListener {
            finish()
        }

        loadingDialog = LoadingDialog(this)
        //binding.tvTitle.text = volumeTitle
        if(volumeTitle.endsWith("E")){
            binding.ivCe.visibility = View.VISIBLE
        }else{
            binding.ivCe.visibility = View.GONE
        }
        //volumeTitle = volumeTitle.replace("E","")



        binding.tvTitle.post { // 等待布局完成
            //adjustTextSize(binding.tvTitle, volumeTitle)
            binding.tvTitle.text = volumeTitle.replace("E","").split("(")[0]
            if(volumeTitle.contains("(")){
                binding.tvAuthor.text = volumeTitle.replace("E","").split("(")[1].replace(")","")
            }else{

            }
        }
        adapter = ChapterListAdapter()
        adapter.setOnItemClickListener(object : OnItemClickListener{
            override fun onItemClick(position: Int, volume: Chapter) {
                LogUtils.e("===============${volume.name}")
                LogUtils.e("===============${volume.id}")
                ChapterPage.Companion.start(
                    this@AppVolumePage,
                    volumeTitle,
                    volume.name,
                    volume.id,
                    //volume.volumeId,
                    0,
                    volumeTabs
                )
            }
        })
        binding.chapterList.layoutManager = LinearLayoutManager(this)
        binding.chapterList.adapter = adapter

        // 绑定滚动条
        binding.scrollBar.attachToRecyclerView(binding.chapterList)
        queryData();
    }

    fun adjustTextSize(textView: TextView, text: String) {
        val paint = textView.paint
        val maxWidth = textView.width - textView.paddingLeft - textView.paddingRight

        var textSize = textView.textSize
        while (paint.measureText(text) > maxWidth && textSize > 1) {
            textSize--
            textView.textSize = textSize / resources.displayMetrics.density
        }
    }
    fun queryData(){
        loadingDialog.show()
        lifecycleScope.launch {
            DatabaseHelper.getInstance().queryChapter(volumeId,
                onSuccess = {
                    adapter.updateData(it)
                }, onFail = {

                })
            loadingDialog.dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.main.setBackgroundColor(Color.parseColor(SPUtils.getInstance().getString(Constant.Background_Color)))
        when(SPUtils.getInstance().getInt(Constant.ThemeColorIndex,0)){
            0 -> {
                ImmersionBar.with(this@AppVolumePage).transparentStatusBar()
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
                ImmersionBar.with(this@AppVolumePage).transparentStatusBar()
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
                ImmersionBar.with(this@AppVolumePage).transparentStatusBar()
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
                ImmersionBar.with(this@AppVolumePage).transparentStatusBar()
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

}
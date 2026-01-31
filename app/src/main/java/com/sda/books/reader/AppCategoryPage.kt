package com.sda.books.reader

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.StringUtils
import com.blankj.utilcode.util.ToastUtils
import com.book.reader.R
import com.sda.books.reader.adapter.SubCategoryAdapter
import com.book.reader.databinding.ActivityCategoryBinding
import com.sda.books.reader.entity.Category

import com.sda.books.reader.frag.SubCategoryFragment
import com.sda.books.reader.loading.LoadingDialog
import com.sda.books.reader.util.AppSettingUtil
import com.sda.books.reader.util.Constant
import com.sda.books.reader.util.onClickWithScaleAnimation

import com.sda.books.reader.vm.AppCategoryViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.gyf.immersionbar.ImmersionBar
import com.sda.books.reader.api.RetrofitClient
import com.sda.books.reader.entity.UpdateDio
import com.sda.books.reader.view.CustomDialog
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class AppCategoryPage : BaseActivity() {

    companion object {
        val mUpdateDbOb = MutableLiveData<Boolean>()
    }

    lateinit var binding: ActivityCategoryBinding
    lateinit var viewModel: AppCategoryViewModel
    lateinit var loadingDialog: LoadingDialog
    var dialogUpdate : CustomDialog?= null
    var dialogNeedUpdate : CustomDialog?= null
    var isClick : Boolean = true
    override fun getRootView(): View {
        return binding.root
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("TAG", "==========0002")



        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[AppCategoryViewModel::class.java]
        binding.viewPager.isUserInputEnabled = false
        dialogNeedUpdate = CustomDialog(this,
            R.layout.update_need_dialog,
            intArrayOf(
                R.id.tv_old_version,
                R.id.tv_new_version,
                R.id.tv_app_size,
                R.id.tv_notice,
                R.id.tv_sure),
            0,
            false,
            false,
            Gravity.CENTER
        )
        dialogUpdate = CustomDialog(this,
            R.layout.update_dialog,
            intArrayOf(
                R.id.tv_old_version,
                R.id.tv_new_version,
                R.id.tv_app_size,
                R.id.tv_notice,
                R.id.tv_cancel,
                R.id.tv_sure),
            0,
            false,
            false,
            Gravity.CENTER
            )

        if(StringUtils.isEmpty(SPUtils.getInstance().getString(Constant.Background_Color))){
            SPUtils.getInstance().put(Constant.Background_Color,"#FFF6F5F3")
            SPUtils.getInstance().put(Constant.Border_Color,"@FF393938")
         }else{

        }
        if(SPUtils.getInstance().getInt(Constant.ThemeColorIndex,0) == 0){
            SPUtils.getInstance().put(Constant.ThemeColorIndex,0)
        }
        if(SPUtils.getInstance().getInt(Constant.LineCount,0) == 0){
            SPUtils.getInstance().put(Constant.LineCount,13)
        }
        if(SPUtils.getInstance().getInt(Constant.SearchAllFile,0) == 0){
            //1全局搜索  2极速搜索
            SPUtils.getInstance().put(Constant.SearchAllFile,1)
        }else{

        }


        //LogUtils.e("008========>>>>${SPUtils.getInstance().getBoolean(Constant.mandatory,false)}")
        lifecycleScope.launch {
            //先判断网络
            if(NetworkUtils.isConnected()){
               // LogUtils.e("008========>>>>")
                var params = mutableMapOf<String, String>()
                params["ver"]= AppUtils.getAppVersionName()
                //params["ver"]= "2.4.10"
                params["platform"]="android"
                params["channel"]="stable"
                if(AppSettingUtil.getIsOpenEn()){
                    params["locale"]="en"
                }else{
                    params["locale"]="zh-CN"
                }

                try {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.versionApi.checkVersion(params)
                    }
                    
                    if (response.isSuccessful && response.body() != null) {
                        val updateEntity = response.body()!!
                        LogUtils.e("下载链接: $updateEntity")
                        SPUtils.getInstance().put(Constant.mandatory, updateEntity.isMandatory())
                        LogUtils.e("下载链接" + SPUtils.getInstance().getBoolean(Constant.mandatory, false))
                        SPUtils.getInstance().put(Constant.loadUrl, updateEntity.getDownloadUrl() ?: "")
                        SPUtils.getInstance().put(Constant.newVersion, updateEntity.latestVersion ?: "")
                        SPUtils.getInstance().put(Constant.appSize, updateEntity.getFileSize() ?: "")
                        LogUtils.e("下载链接" + updateEntity.isMandatory())
                        
                        if (updateEntity.isMandatory()) {
                            val strNotice = updateEntity.getReleaseNotesText()
                            SPUtils.getInstance().put(Constant.loadNotice, strNotice)
                            dialogNeedUpdate?.let {
                                it.show()
                                var views = it.views
                                var tv_old_version = views[0] as TextView
                                var tv_new_version = views[1] as TextView
                                var tv_app_size = views[2] as TextView
                                var tv_notice = views[3] as TextView
                                tv_old_version.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG)
                                tv_old_version.getPaint().setAntiAlias(true)
                                tv_old_version.text = AppUtils.getAppVersionName()
                                tv_new_version.text = "新版" + SPUtils.getInstance().getString(Constant.newVersion)
                                tv_app_size.text = SPUtils.getInstance().getString(Constant.appSize)
                                tv_notice.text = SPUtils.getInstance().getString(Constant.loadNotice)
                                it.setOnDialogItemClickListener { dialog, view ->
                                    when (view.id) {
                                        R.id.tv_sure -> {
                                            if (isClick) {
                                                loadNewApp(SPUtils.getInstance().getString(Constant.loadUrl))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            if (updateEntity.hasUpdate()) {
                                val strNotice = updateEntity.getReleaseNotesText()
                                SPUtils.getInstance().put(Constant.loadNotice, strNotice)
                                dialogUpdate?.let {
                                    it.show()
                                    var views = it.views
                                    var tv_old_version = views[0] as TextView
                                    var tv_new_version = views[1] as TextView
                                    var tv_app_size = views[2] as TextView
                                    var tv_notice = views[3] as TextView
                                    tv_old_version.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG)
                                    tv_old_version.getPaint().setAntiAlias(true)
                                    tv_old_version.text = AppUtils.getAppVersionName()
                                    tv_new_version.text = "新版" + (updateEntity.latestVersion ?: "")
                                    tv_app_size.text = SPUtils.getInstance().getString(Constant.appSize)
                                    tv_notice.text = strNotice
                                    it.setOnDialogItemClickListener(object : CustomDialog.OnCustomDialogItemClickListener {
                                        override fun OnCustomDialogItemClick(dialog: CustomDialog?, view: View?) {
                                            when (view?.id) {
                                                R.id.tv_cancel -> {
                                                    dialogUpdate?.dismiss()
                                                }
                                                R.id.tv_sure -> {
                                                    dialogUpdate?.dismiss()
                                                    updateEntity.getDownloadUrl()?.let { url ->
                                                        loadNewApp(url)
                                                    }
                                                }
                                            }
                                        }
                                    })
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    LogUtils.e("版本检查失败: ${e.message}")
                }
            }else{
                if(SPUtils.getInstance().getBoolean(Constant.mandatory,false)){
                    if(!AppUtils.getAppVersionName().equals(SPUtils.getInstance().getString(Constant.newVersion)) ){
                        if(!StringUtils.isEmpty(SPUtils.getInstance().getString(Constant.loadUrl))){
                            dialogNeedUpdate?.let {
                                it.show()
                                var views = it.views
                                var tv_old_version = views[0] as TextView
                                var tv_new_version = views[1] as TextView
                                var tv_app_size = views[2] as TextView
                                var tv_notice = views[3] as TextView
                                tv_old_version.getPaint().setFlags(Paint. STRIKE_THRU_TEXT_FLAG ); //中间横线
                                tv_old_version.getPaint().setAntiAlias(true);// 抗锯齿
                                tv_old_version.text = AppUtils.getAppVersionName()
                                tv_new_version.text = "新版"+SPUtils.getInstance().getString(Constant.newVersion)
                                tv_app_size.text = SPUtils.getInstance().getString(Constant.appSize)

                                tv_notice.text = SPUtils.getInstance().getString(Constant.loadNotice)
                                it.setOnDialogItemClickListener { dialog, view ->
                                    when(view.id){
                                        R.id.tv_sure -> {
                                            //dialog.cancel()
                                            NetworkUtils.openWirelessSettings()
                                            /*if(isClick){
                                                //打开网络设置
                                                NetworkUtils.openWirelessSettings()

                                                loadNewApp(SPUtils.getInstance().getString(Constant.loadUrl))
                                            }*/

                                        }
                                    }
                                }
                            }


                        }else{
                            ToastUtils.showLong("请下载新的版本")

                        }
                    }else{

                    }


                }
            }
        }


        mUpdateDbOb.observe(this, {
            viewModel.queryData()
        })


        viewModel.categoryList.observe(this) {

            tabTv = "${viewModel.categoryList.value?.get(0)?.cateName}"
            updateUI(it)
            if (it.size == 0) {
                Toasty.normal(
                    applicationContext,
                    getString(R.string.toast_msg1)
                ).show()
                return@observe
            }
            viewModel.updateCurrentSelectParentId(it[0].id)
        }
        loadingDialog = LoadingDialog(this)
        // 确保设置按钮可点击
        binding.ivSetting.isClickable = true
        binding.ivSetting.isFocusable = true
        binding.ivSetting.onClickWithScaleAnimation {
            startActivity(Intent(this, AppSettingPage::class.java))
        }

        viewModel.queryData()
        viewModel.isShowLoading.observe(this) {
            if (it) {
                loadingDialog.show()
            } else {
                loadingDialog.dismiss()
            }
        }

        // 监听搜索框文本变化，显示/隐藏清空按钮
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val hasText = s?.toString()?.isNotEmpty() == true
                binding.ivClear.visibility = if (hasText) View.VISIBLE else View.GONE
            }
        })
        
        // 清空按钮点击事件
        binding.ivClear.onClickWithScaleAnimation {
            binding.etSearch.setText("")
            binding.etSearch.requestFocus()
        }

        binding.search.onClickWithScaleAnimation {
            if (binding.etSearch.text.isNotEmpty()) {
                AppSearchResultPage.Companion.start(
                    this,
                    "${binding.etSearch.text}",
                    viewModel.currentSubCategoryId,
                    viewModel.currentSelectParentId
                )
            }
        }
    }
    private fun loadNewApp(appUrl: String){
        isClick = false
        LogUtils.e("008========>>>>下载进度001")
        // 1. 创建一个 ACTION_VIEW 类型的 Intent
        val intent = Intent(Intent.ACTION_VIEW)
        // 2. 将 URL 字符串解析为 Uri 对象并设置给 Intent
        intent.data = Uri.parse(appUrl)
        // 3. 启动这个 Intent
        startActivity(intent)
        isClick =  true
    }

    //@SuppressLint("ResourceType")
    override fun onResume() {
        super.onResume()
        if(SPUtils.getInstance().getInt(Constant.TextViewLineHeight,0) == 0){
            SPUtils.getInstance().put(Constant.TextViewLineHeight,30)
        }
        ImmersionBar.with(this@AppCategoryPage).transparentStatusBar()
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




        updateHitTip()
    }

    fun updateHitTip() {
        if (viewModel.categoryList.value?.isEmpty() != false) return
        val language = if (AppSettingUtil.getIsOpenEn()) "en" else "zh"
        val formattedString = String.Companion.format(
            LanguageUtils.getStringByLanguage(this, R.string.search_bible_hint1, language),
            if (viewModel.categoryList.value!![currentCategoryIndex].cateName.contains("-")){
                viewModel.categoryList.value!![currentCategoryIndex].cateName.split(
                    "-",
                    limit = 2
                )[1]
            }

            else{
                viewModel.categoryList.value!![currentCategoryIndex].cateName
            }
        )

        LogUtils.e("==========${formattedString}")

        binding.etSearch.hint = "搜索\"${formattedString}\"关键词"

    }


    var currentCategoryIndex = 0
    private var tabTv = ""
    private fun updateUI(categoryList: List<Category>) {


        val adapter = SubCategoryAdapter(this, categoryList, tabTv)

        binding.categoryTab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {


                LogUtils.e("================${categoryList.size}")

                val textView = tab.customView as TextView?
                textView?.setTextAppearance(R.style.tabSelectStyle)


                viewModel.updateCurrentSelectParentId(
                    viewModel.categoryList.value?.get(tab.position)?.id ?: 0
                )

                currentCategoryIndex = tab.position
                tabTv = "${viewModel.categoryList.value?.get(tab.position)?.cateName}"

                SubCategoryFragment.mTabData.value = tabTv
                updateHitTip()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                val textView = tab.customView as TextView?
                textView?.setTextAppearance(R.style.tabStyle)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.viewPager.adapter = adapter
        // 联动 TabLayout 和 ViewPager2
        TabLayoutMediator(binding.categoryTab, binding.viewPager) { tab, position ->
            tab.text = if (categoryList[position].cateName.contains("-"))
                categoryList[position].cateName.split(
                    "-",
                    limit = 2
                )[1] else categoryList[position].cateName
            val textView = TextView(this)
            textView.setTextAppearance(R.style.tabStyle)
            textView.text = if (categoryList[position].cateName.contains("-"))
                categoryList[position].cateName.split(
                    "-",
                    limit = 2
                )[1] else categoryList[position].cateName
            tab.setCustomView(textView)
            textView.setPadding(10, 10, 10, 10)
//            textView.setBackgroundResource(R.drawable.btn_ripple)

        }.attach()
    }

}

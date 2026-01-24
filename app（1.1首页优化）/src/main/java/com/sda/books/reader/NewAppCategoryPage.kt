package com.sda.books.reader

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.StringUtils
import com.book.reader.R

import com.book.reader.databinding.ActivityCategoryNewBinding
import com.sda.books.reader.loading.LoadingDialog
import com.sda.books.reader.util.Constant
import com.sda.books.reader.vm.AppCategoryViewModel
import com.gyf.immersionbar.ImmersionBar


class NewAppCategoryPage : BaseActivity() {

    companion object {
        val mUpdateDbOb = MutableLiveData<Boolean>()
    }

    lateinit var binding: ActivityCategoryNewBinding
    lateinit var viewModel: AppCategoryViewModel
    lateinit var loadingDialog: LoadingDialog
    override fun getRootView(): View {
        return binding.root
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("TAG", "==========0002")
        binding = ActivityCategoryNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(StringUtils.isEmpty(SPUtils.getInstance().getString(Constant.Background_Color))){
            SPUtils.getInstance().put(Constant.Background_Color,"#FFF6F5F3")
            SPUtils.getInstance().put(Constant.Border_Color,"@FF393938")
         }else{

        }
        if(SPUtils.getInstance().getInt(Constant.ThemeColorIndex,0) == 0){
            SPUtils.getInstance().put(Constant.ThemeColorIndex,0)
        }
        if(SPUtils.getInstance().getInt(Constant.LineCount,0) == 0){
            SPUtils.getInstance().put(Constant.LineCount,10)
        }
        if(SPUtils.getInstance().getInt(Constant.SearchAllFile,0) == 0){
            //1全局搜索  2极速搜索
            SPUtils.getInstance().put(Constant.SearchAllFile,1)
        }else{

        }

        mUpdateDbOb.observe(this, {
            viewModel.queryData()
        })
        viewModel = ViewModelProvider(this)[AppCategoryViewModel::class.java]


        loadingDialog = LoadingDialog(this)
        binding.ivSetting.setOnClickListener {
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

        binding.search.setOnClickListener {

            if (binding.etSearch.text.isEmpty()) return@setOnClickListener
            AppSearchResultPage.start(
                this,
                "${binding.etSearch.text}",
                viewModel.currentSubCategoryId,
                viewModel.currentSelectParentId
            )
        }
    }

    //@SuppressLint("ResourceType")
    override fun onResume() {
        super.onResume()
        if(SPUtils.getInstance().getInt(Constant.TextViewLineHeight,0) == 0){
            SPUtils.getInstance().put(Constant.TextViewLineHeight,60)
        }
        ImmersionBar.with(this@NewAppCategoryPage).transparentStatusBar()
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






}
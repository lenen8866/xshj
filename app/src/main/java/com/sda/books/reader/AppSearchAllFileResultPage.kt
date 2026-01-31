package com.sda.books.reader

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.SPUtils
import com.book.reader.R
import com.sda.books.reader.adapter.SearchResultContentAdapter
import com.book.reader.databinding.ActivitySearchResultBinding
import com.sda.books.reader.db.DatabaseHelper
import com.sda.books.reader.entity.Category
import com.sda.books.reader.entity.SearchResultEntity
import com.sda.books.reader.loading.LoadingDialog
import com.sda.books.reader.util.Constant
import com.sda.books.reader.util.getChapterContentShowList
import com.google.android.material.tabs.TabLayout
import com.gyf.immersionbar.ImmersionBar
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch


class AppSearchAllFileResultPage : BaseActivity() {


    companion object {
        fun start(context: Context,searchContent:String,queryId:Int,categoryId:Int){
            context.startActivity(Intent(context,AppSearchAllFileResultPage::class.java).apply {
                putExtra("searchContent",searchContent)
                putExtra("queryId",queryId)
                putExtra("categoryId",categoryId)
            })
        }
    }

    lateinit var binding:ActivitySearchResultBinding

    lateinit var loadingDialog: LoadingDialog
    private var searchContent = ""
    lateinit var adapter: SearchResultContentAdapter
    private var categoryId = 0
    private var queryId = 0
    private var selectSubCategoryId = 0

    // 添加分页相关变量
    private var currentPage = 0
    private val pageSize = 40 // 每页数量
    private var isLoading = false
    private var hasMore = true
    override fun getRootView(): View {
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Log.e("TAG","==========0003")
        searchContent = intent.getStringExtra("searchContent") ?:""
        categoryId = intent.getIntExtra("categoryId",0)
        queryId = intent.getIntExtra("queryId",0)
        selectSubCategoryId = queryId
        binding = ActivitySearchResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadingDialog = LoadingDialog(this)
        binding.llSearchResult.visibility = View.VISIBLE

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.searchContentList.layoutManager = LinearLayoutManager(this)
        adapter = SearchResultContentAdapter(this,pageSize)
        binding.searchContentList.adapter = adapter

        queryCategoryList()

        binding.tvResetSearch.setOnClickListener {
            updateSelectSubCategory(subCategoryList[0])
            binding.categoryTab.visibility = View.VISIBLE
            binding.subCategoryTab.visibility = View.VISIBLE
        }

        binding.etSearch.setText(searchContent)

        binding.search.setOnClickListener {
            /*updateSelectSubCategory(subCategoryList[0])
            binding.categoryTab.visibility = View.VISIBLE
            binding.subCategoryTab.visibility = View.VISIBLE*/
            val selectSubCategoryId = selectSubCategory?.id ?: -1
            /*binding.categoryTab.visibility = View.GONE
            binding.subCategoryTab.visibility = View.GONE*/
            lifecycleScope.launch {
                loadingDialog.show()
                if(selectSubCategoryId == -1){
                    searchLimlt(subCategoryList.map { it.id })
                }else {
                    searchLimlt(listOf(selectSubCategoryId))
                }
                loadingDialog.dismiss()
            }
        }


    }

    fun isAllEnglish(text: String): Boolean {
        return text.matches("^[a-zA-Z\\x20-\\x7E]+$".toRegex())
    }

    private suspend fun searchDatabase(
        queryIds: List<Int>,
        searchContentList: List<String>,
    ): List<SearchResultEntity> {
//        Toasty.normal(this,"page:$page pageSize:$pageSize",Toasty.LENGTH_SHORT).show()
        val result = DatabaseHelper.getInstance().querylimitChapterWithSubCategoryIdFindAll(queryIds,searchContentList)

       // LogUtils.e("查询id==========>>>${result.size}")
        val searchResultList = mutableListOf<SearchResultEntity>()
        binding.tvSearchCount.text = "${result.size}"

        result.forEach { chapter ->
            //LogUtils.e("=============>>>>>>>001")


            val contentList = getChapterContentShowList(chapter.content)
            contentList.forEachIndexed { index, matchContent ->
                var isAllContains = true
                for (item in searchContentList) {
                    if(isAllEnglish(item)){
                        if (!matchContent.getShowContent().contains(item+" ")) {
                            isAllContains = false
                            break
                        }
                    }else{
                        if (!matchContent.getShowContent().contains(item)) {
                            isAllContains = false
                            break
                        }
                    }

                }


                if (isAllContains) {
                    searchResultList.add(
                        SearchResultEntity().apply {
                            content = matchContent
                            this.chapter = chapter
                            scrollIndex = index
                        }
                    )

                    //return@searchDatabase searchResultList
                    /*// 如果达到当前页所需数量，提前返回
                    if (searchResultList.size >= pageSize){
                        return@searchDatabase searchResultList
                    }else{
                        //loadNextPage()
                    }*/
                }



            }
        }
        /*if(searchResultList.size < 40){
            LogUtils.e
            loadNextPage()
        }
*/
        return searchResultList
    }


    private suspend fun searchLimlt(queryIds: List<Int>) {
        val searchContent = binding.etSearch.text.toString()
        if (searchContent.isEmpty()) return

        // 重置分页状态
        currentPage = 0
        hasMore = true
        isLoading = true
        adapter.clearData()
        var list = ArrayList<String>()
        if(isAllEnglish(searchContent)){
            list.add(searchContent)
        }else{
            searchContent.split(" ").filter {
                list.add(it)
            }
        }

        adapter.updateMatchList(list)
        loadingDialog.show()

        try {
            val results = searchDatabase(queryIds, adapter.matchList)
            results.map { it.tabs = tabs }
            if (results.isNotEmpty()) {
                currentPage = 1
                adapter.updateData(results)
                hasMore = results.size >= pageSize
            } else {
                Toasty.normal(
                    applicationContext,
                    "本次\"${selectCategory?.getShowName() ?: ""}-${selectSubCategory?.getShowName() ?: ""}\"搜索,没有找到关键词"
                ).show()
            }
        } catch (e: Exception) {
            Log.e("Search", "搜索失败", e)
        } finally {
            isLoading = false
            loadingDialog.dismiss()
        }
    }


    private var categoryList = listOf<Category>()
    private fun queryCategoryList(){
        lifecycleScope.launch {
            loadingDialog.show()
            categoryList = DatabaseHelper.getInstance().queryCategoryList()
            updateUI(categoryList)
            loadingDialog.dismiss()
        }
    }

    var isFirstEnter = true
    private suspend fun updateUI(categoryList:List<Category>){

        categoryList.forEach {
            binding.categoryTab.addTab(binding.categoryTab.newTab().setText(if(it.cateName.contains("-")) it.cateName.split("-", limit = 2)[1] else it.cateName))
        }

        binding.categoryTab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                updateCategory(categoryList[tab.position])
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.subCategoryTab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                updateSelectSubCategory(subCategoryList[tab.position])
                isFirstEnter = false
                val selectSubCategoryId = selectSubCategory?.id ?: -1
                lifecycleScope.launch {
                    loadingDialog.show()
                    if (selectSubCategoryId == -1) {
                        searchLimlt(subCategoryList.map { it.id })
                    } else {
                        searchLimlt(listOf(selectSubCategoryId))
                    }
                    loadingDialog.dismiss()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        val selectIndex = categoryList.indexOfFirst { it.id == categoryId }
        val categoryTabCurrentIndex = binding.categoryTab.selectedTabPosition
        //如果tab的默认下标 已经是想要选中的下标
        if(selectIndex==-1){
//            Toast.makeText(this,getString(R.string.toast_msg1),Toast.LENGTH_SHORT).show()
        }else {
            if (selectIndex == categoryTabCurrentIndex) {
                updateCategory(categoryList[selectIndex])
            } else {
                binding.categoryTab.getTabAt(selectIndex)?.select()
            }
        }
        if(queryId == -1){
            searchLimlt(subCategoryList.map { it.id })
        }else {
            Log.i("cccccc","searcj111")
            searchLimlt(listOf(queryId))
        }
    }
    var selectSubCategory:Category? = null
    var selectCategory:Category? = null
    var tabs = ""
    private fun updateSelectSubCategory(category:Category){
        selectSubCategory = category
        tabs = "${selectCategory?.getShowName()}-${selectSubCategory?.getShowName()}"
        binding.tvHasSelect.text = "已选中:${selectCategory?.getShowName()}-${selectSubCategory?.getShowName()}"
        binding.tvSearchName.text = binding.etSearch.text
    }

    private fun updateCategory(category: Category){

        if(!isFirstEnter){
            //切换一级分类 二级分类选择全部
            selectSubCategoryId = -1
        }
        updateSubChapterTab(category.id)
        isFirstEnter = false

        selectCategory = category
        tabs = "${selectCategory?.getShowName()}-${selectSubCategory?.getShowName()}"
        binding.tvHasSelect.text = "已选中:${selectCategory?.getShowName()}-${selectSubCategory?.getShowName()}"
        binding.tvSearchName.text = binding.etSearch.text


    }

    private var subCategoryList = mutableListOf<Category>()
    private fun updateSubChapterTab(parentId:Int){
        val data = DatabaseHelper.getInstance().querySubCategory(parentId)
        subCategoryList.clear()
        subCategoryList.add(Category()
            .apply {
                cateName = "全部"
                id = -1
            })

        subCategoryList.addAll(data)
        binding.subCategoryTab.removeAllTabs()
        subCategoryList.forEach {
            if(it.id == -1){
                binding.subCategoryTab.addTab(binding.subCategoryTab.newTab().setText(it.cateName))
            }else {
                binding.subCategoryTab.addTab(binding.subCategoryTab.newTab().setText(if(it.cateName.contains("-")) it.cateName.split("-", limit = 2)[1] else it.cateName))
            }
        }

        Log.i("cccccccc","selectSubCategoryId====$selectSubCategoryId")
        var selectIndex = subCategoryList.indexOfFirst { it.id == selectSubCategoryId }
        if(selectIndex == -1){
            selectIndex = 0
        }
        binding.subCategoryTab.getTabAt(selectIndex)?.select()
    }
    private suspend fun search(queryIds:List<Int>){

        val searchContent = binding.etSearch.text
        if(searchContent.isEmpty())return
        val searchContentList = searchContent.split(" ").filter {
            it.isNotEmpty()
        }

        Log.i("ccccccc","queryIds====${queryIds}")

        adapter.updateMatchList(searchContentList)

        val result =  DatabaseHelper.getInstance().queryChapterWithSubCategoryId(queryIds)
        val searchResultList = mutableListOf<SearchResultEntity>()
        result.forEach {
            val contentList = getChapterContentShowList(it.content)
//                val contentList =  it.content.split("\n")
            contentList.forEachIndexed { index,matchContent->
                //查找每一个段落是否包含搜素吃
                var isAllContains = true
                for(item in searchContentList){
                    //如果找到 直接遍历下一段
                    if(!matchContent.getShowContent().contains(item)){
                        isAllContains = false
                        break
                    }
                }
                if(isAllContains){
                    searchResultList
                        .add(
                            SearchResultEntity()
                                .apply {
                                    //这个是已经处理过的content
                                    content = matchContent
                                    chapter = it
                                    scrollIndex = index
                                }
                        )
                }
            }
        }
        adapter.updateData(searchResultList)

        if(searchResultList.isEmpty()){

            Toasty.normal(applicationContext, "本次\"${selectCategory?.getShowName() ?:""}-${selectSubCategory?.getShowName() ?:""}\"搜索,没有找到关键词").show();
        }
    }

    override fun onResume() {
        super.onResume()
        binding.main.setBackgroundColor(Color.parseColor(SPUtils.getInstance().getString(Constant.Background_Color)))
        when(SPUtils.getInstance().getInt(Constant.ThemeColorIndex,0)){
            0 -> {
                ImmersionBar.with(this@AppSearchAllFileResultPage).transparentStatusBar()
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
                ImmersionBar.with(this@AppSearchAllFileResultPage).transparentStatusBar()
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
                ImmersionBar.with(this@AppSearchAllFileResultPage).transparentStatusBar()
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
                ImmersionBar.with(this@AppSearchAllFileResultPage).transparentStatusBar()
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
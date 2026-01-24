package com.sda.books.reader

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
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


class AppSearchResultPage : BaseActivity() {


    companion object {
        fun start(context: Context,searchContent:String,queryId:Int,categoryId:Int){
            context.startActivity(Intent(context,AppSearchResultPage::class.java).apply {
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
    private val pageSize = 20 // 每页数量
    private var isLoading = false
    private var hasMore = true
    override fun getRootView(): View {
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        searchContent = intent.getStringExtra("searchContent") ?:""
        categoryId = intent.getIntExtra("categoryId",0)
        queryId = intent.getIntExtra("queryId",-1)
        LogUtils.e("查询页queryId===================>>>>>>>${queryId}")
        selectSubCategoryId = queryId
        LogUtils.e("查询页queryId===================>>>>>>>${selectSubCategoryId}")
        LogUtils.e("查询页categoryId===================>>>>>>>${categoryId}")


        binding = ActivitySearchResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadingDialog = LoadingDialog(this)

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
                LogUtils.e("====================>>"+selectSubCategoryId);
                if(selectSubCategoryId == -1){
                    searchLimlt(subCategoryList.map { it.id })
                    LogUtils.e("offsetid=============>>001--5")
                   // LogUtils.e("====================>>001");
                    loadItemCount(subCategoryList.map { it.id }, adapter.matchList)
                    LogUtils.e("查询Id0007----${subCategoryList.map { it.id }}")
                }else {
                    searchLimlt(listOf(selectSubCategoryId))
                    LogUtils.e("offsetid=============>>001--6")
                    //LogUtils.e("====================>>002");
                    loadItemCount(listOf(selectSubCategoryId),adapter.matchList)
                    LogUtils.e("查询Id0008----${selectSubCategoryId}")
                }
                loadingDialog.dismiss()
            }
        }

        // 添加滚动监听
        binding.searchContentList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                // 当滑动到第30条数据时加载下一页（预加载10条）
                if (!isLoading && hasMore) {
                    if (lastVisibleItemPosition >= totalItemCount - 10) {
                        loadNextPage()
                    }
                }
            }
        })
    }
    private fun loadNextPage() {
        if (isLoading || !hasMore) return

        isLoading = true
        // 显示加载状态
        adapter.showLoading(true)

        lifecycleScope.launch {
            try {
                val queryIds = if (selectSubCategoryId == -1) {
                    subCategoryList.map { it.id }
                } else {
                    listOf(selectSubCategoryId)
                }

                val searchContent = binding.etSearch.text.toString()
                if (searchContent.isEmpty()) return@launch


                var list = ArrayList<String>()
                //LogUtils.e("查询id==========${searchContent}")
                if(isAllEnglish(searchContent)){
                    list.add(searchContent)
                }else{
                    searchContent.split(" ").filter {
                        // LogUtils.e("查询id==========${it}")
                        list.add(it)
                    }
                }

                //list.add(searchContent.toString().trim())
                val newResults = searchDatabase(
                    queryIds,
                    list
                )
                LogUtils.e("页数--${currentPage}==========>>>${newResults.size}")
                newResults.distinct()

               // LogUtils.e("offsetid=============>>002")
               // newResults.map { it.tabs = tabs }
               // LogUtils.e("页数${currentPage}==========>>>${newResults.size}")
                if (newResults.isNotEmpty()) {
                    currentPage++
                    adapter.appendData(newResults)
                    //LogUtils.e("列表数据===========>>>>>>${currentPage}")
                    //LogUtils.e("列表数据===========>>>>>>${newResults.size}")
                    hasMore = newResults.size >= pageSize
                }
                // 添加底线判断逻辑
                val shouldAddEndMarker = newResults.isEmpty() || newResults.size < pageSize
                // 添加底线标记
                if (shouldAddEndMarker && !adapter.hasEndMarker()) {
                    adapter.appendData(mutableListOf(SearchResultEntity().apply {
                        isEndData = true
                    }))
                    hasMore = false
                }
            } catch (e: Exception) {
                Log.e("Pagination", "加载下一页失败", e)
            } finally {
                isLoading = false
                adapter.showLoading(false)
                // 没有更多数据时显示底线
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
        val result = DatabaseHelper.getInstance().querylimitChapterWithSubCategoryId1(queryIds,searchContentList, pageSize,currentPage)


        //val resCount = DatabaseHelper.getInstance().querylimitChapterWithSubCategoryId2(queryIds,searchContentList)

        //LogUtils.e("查询id=============>>====${result.size}")
        val searchResultList = mutableListOf<SearchResultEntity>()
        LogUtils.e("总页数======${result.size}=====列表数据===========>>>>>>${searchResultList.distinct().size}")
        result.forEach { chapter ->
            LogUtils.e("查询页数======${chapter}=====列表数据===========>>>>>>${searchResultList.distinct().size}")
            val contentList = getChapterContentShowList(chapter.content)

            LogUtils.e("=============${contentList}")
            LogUtils.e("=============${searchContentList}")



            for (index in 0 until contentList.size){
                var isAllContains = false
                for (item in searchContentList) {
                    if(isAllEnglish(item)){
                        searchResultList.add(
                            SearchResultEntity().apply {
                                content = contentList[index]
                                this.chapter = chapter
                                scrollIndex = index
                            }
                        )

                    }else{
                        searchResultList.add(
                            SearchResultEntity().apply {
                                content = contentList[index]
                                this.chapter = chapter
                                scrollIndex = index
                            }
                        )

                    }

                }
            }

        }
        LogUtils.e("页数${pageSize}=====列表数据===========>>>>>>${searchResultList.distinctBy { it.chapter }.size}")
        if (searchResultList.size >= pageSize){
            return@searchDatabase searchResultList.distinctBy {
                it.chapter
            }
        }else{
            loadNextPage()
        }


        return searchResultList
    }


    private suspend fun searchLimlt(queryIds: List<Int>) {
        LogUtils.e("=================>>>>>${queryIds}")

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
            LogUtils.e("offsetid=============>>001")
            //LogUtils.e("查询id==========>>>===》》${results.size}")
            results.map { it.tabs = tabs }
            //LogUtils.e("查询id==========>>>===${results.size}")
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

    private suspend fun loadItemCount(
        queryIds: List<Int>,
        matchList: List<String>
    ) {
        //LogUtils.e("查询Id0001----${queryIds}")

        var count = DatabaseHelper.getInstance().querylimitChapterWithSubCategoryIdCount(queryIds,matchList);
        LogUtils.e("====================>>$count")
        if(SPUtils.getInstance().getInt(Constant.SearchAllFile,0) == 1){
            binding.llSearchResult.visibility = View.VISIBLE
            binding.tvSearchName.text = binding.etSearch.text
            binding.tvSearchCount.text = "${count}"
        }else{
            binding.llSearchResult.visibility = View.GONE
        }
    }


    private var categoryList = listOf<Category>()
    private fun queryCategoryList(){
        lifecycleScope.launch {
            loadingDialog.show()
            DatabaseHelper.getInstance().openDatabase()
            categoryList = DatabaseHelper.getInstance().queryCategoryList()
            //categoryList = DatabaseHelper.getInstance().querySubCategory1("")
            updateUI(categoryList)

            loadingDialog.dismiss()
        }
    }

    var isFirstEnter = true
    private suspend fun updateUI(categoryList:List<Category>){

        LogUtils.e("=====================${categoryList}")
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
                        LogUtils.e("offsetid=============>>001--1")
                        //LogUtils.e("====================>>003");
                        loadItemCount(subCategoryList.map { it.id },adapter.matchList)
                        LogUtils.e("查询Id0002----${subCategoryList}")
                    } else {
                        searchLimlt(listOf(selectSubCategoryId))
                        LogUtils.e("offsetid=============>>001--2")
                        //LogUtils.e("====================>>004");
                        loadItemCount(listOf(selectSubCategoryId),adapter.matchList)
                        LogUtils.e("查询Id0003----${selectSubCategoryId}")
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
       /* if(queryId == -1){
            searchLimlt(subCategoryList.map { it.id })
            LogUtils.e("offsetid=============>>001--3")
            LogUtils.e("====================>>005");
            loadItemCount(subCategoryList.map { it.id },adapter.matchList)
            LogUtils.e("查询Id0004----${subCategoryList.map { it.id }}")
        }else {
            Log.i("cccccc","searcj111")
            searchLimlt(listOf(queryId))
            LogUtils.e("offsetid=============>>001--4")
            LogUtils.e("====================>>005");
            loadItemCount(listOf(queryId),adapter.matchList)
            LogUtils.e("查询Id0006----${listOf(queryId)}")
        }*/
    }
    var selectSubCategory:Category? = null
    var selectCategory:Category? = null
    var tabs = ""
    private fun updateSelectSubCategory(category:Category){
        selectSubCategory = category
        tabs = "${selectCategory?.getShowName()}-${selectSubCategory?.getShowName()}"
        binding.tvHasSelect.text = "已选中:${selectCategory?.getShowName()}-${selectSubCategory?.getShowName()}"
    }

    private fun updateCategory(category: Category){

        if(!isFirstEnter){
            //切换一级分类 二级分类选择全部
            selectSubCategoryId = -1
        }
        //updateSubChapterTab(category.id)
        updateSubChapterTabName(category.cateName)
        isFirstEnter = false

        selectCategory = category
        tabs = "${selectCategory?.getShowName()}-${selectSubCategory?.getShowName()}"
        binding.tvHasSelect.text = "已选中:${selectCategory?.getShowName()}-${selectSubCategory?.getShowName()}"
    }

    private var subCategoryList = mutableListOf<Category>()

    private fun updateSubChapterTabName(name: String){
        val data = DatabaseHelper.getInstance().querySubCategory1(name)
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
        //LogUtils.e("列表数据===========>>>>>>${searchContentList.size}")
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
                ImmersionBar.with(this@AppSearchResultPage).transparentStatusBar()
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
                ImmersionBar.with(this@AppSearchResultPage).transparentStatusBar()
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
                ImmersionBar.with(this@AppSearchResultPage).transparentStatusBar()
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
                ImmersionBar.with(this@AppSearchResultPage).transparentStatusBar()
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
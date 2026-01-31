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
import com.sda.books.reader.entity.ChapterContentItem
import com.sda.books.reader.loading.LoadingDialog
import com.sda.books.reader.util.Constant
import com.sda.books.reader.util.AppSettingUtil
import com.google.android.material.tabs.TabLayout
import com.gyf.immersionbar.ImmersionBar
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch


class AppSearchResultPage : BaseActivity() {


    companion object {
        private const val EXTRA_SEARCH_CONTENT = "searchContent"
        private const val EXTRA_QUERY_ID = "queryId"
        private const val EXTRA_CATEGORY_ID = "categoryId"
        
        fun start(context: Context, searchContent: String, queryId: Int, categoryId: Int) {
            context.startActivity(Intent(context, AppSearchResultPage::class.java).apply {
                putExtra(EXTRA_SEARCH_CONTENT, searchContent)
                putExtra(EXTRA_QUERY_ID, queryId)
                putExtra(EXTRA_CATEGORY_ID, categoryId)
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

        searchContent = intent.getStringExtra(EXTRA_SEARCH_CONTENT) ?: ""
        categoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, 0)
        queryId = intent.getIntExtra(EXTRA_QUERY_ID, -1)
        selectSubCategoryId = queryId


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
        
        // 监听搜索框文本变化，显示/隐藏清空按钮
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val hasText = s?.toString()?.isNotEmpty() == true
                binding.ivClear.visibility = if (hasText && !isLoading) View.VISIBLE else View.GONE
            }
        })
        
        // 清空按钮点击事件
        binding.ivClear.setOnClickListener {
            binding.etSearch.setText("")
            binding.etSearch.requestFocus()
        }
        
        // 初始化清空按钮显示状态
        if (searchContent.isNotEmpty()) {
            binding.ivClear.visibility = View.VISIBLE
        }

        binding.search.setOnClickListener {
            if (isLoading) return@setOnClickListener
            
            val searchText = binding.etSearch.text.toString().trim()
            if (searchText.isEmpty()) {
                Toasty.normal(applicationContext, "请输入搜索关键词").show()
                return@setOnClickListener
            }
            
            lifecycleScope.launch {
                val queryIds = getQueryIds()
                searchLimlt(queryIds)
            }
        }

        // 添加滚动监听
        binding.searchContentList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                // 当滑动到倒数第10条数据时加载下一页（预加载）
                if (!isLoading && hasMore && lastVisibleItemPosition >= totalItemCount - 10) {
                    lifecycleScope.launch {
                        loadNextPage()
                    }
                }
            }
        })
    }
    private suspend fun loadNextPage() {
        if (isLoading || !hasMore) return

        isLoading = true
        adapter.showLoading(true)

        try {
            val queryIds = getQueryIds()
            val searchContent = binding.etSearch.text.toString()
            if (searchContent.isEmpty()) {
                isLoading = false
                adapter.showLoading(false)
                return
            }

            val keywords = parseSearchKeywords(searchContent)
            val newResults = searchDatabase(queryIds, keywords)
            
            if (newResults.isNotEmpty()) {
                currentPage++
                adapter.appendData(newResults)
                hasMore = newResults.size >= pageSize
            }
            
            // 添加底线标记
            val shouldAddEndMarker = newResults.isEmpty() || newResults.size < pageSize
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
        }
    }
    private fun isAllEnglish(text: String): Boolean {
        return text.matches("^[a-zA-Z\\x20-\\x7E]+$".toRegex())
    }
    
    /**
     * 将搜索文本转换为关键词列表
     */
    private fun parseSearchKeywords(searchContent: String): List<String> {
        return if (isAllEnglish(searchContent)) {
            listOf(searchContent)
        } else {
            searchContent.split(" ").filter { it.isNotBlank() }
        }
    }
    
    /**
     * 获取当前选中的查询 ID 列表
     */
    private fun getQueryIds(): List<Int> {
        val selectedId = selectSubCategory?.id ?: selectSubCategoryId
        return if (selectedId == -1) {
            subCategoryList.map { it.id }
        } else {
            listOf(selectedId)
        }
    }

    private suspend fun searchDatabase(
        queryIds: List<Int>,
        searchContentList: List<String>,
    ): List<SearchResultEntity> {

        val result = DatabaseHelper.getInstance()
            .querylimitChapterWithSubCategoryId1(queryIds, searchContentList, pageSize, currentPage)


        //val resCount = DatabaseHelper.getInstance().querylimitChapterWithSubCategoryId2(queryIds,searchContentList)

        //LogUtils.e("查询id=============>>====${result.size}")
        val searchResultList = mutableListOf<SearchResultEntity>()
        LogUtils.e("总页数======${result.size}=====列表数据===========>>>>>>${searchResultList.distinct().size}")
        result.forEach { chapter ->
            LogUtils.e("查询页数======${chapter}=====列表数据===========>>>>>>${searchResultList.distinct().size}")
            // 搜索阶段不要受内容显示模式（中/双/EN）影响，否则会导致
            // 例如当前只显示英文时，无法在同一章节中命中中文行
            // 这里手动构造“不过滤语言的内容行列表”
            val contentList = chapter.content
                .split("\n")
                .map { ChapterContentItem().init(it) }
                .filter { !it.isNoFilter() }

            LogUtils.e("=============${contentList}")
            LogUtils.e("=============${searchContentList}")



            // 从 chapter.categoryId 查询分类路径
            val categoryTabs = try {
                val category = DatabaseHelper.getInstance().queryCategoryById(chapter.categoryId)
                if (category != null && category.cateName.isNotEmpty()) {
                    // 如果分类名称包含 "/"，说明是 "父分类/子分类" 格式
                    if (category.cateName.contains("/")) {
                        val parts = category.cateName.split("/", limit = 2)
                        "${parts[0]}-${parts[1]}"
                    } else {
                        // 如果没有 "/"，尝试查询父分类
                        val parentCategory = DatabaseHelper.getInstance().queryCategoryParent(chapter.categoryId)
                        if (parentCategory != null) {
                            "${parentCategory.cateName}-${category.cateName}"
                        } else {
                            category.cateName
                        }
                    }
                } else {
                    tabs // 如果查询失败，使用搜索页面的 tabs
                }
            } catch (e: Exception) {
                LogUtils.e("查询分类路径失败: ${e.message}")
                tabs // 如果查询失败，使用搜索页面的 tabs
            }

            for (index in 0 until contentList.size) {
                val lineText = contentList[index].getShowContent()
                // 只要这一行包含任意一个搜索词，就认为是命中；避免把完全不相关的行塞进结果列表
                val hit = searchContentList.any { keyword ->
                    keyword.isNotBlank() && lineText.contains(keyword, ignoreCase = true)
                }
                if (hit) {
                    searchResultList.add(
                        SearchResultEntity().apply {
                            content = contentList[index]
                            this.chapter = chapter
                            scrollIndex = index
                            // 设置分类路径
                            this.tabs = categoryTabs
                        }
                    )
                }
            }

        }
        LogUtils.e("页数${pageSize}=====列表数据===========>>>>>>${searchResultList.size}")
        if (searchResultList.size >= pageSize){
            return@searchDatabase searchResultList
        }else{
            loadNextPage()
        }


        return searchResultList
    }


    private suspend fun searchLimlt(queryIds: List<Int>) {
        LogUtils.e("=================>>>>>${queryIds}")

        val searchContent = binding.etSearch.text.toString()
        if (searchContent.isEmpty()) return

        // 根据输入内容和当前内容模式，智能切换“中/双/EN”以保证能看到命中的语言
        val currentMode = AppSettingUtil.getContentMode() // 0=中，1=双，2=EN
        val isEnglishQuery = isAllEnglish(searchContent)
        if (currentMode == 0 && isEnglishQuery) {
            // 只显示中文时，如果用户输入纯英文关键词，强制切到双语，方便看到英文上下文
            AppSettingUtil.updateContentMode(1)
        } else if (currentMode == 2 && !isEnglishQuery) {
            // 只显示英文时，如果用户输入中文或中英混合，也切到双语，方便看到中文位置
            AppSettingUtil.updateContentMode(1)
        }

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

        // ====== 搜索开始：禁用搜索框和搜索按钮，显示加载状态 ======
        setSearchLoadingState(true)

        loadingDialog.show()

        try {
            // ====== 先获取并显示结果总数 ======
            val totalCount = DatabaseHelper.getInstance().querylimitChapterWithSubCategoryIdCount(queryIds, adapter.matchList)
            LogUtils.e("====================>>$totalCount")
            if(SPUtils.getInstance().getInt(Constant.SearchAllFile,0) == 1){
                binding.llSearchResult.visibility = View.VISIBLE
                binding.tvSearchName.text = binding.etSearch.text
                binding.tvSearchCount.text = "${totalCount}"
            }else{
                binding.llSearchResult.visibility = View.GONE
            }
            
            // ====== 再获取并显示结果列表 ======
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
            // ====== 搜索完成：恢复搜索框和搜索按钮可用状态 ======
            setSearchLoadingState(false)
        }
    }



    private var categoryList = listOf<Category>()
    
    private fun queryCategoryList() {
        lifecycleScope.launch {
            loadingDialog.show()
            DatabaseHelper.getInstance().openDatabase()
            categoryList = DatabaseHelper.getInstance().queryCategoryList()
            updateUI(categoryList)
            loadingDialog.dismiss()
        }
    }

    var isFirstEnter = true
    
    private suspend fun updateUI(categoryList: List<Category>) {
        // 添加分类 Tab
        categoryList.forEach { category ->
            val tabName = if (category.cateName.contains("-")) {
                category.cateName.split("-", limit = 2)[1]
            } else {
                category.cateName
            }
            binding.categoryTab.addTab(binding.categoryTab.newTab().setText(tabName))
        }

        // 设置分类 Tab 监听器
        binding.categoryTab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                updateCategory(categoryList[tab.position])
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // 设置子分类 Tab 监听器
        binding.subCategoryTab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                updateSelectSubCategory(subCategoryList[tab.position])
                isFirstEnter = false
                lifecycleScope.launch {
                    loadingDialog.show()
                    val queryIds = getQueryIds()
                    searchLimlt(queryIds)
                    loadingDialog.dismiss()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // 选中初始分类
        val selectIndex = categoryList.indexOfFirst { it.id == categoryId }
        if (selectIndex != -1) {
            val currentIndex = binding.categoryTab.selectedTabPosition
            if (selectIndex == currentIndex) {
                updateCategory(categoryList[selectIndex])
            } else {
                binding.categoryTab.getTabAt(selectIndex)?.select()
            }
        }
    }
    var selectSubCategory: Category? = null
    var selectCategory: Category? = null
    var tabs = ""
    
    private fun updateSelectSubCategory(category: Category) {
        selectSubCategory = category
        updateTabsAndDisplay()
    }

    private fun updateCategory(category: Category) {
        if (!isFirstEnter) {
            // 切换一级分类时，二级分类选择全部
            selectSubCategoryId = -1
        }
        lifecycleScope.launch {
            updateSubChapterTabName(category.cateName)
        }
        isFirstEnter = false

        selectCategory = category
        updateTabsAndDisplay()
    }
    
    /**
     * 更新分类路径和显示文本
     */
    private fun updateTabsAndDisplay() {
        tabs = "${selectCategory?.getShowName()}-${selectSubCategory?.getShowName()}"
        binding.tvHasSelect.text = "已选中:${selectCategory?.getShowName()}-${selectSubCategory?.getShowName()}"
    }

    private var subCategoryList = mutableListOf<Category>()

    private suspend fun updateSubChapterTabName(name: String) {
        val data = DatabaseHelper.getInstance().querySubCategory1(name)
        updateSubCategoryTabs(data)
    }
    
    /**
     * 更新子分类 Tab（通用方法）
     */
    private fun updateSubCategoryTabs(data: List<Category>) {
        subCategoryList.clear()
        subCategoryList.add(Category().apply {
            cateName = "全部"
            id = -1
        })
        subCategoryList.addAll(data)
        
        binding.subCategoryTab.removeAllTabs()
        subCategoryList.forEach { category ->
            val tabName = if (category.id == -1) {
                category.cateName
            } else {
                if (category.cateName.contains("-")) {
                    category.cateName.split("-", limit = 2)[1]
                } else {
                    category.cateName
                }
            }
            binding.subCategoryTab.addTab(binding.subCategoryTab.newTab().setText(tabName))
        }

        val selectIndex = subCategoryList.indexOfFirst { it.id == selectSubCategoryId }
            .takeIf { it != -1 } ?: 0
        binding.subCategoryTab.getTabAt(selectIndex)?.select()
    }

    /**
     * 设置搜索加载状态
     * @param loading true=搜索中（禁用输入和按钮，显示加载动画），false=搜索完成（恢复可用）
     */
    private fun setSearchLoadingState(loading: Boolean) {
        binding.etSearch.isEnabled = !loading
        binding.search.isEnabled = !loading
        binding.search.isClickable = !loading
        binding.search.alpha = if (loading) 0.5f else 1.0f
        
        // 搜索中：隐藏搜索图标，显示加载进度条（在搜索按钮内）
        // 搜索完成：显示搜索图标，隐藏加载进度条
        binding.ivSearchIcon.visibility = if (loading) View.GONE else View.VISIBLE
        binding.pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
        
        // 搜索中隐藏清空按钮，搜索完成后根据是否有文本决定显示
        if (loading) {
            binding.ivClear.visibility = View.GONE
        } else {
            val hasText = binding.etSearch.text.toString().isNotEmpty()
            binding.ivClear.visibility = if (hasText) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        binding.main.setBackgroundColor(Color.parseColor(SPUtils.getInstance().getString(Constant.Background_Color)))
        
        val themeIndex = SPUtils.getInstance().getInt(Constant.ThemeColorIndex, 0)
        val statusBarColor = when (themeIndex) {
            0 -> R.color.title_1
            1 -> R.color.title_2
            2 -> R.color.title_3
            3 -> R.color.title_4
            else -> R.color.title_1
        }
        
        ImmersionBar.with(this@AppSearchResultPage)
            .transparentStatusBar()
            .transparentBar()
            .transparentNavigationBar()
            .statusBarColor(statusBarColor)
            .statusBarDarkFont(true)
            .navigationBarDarkIcon(true)
            .autoDarkModeEnable(true)
            .init()
    }

}
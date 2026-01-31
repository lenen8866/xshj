package com.sda.books.reader.frag

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.LogUtils
import com.sda.books.reader.AppVolumePage
import com.book.reader.R
import com.sda.books.reader.adapter.VolumeListAdapter
import com.sda.books.reader.adapter.VolumeListAdapter.OnItemClickListener
import com.book.reader.databinding.FragmentSubCategoryBinding
import com.sda.books.reader.db.DatabaseHelper
import com.sda.books.reader.entity.Category
import com.sda.books.reader.entity.Volume
import com.sda.books.reader.entity.getShowVolumeName
import com.sda.books.reader.loading.LoadingDialog
import com.sda.books.reader.vm.AppCategoryViewModel
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class SubCategoryFragment : Fragment() {

    companion object {
        val mTabData = MutableLiveData<String>()
        private const val categoryIdParams = "categoryId"
        private const val categoryIdParamsName = "categoryName"
        private const val TAG = "SubCategoryFragment"
        
        // 创建带有参数的 Fragment 实例
        fun newInstance(categoryId: Int, tabTv: String): SubCategoryFragment {
            val args = Bundle().apply {
                putInt(categoryIdParams, categoryId)
                putString(categoryIdParamsName, tabTv)
            }
            return SubCategoryFragment().apply {
                arguments = args
            }
        }
    }

    private var parentName: String = ""
    private var name: String = ""
    private var isDataLoaded = false
    
    // 🔧 修复关键：使用复合键缓存（parentName + subCategoryId）
    // 避免不同主分类下相同 subCategoryId 的冲突
    private val chapterMapList = mutableMapOf<String, List<Volume>>()
    
    lateinit var viewModel: AppCategoryViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            parentName = it.getString(categoryIdParamsName).toString()
        }
        
        Log.d(TAG, "🔵 onCreate: parentName = $parentName")
    }

    lateinit var binding : FragmentSubCategoryBinding
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSubCategoryBinding.inflate(inflater)
        return binding.root
    }
    
    private var canScroll = false

    var subCategoryList = mutableListOf<Category>()
    lateinit var chapterListAdapter:VolumeListAdapter
    lateinit var loadingDialog : LoadingDialog
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(AppCategoryViewModel::class.java)
        
        mTabData.observe(viewLifecycleOwner) { newParentName ->
            Log.d(TAG, "🔵 mTabData.observe: newParentName = $newParentName, currentParentName = $parentName")
            
            if (newParentName != parentName) {
                parentName = newParentName
                isDataLoaded = false
                
                // 🔧 关键修复：清空当前Fragment的缓存
                Log.d(TAG, "✅ 检测到主分类变化，清空缓存并重新加载")
            }
            
            if (!isDataLoaded) {
                loadListData()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔵 onResume: parentName = $parentName, isDataLoaded = $isDataLoaded")
        
        if (!isDataLoaded && ::binding.isInitialized) {
            Log.d(TAG, "⚠️ onResume 检测到数据未加载，主动加载")
            loadListData()
        }
    }

    private fun loadListData() {
        if (isDataLoaded) {
            Log.d(TAG, "⚠️ 数据已加载，跳过重复加载")
            return
        }
        
        Log.d(TAG, "========================================")
        Log.d(TAG, "🔄 开始加载子分类数据: parentName = $parentName")
        
        subCategoryList.clear()
        loadingDialog = LoadingDialog(requireContext())
        
        lifecycleScope.launch {
            loadingDialog.show()

            val  data = DatabaseHelper.getInstance().querySubCategory1(parentName)
            
            Log.d(TAG, "✅ 查询到 ${data.size} 个子分类")

            subCategoryList.clear()
            binding.subCategoryTab.removeAllTabs()

            subCategoryList.add(Category()
                .apply {
                    cateName = "全部"
                    id = -1
                })

            viewModel.updateSubCategoryMap(-1,data)
            subCategoryList.addAll(data)
            subCategoryList.forEach {
                LogUtils.e("=========>>>>>>>>>${it.cateName}")
                val tab = binding.subCategoryTab.newTab()
                val textView = TextView(requireContext())
                tab.setCustomView(textView)
                textView.setTextAppearance(R.style.subTabStyle)
                if(it.id == -1){
                    textView.text = it.cateName
                    textView.setTextAppearance(R.style.subTabSelectStyle)
                }else {
                    textView.text = if( it.cateName.contains("-")) it.cateName.split("-", limit = 2)[1] else it.cateName
                }
                textView.gravity = Gravity.CENTER
                textView.setPadding(10,10,10,10)
                binding.subCategoryTab.addTab(tab)
            }
            name = "${subCategoryList[0].cateName}"
            
            binding.subCategoryTab.clearOnTabSelectedListeners()
            
            binding.subCategoryTab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    updateChapterList(subCategoryList[tab.position].id)
                    viewModel.updateCurrentSelectSubCategoryId(subCategoryList[tab.position].id)
                    ( tab.customView as TextView?)?.apply {
                        setTextAppearance(R.style.subTabSelectStyle)
                    }
                    name = "${subCategoryList[tab.position].cateName}"
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    ( tab.customView as TextView?)?.apply {
                        setTextAppearance(R.style.subTabStyle)
                    }
                }

                override fun onTabReselected(tab: TabLayout.Tab) {}
            })

            binding.volumeList.layoutManager = LinearLayoutManager(requireContext())
            chapterListAdapter = VolumeListAdapter()
            binding.volumeList.adapter = chapterListAdapter
            
            binding.volumeList.viewTreeObserver.addOnGlobalLayoutListener {
                checkListScrollability()
            }
            
            updateChapterList(subCategoryList[0].id)
            
            chapterListAdapter.setOnItemClickListener(object : OnItemClickListener{
                override fun onItemClick(position: Int, volume: Volume) {
                    // 这里决定“卷列表 → 详情页/章节页”顶部显示的分类文字
                    // 目标：即使当前筛选是“全部”，也要根据书自身的分类（volume.categoryId）
                    // 显示成“父分类-具体分类”，比如 “圣经-旧约” / “圣经-新约”

                    // 1. 父分类展示名：去掉前面的编号前缀（如果有的话，比如 "1-圣经" -> "圣经"）
                    val parentDisplayName = if (parentName.contains("-")) {
                        parentName.split("-", limit = 2)[1]
                    } else {
                        parentName
                    }

                    // 2. 根据当前这本书自己的 categoryId，找到它真实所属的二级分类（旧约 / 新约）
                    val realSubCategory = subCategoryList.firstOrNull { it.id == volume.categoryId }
                    val realSubName = realSubCategory?.cateName

                    // 3. 组合要传给后面页面的 tabs：
                    //    - 优先用“书自身的分类名”，比如 旧约 / 新约
                    //    - 如果没查到，就退回当前选中的 Tab 名（name），保持原有行为
                    val finalSubName = realSubName?.takeIf { it.isNotBlank() } ?: name
                    val volumeTabs = "$parentDisplayName-$finalSubName"

                    startActivity(Intent(requireContext(), AppVolumePage::class.java).apply {
                        putExtra("volumeTitle", getShowVolumeName(volume.volName))
                        putExtra("volumeId", volume.id)
                        putExtra("volumeTabs", volumeTabs)
                    })
                }
            })
            
            loadingDialog.dismiss()
            
            isDataLoaded = true
            Log.d(TAG, "✅ 数据加载完成")
            Log.d(TAG, "========================================\n")
        }
    }

     private fun checkListScrollability() {
        val layoutManager = binding.volumeList.layoutManager as LinearLayoutManager
        val adapter = binding.volumeList.adapter as VolumeListAdapter

        val canScroll = binding.volumeList.canScrollVertically(1) ||
                binding.volumeList.canScrollVertically(-1)

        chapterListAdapter.setShowFooter(canScroll)
        this.canScroll = canScroll
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        LogUtils.e("001=========>>>>>>>>>${parentName}")
        outState.putString("parentName", parentName);
    }

    // 🔧 关键修复：生成复合缓存键
    private fun getCacheKey(subCategoryId: Int): String {
        return "$parentName-$subCategoryId"
    }

    private fun updateChapterList(subCategoryId: Int) {
        val totalStartTime = System.currentTimeMillis()
        
        // 🔧 使用复合键
        val cacheKey = getCacheKey(subCategoryId)
        
        Log.d(TAG, "========================================")
        Log.d(TAG, "🔄 开始切换子分类: subCategoryId=$subCategoryId (主分类: $parentName)")
        Log.d(TAG, "🔑 缓存键: $cacheKey")
        
        var queryIds = listOf(subCategoryId)
        if (subCategoryId == -1) {
            queryIds = subCategoryList.map { it.id }
        }
        
        // 🔧 使用复合键查找缓存
        val cache = chapterMapList[cacheKey]
        
        if (cache != null) {
            Log.d(TAG, "✅ 缓存命中！直接使用缓存数据 (${cache.size}项)")
            
            val updateStartTime = System.currentTimeMillis()
            
            // 🔧 关键修复：创建新的List实例，避免DiffUtil认为是同一个引用
            val newList = cache.toList()
            chapterListAdapter.updateData(newList)
            
            val updateEndTime = System.currentTimeMillis()
            
            chapterListAdapter.setDataComplete(true)
            
            binding.volumeList.post {
                checkListScrollability()
            }
            
            binding.volumeList.scrollToPosition(0)
            
            val totalTime = System.currentTimeMillis() - totalStartTime
            Log.d(TAG, "⏱️ 列表更新耗时: ${updateEndTime - updateStartTime}ms")
            Log.d(TAG, "⏱️ 总耗时: ${totalTime}ms (使用缓存)")
            Log.d(TAG, "========================================\n")
            
            return
        }
        
        Log.d(TAG, "⚠️ 缓存未命中，开始查询数据库...")
        loadingDialog.show()
        
        lifecycleScope.launch {
            try {
                val dbStartTime = System.currentTimeMillis()
                
                DatabaseHelper.getInstance().queryVolume(
                    queryIds,
                    onSuccess = { data ->
                        val dbEndTime = System.currentTimeMillis()
                        Log.d(TAG, "⏱️ 数据库查询耗时: ${dbEndTime - dbStartTime}ms (${data.size}项)")
                        
                        // 🔧 使用复合键存入缓存
                        chapterMapList[cacheKey] = data
                        Log.d(TAG, "✅ 数据已缓存 (key=$cacheKey)")
                        
                        val updateStartTime = System.currentTimeMillis()
                        
                        // 🔧 创建新的List实例
                        val newList = data.toList()
                        chapterListAdapter.updateData(newList)
                        
                        val updateEndTime = System.currentTimeMillis()
                        
                        chapterListAdapter.setDataComplete(true)
                        
                        loadingDialog.dismiss()
                        
                        binding.volumeList.post {
                            checkListScrollability()
                        }
                        
                        val totalTime = System.currentTimeMillis() - totalStartTime
                        Log.d(TAG, "⏱️ 列表更新耗时: ${updateEndTime - updateStartTime}ms")
                        Log.d(TAG, "⏱️ 总耗时: ${totalTime}ms (首次查询)")
                        Log.d(TAG, "========================================\n")
                        
                    }, 
                    onFail = {
                        loadingDialog.dismiss()
                        Log.e(TAG, "❌ 数据库查询失败")
                        Log.d(TAG, "========================================\n")
                    }
                )
            } catch (e: Exception) {
                loadingDialog.dismiss()
                Log.e(TAG, "❌ 异常: ${e.message}")
                Log.d(TAG, "========================================\n")
            }
        }
        
        binding.volumeList.scrollToPosition(0)
    }
    
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        Log.d(TAG, "🔵 onHiddenChanged: hidden = $hidden, parentName = $parentName")
        
        if (!hidden) {
            val currentTabData = mTabData.value
            if (currentTabData != parentName) {
                Log.d(TAG, "⚠️ 检测到主分类不一致: mTabData = $currentTabData, parentName = $parentName")
                parentName = currentTabData ?: parentName
                isDataLoaded = false
                
                if (::binding.isInitialized) {
                    loadListData()
                }
            }
        }
    }
}

package com.sda.books.reader.frag

import android.content.Intent
import android.os.Bundle
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

    //private var parentId = -1
    private var parentName: String = ""
    private var name: String = ""
    private val chapterMapList = mutableMapOf<Int,List<Volume>?>()
    lateinit var viewModel: AppCategoryViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            //parentId = it.getInt(categoryIdParams)

            parentName = it.getString(categoryIdParamsName).toString()

        }
        /*mTabData.observe(this, {
            parentName = it
            LogUtils.e("003=========>>>>>>>>>${parentName}")

        })*/
        //loadListData()
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
    private var canScroll = false // 标记列表是否可以滚动

    var subCategoryList = mutableListOf<Category>()
    lateinit var chapterListAdapter:VolumeListAdapter
    lateinit var loadingDialog : LoadingDialog
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*if (savedInstanceState != null) {
            //parentId = savedInstanceState.getInt("parentId"); // 恢复数据
            parentName = savedInstanceState.getString("parentName").toString()
            LogUtils.e("004=========>>>>>>>>>${parentName}")
        }else{

        }*/
        viewModel = ViewModelProvider(requireActivity()).get(AppCategoryViewModel::class.java)
       // loadListData()
        mTabData.observe(viewLifecycleOwner, {
            parentName = it
            //LogUtils.e("003=========>>>>>>>>>${parentName}")
            loadListData()
        })


    }

    private fun loadListData() {
        subCategoryList.clear()
        loadingDialog = LoadingDialog(requireContext())
        lifecycleScope.launch {
            loadingDialog.show()

            val  data = DatabaseHelper.getInstance().querySubCategory1(parentName);



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
                //去掉阴影
//                textView.setBackgroundResource(R.drawable.btn_ripple)
                textView.gravity = Gravity.CENTER
                textView.setPadding(10,10,10,10)
                binding.subCategoryTab.addTab(tab)
            }
            name = "${subCategoryList[0].cateName}"
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
                    // 未选中时的样式
                    ( tab.customView as TextView?)?.apply {
                        setTextAppearance(R.style.subTabStyle)
                    }
                }

                override fun onTabReselected(tab: TabLayout.Tab) {}
            })

            binding.volumeList.layoutManager = LinearLayoutManager(requireContext())
            chapterListAdapter = VolumeListAdapter()
            binding.volumeList.adapter = chapterListAdapter
            // 添加布局监听器检查列表是否可滚动
            binding.volumeList.viewTreeObserver.addOnGlobalLayoutListener {
                checkListScrollability()
            }
            updateChapterList(subCategoryList[0].id);
            chapterListAdapter.setOnItemClickListener(object : OnItemClickListener{
                override fun onItemClick(position: Int, volume: Volume) {
                    startActivity(Intent(requireContext(),AppVolumePage::class.java).apply {
                        putExtra("volumeTitle", getShowVolumeName(volume.volName))
                        putExtra("volumeId",volume.id)
                        putExtra("volumeTabs", "$parentName-$name")
                    })
                }
            })
            loadingDialog.dismiss()
        }
    }

     private fun checkListScrollability() {
        val layoutManager = binding.volumeList.layoutManager as LinearLayoutManager
        val adapter = binding.volumeList.adapter as VolumeListAdapter

        // 检查列表是否可滚动
        val canScroll = binding.volumeList.canScrollVertically(1) ||
                binding.volumeList.canScrollVertically(-1)

        // 只有当列表可滚动且数据已加载完成时才显示底部提示
        chapterListAdapter.setShowFooter(canScroll)
        this.canScroll = canScroll
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //outState.putInt("parentId", parentId); // 保存数据
        LogUtils.e("001=========>>>>>>>>>${parentName}")
        outState.putString("parentName", parentName);
    }

    private fun updateChapterList(subCategoryId:Int){
        loadingDialog.show()
        var queryIds = listOf(subCategoryId)
        if(subCategoryId == -1){
            queryIds = subCategoryList.map { it.id }
        }
        lifecycleScope.launch {
            try {
                DatabaseHelper.getInstance().queryVolume(
                    queryIds,
                    onSuccess = {
                        chapterMapList[subCategoryId] = it
                        chapterListAdapter.updateData(it)
                        chapterListAdapter.setDataComplete(true)

                        // 确保在任何情况下都关闭loading
                        loadingDialog.dismiss()

                        // 更新滚动状态
                        binding.volumeList.post {
                            checkListScrollability()
                        }
                    }, onFail = {
                        // 确保在失败时也关闭loading
                        loadingDialog.dismiss()
                    })
            } catch (e: Exception) {
                // 捕获所有异常并关闭loading
                loadingDialog.dismiss()
            }
        }

        /*val cache = chapterMapList[subCategoryId]
        LogUtils.e("=======>>>>>>"+cache)
        if(cache == null){
            lifecycleScope.launch {
                try {
                    DatabaseHelper.getInstance().queryVolume(
                        queryIds,
                        onSuccess = {
                            chapterMapList[subCategoryId] = it
                            chapterListAdapter.updateData(it)
                            chapterListAdapter.setDataComplete(true)

                            // 确保在任何情况下都关闭loading
                            loadingDialog.dismiss()

                            // 更新滚动状态
                            binding.volumeList.post {
                                checkListScrollability()
                            }
                        }, onFail = {
                            // 确保在失败时也关闭loading
                            loadingDialog.dismiss()
                        })
                } catch (e: Exception) {
                    // 捕获所有异常并关闭loading
                    loadingDialog.dismiss()
                }
            }
        } else {
            chapterListAdapter.updateData(cache)
            chapterListAdapter.setDataComplete(true)

            // 确保在使用缓存时也关闭loading
            loadingDialog.dismiss()

            // 更新滚动状态
            binding.volumeList.post {
                checkListScrollability()
            }
        }*/
        binding.volumeList.scrollToPosition(0)
    }
}
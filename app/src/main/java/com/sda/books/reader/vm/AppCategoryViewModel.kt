package com.sda.books.reader.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.LogUtils
import com.sda.books.reader.db.DatabaseHelper
import com.sda.books.reader.entity.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppCategoryViewModel : ViewModel() {

    var categoryList = MutableLiveData<List<Category>>()

    var isShowLoading = MutableLiveData<Boolean>()

    var subCategoryMap = mutableMapOf<Int, List<Category>>()

    var currentSelectParentId = -1

    var currentSubCategoryId = -1

    /**
     * 查询分类数据
     * @param forceRefresh 是否强制刷新，默认 false
     */
    fun queryData(forceRefresh: Boolean = false) {
        // 如果已有数据且不强制刷新，则直接返回
        if (!forceRefresh && categoryList.value?.isNotEmpty() == true) {
            return
        }

        viewModelScope.launch {
            try {
                // 在主线程更新加载状态
                withContext(Dispatchers.Main) {
                    isShowLoading.value = true
                }

                // 在 IO 线程执行数据库操作
                val result = withContext(Dispatchers.IO) {
                    DatabaseHelper.getInstance().openDatabase()
                    DatabaseHelper.getInstance().queryCategoryList()
                }

                // 在主线程更新数据
                withContext(Dispatchers.Main) {
                    categoryList.value = result.ifEmpty { mutableListOf() }
                    isShowLoading.value = false
                }

                // 如果查询结果不为空且当前未选中父分类，则设置第一个分类为默认选中
                if (result.isNotEmpty() && currentSelectParentId == -1) {
                    currentSelectParentId = result[0].id
                }

            } catch (e: Exception) {
                LogUtils.e("查询分类数据失败: ${e.message}", e)
                // 在主线程更新状态
                withContext(Dispatchers.Main) {
                    isShowLoading.value = false
                    // 如果查询失败且当前没有数据，设置为空列表
                    if (categoryList.value == null) {
                        categoryList.value = mutableListOf()
                    }
                }
            }
        }
    }

    /**
     * 刷新分类数据
     */
    fun refreshData() {
        queryData(forceRefresh = true)
    }

    /**
     * 更新子分类映射
     */
    fun updateSubCategoryMap(parentId: Int, subCategoryList: List<Category>) {
        subCategoryMap[parentId] = subCategoryList
    }

    /**
     * 更新当前选中的父分类ID
     */
    fun updateCurrentSelectParentId(id: Int) {
        currentSelectParentId = id
    }

    /**
     * 更新当前选中的子分类ID
     */
    fun updateCurrentSelectSubCategoryId(id: Int) {
        currentSubCategoryId = id
    }
}
package com.sda.books.reader.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.LogUtils
import com.sda.books.reader.db.DatabaseHelper
import com.sda.books.reader.entity.Category
import kotlinx.coroutines.launch

class AppCategoryViewModel : ViewModel() {

    var categoryList = MutableLiveData<List<Category>>()

    var isShowLoading = MutableLiveData<Boolean>()

    var subCategoryMap = mutableMapOf<Int,List<Category>>()

    var currentSelectParentId = 1

    var currentSubCategoryId = -1
    fun queryData(){
        if(categoryList.value?.isNotEmpty() == true)return
        viewModelScope.launch {
            isShowLoading.value  = true
            //categoryList.value = null
            DatabaseHelper.getInstance().openDatabase()
            LogUtils.e("=============${DatabaseHelper.getInstance().queryCategoryList()}")

            categoryList.value = if(DatabaseHelper.getInstance().queryCategoryList().size==0) mutableListOf() else DatabaseHelper.getInstance().queryCategoryList()
            isShowLoading.value = false
        }
    }

    fun updateSubCategoryMap(parentId:Int,subCategoryList:List<Category>){
        subCategoryMap[parentId] = subCategoryList
    }

    fun updateCurrentSelectParentId(id:Int){
        currentSelectParentId = id
    }

    fun updateCurrentSelectSubCategoryId(id:Int){
        currentSubCategoryId = id
    }

    fun getQueryIdList():Int{
        return  currentSubCategoryId
    }

}
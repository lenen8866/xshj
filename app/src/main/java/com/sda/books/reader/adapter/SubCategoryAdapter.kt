package com.sda.books.reader.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.sda.books.reader.entity.Category
import com.sda.books.reader.frag.SubCategoryFragment

// 适配器类
class SubCategoryAdapter(
    fragmentActivity: FragmentActivity,
    val categoryList: List<Category>,
    var tabTv: String
) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = categoryList.size

    override fun createFragment(position: Int): Fragment {
        return SubCategoryFragment.newInstance(categoryList[position].id, tabTv)
    }
}
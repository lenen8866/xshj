package com.sda.books.reader.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.book.reader.databinding.ThemeItemBinding
import com.sda.books.reader.entity.ThemeEntity
import androidx.core.graphics.toColorInt

class ThemeListAdapter(val themeList:List<ThemeEntity>,val onItemClick:OnItemClickListener) : RecyclerView.Adapter<ThemeListAdapter.ThemeListHolder>() {

    // 点击事件接口
    interface OnItemClickListener {
        fun onItemClick(position: Int, theme : ThemeEntity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeListHolder {
        val binding = ThemeItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ThemeListHolder(binding)
    }

    override fun getItemCount(): Int = themeList.size

    override fun onBindViewHolder(holder: ThemeListHolder, position: Int) {
        val theme = themeList[position]
        holder.onBinding(theme)
        holder.itemView.setOnClickListener {
            themeList.forEach {
                it.isSelect = false
            }
            theme.isSelect = true
            notifyDataSetChanged()
            onItemClick.onItemClick(position,theme)
        }
    }

    fun reset(){
        themeList.forEach {
            it.isSelect = false
        }
        themeList[0].isSelect = true
        notifyDataSetChanged()
    }

    inner class  ThemeListHolder(val themeItemBinding: ThemeItemBinding) : RecyclerView.ViewHolder(themeItemBinding.root) {

        fun onBinding(themeEntity: ThemeEntity){
            val shape = GradientDrawable()
            shape.shape = GradientDrawable.RECTANGLE  // 矩形形状
            shape.setColor(themeEntity.color.toColorInt())
            if(themeEntity.isSelect){
                shape.setStroke(12, themeEntity.borderColor.toColorInt())
                shape.cornerRadius = 15f
            }else {
                shape.setStroke(6, themeEntity.borderColor.toColorInt())
                shape.cornerRadius = 0f
            }
            themeItemBinding.themeView.background = shape
        }
    }
}
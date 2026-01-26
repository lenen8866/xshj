package com.sda.books.reader.adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.book.reader.databinding.ChapterItemBinding
import com.sda.books.reader.entity.Chapter
import com.sda.books.reader.util.copyText
import com.sda.books.reader.util.onClickWithDebounce

class ChapterListAdapter :
    RecyclerView.Adapter<ChapterListAdapter.TextViewHolder>() {

    private var items = mutableListOf<Chapter>()
    // 点击事件接口
    interface OnItemClickListener {
        fun onItemClick(position: Int, volume : Chapter)
    }

    private var listener: OnItemClickListener? = null

    // 设置点击事件监听器
    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    fun updateData(data:List<Chapter>){
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    // 创建 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewHolder {
        val binding = ChapterItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TextViewHolder(binding)
    }

    // 绑定数据到 ViewHolder
    override fun onBindViewHolder(holder: TextViewHolder, position: Int) {
        val item = items[position]
        //注销首行间距逻辑  250807
        /*if(position == 0){
            val params =  holder.itemView.layoutParams as MarginLayoutParams
            params.topMargin = 81
        }else {
            val params =  holder.itemView.layoutParams as MarginLayoutParams
            params.topMargin = 0
        }*/
        holder.bind(item)

        holder.itemView.setOnLongClickListener {
            var content = item.name
            content = content.replace(Regex("\\{.*?\\}|\\[.*?]|\\(.*?\\)"), "")
            copyText(content)
            return@setOnLongClickListener true
        }
    }

    // 返回项目数量
    override fun getItemCount(): Int = items.size

    // ViewHolder 类
    inner class TextViewHolder(private val binding: ChapterItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // ✅ 使用防抖点击，默认 500ms 防抖
            binding.root.onClickWithDebounce(debounceInterval = 500L) {
                val position = bindingAdapterPosition // ✅ 修复：使用 bindingAdapterPosition 替代 adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(position, items[position])
                }
            }
        }

        // 绑定数据到视图
        fun bind(chapter: Chapter) {
            var content = chapter.name
            content = content.replace(Regex("\\{.*?\\}|\\[.*?]|\\(.*?\\)"), "")
            binding.tvName.text = content
        }
    }
}
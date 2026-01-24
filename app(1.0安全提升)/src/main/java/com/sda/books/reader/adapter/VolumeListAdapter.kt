package com.sda.books.reader.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.sda.books.reader.LanguageUtils
import com.book.reader.R
import com.book.reader.databinding.VolumeItemBinding
import com.sda.books.reader.entity.Volume
import com.sda.books.reader.entity.getShowVolumeName
import com.sda.books.reader.util.AppSettingUtil
import com.sda.books.reader.util.copyText

class VolumeListAdapter() :
    RecyclerView.Adapter<ViewHolder>() {

    private var items = mutableListOf<Volume>()

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_FOOTER = 1
    }

    private var showFooter = false
    private var isDataComplete = false // 标记数据是否已加载完成

    // 设置是否显示底部提示
    fun setShowFooter(show: Boolean) {
        if (showFooter != show) {
            showFooter = show
            notifyDataSetChanged()
        }
    }

    // 设置数据是否已加载完成
    fun setDataComplete(complete: Boolean) {
        if (isDataComplete != complete) {
            isDataComplete = complete
            notifyDataSetChanged()
        }
    }

    // 点击事件接口
    interface OnItemClickListener {
        fun onItemClick(position: Int, volume: Volume)
    }

    private var listener: OnItemClickListener? = null

    // 设置点击事件监听器
    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }


    fun updateData(data: List<Volume>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    // 添加数据（用于分页加载）
    fun appendData(data: List<Volume>) {
        val startPosition = items.size
        items.addAll(data)
        notifyItemRangeInserted(startPosition, data.size)
    }

    // 创建 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            TYPE_ITEM -> {
                val binding = VolumeItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                VolumeViewHolder(binding)
            }
            TYPE_FOOTER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.loading_end_item, parent, false)
                FooterViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }


    override fun getItemViewType(position: Int): Int {
        return if (position == items.size) TYPE_FOOTER else TYPE_ITEM
    }



    override fun getItemCount(): Int = items.size + if (showFooter && isDataComplete) 1 else 0


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is VolumeViewHolder -> {
                val item = items[position]
                holder.itemView.setOnClickListener {
                    listener?.onItemClick(position, item)
                }
                holder.itemView.setOnLongClickListener {
                    copyText(getShowVolumeName(item.volName))
                    true
                }
                holder.bind(item)
            }

            is FooterViewHolder -> {
                // 底部提示不需要特殊处理
                holder.itemView.findViewById<FrameLayout>(R.id.root).visibility = View.VISIBLE
                val tv = holder.itemView.findViewById<TextView>(R.id.endTv)
                val language = if (AppSettingUtil.getIsOpenEn()) "en" else "zh"
                val str =
                    LanguageUtils.getStringByLanguage(holder.itemView.context, R.string.toast_msg2, language)
                tv.text = str
            }
        }
    }

    inner class VolumeViewHolder(private val binding: VolumeItemBinding) :
        ViewHolder(binding.root) {
        fun bind(volume: Volume) {
            //binding.tvName.text = getShowVolumeName(volume.volName).replace("E","")
            var  strName = getShowVolumeName(volume.volName).trim()
            if(strName.endsWith("E")){
                binding.tvName.text = strName.replace("E","").split("(")[0]
                if(strName.replace("E","").contains("(")){
                    binding.tvAuthor.text = strName.replace("E","").split("(")[1].replace(")","").replace("E","")

                }
               binding.ivPic.visibility = View.VISIBLE
            }else{
                binding.tvName.text = strName.replace("E","").split("(")[0]
                if(strName.contains("(")){
                    binding.tvAuthor.text = strName.replace("E","").split("(")[1].replace(")","").replace("E","")

                }
                binding.ivPic.visibility = View.GONE
            }
            //LogUtils.e("==============>>>>>>>>>${volume.volName}")
        }
    }

    inner class FooterViewHolder(itemView: View) : ViewHolder(itemView)


}
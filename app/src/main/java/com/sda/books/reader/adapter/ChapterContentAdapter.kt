package com.sda.books.reader.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.book.reader.R
import com.book.reader.databinding.ChapterContentItemBinding
import com.bumptech.glide.Glide
import com.sda.books.reader.App
import com.sda.books.reader.entity.ChapterContentItem
import com.sda.books.reader.event.EventBus
import com.sda.books.reader.getActualLineSpacing
import com.sda.books.reader.maxScreenPadding
import com.sda.books.reader.store.StoreManager
import com.sda.books.reader.util.AppSettingUtil
import com.sda.books.reader.util.dpToPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChapterContentAdapter(
    private val matchContent: ArrayList<String>,
    private val lifecycleScope: LifecycleCoroutineScope
) : RecyclerView.Adapter<ChapterContentAdapter.ChapterContentHolder>() {

    private val data = mutableListOf<ChapterContentItem>()
    private var topMargin = -1

    // 高亮相关
    private var highlightPosition = -1
    private val highlightHandler = Handler(Looper.getMainLooper())
    private var highlightRunnable: Runnable? = null

    // 经文引用正则（缓存匹配结果）
    private val bibleReferenceRegex = Regex("""^\s*[A-Za-z\u4e00-\u9fa5]+\s*\d+(?:\s*[:：]\s*\d+)?\s*$""")
    private val referenceCache = mutableMapOf<String, Boolean>()

    fun updateData(newData: List<ChapterContentItem>) {
        data.clear()
        data.addAll(newData)
        notifyDataSetChanged()
    }

    fun highlightPosition(position: Int) {
        val previous = highlightPosition
        highlightPosition = position

        if (previous != -1) notifyItemChanged(previous)
        notifyItemChanged(position)

        highlightRunnable?.let { highlightHandler.removeCallbacks(it) }
        highlightRunnable = Runnable {
            highlightPosition = -1
            notifyItemChanged(position)
        }
        highlightHandler.postDelayed(highlightRunnable!!, 3000)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterContentHolder {
        val binding = ChapterContentItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChapterContentHolder(binding)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ChapterContentHolder, position: Int) {
        holder.bind(data[position])
    }

    /**
     * 检查文本是否为经文引用
     */
    private fun isBibleReference(text: String): Boolean {
        return referenceCache.getOrPut(text) {
            bibleReferenceRegex.matches(text.trim())
        }
    }

    inner class ChapterContentHolder(
        private val binding: ChapterContentItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChapterContentItem) {
            val position = bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return

            // 设置高亮背景
            applyHighlight(position)

            // 处理图片内容
            if (item.isImg()) {
                showImage(item.getShowContent())
                return
            }

            // 处理文本内容
            showText(item)
        }

        private fun applyHighlight(position: Int) {
            if (position == highlightPosition) {
                val drawable = GradientDrawable().apply {
                    setColor(Color.parseColor("#EEEEEE"))
                    cornerRadius = 8f.dpToPx().toFloat()
                }
                binding.root.background = drawable
                binding.root.animate().alpha(1f).setDuration(300).start()
            } else {
                binding.root.background = ContextCompat.getDrawable(
                    binding.root.context,
                    android.R.color.transparent
                )
            }
        }

        private fun showImage(url: String) {
            binding.tvContent.visibility = View.GONE
            binding.ivImg.visibility = View.VISIBLE

            val imageFile = if (StoreManager.checkFileIsDownLoadFinish(url)) {
                StoreManager.getDesFile(url)
            } else {
                StoreManager.startDownLoad(url)
                url
            }

            Glide.with(App.context)
                .load(imageFile)
                .into(binding.ivImg)
        }

        private fun showText(item: ChapterContentItem) {
            binding.ivImg.visibility = View.GONE
            binding.tvContent.visibility = View.VISIBLE

            // 获取设置参数
            val textSize = AppSettingUtil.getTextSize()
            val hSpace = AppSettingUtil.getTextLetterSpacing()
            val screenPadding = AppSettingUtil.getScreenPadding()
            val sectionSpacing = AppSettingUtil.getTextSectionHLetterSpacing()

            // 设置基础样式
            binding.tvContent.apply {
                setTextColor(Color.BLACK)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize.toFloat())

                if (!item.isEng()) {
                    letterSpacing = hSpace / 60f * 2
                }
            }

            // 更新边距
            updateMargins(screenPadding, sectionSpacing)

            // 渲染内容（HTML + 经文引用样式）
            val htmlContent = Html.fromHtml(item.getUserContent(), Html.FROM_HTML_MODE_LEGACY)
            val styledContent = applyBibleReferenceStyle(htmlContent, textSize.toFloat())
            binding.tvContent.text = styledContent

            // 更新 TextView（保留原有逻辑）
            item.updateTextView(binding.tvContent)

            // 回调高度（使用传入的 lifecycleScope）
            binding.tvContent.post {
                LogUtils.e("行高==========----===${binding.tvContent.measuredHeight}")
                lifecycleScope.launch(Dispatchers.Main) {
                    EventBus.sendChapterHeight(binding.tvContent.measuredHeight)
                }
            }
        }

        private fun updateMargins(screenPadding: Int, sectionSpacing: Int) {
            val params = binding.tvContent.layoutParams as MarginLayoutParams
            val horizontalMargin = ((screenPadding / 30f) * maxScreenPadding).dpToPx()

            params.marginStart = horizontalMargin
            params.marginEnd = horizontalMargin

            if (topMargin != -1) {
                params.topMargin = topMargin
            } else {
                binding.tvContent.post {
                    topMargin = getActualLineSpacing(binding.tvContent).toInt()
                    params.topMargin = topMargin
                    binding.tvContent.layoutParams = params
                }
            }

            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION && position > 0) {
                params.topMargin += sectionSpacing.dpToPx()
            }

            binding.tvContent.layoutParams = params
        }

        /**
         * 应用经文引用样式
         * 识别整行匹配的经文引用（如：约3:16、得 3：3）并应用特殊样式
         */
        private fun applyBibleReferenceStyle(text: CharSequence, normalTextSize: Float): CharSequence {
            val lines = text.split("\n")
            val builder = SpannableStringBuilder()

            lines.forEachIndexed { index, line ->
                val start = builder.length
                builder.append(line)
                val end = builder.length

                // 去除 HTML 标签后检查是否为经文引用
                val plainText = line.toString().replace(Regex("<[^>]*>"), "")
                if (isBibleReference(plainText)) {
                    // 1. 灰色文字
                    builder.setSpan(
                        ForegroundColorSpan(Color.parseColor("#999999")),
                        start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    // 2. 缩小字体（比正文小 3sp）
                    val smallerSize = (normalTextSize - 3f) * App.context.resources.displayMetrics.scaledDensity
                    builder.setSpan(
                        AbsoluteSizeSpan(smallerSize.toInt()),
                        start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    // 3. 减小行距（通过缩小字体已间接实现紧凑效果）
                }

                // 添加换行符（除了最后一行）
                if (index < lines.size - 1) {
                    builder.append("\n")
                }
            }

            return builder
        }
    }
}

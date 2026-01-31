package com.sda.books.reader.adapter

import android.R
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.sda.books.reader.App
import com.book.reader.databinding.ChapterContentItemBinding
import com.sda.books.reader.dpToPx
import com.sda.books.reader.entity.ChapterContentItem
import com.sda.books.reader.getActualLineSpacing
import com.sda.books.reader.maxScreenPadding
import com.sda.books.reader.store.StoreManager
import com.sda.books.reader.util.AppSettingUtil
import com.sda.books.reader.util.dpToPx
import com.bumptech.glide.Glide
import com.sda.books.reader.event.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChapterContentAdapter(val matchContent:ArrayList<String>) : RecyclerView.Adapter<ChapterContentAdapter.ChapterContentHolder>() {

    var data = mutableListOf<ChapterContentItem>()

    private var topMargin = -1
    private var highlightPosition = -1
    private val highlightHandler = Handler(Looper.getMainLooper())
    private var highlightRunnable: Runnable? = null

    fun setHighlightPosition(position: Int) {
        highlightPosition = position
        notifyItemChanged(position)

        highlightRunnable?.let { highlightHandler.removeCallbacks(it) }

        highlightRunnable = Runnable {
            highlightPosition = -1
            notifyItemChanged(position)
        }
        highlightHandler.postDelayed(highlightRunnable!!, 3000)
    }

    fun updateData(data:List<ChapterContentItem>){
        Log.i("cccccccc","updateData")
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChapterContentHolder{
        val binding = ChapterContentItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false)
        return  ChapterContentHolder(binding)
    }

    override fun getItemCount(): Int {
        return  data.size
    }

    override fun onBindViewHolder(holder: ChapterContentHolder, position: Int) {
        holder.bind(data[position])
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

    inner class ChapterContentHolder(private val binding: ChapterContentItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chapterContentItem : ChapterContentItem) {
            if (adapterPosition == highlightPosition) {
                val highlightDrawable = GradientDrawable()
                highlightDrawable.setColor(Color.parseColor("#EEEEEE"))
                highlightDrawable.cornerRadius = 8f.dpToPx().toFloat()
                binding.root.background = highlightDrawable
                binding.root.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            } else {
                binding.root.background = ContextCompat.getDrawable(
                    binding.root.context,
                    R.color.transparent
                )
            }

            if(chapterContentItem.isImg()){
                binding.tvContent.visibility = View.GONE
                binding.ivImg.visibility = View.VISIBLE
                val url = chapterContentItem.getShowContent()
                val isDownload = StoreManager.checkFileIsDownLoadFinish(url)
                if(isDownload){
                    Glide.with(App.context)
                        .load(StoreManager.getDesFile(url))
                        .into(binding.ivImg)
                }else {
                    StoreManager.startDownLoad(url)
                    Glide.with(binding.root.context)
                        .load(url)
                        .into(binding.ivImg)
                }
                return
            }

            binding.ivImg.visibility = View.GONE
            binding.tvContent.visibility = View.VISIBLE

            val hSpace = AppSettingUtil.getTextLetterSpacing()
            val vSpace = AppSettingUtil.getTextLineSpacingMultiplier()
            val screenPadding = AppSettingUtil.getScreenPadding()
            val textSize = AppSettingUtil.getTextSize()
            val firstLetterSpacing = AppSettingUtil.getTextFristLetterSpacing()
            val sectionHLetterSpacing = AppSettingUtil.getTextSectionHLetterSpacing()
            val contentMode = AppSettingUtil.getContentMode() // 0=中，1=双，2=EN

            // ========== 修复1：直接使用富文本对象 ==========
            val userContent = chapterContentItem.getUserContent()


            // 设置富文本内容

            binding.tvContent.setTextColor(Color.BLACK)


            // =====================================

            binding.tvContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize * 1f)
            binding.tvContent.apply {
                if (!chapterContentItem.isEng()) {
                    letterSpacing = hSpace / 60f * 2
                }
                //setLineSpacing(0f, vSpace / 60f * 1 + 1)
                val params = layoutParams as MarginLayoutParams
                params.marginStart = dpToPx(((screenPadding / 30f) * maxScreenPadding))
                params.marginEnd = dpToPx(((screenPadding / 30f) * maxScreenPadding))
                if(topMargin != -1){
                    // 只中文模式下，上边距压缩一点，让正文更贴顶
                    params.topMargin = if (contentMode == 0) {
                        (topMargin * 0.6f).toInt()
                    } else {
                        topMargin
                    }
                    layoutParams = params
                }else {
                    post {
                        topMargin = getActualLineSpacing(this).toInt()
                        params.topMargin = if (contentMode == 0) {
                            (topMargin * 0.6f).toInt()
                        } else {
                            topMargin
                        }
                        layoutParams = params
                    }
                }

//                text = formatContentWithSettings(text.toString(), firstLetterSpacing)
                // 使用 Spannable 添加首行缩进



                /*val spannable = SpannableString(spannedText)
                spannable.setSpan(
                    LeadingMarginSpan.Standard((40 * firstLetterSpacing).dpToPx(), 0),
                    0,
                    spannable.length,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                )
                binding.tvContent.text = spannable*/
               // binding.tv1.setPadding(0, vHSpace.dpToPx(), 0, 0)

                if (adapterPosition > 0) {
                    // 只中文模式：段落间距稍微收紧一点
                    val sectionSpacingFactor = if (contentMode == 0) 0.6f else 1f
                    params.topMargin += (sectionHLetterSpacing * sectionSpacingFactor).dpToPx()
                    layoutParams = params
                }
            }
            chapterContentItem.updateTextView(binding.tvContent)
            // 获取富文本内容
            val spannedText = if (matchContent.isNotEmpty()) {
                //val highlightedText = highlightKeywords(userContent, matchContent)

                //LogUtils.e("==================>>>>>${matchContent}")
                // var highlightedText = redTxt(userContent, matchContent)
                //Html.fromHtml(highlightedText, Html.FROM_HTML_MODE_LEGACY)
                Html.fromHtml(userContent, Html.FROM_HTML_MODE_LEGACY)
            } else {
                Html.fromHtml(userContent, Html.FROM_HTML_MODE_LEGACY)
            }
            binding.tvContent.text = spannedText
            binding.tvContent.post {
                //LogUtils.e("行高==========----===${binding.tvContent.lineHeight}")
                LogUtils.e("行高==========----===${binding.tvContent.measuredHeight}")
                CoroutineScope(Dispatchers.Main).launch {
                    EventBus.sendChapterHeight(binding.tvContent.measuredHeight)
                }
            }

        }
        private fun redTxt(text: String, keywords: List<String>): String{
            //return text.replace(keywords[0],"<font color='red'>${keywords[0]}</font>")
            /*for (index in 0 until keywords.size){
                 text.replace(keywords[index],"<font color='red'>${keywords[index]}</font>")
            }*/
           if(keywords.size == 1){
               if(isEnglishText(keywords[0])){
                   return text.replace(keywords[0]+" ","<font color='red'>${keywords[0]+" "} </font>")
               }else{
                   return text.replace(keywords[0],"<font color='red'>${keywords[0]} </font>")
               }

           }else if(keywords.size == 2){

               if(isEnglishText(keywords[0])){
                   return text.replace(keywords[0]+" ","<font color='red'>${keywords[0]+" "} </font>")
                       .replace(keywords[1]+" ","<font color='red'>${keywords[1]+" "}</font>")
               }else{
                   return text.replace(keywords[0],"<font color='red'>${keywords[0]} </font>")
                       .replace(keywords[1],"<font color='red'>${keywords[1]+" "}</font>")
               }

           }else if(keywords.size == 3){
               if(isEnglishText(keywords[0])){
                   return text.replace(keywords[0]+" ","<font color='red'>${keywords[0]+" "}</font>")
                       .replace(keywords[1]+" ","<font color='red'>${keywords[1]+" "}</font>")
                       .replace(keywords[2]+" ","<font color='red'>${keywords[2]+" "}</font>")
               }else{
                   return text.replace(keywords[0],"<font color='red'>${keywords[0]}</font>")
                       .replace(keywords[1],"<font color='red'>${keywords[1]}</font>")
                       .replace(keywords[2],"<font color='red'>${keywords[2]}</font>")
               }

           }else if(keywords.size == 4){
               if(isEnglishText(keywords[0])){
                   return text.replace(keywords[0]+" ","<font color='red'>${keywords[0]+" "}</font>")
                       .replace(keywords[1]+" ","<font color='red'>${keywords[1]+" "}</font>")
                       .replace(keywords[2]+" ","<font color='red'>${keywords[2]+" "}</font>")
                       .replace(keywords[3]+" ","<font color='red'>${keywords[3]+" "}</font>")
               }else{
                   return text.replace(keywords[0],"<font color='red'>${keywords[0]}</font>")
                       .replace(keywords[1],"<font color='red'>${keywords[1]}</font>")
                       .replace(keywords[2],"<font color='red'>${keywords[2]}</font>")
                       .replace(keywords[3],"<font color='red'>${keywords[3]}</font>")
               }

           }else if(keywords.size == 5){
               if(isEnglishText(keywords[0])){
                   return text.replace(keywords[0]+" ","<font color='red'>${keywords[0]+" "}</font>")
                       .replace(keywords[1]+" ","<font color='red'>${keywords[1]+" "}</font>")
                       .replace(keywords[2]+" ","<font color='red'>${keywords[2]+" "}</font>")
                       .replace(keywords[3]+" ","<font color='red'>${keywords[3]+" "}</font>")
                       .replace(keywords[4]+" ","<font color='red'>${keywords[4]+" "}</font>")
               }else{
                   return text.replace(keywords[0],"<font color='red'>${keywords[0]}</font>")
                       .replace(keywords[1],"<font color='red'>${keywords[1]}</font>")
                       .replace(keywords[2],"<font color='red'>${keywords[2]}</font>")
                       .replace(keywords[3],"<font color='red'>${keywords[3]}</font>")
                       .replace(keywords[4],"<font color='red'>${keywords[4]}</font>")
               }

           }



            return text

        }

        // ========== 新增：安全的关键词高亮方法 ==========
        private fun highlightKeywords(text: String, keywords: List<String>): String {
            if (keywords.isEmpty()) return text

            var safeText = text.replace(Regex("<[^>]*>"), "")
//            // 转义HTML特殊字符
//            var safeText = text
//                .replace("&", "&amp;")
//                .replace("<", "&lt;")
//                .replace(">", "&gt;")
//                .replace("\"", "&quot;")
//                .replace("'", "&#39;")

            // 为每个关键词创建不区分大小写的正则模式
//            val patterns = keywords.map {
//                Pattern.compile(Pattern.quote(it), Pattern.CASE_INSENSITIVE)
//            }

            // 按长度降序排序关键词,避免短关键词在长关键词内匹配
            val sortedKeywords = keywords.sortedByDescending { it.length }

            // 安全替换关键词
            sortedKeywords.forEach { keyword ->
//                val safeKeyword = keyword
//                    .replace("&", "&amp;")
//                    .replace("<", "&lt;")
//                    .replace(">", "&gt;")
//                    .replace("\"", "&quot;")
//                    .replace("'", "&#39;")

                // 使用正则进行不区分大小写的替换
                safeText = safeText.replace(
                    "(?i)${Regex.escape(keyword)}".toRegex(),
                    "<font color='red'>$0</font>"
                )
            }

            // 保留换行符
            return safeText
        }
    }

    private fun formatContentWithSettings(
        content: String,
        indent: Int,
    ): String {
        val indentSpaces = "\u3000".repeat(indent)
        val paragraphs = content.split("\n\n")
        var result = ""
        paragraphs.forEachIndexed { index, paragraph ->
            result += if (paragraph.isNotBlank()) {
                "$indentSpaces$paragraph"
            } else {
                paragraph
            }
            // 保留段落间的空行
            if (index < paragraphs.size - 1) {
                result += "\n\n"
            }
        }
        return result
    }
    fun isEnglishText(text: String): Boolean {
        // 允许英文字母、空格、常见标点符号
        val pattern = "^[a-zA-Z\\s.,?!'-]+$".toRegex()
        return pattern.matches(text)
    }

    fun isFontSize14_5(text: String): Boolean {
        // 正则：匹配 CSS 或 JS 格式的 font-size:14.5（支持空格、可选单位）
        val regex = Regex("[\\u4e00-\\u9fff]{1,2}\\d{1,3}:\\d{1,3}")
        return regex.containsMatchIn(text)
    }
}

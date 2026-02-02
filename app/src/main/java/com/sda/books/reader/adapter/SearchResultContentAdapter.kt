package com.sda.books.reader.adapter

import android.app.Activity
import android.text.Html
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.blankj.utilcode.util.LogUtils
import com.sda.books.reader.ChapterPage
import com.sda.books.reader.LanguageUtils
import com.book.reader.R
import com.book.reader.databinding.LoadingEndItemBinding
import com.book.reader.databinding.LoadingItemBinding
import com.book.reader.databinding.SearchResultItemBinding
import com.sda.books.reader.dpToPx
import com.sda.books.reader.entity.SearchResultEntity
import com.sda.books.reader.entity.getShowVolumeName
import com.sda.books.reader.maxScreenPadding
import com.sda.books.reader.util.AppSettingUtil

class SearchResultContentAdapter(val activity: Activity,val pageSize:Int) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_END_LOADING = 2
    }

    var data = mutableListOf<SearchResultEntity>()
    var matchList = listOf<String>()
    private var isLoading = false
    var tempContentMode: Int = -1

    fun updateData(data: List<SearchResultEntity>) {
        this.data.clear()
        this.data.addAll(data)

        // 如果数据不足一页，直接添加底线
        if (this.data.isNotEmpty() && this.data.size < pageSize && !hasEndMarker()) {
            this.data.add(SearchResultEntity().apply { isEndData = true })
        }
        notifyDataSetChanged()
    }

    // 检查是否已添加底线标记
    fun hasEndMarker(): Boolean {
        return this.data.any { it.isEndData }
    }

    fun appendData(newData: List<SearchResultEntity>) {
        val startPosition = data.size
        data.addAll(newData)
        notifyItemRangeInserted(startPosition, newData.size)
    }

    fun clearData() {
        data.clear()
        notifyDataSetChanged()
    }

    fun showLoading(show: Boolean) {
        // 保留接口，但当前不使用加载项
    }


    fun updateMatchList(matchList: List<String>) {
        this.matchList = matchList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_END_LOADING) {
            val binding = LoadingEndItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            LoadingEndViewHolder(binding)
        } else {
            val binding = SearchResultItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            SearchResultContentHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SearchResultContentHolder -> holder.bind(data[position],position)
            is LoadingViewHolder -> holder.bind()
            is LoadingEndViewHolder -> holder.bind()
            // 不需要为加载视图绑定数据
        }
    }


    override fun getItemViewType(position: Int): Int {
        return if (data[position].isEndData) TYPE_END_LOADING else TYPE_ITEM
    }


    override fun getItemCount(): Int {
        return data.size
    }

    inner class LoadingEndViewHolder(private val binding: LoadingEndItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            val language = if (AppSettingUtil.getIsOpenEn()) "en" else "zh"
            val str =
                LanguageUtils.getStringByLanguage(context = activity, R.string.toast_msg2, language)
            binding.endTv.text = str
            binding.root.visibility = View.VISIBLE
        }
    }


    inner class LoadingViewHolder(private val binding: LoadingItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            // 可以在这里设置加载动画
            binding.progressBar.visibility = View.VISIBLE
        }
    }


    // ViewHolder 类
    inner class SearchResultContentHolder(private val binding: SearchResultItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // 绑定数据到视图
        fun bind(searchResultEntity: SearchResultEntity, position: Int) {
            val volName = getShowVolumeName(searchResultEntity.chapter.volName)
            binding.root.setOnClickListener {
                LogUtils.e("scrollIndex============${Gson().toJson(searchResultEntity)}")
                ChapterPage.start(
                    activity, volName,
                    searchResultEntity.chapter.name,
                    searchResultEntity.chapter.id,
                    searchResultEntity.scrollIndex,
                    searchResultEntity.tabs,
                    ArrayList(matchList),
                    // 传入“用户点击的那一行原文”，用于在 ChapterPage 精准定位到对应命中点
                    searchResultEntity.content.getShowContent().replace("*", "").trim(),
                    // 传入本次搜索的临时显示模式（不修改系统设置）
                    tempContentMode
                )
            }

            val hSpace = AppSettingUtil.getTextLetterSpacing()
            val vSpace = AppSettingUtil.getTextLineSpacingMultiplier()
            val screenPadding = AppSettingUtil.getScreenPadding()
            val textSize = AppSettingUtil.getTextSize()

            // 获取原始内容并处理
            val rawContent = searchResultEntity.content.getShowContent()
            val isEnglish = searchResultEntity.content.isEng()
            
            // 统一高亮处理：确保所有分支都设置 tvContent
            val highlightedContent = if (matchList.isNotEmpty()) {
                // 有搜索关键词：应用高亮（一次性匹配所有关键词，避免 HTML 嵌套）
                val prefix = if (isEnglish) "" else "::${position + 1}:: "
                highlightKeywords(rawContent, matchList, isEnglish)
                    .let { prefix + it }
            } else {
                // 无搜索关键词：直接显示（中文模式添加序号前缀）
                val prefix = if (isEnglish) "" else "::${position + 1}:: "
                prefix + rawContent
            }
            
            // 统一替换 "*" 符号（与 ChapterPage 保持一致）
            val finalContent = highlightedContent.replace("*", "")
            
            // 修复：使用正确的 Html.fromHtml 参数（Html.FROM_HTML_MODE_LEGACY）
            binding.tvContent.text = Html.fromHtml(finalContent, Html.FROM_HTML_MODE_LEGACY)

            // 设置其他 UI 元素
            if (volName.endsWith("E")) {
                binding.ivCe.visibility = View.VISIBLE
            } else {
                binding.ivCe.visibility = View.GONE
            }
            binding.tvVolumeName.text = volName.replace("E", "").split("(")[0]
            if (volName.contains("(")) {
                binding.tvAuthor.text = volName.replace("E", "").split("(")[1].replace(")", "")
            }
            binding.tvChapter.text = searchResultEntity.chapter.name

            // 设置文本样式（段落间距通过 setLineSpacing 控制，而不是传给 Html.fromHtml）
            binding.tvContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize * 1f)
            binding.tvContent.apply {
                if (!isEnglish) {
                    letterSpacing = hSpace / 100f * 2
                }
                // 段落间距通过 setLineSpacing 控制
                setLineSpacing(0f, vSpace / 100f * 1 + 1)
                val params = layoutParams as MarginLayoutParams
                params.marginStart = dpToPx(((screenPadding / 100f) * maxScreenPadding))
                params.marginEnd = dpToPx(((screenPadding / 100f) * maxScreenPadding))
                layoutParams = params
            }
        }

        /**
         * 统一的高亮关键词函数
         * @param text 原始文本
         * @param keywords 关键词列表（1-6个）
         * @param isEnglish 是否为英文内容（影响匹配方式）
         * @return 高亮后的 HTML 字符串
         */
        private fun highlightKeywords(text: String, keywords: List<String>, isEnglish: Boolean): String {
            if (keywords.isEmpty()) {
                return text
            }

            // 过滤并清理关键词
            val validKeywords = keywords.map { it.trim() }.filter { it.isNotEmpty() }
            if (validKeywords.isEmpty()) {
                return text
            }

            if (isEnglish) {
                // 英文：一次性构建包含所有关键词的正则表达式（使用单词边界，忽略大小写）
                return highlightEnglishKeywords(text, validKeywords)
            } else {
                // 中文：一次性构建包含所有关键词的正则表达式（直接匹配）
                return highlightChineseKeywords(text, validKeywords)
            }
        }

        /**
         * 高亮英文关键词（区分大小写，单个字母启用单词边界过滤）
         */
        private fun highlightEnglishKeywords(text: String, keywords: List<String>): String {
            var result = text
            
            // 分离单个字母和多字符关键词
            val singleLetters = keywords.filter { it.length == 1 && (it[0] in 'a'..'z' || it[0] in 'A'..'Z') }
            val multiCharKeywords = keywords.filter { it.length > 1 || (it.length == 1 && it[0] !in 'a'..'z' && it[0] !in 'A'..'Z') }
            
            // 先处理单个字母（需要单词边界过滤）
            singleLetters.forEach { kw ->
                result = highlightStandaloneLetter(result, kw[0])
            }
            
            // 再处理多字符关键词（直接匹配）
            if (multiCharKeywords.isNotEmpty()) {
                multiCharKeywords.filter { it.isNotEmpty() }.forEach { kw ->
                    result = highlightWholeWordIgnoreCase(result, kw)
                }
            }
            
            return result
        }
        
        /**
         * 高亮独立的单个字母（不在单词内部的字母，大小写都高亮）
         */
        private fun highlightStandaloneLetter(text: String, letter: Char): String {
            val lowerLetter = letter.lowercaseChar()
            val upperLetter = letter.uppercaseChar()
            val result = StringBuilder()
            
            var i = 0
            while (i < text.length) {
                // 跳过已有的 HTML 标签
                if (text.startsWith("<font", i)) {
                    val endIdx = text.indexOf("</font>", i)
                    if (endIdx != -1) {
                        result.append(text.substring(i, endIdx + 7))
                        i = endIdx + 7
                        continue
                    }
                }
                
                val c = text[i]
                if (c == lowerLetter || c == upperLetter) {
                    val prevChar = if (i > 0) text[i - 1] else ' '
                    val prevIsEnglishLetter = prevChar in 'A'..'Z' || prevChar in 'a'..'z'
                    val nextChar = if (i < text.length - 1) text[i + 1] else ' '
                    val nextIsEnglishLetter = nextChar in 'A'..'Z' || nextChar in 'a'..'z'
                    
                    if (!prevIsEnglishLetter && !nextIsEnglishLetter) {
                        result.append("<font color='red'>$c</font>")
                    } else {
                        result.append(c)
                    }
                } else {
                    result.append(c)
                }
                i++
            }
            return result.toString()
        }

        /**
         * 高亮完整英文单词（忽略大小写，必须是完整单词）
         */
        private fun highlightWholeWordIgnoreCase(text: String, word: String): String {
            if (word.isBlank()) return text
            
            val lowerWord = word.lowercase()
            val result = StringBuilder()
            var i = 0
            
            while (i < text.length) {
                // 跳过已有的 HTML 标签
                if (text.startsWith("<font", i)) {
                    val endIdx = text.indexOf("</font>", i)
                    if (endIdx != -1) {
                        result.append(text.substring(i, endIdx + 7))
                        i = endIdx + 7
                        continue
                    }
                }
                
                // 动态获取当前位置的小写文本（不包含已处理部分）
                val remainingText = text.substring(i)
                val lowerRemainingText = remainingText.lowercase()
                val idx = lowerRemainingText.indexOf(lowerWord)
                
                if (idx == -1) {
                    result.append(remainingText)
                    break
                }
                
                // 计算在原文本中的实际位置
                val actualIdx = i + idx
                val before = if (actualIdx > 0) text[actualIdx - 1] else ' '
                val afterIndex = actualIdx + word.length
                val after = if (afterIndex < text.length) text[afterIndex] else ' '
                val beforeIsLetter = before in 'A'..'Z' || before in 'a'..'z'
                val afterIsLetter = after in 'A'..'Z' || after in 'a'..'z'
                
                if (!beforeIsLetter && !afterIsLetter) {
                    // 找到完整单词，高亮
                    result.append(remainingText.substring(0, idx))
                    result.append("<font color='red'>")
                    result.append(text.substring(actualIdx, actualIdx + word.length))
                    result.append("</font>")
                    i = actualIdx + word.length
                } else {
                    // 不是完整单词，继续查找
                    result.append(remainingText.substring(0, idx + 1))
                    i = actualIdx + 1
                }
            }
            return result.toString()
        }

        /**
         * 高亮中文关键词（一次性匹配所有关键词，避免 HTML 嵌套）
         */
        private fun highlightChineseKeywords(text: String, keywords: List<String>): String {
            var result = text
            
            // 分离单个英文字母和其他关键词（即使在中文内容中，英文字母也需要单词边界过滤）
            val singleLetters = keywords.filter { it.length == 1 && (it[0] in 'a'..'z' || it[0] in 'A'..'Z') }
            val otherKeywords = keywords.filter { it.length != 1 || (it[0] !in 'a'..'z' && it[0] !in 'A'..'Z') }
            
            // 先处理单个英文字母（需要单词边界过滤）
            singleLetters.forEach { kw ->
                result = highlightStandaloneLetter(result, kw[0])
            }
            
            // 再处理其他关键词：英文按完整单词忽略大小写，其它关键词直接匹配
            otherKeywords.filter { it.isNotBlank() }.forEach { keyword ->
                result = if (hasEnglishChars(keyword)) {
                    highlightWholeWordIgnoreCase(result, keyword)
                } else {
                    result.replace(keyword, "<font color='red'>$keyword</font>")
                }
            }
            
            return result
        }

        private fun hasEnglishChars(text: String): Boolean {
            return text.any { it in 'A'..'Z' || it in 'a'..'z' }
        }
    }
}
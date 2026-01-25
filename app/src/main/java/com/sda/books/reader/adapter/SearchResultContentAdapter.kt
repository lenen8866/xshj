package com.sda.books.reader.adapter

import android.app.Activity
import android.graphics.Color
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.util.Log
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
import com.sda.books.reader.WordSpacingSpan
import com.book.reader.databinding.LoadingEndItemBinding
import com.book.reader.databinding.LoadingItemBinding
import com.book.reader.databinding.SearchResultItemBinding
import com.sda.books.reader.dpToPx
import com.sda.books.reader.entity.SearchResultEntity
import com.sda.books.reader.entity.getShowVolumeName
import com.sda.books.reader.maxScreenPadding
import com.sda.books.reader.util.AppSettingUtil
import java.util.regex.Pattern

class SearchResultContentAdapter(val activity: Activity,val pageSize:Int) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_END_LOADING = 2
    }

    var data = mutableListOf<SearchResultEntity>()
    var matchList = listOf<String>()
    private var isLoading = false

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
//        if (isLoading == show) return
//
//        isLoading = show
//        if (show) {
//            // 添加加载项
//            data.add(SearchResultEntity().apply { isLoader = true })
//            notifyItemInserted(data.size - 1)
//        } else {
//            // 移除加载项
//            if (data.isNotEmpty() && data.last().isLoader) {
//                data.removeAt(data.size - 1)
//                notifyItemRemoved(data.size)
//            }
//        }
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
        fun bind(searchResultEntity: SearchResultEntity,position: Int) {

            val volName = getShowVolumeName(searchResultEntity.chapter.volName)
            binding.root.setOnClickListener {
                LogUtils.e("scrollIndex============${Gson().toJson(searchResultEntity) }")
                ChapterPage.start(
                    activity, volName,
                    searchResultEntity.chapter.name,
                    searchResultEntity.chapter.id,
                    searchResultEntity.chapter.indexId,
                    searchResultEntity.tabs,
                    ArrayList(matchList)
                )
            }
            val hSpace = AppSettingUtil.getTextLetterSpacing()
            val vSpace = AppSettingUtil.getTextLineSpacingMultiplier()
            val screenPadding = AppSettingUtil.getScreenPadding()
            val textSize = AppSettingUtil.getTextSize()

           // LogUtils.e("=============>>"+matchList)
            /*val paragraphSpacing = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                AppSettingUtil.getTextSectionHLetterSpacing().toFloat(),
                 resources.displayMetrics
            ).toInt()*/
            val paragraphSpacing = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                AppSettingUtil.getTextSectionHLetterSpacing().toFloat(),
                activity.resources.displayMetrics
            ).toInt()

            val max = 50
            //LogUtils.e("position===========${searchResultEntity.chapter.id}")

            if (searchResultEntity.content.isEng()) {


                if (matchList.size > 0) {
                    when (matchList.size) {
                        1 -> {

                            /*binding.tvContent.text = Html.fromHtml(
                                searchResultEntity.content.getShowContent()
                                    .replace(
                                        matchList[0]+"",
                                        "<font color='red'>${matchList[0]+""}</font>"
                                    ).replace("*",""), paragraphSpacing
                            )*/
                            //LogUtils.e("查询id=============>>${containsWordIgnoreCase(searchResultEntity.content.getShowContent(),matchList[0])}")
                            binding.tvContent.text = Html.fromHtml(highlightWordIgnoreCase("::${position+1}:: "+searchResultEntity.content.getShowContent(),matchList[0].trimEnd()).replace("*",""),
                                paragraphSpacing)


                        }

                        2 -> {
                           if(searchResultEntity.content.getShowContent().contains(matchList[0]+" ") &&
                               searchResultEntity.content.getShowContent().contains(matchList[1]+" ") ){
                               binding.tvContent.text = Html.fromHtml(
                                   searchResultEntity.content.getShowContent()
                                       .replace(
                                           matchList[0]+" ",
                                           "<font color='red'>${matchList[0]+" "}</font>"
                                       )
                                       .replace(
                                           matchList[1]+" ",
                                           "<font color='red'>${matchList[1]+" "}</font>"
                                       ).replace("*",""), paragraphSpacing
                               )
                           }

                        }

                        3 -> {

                            binding.tvContent.text = Html.fromHtml(
                                searchResultEntity.content.getShowContent()
                                    .replace(
                                        matchList[0]+" ",
                                        "<font color='red'>${matchList[0]+" "}</font>"
                                    )
                                    .replace(
                                        matchList[1]+" ",
                                        "<font color='red'>${matchList[1]+" "}</font>"
                                    )
                                    .replace(
                                        matchList[2]+" ",
                                        "<font color='red'>${matchList[2]+" "}</font>"
                                    ).replace("*",""), paragraphSpacing
                            )
                        }

                        4 -> {

                            binding.tvContent.text = Html.fromHtml(
                                searchResultEntity.content.getShowContent()
                                    .replace(
                                        matchList[0]+" ",
                                        "<font color='red'>${matchList[0]+" "}</font>"
                                    )
                                    .replace(
                                        matchList[1]+" ",
                                        "<font color='red'>${matchList[1]+" "}</font>"
                                    )
                                    .replace(
                                        matchList[2]+" ",
                                        "<font color='red'>${matchList[2]+" "}</font>"
                                    )
                                    .replace(
                                        matchList[3]+" ",
                                        "<font color='red'>${matchList[3]+" "}</font>"
                                    ).replace("*",""), paragraphSpacing
                            )
                        }
                        5 -> {

                            binding.tvContent.text = Html.fromHtml(
                                searchResultEntity.content.getShowContent()
                                    .replace(
                                        matchList[0]+" ",
                                        "<font color='red'>${matchList[0]+" "}</font>"
                                    )
                                    .replace(
                                        matchList[1]+" ",
                                        "<font color='red'>${matchList[1]+" "}</font>"
                                    )
                                    .replace(
                                        matchList[2]+" ",
                                        "<font color='red'>${matchList[2]+" "}</font>"
                                    )
                                    .replace(
                                        matchList[3]+" ",
                                        "<font color='red'>${matchList[3]+" "}</font>"
                                    ).replace(
                                        matchList[4]+" ",
                                        "<font color='red'>${matchList[4]+" "}</font>"
                                    ).replace("*",""), paragraphSpacing
                            )
                        }
                        6 -> {

                            binding.tvContent.text = Html.fromHtml(
                                searchResultEntity.content.getShowContent()
                                    .replace(
                                        matchList[0]+" ",
                                        "<font color='red'>${matchList[0]+" "}</font>"
                                    )
                                    .replace(
                                        matchList[1]+" ",
                                        "<font color='red'>${matchList[1]+" "}</font>"
                                    )
                                    .replace(
                                        matchList[2]+" ",
                                        "<font color='red'>${matchList[2]+" "}</font>"
                                    )
                                    .replace(
                                        matchList[3]+" ",
                                        "<font color='red'>${matchList[3]+" "}</font>"
                                    ).replace(
                                        matchList[4]+" ",
                                        "<font color='red'>${matchList[4]+" "}</font>"
                                    ).replace(
                                        matchList[5]+" ",
                                        "<font color='red'>${matchList[5]+" "}</font>"
                                    ).replace("*",""), paragraphSpacing
                            )
                        }
                    }

                } else {

                    binding.tvContent.text = Html.fromHtml(
                        searchResultEntity.content.getShowContent().replace("*",""),
                        paragraphSpacing
                    )
                }

            } else {

                var str = searchResultEntity.content.getShowContent()
                for (item in matchList) {
                    Log.e("Tag", item)
                    searchResultEntity.content.getShowContent()
                        .replace(item, "<font color='red'>============</font>")
                }
                Log.e("Tag", str)
                //Log.e("Tag",searchResultEntity.content.getShowContent().replace(matchList[0],"==========="))
                if (matchList.size > 0) {
                    when (matchList.size) {
                        1 -> {

                            binding.tvContent.text = Html.fromHtml("::${position+1}:: "+
                                searchResultEntity.content.getShowContent()
                                    .replace(
                                        matchList[0],
                                        "<font color='red'>${matchList[0]}</font>"
                                    ).replace("*",""), paragraphSpacing
                            )

                        }

                        2 -> {

                            binding.tvContent.text = Html.fromHtml("::${position+1}:: "+
                                searchResultEntity.content.getShowContent()
                                    .replace(
                                        matchList[0],
                                        "<font color='red'>${matchList[0]}</font>"
                                    )
                                    .replace(
                                        matchList[1],
                                        "<font color='red'>${matchList[1]}</font>"
                                    ).replace("*",""), paragraphSpacing
                            )
                        }

                        3 -> {

                            binding.tvContent.text = Html.fromHtml("::${position+1}:: "+
                                searchResultEntity.content.getShowContent()
                                    .replace(
                                        matchList[0],
                                        "<font color='red'>${matchList[0]}</font>"
                                    )
                                    .replace(
                                        matchList[1],
                                        "<font color='red'>${matchList[1]}</font>"
                                    )
                                    .replace(
                                        matchList[2],
                                        "<font color='red'>${matchList[2]}</font>"
                                    ).replace("*",""), paragraphSpacing
                            )
                        }

                        4 -> {

                            binding.tvContent.text =Html.fromHtml( "::${position+1}:: "+
                                searchResultEntity.content.getShowContent()
                                    .replace(
                                        matchList[0],
                                        "<font color='red'>${matchList[0]}</font>"
                                    )
                                    .replace(
                                        matchList[1],
                                        "<font color='red'>${matchList[1]}</font>"
                                    )
                                    .replace(
                                        matchList[2],
                                        "<font color='red'>${matchList[2]}</font>"
                                    )
                                    .replace(
                                        matchList[3],
                                        "<font color='red'>${matchList[3]}</font>"
                                    ).replace("*",""), paragraphSpacing
                            )
                        }
                        5 -> {

                            binding.tvContent.text = Html.fromHtml("::${position+1}:: "+
                                searchResultEntity.content.getShowContent()
                                    .replace(
                                        matchList[0],
                                        "<font color='red'>${matchList[0]}</font>"
                                    )
                                    .replace(
                                        matchList[1],
                                        "<font color='red'>${matchList[1]}</font>"
                                    )
                                    .replace(
                                        matchList[2],
                                        "<font color='red'>${matchList[2]}</font>"
                                    )
                                    .replace(
                                        matchList[3],
                                        "<font color='red'>${matchList[3]}</font>"
                                    )
                                    .replace(
                                        matchList[4],
                                        "<font color='red'>${matchList[4]}</font>"
                                    ).replace("*",""), paragraphSpacing
                            )
                        }
                        6 -> {

                            binding.tvContent.text =Html.fromHtml( "::${position+1}:: "+
                                searchResultEntity.content.getShowContent()
                                    .replace(
                                        matchList[0],
                                        "<font color='red'>${matchList[0]}</font>"
                                    )
                                    .replace(
                                        matchList[1],
                                        "<font color='red'>${matchList[1]}</font>"
                                    )
                                    .replace(
                                        matchList[2],
                                        "<font color='red'>${matchList[2]}</font>"
                                    )
                                    .replace(
                                        matchList[3],
                                        "<font color='red'>${matchList[3]}</font>"
                                    )
                                    .replace(
                                        matchList[4],
                                        "<font color='red'>${matchList[4]}</font>"
                                    )
                                    .replace(
                                        matchList[5],
                                        "<font color='red'>${matchList[5]}</font>"
                                    ).replace("*",""), paragraphSpacing
                            )
                        }
                    }

                } else {

                    binding.tvContent.text = Html.fromHtml("::${position+1}:: "+
                        searchResultEntity.content.getShowContent().replace("*",""),
                      paragraphSpacing
                    )

                }
            }

            //binding.tvVolumeName.text = volName
            if(volName.endsWith("E")){
                binding.ivCe.visibility = View.VISIBLE
            }else{
                binding.ivCe.visibility = View.GONE
            }
            binding.tvVolumeName.text = volName.replace("E","").split("(")[0]
            if(volName.contains("(")){
                binding.tvAuthor.text = volName.replace("E","").split("(")[1].replace(")","")
            }

            binding.tvChapter.text = searchResultEntity.chapter.name


            binding.tvContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize * 1f)
            binding.tvContent.apply {
                if (searchResultEntity.content.isEng()) {

                } else {
                    letterSpacing = hSpace / 100f * 2
                }
                setLineSpacing(0f, vSpace / 100f * 1 + 1)
                val params = layoutParams as MarginLayoutParams
                params.marginStart = dpToPx(((screenPadding / 100f) * maxScreenPadding))
                params.marginEnd = dpToPx(((screenPadding / 100f) * maxScreenPadding))
                layoutParams = params
            }
        }
    }
}
fun containsWordIgnoreCase(text: String, word: String): Boolean {
    val regex = Regex("""\b$word\b""", RegexOption.IGNORE_CASE)
    return regex.containsMatchIn(text)
}
fun highlightWordIgnoreCase(originalText: String, targetWord: String): String {
    // 检查目标单词是否为空
    /*if (targetWord.isBlank()) {
        return Pair(false, originalText)
    }*/

    // 创建不区分大小写的正则表达式，确保匹配整个单词
    val pattern = Regex("\\b$targetWord\\b", RegexOption.IGNORE_CASE)

    // 检查是否包含目标单词
    val containsWord = pattern.containsMatchIn(originalText)

    // 如果不包含，直接返回原文本
    /*if (!containsWord) {
        return Pair(false, originalText)
    }*/

    // 替换匹配的单词，加上红色标记
    // 这里使用HTML的span标签示例，实际使用中可根据需要修改
    /*val highlightedText = originalText.replace(pattern) { matchResult ->
        "<span style=\"color: red;\">${matchResult.value}</span>"
    }*/

    val highlightedText = originalText.replace(pattern,"<span style=\"color: red;\">${targetWord}</span>")
    LogUtils.e("查询id=============>>${pattern}")
    LogUtils.e("查询id=============>>${highlightedText}")

    return highlightedText;
    //return Pair(true, highlightedText)
}

fun matchAndStyleEfficient(
    text: String,
    targets: List<String>,
    color: Int = Color.RED
): SpannableString {
    var spannable = SpannableString(text)
    var spannableStringBuilder = SpannableStringBuilder(text)
    if (targets.isEmpty()) return spannable

    // 构建所有目标词的正则表达式
    val pattern = Pattern.compile(
        targets.joinToString("|") { escapeRegex(it) },
        Pattern.CASE_INSENSITIVE
    )
    val matcher = pattern.matcher(text)
    //Log.e("002==========>>>>",matcher.replaceAll("")

    while (matcher.find()) {
        //spannable = SpannableString("<font color='red'>${spannable}</font>")
        /*spannable.setSpan(
            ForegroundColorSpan(color),
            matcher.start(),
            matcher.end(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )*/
        /* var start = text.indexOf(targets[0])
         var end = start + targets[0].length
         spannableStringBuilder.replace(start,end,"<font color='red'>${targets[0]}</font>")*/
        //text.replace(targets[0],"<font color='red'>${targets[0]}</font>")
    }
    Log.e("004==========>>>>", targets[0])
    Log.e("004==========>>>>", "" + targets[0].length)
    //text = "犹太巡抚"
    var newStr = text
    newStr.replace("犹太巡抚", "")
    newStr.replace("犹太巡抚", "")
    text.replace("犹太巡抚", "")
    text.replace("犹太巡抚", "")
    text.replace("犹太巡抚", "")
    text.replace("犹太巡抚", "")
    text.replace("犹太巡抚", "")
    text.replace("犹太巡抚", "")

    text.replace("犹太巡抚", "")

    Log.e("005==========>>>>", newStr)
    //return spannable
    return SpannableString(newStr)
}


/**
 * 为现有 SpannableString 添加单词间距
 */
fun SpannableString.applyWordSpacing(spacing: Float): SpannableString {
    val text = this.toString()
    val words = text.split(" ")

    if (words.size <= 1) return this // 没有空格则直接返回

    var currentIndex = 0

    Log.i("cccccc", "addSpace")
    // 为每个空格添加间距
    for (i in 0 until words.size - 1) {
        currentIndex += words[i].length
        this.setSpan(
            WordSpacingSpan(spacing),
            currentIndex,
            currentIndex + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        currentIndex += 1 // 移动到下一个字符
    }

    return this
}

private fun escapeRegex(input: String): String {
    // 使用正则表达式转义特殊字符
    return Regex.escape(input)
}
package com.sda.books.reader.act

import android.os.Bundle
import android.text.Html
import com.aleyn.mvvm.base.BaseVMActivity
import com.book.reader.R
import com.book.reader.databinding.ActAboutUsBinding
import com.sda.books.reader.model.MainViewModel

class AboutUsAct:BaseVMActivity<MainViewModel,ActAboutUsBinding>() {
    var str = "<h3 align=\"center\"><b>软件简介：</b></h3>\n" +
            "\n" +
            "\n" +
            "<p><i>“从前所写的圣经都是为教训我们写的，叫我们因圣经所生的忍耐和安慰可以得着盼望。”（<b>罗马书 15:4</b>）</i></p >\n" +
            "\n" +
            "<p><b>1888年的信息</b>指的是基督复临安息日会内一个重要的信仰转折点。这道信息最初在1888年总会代表大会上由 <b>E.J.瓦格纳</b> 和 <b>A.T.琼斯</b> 所提出，主题是 <b>因信基督得以称义</b>。</p >\n" +
            "\n" +
            "<p>它强调了 <b>基督的信心</b> 与 <b>祂的义之力量</b>，可通过突出其对“<b>被高举的救主</b>”和“<b>基督无与伦比的魅力</b>”来引入。这项充满盼望与转变的信息，展现了上帝的爱如何积极地拯救并改变人心。</p >\n" +
            "\n" +
            "<p>这信息也可被理解为对《启示录》第18章中“<b>另有一位天使</b>”信息的更深层阐释。</p >\n" +
            "\n" +
            "<p><b>基督之义的信息</b>是贯穿整本圣经的福音主题。从旧约中的先祖和先知，到新约中的使徒们（特别是 <b>使徒保罗</b>），都对此做出了深刻的阐述。</p >\n" +
            "\n" +
            "<p>在末后的时代，上帝拣选祂的仆人 <b>怀爱伦</b>，将 <b>基督测不透的义</b> 再一次全方位地启示出来，同时又拣选 <b>琼斯和瓦格纳</b>，明确指出了 <b>如何使基督的义成为我们的义</b> 这一关键主题。</p >\n" +
            "\n" +
            "<p>本软件致力于向世人传达这 <b>三位信使</b> 带来的奇妙信息。<br>\n" +
            "<b>愿上帝赐福于每一位领受者！</b></p >"
    override fun initData() {
        mBinding.ivBack.setOnClickListener {
            finish()
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        mBinding.tvContent.text = Html.fromHtml(str,Html.FROM_HTML_MODE_LEGACY)
    }

    override val layoutId: Int = R.layout.act_about_us
}
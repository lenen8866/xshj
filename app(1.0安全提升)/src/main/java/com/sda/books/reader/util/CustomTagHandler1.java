package com.sda.books.reader.util;

import android.content.res.Resources;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.style.LeadingMarginSpan;

import org.xml.sax.XMLReader;

// 自定义标签处理器，处理段落间距
public   class CustomTagHandler1 implements Html.TagHandler {
    private static final int PARAGRAPH_MARGIN_DP = 600; // 段落间距，可根据需要调整
    private int marginPx;

    public CustomTagHandler1() {
        // 将dp转换为px
        marginPx = (int) (PARAGRAPH_MARGIN_DP * Resources.getSystem().getDisplayMetrics().density);
    }

    @Override
    public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
        if (tag.equalsIgnoreCase("p")) {
            if (opening) {
                // 记录<p>标签开始位置
                output.setSpan(new LeadingMarginSpan.Standard(0, marginPx), output.length(), output.length(), Spanned.SPAN_MARK_MARK);
            } else {
                // 查找对应的开始标签
                Object[] spans = output.getSpans(0, output.length(), LeadingMarginSpan.Standard.class);
                if (spans.length > 0) {
                    int start = output.getSpanStart(spans[spans.length - 1]);
                    output.setSpan(new LeadingMarginSpan.Standard(0, marginPx), start, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }
}

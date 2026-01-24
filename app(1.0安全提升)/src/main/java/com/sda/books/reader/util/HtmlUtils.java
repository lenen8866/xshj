package com.sda.books.reader.util;

import android.text.Html;
import android.text.Spanned;
import android.text.style.LeadingMarginSpan;
import androidx.annotation.NonNull;
import org.xml.sax.XMLReader;

public class HtmlUtils {

    // 处理HTML文本，设置段落间距
    public static Spanned fromHtml(@NonNull String html) {
        return Html.fromHtml(html, null, new CustomTagHandler1());
    }


}

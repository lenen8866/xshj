package com.sda.books.reader.util

import android.text.Editable
import android.text.Html
import android.util.Log
import org.xml.sax.XMLReader

class CustomTagHandler : Html.TagHandler {
    override fun handleTag(
        opening: Boolean,
        tag: String,
        output: Editable,
        xmlReader: XMLReader
    ) {
        Log.i("cccccc","tag====$tag")

    }



}
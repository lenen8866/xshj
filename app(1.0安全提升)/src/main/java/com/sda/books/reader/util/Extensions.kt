package com.sda.books.reader.util

import android.content.Context
import android.util.TypedValue
import com.sda.books.reader.App

fun Float.dpToPx(context: Context = App.context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        context.resources.displayMetrics
    ).toInt()
}

fun Int.dpToPx(context: Context = App.context): Int {
    return this.toFloat().dpToPx(context)
}
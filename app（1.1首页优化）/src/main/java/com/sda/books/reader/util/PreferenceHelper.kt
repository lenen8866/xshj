package com.sda.books.reader.util


import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("db_prefs", Context.MODE_PRIVATE)

    var currentDbPath: String?
        get() = prefs.getString("current_db_path", null)
        set(value) = prefs.edit().putString("current_db_path", value).apply()
}
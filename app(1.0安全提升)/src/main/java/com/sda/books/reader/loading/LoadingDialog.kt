package com.sda.books.reader.loading
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.book.reader.R

class LoadingDialog(context: Context) : Dialog(context, R.style.LoadingDialogStyle) {

    init {
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setDimAmount(0f)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Log.e("TAG","==========0007")
        setContentView(R.layout.loading)
        setCancelable(false) // 禁止点击外部关闭
    }


}
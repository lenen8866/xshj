package com.sda.books.reader.util

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.EditText

class NumberRangeInputFilter(
    private val editText: EditText,
    private val minValue: Int = 1,
    private val maxValue: Int = 100,
    onChange:(Int)->Unit
) {
    private var isAdjusting = false
    private var lastValidText = ""

    init {
        // 设置输入类型为整数
        editText.inputType = InputType.TYPE_CLASS_NUMBER

        // 添加文本变化监听
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isAdjusting) return

                val currentText = s.toString()

                // 空值处理（允许清空）
                if (currentText.isEmpty()) {
                    lastValidText = ""
                    return
                }

                try {
                    val value = currentText.toInt()

                    // 检查范围
                    if (value in minValue..maxValue) {
                        lastValidText = currentText  // 有效范围，保存为上次有效值
                        onChange(lastValidText.toInt())
                    } else {
                        // 超出范围，恢复上次有效文本
                        adjustText(lastValidText)
                    }
                } catch (e: NumberFormatException) {
                    // 处理非数字输入（理论上不会发生）
                    adjustText(lastValidText)
                }
            }

            override fun afterTextChanged(s: Editable?) {}

            private fun adjustText(newText: String) {
                isAdjusting = true
                editText.setText(newText)
                editText.setSelection(editText.text.length)  // 保持光标在末尾
                isAdjusting = false
            }
        })
    }
}
package com.sda.books.reader.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;

public class NonScrollableSelectableTextView extends androidx.appcompat.widget.AppCompatTextView {
    public NonScrollableSelectableTextView(Context context) {
        super(context);
    }

    public NonScrollableSelectableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NonScrollableSelectableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        // 禁用焦点变化时的滚动
        if (!focused) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
        }
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        // 禁用选择变化时的滚动
        if (selStart >= 0 && selEnd >= 0) {
            // 可选：只在文本过长时禁用滚动
            if (getLayout() != null && getLayout().getLineCount() > getMaxLines()) {
                return;
            }
        }
        super.onSelectionChanged(selStart, selEnd);
    }
}

package com.sda.books.reader.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;

import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import com.blankj.utilcode.util.LogUtils;
import com.book.reader.R;

import com.sda.books.reader.util.AppSettingUtil;

import android.util.Log;

public class SelectableTextHelper {

    private static final int DEFAULT_SELECTION_LENGTH = 1;
    private static final int DEFAULT_SHOW_DURATION = 100;
    private static final int OPERATE_WINDOW_OFFSET = 16;
    private static final int OPERATE_WINDOW_ELEVATION = 8;
    private static final String TAG = "SelectableText";

    private CursorHandle mStartHandle;
    private CursorHandle mEndHandle;
    private OperateWindow mOperateWindow;
    private SelectionInfo mSelectionInfo = new SelectionInfo();
    private OnSelectListener mSelectListener;

    private Context mContext;
    private TextView mTextView;
    private Spannable mSpannable;

    private int mTouchX;
    private int mTouchY;

    private int mSelectedColor;
    private int mCursorHandleColor;
    private int mCursorHandleSize;
    private BackgroundColorSpan mSpan;
    private boolean isHideWhenScroll;
    private boolean isHide = true;

    private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;
    ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener;

    public SelectableTextHelper(Builder builder) {
        mTextView = builder.mTextView;
        mContext = mTextView.getContext();
        mSelectedColor = builder.mSelectedColor;
        mCursorHandleColor = builder.mCursorHandleColor;
        mCursorHandleSize = TextLayoutUtil.dp2px(mContext, builder.mCursorHandleSizeInDp);
        init();
    }

    private void init() {
        mTextView.setText(mTextView.getText(), TextView.BufferType.SPANNABLE);
        mTextView.setOnLongClickListener(v -> {
            showSelectView(mTouchX, mTouchY);
            return true;
        });

        mTextView.setOnTouchListener((v, event) -> {
            mTouchX = (int) event.getX();
            mTouchY = (int) event.getY();
            return false;
        });

        mTextView.setOnClickListener(v -> {
            resetSelectionInfo();
            hideSelectView();
        });
        
        mTextView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                // 不需要处理
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                destroy();
            }
        });

        mOnPreDrawListener = () -> {
            if (isHideWhenScroll) {
                isHideWhenScroll = false;
                postShowSelectView(DEFAULT_SHOW_DURATION);
            }
            return true;
        };
        mTextView.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);

        mOnScrollChangedListener = () -> {
            if (!isHideWhenScroll && !isHide) {
                isHideWhenScroll = true;
                dismissAllHandles();
            }
        };
        mTextView.getViewTreeObserver().addOnScrollChangedListener(mOnScrollChangedListener);

        mOperateWindow = new OperateWindow(mContext);
    }
    
    private void dismissAllHandles() {
        if (mOperateWindow != null) {
            mOperateWindow.dismiss();
        }
        if (mStartHandle != null) {
            mStartHandle.dismiss();
        }
        if (mEndHandle != null) {
            mEndHandle.dismiss();
        }
    }

    private void postShowSelectView(int duration) {
        mTextView.removeCallbacks(mShowSelectViewRunnable);
        if (duration <= 0) {
            mShowSelectViewRunnable.run();
        } else {
            mTextView.postDelayed(mShowSelectViewRunnable, duration);
        }
    }

    private final Runnable mShowSelectViewRunnable = new Runnable() {
        @Override
        public void run() {
            if (isHide) return;
            if (mOperateWindow != null) {
                mOperateWindow.show();
            }
            if (mStartHandle != null) {
                showCursorHandle(mStartHandle);
            }
            if (mEndHandle != null) {
                showCursorHandle(mEndHandle);
            }
        }
    };

    private void hideSelectView() {
        isHide = true;
        dismissAllHandles();
    }

    private void resetSelectionInfo() {
        mSelectionInfo.mSelectionContent = null;
        if (mSpannable != null && mSpan != null) {
            mSpannable.removeSpan(mSpan);
            mSpan = null;
        }
    }

    private void showSelectView(int x, int y) {
        hideSelectView();
        resetSelectionInfo();
        isHide = false;
        if (mStartHandle == null) mStartHandle = new CursorHandle(true);
        if (mEndHandle == null) mEndHandle = new CursorHandle(false);

        int startOffset = TextLayoutUtil.getPreciseOffset(mTextView, x, y);
        int endOffset = startOffset + DEFAULT_SELECTION_LENGTH;
        if (mTextView.getText() instanceof Spannable) {
            mSpannable = (Spannable) mTextView.getText();
        }
        if (mSpannable == null || startOffset >= mTextView.getText().length()) {
            return;
        }
        selectText(startOffset, endOffset);
        showCursorHandle(mStartHandle);
        showCursorHandle(mEndHandle);
        mOperateWindow.show();
    }

    private void showCursorHandle(CursorHandle cursorHandle) {
        Layout layout = mTextView.getLayout();
        int offset = cursorHandle.isLeft ? mSelectionInfo.mStart : mSelectionInfo.mEnd;
        cursorHandle.show((int) layout.getPrimaryHorizontal(offset), layout.getLineBottom(layout.getLineForOffset(offset)));
    }

    private void selectText(int startPos, int endPos) {
        if (startPos != -1) {
            mSelectionInfo.mStart = startPos;
        }
        if (endPos != -1) {
            mSelectionInfo.mEnd = endPos;
        }
        
        // 确保 start <= end
        if (mSelectionInfo.mStart > mSelectionInfo.mEnd) {
            int temp = mSelectionInfo.mStart;
            mSelectionInfo.mStart = mSelectionInfo.mEnd;
            mSelectionInfo.mEnd = temp;
        }

        if (mSpannable == null) {
            return;
        }
        
        // 创建或重用 BackgroundColorSpan
        if (mSpan == null) {
            mSpan = new BackgroundColorSpan(mSelectedColor);
        }
        
        // 更新选择内容和样式
        mSelectionInfo.mSelectionContent = mSpannable.subSequence(mSelectionInfo.mStart, mSelectionInfo.mEnd).toString();
        mSpannable.setSpan(mSpan, mSelectionInfo.mStart, mSelectionInfo.mEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        
        // 通知监听器
        if (mSelectListener != null) {
            mSelectListener.onTextSelected(mSelectionInfo.mSelectionContent);
        }
    }

    public void setSelectListener(OnSelectListener selectListener) {
        mSelectListener = selectListener;
    }

    public void destroy() {
        mTextView.getViewTreeObserver().removeOnScrollChangedListener(mOnScrollChangedListener);
        mTextView.getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
        resetSelectionInfo();
        hideSelectView();
        mStartHandle = null;
        mEndHandle = null;
        mOperateWindow = null;
    }

    /**
     * Operate windows : copy, select all
     */
    private class OperateWindow {

        private PopupWindow mWindow;
        private int[] mTempCoors = new int[2];

        private int mWidth;
        private int mHeight;

        public OperateWindow(final Context context) {
            View contentView = LayoutInflater.from(context).inflate(R.layout.layout_operate_windows, null);
            contentView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            mWidth = contentView.getMeasuredWidth();
            mHeight = contentView.getMeasuredHeight();
            mWindow =
                new PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
            mWindow.setClippingEnabled(false);

            contentView.findViewById(R.id.tv_copy).setOnClickListener(v -> {
                ClipboardManager clip = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                String selectedText = mSelectionInfo.mSelectionContent;
                int vFristSpace = AppSettingUtil.INSTANCE.getTextFristLetterSpacing();
                String indentSpaces = "\u3000".repeat(vFristSpace);
                
                // 处理文本：移除 HTML 标签、替换缩进空格、移除星号
                String processedText = new SpannableString(Html.fromHtml(selectedText))
                    .toString()
                    .replace(indentSpaces, "\n")
                    .replace("*", "");
                
                clip.setPrimaryClip(ClipData.newPlainText(selectedText, processedText));
                
                if (mSelectListener != null) {
                    mSelectListener.onTextSelected(processedText);
                }
                
                SelectableTextHelper.this.resetSelectionInfo();
                SelectableTextHelper.this.hideSelectView();
            });
            
            contentView.findViewById(R.id.tv_select_all).setOnClickListener(v -> {
                hideSelectView();
                selectText(0, mTextView.getText().length());
                isHide = false;
                showCursorHandle(mStartHandle);
                showCursorHandle(mEndHandle);
                mOperateWindow.show();
            });
        }

        public void show() {
            mTextView.getLocationInWindow(mTempCoors);
            Layout layout = mTextView.getLayout();
            int posX = (int) layout.getPrimaryHorizontal(mSelectionInfo.mStart) + mTempCoors[0];
            int posY = layout.getLineTop(layout.getLineForOffset(mSelectionInfo.mStart)) + mTempCoors[1] - mHeight - OPERATE_WINDOW_OFFSET;
            
            // 边界检查，确保窗口不超出屏幕
            int screenWidth = TextLayoutUtil.getScreenWidth(mContext);
            posX = Math.max(OPERATE_WINDOW_OFFSET, Math.min(posX, screenWidth - mWidth - OPERATE_WINDOW_OFFSET));
            posY = Math.max(OPERATE_WINDOW_OFFSET, posY);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mWindow.setElevation(OPERATE_WINDOW_ELEVATION);
            }
            mWindow.showAtLocation(mTextView, Gravity.NO_GRAVITY, posX, posY);
        }

        public void dismiss() {
            mWindow.dismiss();
        }

        public boolean isShowing() {
            return mWindow.isShowing();
        }
    }

    private class CursorHandle extends View {

        private PopupWindow mPopupWindow;
        private Paint mPaint;

        private int mCircleRadius = mCursorHandleSize / 2;
        private int mWidth = mCircleRadius * 2;
        private int mHeight = mCircleRadius * 2;
        private int mPadding = 25;
        private boolean isLeft;
        
        // 获取另一个手柄的引用
        private CursorHandle getOtherHandle() {
            return (this == SelectableTextHelper.this.mStartHandle) 
                ? SelectableTextHelper.this.mEndHandle 
                : SelectableTextHelper.this.mStartHandle;
        }

        public CursorHandle(boolean isLeft) {
            super(mContext);
            this.isLeft = isLeft;
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setColor(mCursorHandleColor);

            mPopupWindow = new PopupWindow(this);
            mPopupWindow.setClippingEnabled(false);
            mPopupWindow.setWidth(mWidth + mPadding * 2);
            mPopupWindow.setHeight(mHeight + mPadding / 2);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawCircle(mCircleRadius + mPadding, mCircleRadius, mCircleRadius, mPaint);
            if (isLeft) {
                canvas.drawRect(mCircleRadius + mPadding, 0, mCircleRadius * 2 + mPadding, mCircleRadius, mPaint);
            } else {
                canvas.drawRect(mPadding, 0, mCircleRadius + mPadding, mCircleRadius, mPaint);
            }
        }

        private int mAdjustX;
        private int mAdjustY;

        private int mBeforeDragStart;
        private int mBeforeDragEnd;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mBeforeDragStart = mSelectionInfo.mStart;
                    mBeforeDragEnd = mSelectionInfo.mEnd;
                    mAdjustX = (int) event.getX();
                    mAdjustY = (int) event.getY();
                    Log.d(TAG, String.format("ACTION_DOWN: isLeft=%b, start=%d, end=%d", 
                        isLeft, mBeforeDragStart, mBeforeDragEnd));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mOperateWindow.show();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mOperateWindow.dismiss();
                    // 修复坐标换算：需要转换为 TextView 内的相对坐标
                    mTextView.getLocationInWindow(mTempCoors);
                    int relativeX = (int) event.getRawX() - mTempCoors[0];
                    int relativeY = (int) event.getRawY() - mTempCoors[1];
                    update(relativeX, relativeY);
                    break;
            }
            return true;
        }

        private void changeDirection() {
            isLeft = !isLeft;
            invalidate();
        }

        public void dismiss() {
            mPopupWindow.dismiss();
        }

        private int[] mTempCoors = new int[2];

        public void update(int x, int y) {
            // 保存当前选择范围（在 resetSelectionInfo 之前）
            int currentStart = mSelectionInfo.mStart;
            int currentEnd = mSelectionInfo.mEnd;
            int oldOffset = isLeft ? currentStart : currentEnd;

            // x, y 已经是 TextView 内的相对坐标，直接使用
            int offset = TextLayoutUtil.getHysteresisOffset(mTextView, x, y, oldOffset);
            
            // 添加日志验证 offset 是否真的能跨越（仅在调试时启用）
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, String.format(
                    "update: isLeft=%b, x=%d, y=%d, oldOffset=%d, newOffset=%d, currentStart=%d, currentEnd=%d, " +
                    "canCrossRight=%b, canCrossLeft=%b",
                    isLeft, x, y, oldOffset, offset, currentStart, currentEnd,
                    offset > currentEnd, offset < currentStart));
            }

            if (offset == oldOffset) {
                return; // 没有变化，直接返回
            }
            
            resetSelectionInfo();
            
            if (isLeft) {
                handleLeftCursorUpdate(offset, currentStart, currentEnd);
            } else {
                handleRightCursorUpdate(offset, currentStart, currentEnd);
            }
        }
        
        private void handleLeftCursorUpdate(int offset, int currentStart, int currentEnd) {
            if (offset > currentEnd) {
                // 越过右端：交换起点和终点，手柄角色互换
                Log.d(TAG, String.format("LEFT handle crossing RIGHT: offset=%d > end=%d, swapping", 
                    offset, currentEnd));
                swapHandlesAndSelect(currentEnd, offset);
            } else {
                // 正常向左移动：更新起点
                selectText(offset, -1);
                mBeforeDragStart = offset;
            }
            updateCursorHandle();
        }
        
        private void handleRightCursorUpdate(int offset, int currentStart, int currentEnd) {
            if (offset < currentStart) {
                // 越过左端：交换起点和终点，手柄角色互换
                Log.d(TAG, String.format("RIGHT handle crossing LEFT: offset=%d < start=%d, swapping", 
                    offset, currentStart));
                swapHandlesAndSelect(offset, currentStart);
            } else {
                // 正常向右移动：更新终点
                selectText(-1, offset);
                mBeforeDragEnd = offset;
            }
            updateCursorHandle();
        }
        
        private void swapHandlesAndSelect(int newStart, int newEnd) {
            CursorHandle otherHandle = getOtherHandle();
            changeDirection();
            otherHandle.changeDirection();
            selectText(newStart, newEnd);
            // 更新当前手柄的参考值
            mBeforeDragStart = newStart;
            mBeforeDragEnd = newEnd;
            // 同步更新另一个手柄的参考值
            otherHandle.mBeforeDragStart = newStart;
            otherHandle.mBeforeDragEnd = newEnd;
            otherHandle.updateCursorHandle();
        }

        private void updateCursorHandle() {
            mTextView.getLocationInWindow(mTempCoors);
            Layout layout = mTextView.getLayout();
            if (isLeft) {
                mPopupWindow.update((int) layout.getPrimaryHorizontal(mSelectionInfo.mStart) - mWidth + getExtraX(),
                    layout.getLineBottom(layout.getLineForOffset(mSelectionInfo.mStart)) + getExtraY(), -1, -1);
            } else {
                mPopupWindow.update((int) layout.getPrimaryHorizontal(mSelectionInfo.mEnd) + getExtraX(),
                    layout.getLineBottom(layout.getLineForOffset(mSelectionInfo.mEnd)) + getExtraY(), -1, -1);
            }
        }

        public void show(int x, int y) {
            mTextView.getLocationInWindow(mTempCoors);
            int offset = isLeft ? mWidth : 0;
            mPopupWindow.showAtLocation(mTextView, Gravity.NO_GRAVITY, x - offset + getExtraX(), y + getExtraY());
        }

        public int getExtraX() {
            return mTempCoors[0] - mPadding + mTextView.getPaddingLeft();
        }

        public int getExtraY() {
            return mTempCoors[1] + mTextView.getPaddingTop();
        }
    }

    private CursorHandle getCursorHandle(boolean isLeft) {
        if (mStartHandle.isLeft == isLeft) {
            return mStartHandle;
        } else {
            return mEndHandle;
        }
    }

    public static class Builder {
        private TextView mTextView;
        private int mCursorHandleColor = 0xFF1379D6;
        private int mSelectedColor = 0xFFAFE1F4;
        private float mCursorHandleSizeInDp = 24;

        public Builder(TextView textView) {
            mTextView = textView;
        }

        public Builder setCursorHandleColor(@ColorInt int cursorHandleColor) {
            mCursorHandleColor = cursorHandleColor;
            return this;
        }

        public Builder setCursorHandleSizeInDp(float cursorHandleSizeInDp) {
            mCursorHandleSizeInDp = cursorHandleSizeInDp;
            return this;
        }

        public Builder setSelectedColor(@ColorInt int selectedBgColor) {
            mSelectedColor = selectedBgColor;
            return this;
        }

        public SelectableTextHelper build() {
            return new SelectableTextHelper(this);
        }
    }
}



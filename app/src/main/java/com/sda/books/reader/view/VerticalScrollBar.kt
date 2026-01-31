package com.sda.books.reader.view

import android.R
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VerticalScrollBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 滚动条样式属性
    private val scrollBarWidth = 5.dpToPx()
    private val scrollBarRadius = 6.dpToPx()
    private val scrollBarMargin = 4.dpToPx()
    private val scrollBarColor = ContextCompat.getColor(context, R.color.darker_gray)

    // 固定滚动条高度为70dp
    private val fixedScrollBarHeight = 70.dpToPx()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = scrollBarColor
        style = Paint.Style.FILL
    }

    private var scrollBarHeight = fixedScrollBarHeight
    private var scrollBarTop = 0
    private var isDragging = false
    private var recyclerView: RecyclerView? = null

    // ===== 透明度控制 =====
    private var scrollBarAlpha = 255
    private val maxAlpha = 255
    private val minAlpha = 0
    private val fadeOutDuration = 1500L
    private val fadeInDuration = 300L
    private var fadeAnimator: ValueAnimator? = null
    private val idleHideDelay = 2000L
    private val hideRunnable = Runnable { startFadeAnimation(false) }
    // =====================

    // 保存滚动监听器
    private var scrollListener: RecyclerView.OnScrollListener? = null

    // 设置关联的RecyclerView
    fun attachToRecyclerView(recyclerView: RecyclerView) {
        // 先解除之前的绑定
        detach()

        this.recyclerView = recyclerView
        scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateScrollPosition()
                showScrollBarTemporarily()
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> scheduleHide()
                    RecyclerView.SCROLL_STATE_DRAGGING,
                    RecyclerView.SCROLL_STATE_SETTLING -> showScrollBarTemporarily()
                }
            }
        }
        scrollListener?.apply {
            recyclerView.addOnScrollListener(this)
        }
        updateScrollPosition()
    }

    // 解除绑定
    fun detach() {
        recyclerView?.let {
            scrollListener?.let { listener ->
                it.removeOnScrollListener(listener)
            }
        }
        recyclerView = null
        scrollListener = null
        isDragging = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (scrollBarHeight == 0 || scrollBarAlpha == 0) return

        paint.alpha = scrollBarAlpha

        val rect = RectF(
            (width - scrollBarWidth - scrollBarMargin).toFloat(),
            scrollBarTop.toFloat(),
            width - scrollBarMargin.toFloat(),
            (scrollBarTop + scrollBarHeight).toFloat()
        )
        canvas.drawRoundRect(rect, scrollBarRadius.toFloat(), scrollBarRadius.toFloat(), paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isInScrollBar(event.x, event.y)) {
                    isDragging = true
                    scrollToPosition(event.y)
                    showScrollBarImmediately()
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    scrollToPosition(event.y)
                    showScrollBarImmediately()
                    return true
                } else if (isInScrollBar(event.x, event.y)) {
                    // 新增：即使没有开始拖动，只要在区域内也显示
                    showScrollBarImmediately()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                scheduleHide()
            }
        }
        return super.onTouchEvent(event)
    }

    // 修复1：正确的触摸区域判断
    private fun isInScrollBar(x: Float, y: Float): Boolean {
        // 计算滚动条的实际位置
        val scrollBarLeft = width - scrollBarWidth - scrollBarMargin
        val scrollBarRight = width - scrollBarMargin
        val scrollBarBottom = scrollBarTop + scrollBarHeight

        // 允许在滚动条区域及其右侧一小块区域触发（增加触摸区域）
        val touchAreaExtension = 10.dpToPx()

        return x >= scrollBarLeft - touchAreaExtension &&
                x <= scrollBarRight + touchAreaExtension &&
                y >= scrollBarTop - touchAreaExtension &&
                y <= scrollBarBottom + touchAreaExtension
    }

    fun updateScrollPosition() {
        recyclerView?.let { rv ->
            val layoutManager =
                rv.layoutManager as? LinearLayoutManager
            layoutManager?.let { lm ->
                val firstVisiblePos = lm.findFirstVisibleItemPosition()
                val visibleItems = lm.childCount
                val totalItems = lm.itemCount

                if (totalItems == 0 || visibleItems == 0) return

                scrollBarHeight = fixedScrollBarHeight

                val scrollRange = height - scrollBarHeight
                val maxScrollItems = totalItems - visibleItems

                scrollBarTop = if (maxScrollItems > 0 && scrollRange > 0) {
                    (scrollRange * firstVisiblePos / maxScrollItems).coerceIn(0, scrollRange)
                } else {
                    0
                }

                invalidate()
            }
        }
    }

    private fun scrollToPosition(y: Float) {
        recyclerView?.let { rv ->
            val layoutManager =
                rv.layoutManager as? LinearLayoutManager
            layoutManager?.let { lm ->
                val totalItems = lm.itemCount
                val visibleItems = lm.childCount
                val maxScrollItems = totalItems - visibleItems

                if (maxScrollItems <= 0) return

                val scrollRange = height - scrollBarHeight
                if (scrollRange <= 0) return

                val scrollPercentage =
                    (y - scrollBarHeight / 2).coerceIn(0f, scrollRange.toFloat()) / scrollRange
                val scrolledItems = (maxScrollItems * scrollPercentage).toInt()

                val targetPos = scrolledItems.coerceIn(0, maxScrollItems)
                lm.scrollToPositionWithOffset(targetPos, 0)
            }
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    // ===== 滚动条显示/隐藏控制 =====

    /** 立即显示滚动条 */
    private fun showScrollBarImmediately() {
        removeCallbacks(hideRunnable)
        fadeAnimator?.cancel()
        scrollBarAlpha = maxAlpha
        invalidate()
    }

    /** 临时显示滚动条 */
    private fun showScrollBarTemporarily() {
        removeCallbacks(hideRunnable)

        if (scrollBarAlpha < maxAlpha) {
            startFadeAnimation(true)
        }

        postDelayed(hideRunnable, idleHideDelay)
    }

    /** 安排隐藏滚动条 */
    private fun scheduleHide() {
        removeCallbacks(hideRunnable)
        postDelayed(hideRunnable, idleHideDelay)
    }

    /** 开始透明度动画 */
    private fun startFadeAnimation(show: Boolean) {
        fadeAnimator?.cancel()

        val startAlpha = scrollBarAlpha
        val endAlpha = if (show) maxAlpha else minAlpha

        fadeAnimator = ValueAnimator.ofInt(startAlpha, endAlpha).apply {
            duration = if (show) fadeInDuration else fadeOutDuration
            addUpdateListener { animation ->
                scrollBarAlpha = animation.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    // 修复2：增加生命周期管理
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        detach()
        removeCallbacks(hideRunnable)
        fadeAnimator?.cancel()
    }

    // 新增：重置状态
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            updateScrollPosition()
        } else {
            removeCallbacks(hideRunnable)
            fadeAnimator?.cancel()
            isDragging = false
        }
    }
}
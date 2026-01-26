package com.sda.books.reader.act

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aleyn.mvvm.base.BaseVMActivity
import com.ansen.shape.AnsenTextView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.ToastUtils
import com.sda.books.reader.AppCategoryPage
import com.book.reader.R
import com.book.reader.databinding.ActWelcomeBinding
import com.sda.books.reader.db.DatabaseHelper
import com.sda.books.reader.model.MainViewModel
import com.sda.books.reader.util.AssetsCopyManager
import com.sda.books.reader.util.Constant
import com.sda.books.reader.view.CustomDialog
import kotlinx.coroutines.launch


class WelcomeAct : BaseVMActivity<MainViewModel, ActWelcomeBinding>() {
    
    private var dialog: CustomDialog? = null
    private var countDownTimer: CountDownTimer? = null
    private var hasNavigated = false // 防止重复跳转
    private var isDbReady = false // DB 是否就绪
    private var hasAgreed = false // 是否已同意协议
    
    override fun initData() {
        // 启动 DB 复制
        startDbCopy()
        
        // 已同意过协议，直接等 DB 就绪后进首页
        if (SPUtils.getInstance().getBoolean(Constant.isFirst, false)) {
            hasAgreed = true
            checkAndNavigate()
        } else {
            // 首次启动，显示协议弹窗
            showPrivacyDialog()
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        dialog = CustomDialog(
            this,
            R.layout.dialog_secret_text,
            intArrayOf(R.id.tv_secret, R.id.tv_cancel, R.id.tv_sure),
            0, false, false, Gravity.CENTER
        )
    }

    /**
     * 启动数据库复制
     */
    private fun startDbCopy() {
        if (SPUtils.getInstance().getBoolean("copyFinish", false)) {
            isDbReady = true
            return
        }
        
        lifecycleScope.launch {
            val targetPath = getDatabasePath(DatabaseHelper.DB_NAME).path
            AssetsCopyManager(applicationContext).copyFile(
                "xshj.db", targetPath,
                object : AssetsCopyManager.CopyProgressCallback {
                    override fun onProgress(progress: Float) {}
                    
                    override fun onComplete() {
                        isDbReady = true
                        SPUtils.getInstance().put("copyFinish", true)
                        ToastUtils.showShort("数据加载完成")
                        checkAndNavigate()
                    }
                    
                    override fun onError(e: Exception) {
                        LogUtils.e("WelcomeAct", "DB复制失败: ${e.message}")
                        ToastUtils.showLong("数据加载失败，请重启应用")
                    }
                }
            )
        }
    }

    /**
     * 显示隐私协议弹窗
     */
    private fun showPrivacyDialog() {
        dialog?.show()
        val views = dialog?.views ?: return
        val tvSecret = views[0] as TextView
        val tvCancel = views[1] as AnsenTextView
        val tvSure = views[2] as AnsenTextView
        
        // ✅ 修复：使用 ContextCompat.getColor() 替代 Resources.getColor()
        // 初始化同意按钮（灰色不可点击）
        tvSure.apply {
            solidColor = ContextCompat.getColor(this@WelcomeAct, R.color.color_999)
            setTextColor(ContextCompat.getColor(this@WelcomeAct, R.color.white))
            resetBackground()
            text = "同意 (3)"
        }
        
        // 3秒倒计时
        var canClick = false
        countDownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millis: Long) {
                tvSure.text = "同意 (${millis / 1000})"
            }
            
            override fun onFinish() {
                canClick = true
                tvSure.apply {
                    text = "同意"
                    solidColor = ContextCompat.getColor(this@WelcomeAct, R.color.colorAccent)
                    setTextColor(ContextCompat.getColor(this@WelcomeAct, R.color.white))
                    resetBackground()
                }
            }
        }.also { it.start() }
        
        // 按钮点击事件
        dialog?.setOnDialogItemClickListener { dlg, view ->
            when (view.id) {
                R.id.tv_secret -> {
                    // 打开隐私政策页面
                    startActivity(Intent(this, SecretAct::class.java))
                }
                R.id.tv_cancel -> {
                    dlg.cancel()
                    finish()
                }
                R.id.tv_sure -> {
                    if (!canClick) return@setOnDialogItemClickListener
                    
                    dlg.cancel()
                    hasAgreed = true
                    SPUtils.getInstance().put(Constant.isFirst, true)
                    
                    if (isDbReady) {
                        navigateToHome()
                    } else {
                        ToastUtils.showLong("数据加载中，请稍候...")
                    }
                }
            }
        }
    }

    /**
     * 检查条件并导航（DB就绪 + 已同意 = 进首页）
     */
    private fun checkAndNavigate() {
        if (hasAgreed && isDbReady) {
            navigateToHome()
        }
    }

    /**
     * 导航到首页（只执行一次）
     */
    private fun navigateToHome() {
        if (hasNavigated) return
        hasNavigated = true
        
        Intent(this, AppCategoryPage::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(this)
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        dialog?.dismiss()
    }

    override val layoutId: Int get() = R.layout.act_welcome
}

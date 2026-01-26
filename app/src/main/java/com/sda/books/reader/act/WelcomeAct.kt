package com.sda.books.reader.act

import android.content.Intent
import android.content.res.AssetManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.aleyn.mvvm.base.BaseVMActivity
import com.ansen.shape.AnsenTextView
import com.blankj.utilcode.util.FileUtils
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


class WelcomeAct:BaseVMActivity<MainViewModel, ActWelcomeBinding>() {
    private var dialog: CustomDialog? = null
    private var isCanClick = false
    val DB_NAME = "xshj.db"
    private lateinit var copyManager: AssetsCopyManager
    private var isFinish = false
    override fun initData() {
        copyManager = AssetsCopyManager(applicationContext)

        // 示例：复制 assets 下的 test.txt 到应用外部缓存目录
        //val assetsFilePath = "test.txt" // assets 根目录下的 test.txt
        val assetsFilePath = DB_NAME
        val targetFilePath = this.getDatabasePath(DatabaseHelper.Companion.DB_NAME).path
        lifecycleScope.launch {
            LogUtils.e("复制进度===========>>>>>>${!SPUtils.getInstance().getBoolean("copyFinish",false)}")
            if(!SPUtils.getInstance().getBoolean("copyFinish",false)){
                copyManager.copyFile(assetsFilePath, targetFilePath,
                    object : AssetsCopyManager.CopyProgressCallback {
                        override fun onProgress(progress: Float) {
                            // 进度：0.0 ~ 1.0，可乘以 100 转为百分比
                            val progressPercent = (progress * 100).toInt()
                            println("复制进度：$progressPercent%")
                            // 这里可以更新 UI，比如进度条：progressBar.progress = progressPercent
                        }

                        override fun onComplete() {
                            println("文件复制完成！目标路径：$targetFilePath")
                            // /data/user/0/com.sda.books.reader/databases/xshj.db
                            // /data/user/0/com.sda.books.reader/databases/xshj.db
                            isFinish = true
                            SPUtils.getInstance().put("copyFinish",true)
                            LogUtils.e("===========>>>>>>${dialog?.isShowing}")
                            ToastUtils.showLong("数据加载完成")
                            if(SPUtils.getInstance().getBoolean(Constant.isFirst,false)){
                                startActivity(Intent(this@WelcomeAct, AppCategoryPage::class.java))
                                finish()
                            }else{

                            }
                        }

                        override fun onError(e: Exception) {
                            println("复制失败：${e.message}")
                            e.printStackTrace()
                        }
                    }
                )
            }

         }


        if( SPUtils.getInstance().getBoolean(Constant.isFirst,false)){
           /* lifecycleScope.launch {
                if(!SPUtils.getInstance().getBoolean("copyFinish",false)){
                    copyManager.copyFile(assetsFilePath, targetFilePath,
                        object : AssetsCopyManager.CopyProgressCallback {
                            override fun onProgress(progress: Float) {
                                // 进度：0.0 ~ 1.0，可乘以 100 转为百分比
                                val progressPercent = (progress * 100).toInt()
                                println("复制进度：$progressPercent%")
                                // 这里可以更新 UI，比如进度条：progressBar.progress = progressPercent
                            }

                            override fun onComplete() {
                                println("文件复制完成！目标路径：$targetFilePath")

                                SPUtils.getInstance().put("copyFinish",true)
                                dismissLoading()
                                startActivity(Intent(this@WelcomeAct, AppCategoryPage::class.java))
                            }

                            override fun onError(e: Exception) {
                                println("复制失败：${e.message}")
                                e.printStackTrace()
                            }
                        }
                    )
                }else{
                    startActivity(Intent(this@WelcomeAct, AppCategoryPage::class.java))
                }
            }*/

            startActivity(Intent(this@WelcomeAct, AppCategoryPage::class.java))
            finish()
        }else{
            dialog?.let {
                it.show()


                var viewList = it.views
                var tv_secret = viewList[0] as TextView
                var tv_cancel = viewList[1] as AnsenTextView
                var tv_sure = viewList[2] as AnsenTextView
                tv_sure.solidColor = this.resources.getColor(R.color.color_999)
                tv_sure.resetBackground()
                val countDownTimer = object : CountDownTimer(3 * 1000, 1000) {
                    // 每隔 1 秒回调（主线程）
                    override fun onTick(millisUntilFinished: Long) {
                        val remainingSeconds = millisUntilFinished / 1000 // 剩余秒数
                        // 更新按钮文字显示倒计时
                        tv_sure.text = "同意 ($remainingSeconds)"
                    }

                    // 倒计时结束回调
                    override fun onFinish() {
                        isCanClick = true
                        tv_sure.text = "同意" // 倒计时结束，恢复按钮文字
                        tv_sure.solidColor = resources.getColor(R.color.colorAccent)
                        tv_sure.setTextColor(resources.getColor(R.color.white))
                        tv_sure.resetBackground()
                    }
                }

// 启动倒计时（关键：调用 start() 才会开始）
                countDownTimer.start()
                it.setOnDialogItemClickListener { dialog, view ->
                    when(view.id){
                        R.id.tv_secret -> {
                            startActivity(Intent(this@WelcomeAct, AppCategoryPage::class.java))
                            finish()
                        }
                        R.id.tv_cancel -> {
                            dialog.cancel()
                            finish()
                        }
                        R.id.tv_sure -> {
                            if(!isCanClick){
                                return@setOnDialogItemClickListener
                            }
                            dialog.cancel()
                            SPUtils.getInstance().put(Constant.isFirst,true)
                            if(isFinish){
                                startActivity(Intent(this@WelcomeAct, AppCategoryPage::class.java))
                                finish()
                            }else{
                                //showLoading()
                                ToastUtils.showLong("数据加载中....")
                            }


                        }
                    }
                }
            }
        }

    }

    override fun initView(savedInstanceState: Bundle?) {
        dialog = CustomDialog(
            this,
            R.layout.dialog_secret_text,
            intArrayOf(
                R.id.tv_secret,
                R.id.tv_cancel,
                R.id.tv_sure),
            0,
            false,
            false,
            Gravity.CENTER
        )





    }

    override val layoutId: Int
        get() = R.layout.act_welcome
}
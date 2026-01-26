package com.sda.books.reader.util

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.book.reader.R
import com.sda.books.reader.store.StoreManager

/**
 * 音频控制器
 * 负责 ExoPlayer 的初始化、播放控制、进度更新、资源释放
 */
class AudioController(private val context: Context) {
    
    private var player: ExoPlayer? = null
    private var audioDuration = 1L
    
    // UI 组件
    private var playerView: PlayerView? = null
    private var audioContainer: View? = null
    private var seekBar: SeekBar? = null
    private var currentTimeView: TextView? = null
    private var durationView: TextView? = null
    private var controlButton: ImageView? = null
    
    // 进度更新
    private val handler = Handler(Looper.getMainLooper())
    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 1000)
        }
    }
    
    /**
     * 绑定 UI 组件
     */
    fun bindViews(
        playerView: PlayerView,
        audioContainer: View,
        seekBar: SeekBar,
        currentTimeView: TextView,
        durationView: TextView,
        controlButton: ImageView
    ) {
        this.playerView = playerView
        this.audioContainer = audioContainer
        this.seekBar = seekBar
        this.currentTimeView = currentTimeView
        this.durationView = durationView
        this.controlButton = controlButton
        
        setupSeekBar()
        setupControlButton()
    }
    
    /**
     * 初始化播放器
     * @param musicUrl 音频/视频 URL
     */
    fun initializePlayer(musicUrl: String) {
        val isDownloaded = StoreManager.checkFileIsDownLoadFinish(musicUrl)
        val uri = if (isDownloaded) {
            Uri.fromFile(StoreManager.getDesFile(musicUrl))
        } else {
            StoreManager.startDownLoad(musicUrl)
            Uri.parse(musicUrl)
        }
        
        player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            playWhenReady = false
            
            // 视频预览
            if (musicUrl.endsWith("mp4")) {
                playerView?.player = this
            }
            
            prepare()
            
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    handlePlaybackStateChanged(playbackState, musicUrl)
                }
            })
        }
    }
    
    /**
     * 播放/暂停切换
     */
    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
                controlButton?.setImageResource(R.drawable.play1)
            } else {
                it.play()
                controlButton?.setImageResource(R.drawable.pause1)
            }
        }
    }
    
    /**
     * 跳转到指定位置
     * @param progress 进度 (0-100)
     */
    fun seekTo(progress: Int) {
        val position = (progress / 100f * audioDuration).toLong()
        player?.seekTo(position)
    }
    
    /**
     * 释放资源
     */
    fun release() {
        handler.removeCallbacks(progressUpdateRunnable)
        player?.release()
        player = null
    }
    
    // ========== 私有方法 ==========
    
    private fun setupSeekBar() {
        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekTo(seekBar?.progress ?: 0)
            }
        })
    }
    
    private fun setupControlButton() {
        controlButton?.setOnClickListener {
            togglePlayPause()
        }
    }
    
    private fun handlePlaybackStateChanged(playbackState: Int, musicUrl: String) {
        when (playbackState) {
            Player.STATE_READY -> {
                audioDuration = player?.duration ?: 1L
                
                if (musicUrl.endsWith("mp4")) {
                    playerView?.visibility = View.VISIBLE
                } else {
                    audioContainer?.visibility = View.VISIBLE
                    durationView?.text = formatTime(audioDuration)
                    handler.post(progressUpdateRunnable)
                }
            }
            Player.STATE_ENDED -> {
                controlButton?.setImageResource(R.drawable.play1)
                seekBar?.progress = 0
            }
        }
    }
    
    private fun updateProgress() {
        player?.let {
            val position = it.currentPosition
            currentTimeView?.text = formatTime(position)
            seekBar?.progress = (position / audioDuration.toFloat() * 100).toInt()
        }
    }
    
    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 60000) % 60
        val hours = ms / 3600000
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}

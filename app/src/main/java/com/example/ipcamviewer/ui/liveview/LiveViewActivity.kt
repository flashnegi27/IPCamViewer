package com.example.ipcamviewer.ui.liveview

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ipcamviewer.data.AppDatabase
import com.example.ipcamviewer.databinding.ActivityLiveViewBinding
import com.example.ipcamviewer.player.VlcPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.MediaPlayer

class LiveViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLiveViewBinding
    private var player: VlcPlayerManager? = null
    private var cameraId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemBars()

        cameraId = intent.getLongExtra(EXTRA_CAMERA_ID, -1L)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.buttonRetry.setOnClickListener { loadAndPlay() }

        loadAndPlay()
    }

    private fun loadAndPlay() {
        binding.textError.visibility = View.GONE
        binding.buttonRetry.visibility = View.GONE
        binding.progressLoading.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            val camera = AppDatabase.getInstance(applicationContext).cameraDao().getById(cameraId)
            withContext(Dispatchers.Main) {
                if (camera == null) {
                    finish()
                    return@withContext
                }
                binding.toolbar.title = camera.name
                startPlayback(camera.buildRtspUrl(), camera.transport == "tcp")
            }
        }
    }

    private fun startPlayback(url: String, useTcp: Boolean) {
        player?.release()
        val vlc = VlcPlayerManager(this)
        player = vlc
        vlc.attach(binding.videoLayout)
        vlc.mediaPlayer.setEventListener { event ->
            runOnUiThread {
                when (event.type) {
                    MediaPlayer.Event.Playing -> {
                        binding.progressLoading.visibility = View.GONE
                        binding.textError.visibility = View.GONE
                        binding.buttonRetry.visibility = View.GONE
                    }
                    MediaPlayer.Event.EncounteredError -> {
                        binding.progressLoading.visibility = View.GONE
                        binding.textError.visibility = View.VISIBLE
                        binding.buttonRetry.visibility = View.VISIBLE
                    }
                }
            }
        }
        vlc.play(url, useTcp)
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }

    companion object {
        const val EXTRA_CAMERA_ID = "extra_camera_id"
    }
}

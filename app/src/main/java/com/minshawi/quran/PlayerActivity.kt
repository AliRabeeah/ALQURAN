package com.minshawi.quran

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PlayerActivity : AppCompatActivity() {

    private var playbackService: PlaybackService? = null
    private var serviceBound = false
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var tvTitle: TextView
    private lateinit var tvArtist: TextView
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnPrev: ImageButton
    private lateinit var btnBack: ImageButton

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val b = binder as PlaybackService.LocalBinder
            playbackService = b.getService()
            serviceBound = true
            playbackService?.onStateChanged = { isPlaying, surah ->
                runOnUiThread { updateUi(isPlaying, surah) }
            }
            updateUi(playbackService?.isPlaying() == true, playbackService?.current())
            startProgressUpdates()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
            playbackService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        tvTitle = findViewById(R.id.tvPlayerScreenTitle)
        tvArtist = findViewById(R.id.tvPlayerScreenArtist)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        seekBar = findViewById(R.id.seekBarFull)
        btnPlayPause = findViewById(R.id.btnPlayPauseFull)
        btnNext = findViewById(R.id.btnNextFull)
        btnPrev = findViewById(R.id.btnPrevFull)
        btnBack = findViewById(R.id.btnBackFull)

        btnBack.setOnClickListener { finish() }
        btnPlayPause.setOnClickListener { playbackService?.togglePause() }
        btnNext.setOnClickListener { playbackService?.playNext() }
        btnPrev.setOnClickListener { playbackService?.playPrevious() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) playbackService?.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val intent = Intent(this, PlaybackService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun updateUi(isPlaying: Boolean, surah: Surah?) {
        if (surah != null) {
            tvTitle.text = "سورة ${surah.arabicName}"
        }
        tvArtist.text = "محمد صديق المنشاوي"
        btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause_white else R.drawable.ic_play_white
        )
    }

    private fun formatTime(ms: Int): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun startProgressUpdates() {
        handler.removeCallbacksAndMessages(null)
        val runnable = object : Runnable {
            override fun run() {
                val service = playbackService
                if (service != null && service.duration() > 0) {
                    seekBar.max = service.duration()
                    seekBar.progress = service.currentPosition()
                    tvCurrentTime.text = formatTime(service.currentPosition())
                    tvTotalTime.text = formatTime(service.duration())
                }
                handler.postDelayed(this, 500)
            }
        }
        handler.post(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) unbindService(connection)
        handler.removeCallbacksAndMessages(null)
    }
}

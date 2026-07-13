package com.minshawi.quran

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity(), DownloadHelper.Listener {

    private lateinit var adapter: SurahAdapter
    private lateinit var downloadHelper: DownloadHelper
    private lateinit var recycler: RecyclerView
    private lateinit var tvStatus: TextView
    private lateinit var btnDownloadAll: Button

    private lateinit var playerBar: View
    private lateinit var tvPlayerTitle: TextView
    private lateinit var btnPlayerToggle: ImageButton
    private lateinit var seekBar: SeekBar

    private var playbackService: PlaybackService? = null
    private var serviceBound = false
    private val handler = Handler(Looper.getMainLooper())

    private var downloadAllQueue: MutableList<Surah> = mutableListOf()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val b = binder as PlaybackService.LocalBinder
            playbackService = b.getService()
            serviceBound = true
            playbackService?.onStateChanged = { isPlaying, surah ->
                runOnUiThread { updatePlayerUi(isPlaying, surah) }
            }
            playbackService?.onCompletion = {
                runOnUiThread { playNextAfter(playbackService?.current()) }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
            playbackService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recycler = findViewById(R.id.recyclerSurahs)
        tvStatus = findViewById(R.id.tvStatus)
        btnDownloadAll = findViewById(R.id.btnDownloadAll)
        playerBar = findViewById(R.id.playerBar)
        tvPlayerTitle = findViewById(R.id.tvPlayerTitle)
        btnPlayerToggle = findViewById(R.id.btnPlayerToggle)
        seekBar = findViewById(R.id.seekBar)

        downloadHelper = DownloadHelper(this)
        downloadHelper.setListener(this)

        adapter = SurahAdapter(
            this,
            onPlayClick = { surah -> playSurah(surah) },
            onDownloadClick = { surah -> downloadHelper.download(surah) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        btnDownloadAll.setOnClickListener { startDownloadAll() }
        btnPlayerToggle.setOnClickListener { playbackService?.togglePause() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) playbackService?.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val intent = Intent(this, PlaybackService::class.java)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        updateStatus()
    }

    private fun updateStatus() {
        val count = StorageHelper.totalDownloadedCount(this)
        tvStatus.text = "تم تحميل $count من 114 سورة"
    }

    private fun startDownloadAll() {
        downloadAllQueue = QuranData.surahs
            .filter { !StorageHelper.isDownloaded(this, it) }
            .toMutableList()
        if (downloadAllQueue.isEmpty()) {
            Toast.makeText(this, "جميع السور محملة بالفعل", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "بدأ تحميل ${downloadAllQueue.size} سورة...", Toast.LENGTH_SHORT).show()
        downloadNextInQueue()
    }

    private fun downloadNextInQueue() {
        if (downloadAllQueue.isEmpty()) return
        val next = downloadAllQueue.removeAt(0)
        downloadHelper.download(next)
    }

    override fun onDownloadComplete(surah: Surah, success: Boolean, error: String?) {
        runOnUiThread {
            adapter.markDownloadFinished(surah)
            updateStatus()
            if (!success) {
                Toast.makeText(this, "تعذر تحميل سورة ${surah.arabicName}: ${error ?: "خطأ غير معروف"}", Toast.LENGTH_LONG).show()
            }
            if (downloadAllQueue.isNotEmpty()) {
                downloadNextInQueue()
            }
        }
    }
    private fun playSurah(surah: Surah) {
        playbackService?.play(surah)
        adapter.currentlyPlaying = surah.number
        playerBar.visibility = View.VISIBLE
        tvPlayerTitle.text = "سورة ${surah.arabicName}"
        startProgressUpdates()
    }

    private fun playNextAfter(surah: Surah?) {
        if (surah == null) return
        val idx = QuranData.surahs.indexOf(surah)
        val next = QuranData.surahs.getOrNull(idx + 1) ?: return
        if (StorageHelper.isDownloaded(this, next)) {
            playSurah(next)
        }
    }

    private fun updatePlayerUi(isPlaying: Boolean, surah: Surah?) {
        adapter.currentlyPlaying = if (isPlaying) surah?.number else null
        btnPlayerToggle.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
    }

    private fun startProgressUpdates() {
        handler.removeCallbacksAndMessages(null)
        val runnable = object : Runnable {
            override fun run() {
                val service = playbackService
                if (service != null && service.duration() > 0) {
                    seekBar.max = service.duration()
                    seekBar.progress = service.currentPosition()
                }
                handler.postDelayed(this, 500)
            }
        }
        handler.post(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadHelper.unregister()
        if (serviceBound) unbindService(connection)
        handler.removeCallbacksAndMessages(null)
    }
}

package com.minshawi.quran

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

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
    private lateinit var containerAyat: LinearLayout
    private lateinit var scrollAyat: ScrollView

    private var ayahList: List<Ayah> = emptyList()
    private var ayahViews: List<TextView> = emptyList()
    private var currentSurahNumber: Int = -1
    private var lastHighlightedIndex: Int = -1

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val b = binder as PlaybackService.LocalBinder
            playbackService = b.getService()
            serviceBound = true
            playbackService?.onStateChanged = { isPlaying, surah ->
                runOnUiThread { updateUi(isPlaying, surah) }
            }
            val current = playbackService?.current()
            updateUi(playbackService?.isPlaying() == true, current)
            if (current != null) loadAyahText(current)
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
        containerAyat = findViewById(R.id.containerAyat)
        scrollAyat = findViewById(R.id.scrollAyat)

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
            if (surah.number != currentSurahNumber) {
                loadAyahText(surah)
            }
        }
        tvArtist.text = "محمد صديق المنشاوي"
        btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause_white else R.drawable.ic_play_white
        )
    }

    private fun loadAyahText(surah: Surah) {
        currentSurahNumber = surah.number
        containerAyat.removeAllViews()
        ayahList = emptyList()
        ayahViews = emptyList()
        lastHighlightedIndex = -1

        val loadingTv = TextView(this).apply {
            text = "جارِ تحميل نص الآيات..."
            setTextColor(ContextCompat.getColor(this@PlayerActivity, R.color.secondaryText))
            gravity = Gravity.CENTER
            textSize = 13f
        }
        containerAyat.addView(loadingTv)

        QuranTextHelper.fetchAyahs(this, surah) { ayahs, error ->
            containerAyat.removeAllViews()
            if (ayahs == null) {
                val errTv = TextView(this).apply {
                    text = "تعذر تحميل نص الآيات${if (error != null) ": $error" else ""}"
                    setTextColor(ContextCompat.getColor(this@PlayerActivity, R.color.secondaryText))
                    gravity = Gravity.CENTER
                    textSize = 13f
                }
                containerAyat.addView(errTv)
                return@fetchAyahs
            }
            ayahList = ayahs
            val views = mutableListOf<TextView>()
            for (ayah in ayahs) {
                val tv = TextView(this).apply {
                    text = "${ayah.text} ﴿${ayah.number}﴾"
                    setTextColor(ContextCompat.getColor(this@PlayerActivity, R.color.secondaryText))
                    textSize = 17f
                    gravity = Gravity.CENTER
                    setPadding(8, 14, 8, 14)
                    setLineSpacing(6f, 1.2f)
                }
                containerAyat.addView(tv)
                views.add(tv)
            }
            ayahViews = views
        }
    }

    /** تظليل تقريبي للآية الحالية بناءً على نسبة الوقت المنقضي (وليس مزامنة حرفية دقيقة) */
    private fun updateHighlight(currentMs: Int, totalMs: Int) {
        if (ayahList.isEmpty() || totalMs <= 0) return

        val totalChars = ayahList.sumOf { it.text.length }.coerceAtLeast(1)
        var cumulative = 0
        var targetIndex = 0
        val progress = currentMs.toFloat() / totalMs.toFloat()

        for ((idx, ayah) in ayahList.withIndex()) {
            cumulative += ayah.text.length
            val ayahEndProgress = cumulative.toFloat() / totalChars
            if (progress <= ayahEndProgress) {
                targetIndex = idx
                break
            }
            targetIndex = idx
        }

        if (targetIndex != lastHighlightedIndex && targetIndex < ayahViews.size) {
            ayahViews.getOrNull(lastHighlightedIndex)?.setTextColor(
                ContextCompat.getColor(this, R.color.secondaryText)
            )
            ayahViews[targetIndex].setTextColor(ContextCompat.getColor(this, R.color.primary))
            lastHighlightedIndex = targetIndex

            val targetView = ayahViews[targetIndex]
            scrollAyat.post {
                scrollAyat.smoothScrollTo(0, targetView.top - 40)
            }
        }
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
                    updateHighlight(service.currentPosition(), service.duration())
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

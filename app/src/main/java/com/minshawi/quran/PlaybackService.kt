package com.minshawi.quran

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat

class PlaybackService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var currentSurah: Surah? = null
    var onStateChanged: ((isPlaying: Boolean, surah: Surah?) -> Unit)? = null
    var onCompletion: (() -> Unit)? = null

    companion object {
        const val CHANNEL_ID = "quran_playback_channel"
        const val NOTIF_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun play(surah: Surah) {
        val file = StorageHelper.localFile(this, surah)
        if (!file.exists()) return

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnPreparedListener {
                it.start()
                currentSurah = surah
                onStateChanged?.invoke(true, surah)
                startForeground(NOTIF_ID, buildNotification(surah, true))
            }
            setOnCompletionListener {
                onStateChanged?.invoke(false, surah)
                onCompletion?.invoke()
            }
            prepareAsync()
        }
    }

    fun togglePause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            onStateChanged?.invoke(false, currentSurah)
            currentSurah?.let { updateNotification(it, false) }
        } else {
            mp.start()
            onStateChanged?.invoke(true, currentSurah)
            currentSurah?.let { updateNotification(it, true) }
        }
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
    fun currentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    fun duration(): Int = mediaPlayer?.duration ?: 0
    fun seekTo(ms: Int) { mediaPlayer?.seekTo(ms) }
    fun current(): Surah? = currentSurah

    fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentSurah = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "تشغيل القرآن", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(surah: Surah, playing: Boolean): android.app.Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("سورة ${surah.arabicName}")
            .setContentText("محمد صديق المنشاوي")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pending)
            .setOngoing(playing)
            .build()
    }

    private fun updateNotification(surah: Surah, playing: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotification(surah, playing))
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}

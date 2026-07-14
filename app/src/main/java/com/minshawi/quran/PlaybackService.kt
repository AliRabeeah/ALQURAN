package com.minshawi.quran

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

class PlaybackService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var currentSurah: Surah? = null
    private lateinit var mediaSession: MediaSessionCompat

    var onStateChanged: ((isPlaying: Boolean, surah: Surah?) -> Unit)? = null
    var onCompletion: (() -> Unit)? = null

    companion object {
        const val CHANNEL_ID = "quran_playback_channel"
        const val NOTIF_ID = 1001
        const val ACTION_PLAY_PAUSE = "com.minshawi.quran.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.minshawi.quran.action.NEXT"
        const val ACTION_PREV = "com.minshawi.quran.action.PREV"
        const val ACTION_STOP = "com.minshawi.quran.action.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        mediaSession = MediaSessionCompat(this, "QuranPlaybackSession")
        mediaSession.isActive = true
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePause()
            ACTION_NEXT -> playNext()
            ACTION_PREV -> playPrevious()
            ACTION_STOP -> stopPlayback()
        }
        return START_NOT_STICKY
    }

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
                updateSessionState(true)
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
            updateSessionState(false)
            currentSurah?.let { updateNotification(it, false) }
        } else {
            mp.start()
            onStateChanged?.invoke(true, currentSurah)
            updateSessionState(true)
            currentSurah?.let { updateNotification(it, true) }
        }
    }

    fun playNext() {
        val current = currentSurah ?: return
        val idx = QuranData.surahs.indexOf(current)
        var i = idx + 1
        while (i < QuranData.surahs.size) {
            val candidate = QuranData.surahs[i]
            if (StorageHelper.isDownloaded(this, candidate)) {
                play(candidate)
                return
            }
            i++
        }
    }

    fun playPrevious() {
        val current = currentSurah ?: return
        val idx = QuranData.surahs.indexOf(current)
        var i = idx - 1
        while (i >= 0) {
            val candidate = QuranData.surahs[i]
            if (StorageHelper.isDownloaded(this, candidate)) {
                play(candidate)
                return
            }
            i--
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
        onStateChanged?.invoke(false, null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateSessionState(playing: Boolean) {
        val state = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(
                if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                currentPosition().toLong(),
                1f
            )
            .build()
        mediaSession.setPlaybackState(state)
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

    private fun actionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotification(surah: Surah, playing: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val contentPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevAction = NotificationCompat.Action(
            android.R.drawable.ic_media_previous, "السابق",
            actionPendingIntent(ACTION_PREV, 1)
        )
        val playPauseAction = NotificationCompat.Action(
            if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (playing) "إيقاف مؤقت" else "تشغيل",
            actionPendingIntent(ACTION_PLAY_PAUSE, 2)
        )
        val nextAction = NotificationCompat.Action(
            android.R.drawable.ic_media_next, "التالي",
            actionPendingIntent(ACTION_NEXT, 3)
        )
        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_delete, "إيقاف",
            actionPendingIntent(ACTION_STOP, 4)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("سورة ${surah.arabicName}")
            .setContentText("محمد صديق المنشاوي")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentPending)
            .setOngoing(playing)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(stopAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun updateNotification(surah: Surah, playing: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotification(surah, playing))
    }

    override fun onDestroy() {
        mediaSession.release()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}

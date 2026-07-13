package com.minshawi.quran

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import java.io.File

/**
 * يدير عمليات تحميل ملفات الصوت عبر خدمة النظام DownloadManager،
 * ثم ينقل الملف النهائي إلى مجلد التطبيق الخاص عند اكتمال التحميل.
 */
class DownloadHelper(private val context: Context) {

    interface Listener {
        fun onDownloadComplete(surah: Surah, success: Boolean)
    }

    private val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val activeDownloads = HashMap<Long, Surah>()
    private var listener: Listener? = null
    private var receiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: return
            val surah = activeDownloads.remove(id) ?: return
            val success = finalizeDownload(id, surah)
            listener?.onDownloadComplete(surah, success)
        }
    }

    fun setListener(l: Listener) {
        listener = l
        if (!receiverRegistered) {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            receiverRegistered = true
        }
    }

    fun unregister() {
        if (receiverRegistered) {
            context.unregisterReceiver(receiver)
            receiverRegistered = false
        }
    }

    fun download(surah: Surah) {
        if (StorageHelper.isDownloaded(context, surah)) return

        val request = DownloadManager.Request(Uri.parse(surah.downloadUrl))
            .setTitle("سورة ${surah.arabicName}")
            .setDescription("جارِ التحميل - المنشاوي")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, surah.fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val id = manager.enqueue(request)
        activeDownloads[id] = surah
    }

    private fun finalizeDownload(downloadId: Long, surah: Surah): Boolean {
        return try {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = manager.query(query)
            var ok = false
            if (cursor.moveToFirst()) {
                val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = cursor.getInt(statusIdx)
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    val tempFile = File(
                        context.getExternalFilesDir(null),
                        surah.fileName
                    )
                    val finalFile = StorageHelper.localFile(context, surah)
                    if (tempFile.exists()) {
                        tempFile.copyTo(finalFile, overwrite = true)
                        tempFile.delete()
                        ok = true
                    }
                }
            }
            cursor.close()
            ok
        } catch (e: Exception) {
            false
        }
    }
}

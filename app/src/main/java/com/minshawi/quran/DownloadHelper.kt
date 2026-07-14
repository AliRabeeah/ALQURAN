package com.minshawi.quran

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class DownloadHelper(private val context: Context) {

    interface Listener {
        fun onDownloadComplete(surah: Surah, success: Boolean, error: String? = null)
    }

    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var listener: Listener? = null

    fun setListener(l: Listener) {
        listener = l
    }

    fun unregister() {
        // لا حاجة لأي تنظيف بعد الآن
    }

    fun download(surah: Surah) {
        if (StorageHelper.isDownloaded(context, surah)) return

        executor.execute {
            var success = false
            var errorMsg: String? = null
            var connection: HttpURLConnection? = null
            try {
                val url = URL(surah.downloadUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 20000
                connection.readTimeout = 30000
                connection.instanceFollowRedirects = true
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    errorMsg = "خطأ من الخادم: $responseCode"
                } else {
                    val tempFile = File(StorageHelper.quranDir(context), surah.fileName + ".part")
                    connection.inputStream.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val finalFile = StorageHelper.localFile(context, surah)
                    if (tempFile.length() > 0) {
                        tempFile.renameTo(finalFile)
                        success = true
                    } else {
                        errorMsg = "الملف الذي تم تحميله فارغ"
                        tempFile.delete()
                    }
                }
            } catch (e: Exception) {
                errorMsg = e.message ?: e.javaClass.simpleName
            } finally {
                connection?.disconnect()
            }

            mainHandler.post {
                listener?.onDownloadComplete(surah, success, errorMsg)
            }
        }
    }
    private fun favPrefs(context: Context) =
        context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    fun isFavorite(context: Context, surah: Surah): Boolean =
        favPrefs(context).getStringSet("favs", emptySet())?.contains(surah.number.toString()) == true

    fun toggleFavorite(context: Context, surah: Surah) {
        val prefs = favPrefs(context)
        val current = HashSet(prefs.getStringSet("favs", emptySet()) ?: emptySet())
        val key = surah.number.toString()
        if (current.contains(key)) current.remove(key) else current.add(key)
        prefs.edit().putStringSet("favs", current).apply()
    }
}

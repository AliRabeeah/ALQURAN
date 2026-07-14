package com.minshawi.quran

import android.content.Context
import java.io.File

object StorageHelper {

    /** مجلد تخزين ملفات القرآن داخل تخزين التطبيق الخاص (لا يحتاج صلاحية تخزين خارجي) */
    fun quranDir(context: Context): File {
        val dir = File(context.filesDir, "minshawi")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun localFile(context: Context, surah: Surah): File =
        File(quranDir(context), surah.fileName)

    fun isDownloaded(context: Context, surah: Surah): Boolean {
        val f = localFile(context, surah)
        return f.exists() && f.length() > 0
    }

    fun deleteAll(context: Context) {
        quranDir(context).listFiles()?.forEach { it.delete() }
    }

    fun totalDownloadedCount(context: Context): Int =
        QuranData.surahs.count { isDownloaded(context, it) }

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

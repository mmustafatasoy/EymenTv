package com.eymen.uydukontrol

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File

/** Checks GitHub Releases for a newer EYMEN TV and opens the system installer for it. */
object Updater {
    private const val LATEST = "https://api.github.com/repos/mmustafatasoy/EymenTv/releases/latest"

    class Info(val version: String, val url: String, val notes: List<String>)

    fun check(): Info? {
        val json = JSONObject(Http.text(LATEST))
        val tag = json.optString("tag_name", "")
        val version = Regex("(\\d+(?:\\.\\d+)*)").findAll(tag).lastOrNull()?.value ?: return null
        val assets = json.optJSONArray("assets") ?: return null
        var url: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val name = a.optString("name", "")
            if (name.endsWith(".apk", ignoreCase = true)) {
                url = a.optString("browser_download_url", "")
                if (name.equals("EYMEN-TV.apk", ignoreCase = true)) break
            }
        }
        val u = url ?: return null
        if (u.isEmpty()) return null
        return Info(version, u, notes(json.optString("body", "")))
    }

    /** The first bullet lines of the release notes, shown on the update notice. */
    private fun notes(body: String): List<String> =
        body.lines().map { it.trim() }.filter { it.startsWith("- ") }.map { it.removePrefix("- ").trim() }.take(3)

    fun newer(candidate: String, installed: String): Boolean {
        val a = candidate.split('.').map { it.toIntOrNull() ?: 0 }
        val b = installed.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** Returns a message for the user when the installer could not be opened yet. */
    fun install(activity: Activity, file: File): String? {
        if (!activity.packageManager.canRequestPackageInstalls()) {
            try {
                activity.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.packageName))
                )
            } catch (e: Exception) {
            }
            return "EYMEN TV için bilinmeyen kaynak iznini aç, sonra tekrar dene"
        }
        return try {
            val uri = FileProvider.getUriForFile(activity, activity.packageName + ".files", file)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
            null
        } catch (e: Exception) {
            "Kurulum ekranı açılamadı: ${e.message}"
        }
    }
}

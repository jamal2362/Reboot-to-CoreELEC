package com.jamal2367.coreelec

import android.content.Context
import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONException
import org.json.JSONObject
import androidx.core.content.edit

/** A release that is newer than the installed app. [apkUrl] is null if the release has no APK attached. */
data class UpdateInfo(val version: String, val apkUrl: String?)

/** Looks up the latest release on GitHub and remembers it for the next app start. */
class UpdateChecker(context: Context) {

    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    private val installedVersion: String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

    /** The update found by an earlier check, available without any network access. */
    fun cachedUpdate(): UpdateInfo? {
        val version = prefs.getString(KEY_LATEST, null) ?: return null
        if (!isNewer(version, installedVersion)) return null
        return UpdateInfo(version, prefs.getString(KEY_APK_URL, null))
    }

    /** Asks GitHub for the latest release. Blocks; returns it if it is newer than the installed version. */
    fun fetchUpdate(): UpdateInfo? {
        val latest = fetchLatestRelease() ?: return cachedUpdate()
        prefs.edit {
            putString(KEY_LATEST, latest.version)
                .putString(KEY_APK_URL, latest.apkUrl)
        }
        return latest.takeIf { isNewer(it.version, installedVersion) }
    }

    private fun fetchLatestRelease(): UpdateInfo? {
        val connection = URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val assets = release.optJSONArray("assets")
            var apkUrl: String? = null
            for (i in 0 until (assets?.length() ?: 0)) {
                val asset = assets!!.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }
            UpdateInfo(release.getString("tag_name").removePrefix("v"), apkUrl)
        } catch (e: IOException) {
            Log.w(TAG, "Update check failed", e)
            null
        } catch (e: JSONException) {
            Log.w(TAG, "Unexpected release data", e)
            null
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val RELEASES_PAGE = "github.com/jamal2362/Reboot-to-CoreELEC/releases"
        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/jamal2362/Reboot-to-CoreELEC/releases/latest"
        private const val KEY_LATEST = "latest_version"
        private const val KEY_APK_URL = "latest_apk_url"
        private const val TIMEOUT_MS = 5_000
        private const val TAG = "UpdateChecker"

        /** Compares dotted version numbers such as "5.0" and "5.10" part by part. */
        fun isNewer(candidate: String, installed: String): Boolean {
            val a = candidate.split('.').map { it.toIntOrNull() ?: 0 }
            val b = installed.split('.').map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(a.size, b.size)) {
                val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
                if (diff != 0) return diff > 0
            }
            return false
        }
    }
}

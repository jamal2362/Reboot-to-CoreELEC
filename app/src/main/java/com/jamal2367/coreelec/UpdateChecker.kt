package com.jamal2367.coreelec

import android.content.Context
import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONException
import org.json.JSONObject

/** Looks up the latest release on GitHub and remembers it for the next app start. */
class UpdateChecker(context: Context) {

    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    private val installedVersion: String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

    /** The newer version found by an earlier check, available without any network access. */
    fun cachedUpdate(): String? = prefs.getString(KEY_LATEST, null)?.takeIf { isNewer(it, installedVersion) }

    /** Asks GitHub for the latest release. Blocks; returns the version if it is newer than the installed one. */
    fun fetchUpdate(): String? {
        val latest = fetchLatestVersion() ?: return cachedUpdate()
        prefs.edit().putString(KEY_LATEST, latest).apply()
        return latest.takeIf { isNewer(it, installedVersion) }
    }

    private fun fetchLatestVersion(): String? {
        val connection = URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body).getString("tag_name").removePrefix("v")
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

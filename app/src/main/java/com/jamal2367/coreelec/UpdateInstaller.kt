package com.jamal2367.coreelec

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateUiState {
    data object Idle : UpdateUiState

    /** [percent] is null while the size of the download is unknown. */
    data class Downloading(val percent: Int?) : UpdateUiState
    data object Installing : UpdateUiState
    data class Failed(val message: String?) : UpdateUiState

    val isBusy: Boolean
        get() = this is Downloading || this is Installing
}

/** Progress of the running update; written by [UpdateInstaller] and [InstallResultReceiver]. */
object UpdateStatus {
    var state: UpdateUiState by mutableStateOf(UpdateUiState.Idle)
}

/** Downloads a release APK straight into a package installer session and commits it. */
class UpdateInstaller(private val context: Context) {

    /** Blocks until the APK is handed to the system; the result arrives in [InstallResultReceiver]. */
    @Throws(IOException::class)
    fun downloadAndInstall(apkUrl: String) {
        UpdateStatus.state = UpdateUiState.Downloading(null)

        val connection = URL(apkUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${connection.responseCode}")
            }
            val length = connection.contentLengthLong

            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            params.setAppPackageName(context.packageName)
            if (length > 0) params.setSize(length)

            val sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)
            try {
                session.openWrite("update.apk", 0, if (length > 0) length else -1).use { output ->
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var written = 0L
                        var lastPercent = -1
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            written += read
                            if (length > 0) {
                                val percent = (written * 100 / length).toInt()
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    UpdateStatus.state = UpdateUiState.Downloading(percent)
                                }
                            }
                        }
                    }
                    session.fsync(output)
                }

                UpdateStatus.state = UpdateUiState.Installing
                session.commit(resultIntent(sessionId).intentSender)
            } catch (e: Exception) {
                session.abandon()
                throw e
            } finally {
                session.close()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun resultIntent(sessionId: Int): PendingIntent {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        // The system fills in the result, so the intent has to be mutable.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags = flags or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(
            context,
            sessionId,
            Intent(context, InstallResultReceiver::class.java),
            flags,
        )
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 30_000
    }
}

/** Receives the package installer's answer: asks the user to confirm, or reports the failure. */
class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirm != null) {
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            PackageInstaller.STATUS_SUCCESS, PackageInstaller.STATUS_FAILURE_ABORTED -> {
                UpdateStatus.state = UpdateUiState.Idle
            }
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.w("InstallResultReceiver", "Install failed ($status): $message")
                UpdateStatus.state = UpdateUiState.Failed(message)
            }
        }
    }
}

package com.jamal2367.coreelec

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.text.Html.FROM_HTML_MODE_LEGACY
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {

    private enum class RebootMode(val command: String, val labelRes: Int, val infoRes: Int) {
        NORMAL("shell:reboot", R.string.reboot_to_coreelec, R.string.reboot_to_coreelec_info),
        UPDATE("shell:reboot update", R.string.first_reboot_to_coreelec, R.string.first_reboot_to_coreelec_info)
    }

    private class NotAuthorizedException : IOException()

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private lateinit var tvStatus: TextView
    private lateinit var rebootButtons: List<Button>

    @Volatile
    private var socket: Socket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isAdbEnabled()) {
            Toast.makeText(this, R.string.enable_usb_debugging_first, Toast.LENGTH_LONG).show()
            openDeveloperSettings()
            finish()
            return
        }

        setContentView(R.layout.activity_coreelec)
        tvStatus = findViewById(R.id.status)

        val btnReboot = findViewById<Button>(R.id.btnReboot)
        val btnRebootUpdate = findViewById<Button>(R.id.btnRebootUpdate)
        rebootButtons = listOf(btnReboot, btnRebootUpdate)

        btnReboot.setOnClickListener { reboot(RebootMode.NORMAL) }
        btnRebootUpdate.setOnClickListener { reboot(RebootMode.UPDATE) }

        setUpInfoButton(findViewById(R.id.btnRebootInfo), RebootMode.NORMAL)
        setUpInfoButton(findViewById(R.id.btnRebootUpdateInfo), RebootMode.UPDATE)
    }

    override fun onDestroy() {
        executor.shutdownNow()
        closeQuietly(socket)
        super.onDestroy()
    }

    private fun setUpInfoButton(button: ImageButton, mode: RebootMode) {
        button.contentDescription = getString(R.string.information) + ": " + getString(mode.labelRes)
        button.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.information)
                .setMessage(Html.fromHtml(getString(mode.infoRes), FROM_HTML_MODE_LEGACY))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun reboot(mode: RebootMode) {
        setBusy(true)
        showStatus(getString(R.string.status_connecting))

        executor.execute {
            val error = try {
                sendRebootCommand(mode)
                null
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return@execute
            } catch (e: NotAuthorizedException) {
                getString(R.string.error_not_authorized)
            } catch (e: ConnectException) {
                getString(R.string.error_adb_unreachable, ADB_PORT)
            } catch (e: SocketTimeoutException) {
                getString(R.string.error_adb_unreachable, ADB_PORT)
            } catch (e: Exception) {
                Log.e(TAG, "Reboot failed", e)
                getString(R.string.error_generic, e.localizedMessage ?: e.javaClass.simpleName)
            }

            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                // Without an error the device is going down; keep the last status visible.
                if (error != null) {
                    showStatus(error)
                    setBusy(false)
                }
            }
        }
    }

    /** Runs on the worker thread. Throws on any failure. */
    private fun sendRebootCommand(mode: RebootMode) {
        val crypto = loadOrCreateCrypto()

        Socket().use { socket ->
            this.socket = socket
            socket.connect(InetSocketAddress(ADB_HOST, ADB_PORT), CONNECT_TIMEOUT_MS)

            AdbConnection.create(socket, crypto).use { connection ->
                // Blocks while the user confirms the "Allow USB debugging?" dialog.
                if (!connection.connect(AUTH_TIMEOUT_S, TimeUnit.SECONDS, false)) {
                    throw NotAuthorizedException()
                }

                runOnUiThread { showStatus(getString(mode.labelRes) + "…") }
                // Give the user a moment to read the status before the screen goes dark.
                Thread.sleep(STATUS_DELAY_MS)

                val stream = connection.open(mode.command)
                Log.d(TAG, "Sent \"${mode.command}\"")

                // Keep the shell alive until the device goes down or the command returns.
                try {
                    while (!stream.isClosed) stream.read()
                } catch (e: IOException) {
                    // Expected: the connection drops when the device reboots.
                }
            }
        }

        // The system kills the app during shutdown. Still running after the grace period
        // means the command returned without rebooting.
        Thread.sleep(REBOOT_GRACE_MS)
        throw IOException(getString(R.string.error_no_reboot))
    }

    private fun loadOrCreateCrypto(): AdbCrypto {
        val publicKey = File(filesDir, PUBLIC_KEY_NAME)
        val privateKey = File(filesDir, PRIVATE_KEY_NAME)
        val base64 = AdbBase64 { data -> Base64.encodeToString(data, Base64.NO_WRAP) }

        if (publicKey.exists() && privateKey.exists()) {
            try {
                return AdbCrypto.loadAdbKeyPair(base64, privateKey, publicKey)
            } catch (e: Exception) {
                Log.w(TAG, "Stored ADB key pair is unreadable, generating a new one", e)
            }
        }

        return AdbCrypto.generateAdbKeyPair(base64).also {
            it.saveAdbKeyPair(privateKey, publicKey)
        }
    }

    private fun setBusy(busy: Boolean) {
        rebootButtons.forEach { it.isEnabled = !busy }
        if (!busy) rebootButtons.first().requestFocus()
    }

    private fun showStatus(text: String) {
        tvStatus.text = text
    }

    private fun closeQuietly(socket: Socket?) {
        try {
            socket?.close()
        } catch (e: IOException) {
            // Nothing left to do.
        }
    }

    private fun openDeveloperSettings() {
        for (action in listOf(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, Settings.ACTION_SETTINGS)) {
            try {
                startActivity(Intent(action))
                return
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "No activity for $action")
            }
        }
    }

    private fun isAdbEnabled(): Boolean {
        return Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
    }

    private companion object {
        const val TAG = "MainActivity"
        const val ADB_HOST = "127.0.0.1"
        const val ADB_PORT = 5555
        const val CONNECT_TIMEOUT_MS = 5_000
        const val AUTH_TIMEOUT_S = 60L
        const val STATUS_DELAY_MS = 1_500L
        const val REBOOT_GRACE_MS = 10_000L
        const val PUBLIC_KEY_NAME = "public.key"
        const val PRIVATE_KEY_NAME = "private.key"
    }
}

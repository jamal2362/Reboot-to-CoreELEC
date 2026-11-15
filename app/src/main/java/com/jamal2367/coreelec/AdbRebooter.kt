package com.jamal2367.coreelec

import android.util.Base64
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

enum class RebootMode(
    val command: String,
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    NORMAL(
        "shell:reboot",
        R.string.reboot_to_coreelec,
        R.string.reboot_to_coreelec_desc,
        R.drawable.ic_restart_24,
    ),
    UPDATE(
        "shell:reboot update",
        R.string.first_reboot_to_coreelec,
        R.string.first_reboot_to_coreelec_desc,
        R.drawable.ic_first_boot_24,
    ),
}

/** The user did not confirm the "Allow USB debugging?" prompt in time. */
class NotAuthorizedException : IOException()

/** The reboot command returned, but the device kept running. */
class NoRebootException : IOException()

/**
 * Sends reboot commands to the device's own ADB daemon over the loopback interface.
 * All calls block and must run off the main thread.
 */
class AdbRebooter(private val keyDir: File) {

    @Volatile
    private var socket: Socket? = null

    /**
     * Connects, authorizes and sends [mode]'s command. [onAuthorized] is called once the
     * connection is established, shortly before the command is sent.
     *
     * Only returns by throwing: on success the system kills the app during shutdown.
     */
    @Throws(IOException::class, InterruptedException::class)
    fun reboot(mode: RebootMode, onAuthorized: () -> Unit): Nothing {
        val crypto = loadOrCreateCrypto()

        Socket().use { socket ->
            this.socket = socket
            socket.connect(InetSocketAddress(ADB_HOST, ADB_PORT), CONNECT_TIMEOUT_MS)

            AdbConnection.create(socket, crypto).use { connection ->
                // Blocks while the user confirms the "Allow USB debugging?" prompt.
                if (!connection.connect(AUTH_TIMEOUT_S, TimeUnit.SECONDS, false)) {
                    throw NotAuthorizedException()
                }

                onAuthorized()
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

        // Still running after the grace period means the command did not reboot the device.
        Thread.sleep(REBOOT_GRACE_MS)
        throw NoRebootException()
    }

    /** Aborts a running [reboot] call. */
    fun close() {
        try {
            socket?.close()
        } catch (e: IOException) {
            // Nothing left to do.
        }
    }

    private fun loadOrCreateCrypto(): AdbCrypto {
        val publicKey = File(keyDir, PUBLIC_KEY_NAME)
        val privateKey = File(keyDir, PRIVATE_KEY_NAME)
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

    companion object {
        const val ADB_PORT = 5555
        private const val TAG = "AdbRebooter"
        private const val ADB_HOST = "127.0.0.1"
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val AUTH_TIMEOUT_S = 60L
        private const val STATUS_DELAY_MS = 1_500L
        private const val REBOOT_GRACE_MS = 10_000L
        private const val PUBLIC_KEY_NAME = "public.key"
        private const val PRIVATE_KEY_NAME = "private.key"
    }
}

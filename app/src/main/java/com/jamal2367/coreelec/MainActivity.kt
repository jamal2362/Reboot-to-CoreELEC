package com.jamal2367.coreelec

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.DhcpInfo
import android.net.wifi.WifiManager
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
import com.tananaev.adblib.AdbStream
import java.io.File
import java.lang.ref.WeakReference
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketException
import java.nio.ByteOrder

class MainActivity : Activity() {

    private var tvIP: TextView? = null
    private var connection: AdbConnection? = null
    private var stream: AdbStream? = null
    private var myAsyncTask: MyAsyncTask? = null
    private val publicKeyName: String = "public.key"
    private val privateKeyName: String = "private.key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coreelec)

        if (!isUsbDebuggingEnabled()) {
            Toast.makeText(this, getString(R.string.enable_usb_debugging_first), Toast.LENGTH_LONG).show()
            openDeveloperSettings()
            finish()
            return
        }

        tvIP = findViewById(R.id.ip)
        tvIP?.text = getGateWayIp(this)

        findViewById<Button>(R.id.btnReboot).setOnClickListener {
            onKeyCE(10)
        }

        findViewById<Button>(R.id.btnRebootUpdate).setOnClickListener {
            onKeyCE(20)
        }

        findViewById<ImageButton>(R.id.btnRebootInfo).setOnClickListener {
            val builder = AlertDialog.Builder(this)

            builder.setTitle(R.string.information)
            builder.setMessage(Html.fromHtml(getString(R.string.reboot_to_coreelec_info), FROM_HTML_MODE_LEGACY)
            )

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        findViewById<ImageButton>(R.id.btnRebootUpdateInfo).setOnClickListener {
            val builder = AlertDialog.Builder(this)

            builder.setTitle(R.string.information)
            builder.setMessage(Html.fromHtml(getString(R.string.first_reboot_to_coreelec_info), FROM_HTML_MODE_LEGACY))

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    private fun onKeyCE(case: Int) {
        connection = null
        stream = null

        myAsyncTask?.cancel()
        myAsyncTask = MyAsyncTask(this)
        myAsyncTask?.execute(case)
    }

    fun adbCommander(ip: String?, case: Int) {
        val socket = Socket(ip, 5555)
        val crypto = readCryptoConfig(filesDir) ?: writeNewCryptoConfig(filesDir)

        try {
            if (stream == null || connection == null) {
                connection = AdbConnection.create(socket, crypto)
                connection?.connect()
            }

            when (case) {
                10 -> {
                    runOnUiThread {
                        Toast.makeText(this, getString(R.string.reboot_to_coreelec) + "...", Toast.LENGTH_SHORT).show()
                    }
                    Thread.sleep(1500)
                    stream = connection?.open("shell:reboot")
                    Log.d("MainActivity", "Case 10 executed")
                }
                20 -> {
                    runOnUiThread {
                        Toast.makeText(this, getString(R.string.first_reboot_to_coreelec) + "...", Toast.LENGTH_SHORT).show()
                    }
                    Thread.sleep(1500)
                    stream = connection?.open("shell:reboot update")
                    Log.d("MainActivity", "Case 20 executed")
                }
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
            Thread.currentThread().interrupt()
        }
    }

    private fun readCryptoConfig(dataDir: File?): AdbCrypto? {
        val pubKey = File(dataDir, publicKeyName)
        val privKey = File(dataDir, privateKeyName)

        var crypto: AdbCrypto? = null
        if (pubKey.exists() && privKey.exists()) {
            crypto = try {
                AdbCrypto.loadAdbKeyPair(AndroidBase64(), privKey, pubKey)
            } catch (e: Exception) {
                null
            }
        }

        return crypto
    }

    private fun writeNewCryptoConfig(dataDir: File?): AdbCrypto? {
        val pubKey = File(dataDir, publicKeyName)
        val privKey = File(dataDir, privateKeyName)

        var crypto: AdbCrypto?

        try {
            crypto = AdbCrypto.generateAdbKeyPair(AndroidBase64())
            crypto.saveAdbKeyPair(privKey, pubKey)
        } catch (e: Exception) {
            crypto = null
        }

        return crypto
    }

    private fun openDeveloperSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS)
        startActivity(intent)
    }

    private fun isUsbDebuggingEnabled(): Boolean {
        return Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
    }

    private fun getWifiManager(context: Context): WifiManager {
        return context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private fun getGateWayIp(context: Context): String? {
        val int2String: String?
        val dhcpInfo = getDhcpInfo(context)!!
        int2String = if (dhcpInfo.ipAddress == 0) {
            localIp
        } else {
            int2String(dhcpInfo.ipAddress)
        }
        return int2String
    }

    private val localIp: String?
        get() {
            var str: String? = null
            try {
                val networkInterfaces = NetworkInterface.getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val inetAddresses = networkInterfaces.nextElement().inetAddresses
                    while (inetAddresses.hasMoreElements()) {
                        val nextElement = inetAddresses.nextElement()
                        if (!nextElement.isLoopbackAddress && nextElement is Inet4Address) {
                            str = nextElement.getHostAddress()
                        }
                    }
                }
            } catch (e: SocketException) {
                e.printStackTrace()
            }
            return str
        }


    private fun int2String(i: Int): String {
        return if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            (i and 255).toString() + "." + (i shr 8 and 255) + "." + (i shr 16 and 255) + "." + (i shr 24 and 255)
        } else (i shr 24 and 255).toString() + "." + (i shr 16 and 255) + "." + (i shr 8 and 255) + "." + (i and 255)
    }

    @Suppress("DEPRECATION")
    private fun getDhcpInfo(context: Context): DhcpInfo? {
        val wifiManager = getWifiManager(context)
        return wifiManager.dhcpInfo
    }

    class MyAsyncTask internal constructor(context: MainActivity) {
        private val activityReference: WeakReference<MainActivity> = WeakReference(context)
        private var thread: Thread? = null

        fun execute(case: Int) {
            thread = Thread {
                val activity = activityReference.get()
                activity?.adbCommander(activity.tvIP?.text.toString(), case)

                if (Thread.interrupted()) {
                    return@Thread
                }
            }
            thread?.start()
        }

        fun cancel() {
            thread?.interrupt()
        }
    }

    class AndroidBase64 : AdbBase64 {
        override fun encodeToString(bArr: ByteArray): String {
            return Base64.encodeToString(bArr, Base64.NO_WRAP)
        }
    }
}

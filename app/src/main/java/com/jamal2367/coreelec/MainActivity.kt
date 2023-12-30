package com.jamal2367.coreelec

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiInfo
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
import java.lang.ref.WeakReference
import java.net.Socket

class MainActivity : Activity() {

    private var tvIP: TextView? = null
    private var connection: AdbConnection? = null
    private var stream: AdbStream? = null
    private var myAsyncTask: MyAsyncTask? = null

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
        tvIP?.text = getIPAddress(this)

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
        Toast.makeText(this, getString(R.string.allow_usb_debugging_dialog), Toast.LENGTH_SHORT).show()

        connection = null
        stream = null

        myAsyncTask?.cancel()
        myAsyncTask = MyAsyncTask(this)
        myAsyncTask?.execute(case)
    }

    fun adbCommander(ip: String?, case: Int) {
        val socket = Socket(ip, 5555)
        val generateAdbKeyPair = AdbCrypto.generateAdbKeyPair(AndroidBase64())

        try {
            if (stream == null || connection == null) {
                connection = AdbConnection.create(socket, generateAdbKeyPair)
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

    private fun openDeveloperSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS)
        startActivity(intent)
    }

    private fun isUsbDebuggingEnabled(): Boolean {
        return Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
    }

    @Suppress("DEPRECATION")
    private fun getIPAddress(context: Context): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo = wifiManager.connectionInfo
        val ip = wifiInfo.ipAddress

        return String.format(
            "%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff
        )
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

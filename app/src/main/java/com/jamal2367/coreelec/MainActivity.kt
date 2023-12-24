package com.jamal2367.coreelec

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import com.tananaev.adblib.AdbStream
import com.tbruyelle.rxpermissions3.RxPermissions
import com.jamal2367.coreelec.utils.AndroidBase64
import com.jamal2367.coreelec.utils.NetworkUtil

import java.lang.ref.WeakReference
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private var tvIP: TextView? = null
    private var connection: AdbConnection? = null
    private var stream: AdbStream? = null
    private var myAsyncTask: MyAsyncTask? = null


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RxPermissions(this).setLogging(true)
        setContentView(R.layout.activity_coreelec)

        tvIP = findViewById(R.id.ip) as? TextView

        if (tvIP != null) {
            tvIP?.text = NetworkUtil.getGateWayIp(this)
        }
        onKeyCE()
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
    }

    private fun onKeyCE() {
        if (!isUsbDebuggingEnabled()) {
            Toast.makeText(this, getString(R.string.enable_usb_debugging_first), Toast.LENGTH_LONG).show()
            openDeveloperSettings()
            finish()
            return
        }

        if (connection == null || stream == null) {
            Toast.makeText(this, getString(R.string.allow_usb_debugging_dialog), Toast.LENGTH_SHORT).show()
            myAsyncTask?.cancel()
            myAsyncTask = MyAsyncTask(this)
            myAsyncTask?.execute()
        }

    }

    fun adbCommander(str: String?, i: Int) {
        val socket = Socket(str, 5555)
        val generateAdbKeyPair = AdbCrypto.generateAdbKeyPair(AndroidBase64())

        if (stream == null || connection == null) {
            val create = AdbConnection.create(socket, generateAdbKeyPair)
            connection = create
            create.connect()
        }

        when (i) {
            10 -> {
                runOnUiThread {
                    Toast.makeText(this@MainActivity,
                        getString(R.string.rebooting_to_coreelec), Toast.LENGTH_SHORT).show()
                }
                stream = connection?.open("shell:reboot update")
            }
            else -> return
        }
    }

    private fun openDeveloperSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS)
        startActivity(intent)
    }

    private fun isUsbDebuggingEnabled(): Boolean {
        return Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
    }

    class MyAsyncTask internal constructor(context: MainActivity) {
        private val activityReference: WeakReference<MainActivity> = WeakReference(context)
        private var thread: Thread? = null

        fun execute() {
            thread = Thread(Runnable {
                val activity = activityReference.get()

                if (activity?.stream == null || activity.tvIP?.text.toString() == 10.toString()) {
                    activity?.adbCommander(activity.tvIP?.text.toString(), 10)
                }

                if (Thread.interrupted()) {
                    return@Runnable
                }
            })
            thread?.start()
        }

        fun cancel() {
            thread?.interrupt()
        }
    }

}

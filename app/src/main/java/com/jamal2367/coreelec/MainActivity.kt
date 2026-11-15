package com.jamal2367.coreelec

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.jamal2367.coreelec.ui.AppTheme
import com.jamal2367.coreelec.ui.RebootScreen

class MainActivity : ComponentActivity() {

    private val viewModel: RebootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (!isAdbEnabled()) {
            Toast.makeText(this, R.string.enable_usb_debugging_first, Toast.LENGTH_LONG).show()
            openDeveloperSettings()
            finish()
            return
        }

        setContent {
            AppTheme {
                RebootScreen(
                    uiState = viewModel.uiState,
                    onReboot = viewModel::reboot,
                )
            }
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
    }
}

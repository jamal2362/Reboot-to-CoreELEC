package com.jamal2367.coreelec

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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

        setContent {
            AppTheme {
                RebootScreen(
                    uiState = viewModel.uiState,
                    adbStatus = viewModel.adbStatus,
                    deviceIp = viewModel.deviceIp,
                    availableUpdate = viewModel.availableUpdate,
                    updateState = viewModel.updateState,
                    onInstallUpdate = viewModel::installUpdate,
                    onReboot = viewModel::reboot,
                    developerOptionsEnabled = viewModel.developerOptionsEnabled,
                    onOpenDeveloperSettings = ::openDeveloperSettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may have changed the debugging settings in the meantime.
        viewModel.developerOptionsEnabled =
            Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        viewModel.refreshAdbStatus()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        viewModel.onWindowFocusChanged(hasFocus)
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

    private companion object {
        const val TAG = "MainActivity"
    }
}

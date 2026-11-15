package com.jamal2367.coreelec

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

sealed interface RebootUiState {
    data object Idle : RebootUiState
    data object Connecting : RebootUiState
    data class Rebooting(val mode: RebootMode) : RebootUiState
    data class Failed(@param:StringRes val messageRes: Int, val messageArg: Any? = null) : RebootUiState

    val isBusy: Boolean
        get() = this is Connecting || this is Rebooting
}

enum class AdbStatus {
    CHECKING,

    /** Reachable and this app's key is trusted: rebooting works without a prompt. */
    READY,

    /** Reachable, but the "Allow USB debugging?" prompt still has to be confirmed. */
    NEEDS_AUTH,

    UNREACHABLE,
}

class RebootViewModel(application: Application) : AndroidViewModel(application) {

    private val rebooter = AdbRebooter(application.filesDir)

    var uiState: RebootUiState by mutableStateOf(RebootUiState.Idle)
        private set

    var adbStatus: AdbStatus by mutableStateOf(AdbStatus.CHECKING)
        private set

    var deviceIp: String? by mutableStateOf(null)
        private set

    /** Whether the developer options are unlocked on the device; set by the activity. */
    var developerOptionsEnabled: Boolean by mutableStateOf(false)

    private val updateChecker = UpdateChecker(application)

    /** Whether the window lost focus (to the "Allow USB debugging?" prompt) while connecting. */
    private var promptShown = false

    private var promptDismissedJob: Job? = null

    private val updateInstaller = UpdateInstaller(application)

    /** A newer release, shown at once from the last check and then refreshed. */
    var availableUpdate: UpdateInfo? by mutableStateOf(updateChecker.cachedUpdate())
        private set

    val updateState: UpdateUiState
        get() = UpdateStatus.state

    /** Downloads the update and hands it to the system's package installer. */
    fun installUpdate() {
        val apkUrl = availableUpdate?.apkUrl ?: return
        if (UpdateStatus.state.isBusy) return
        UpdateStatus.state = UpdateUiState.Downloading(null)

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { updateInstaller.downloadAndInstall(apkUrl) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Update failed", e)
                UpdateStatus.state = UpdateUiState.Failed(e.localizedMessage ?: e.javaClass.simpleName)
            }
        }
    }

    init {
        viewModelScope.launch {
            availableUpdate = withContext(Dispatchers.IO) { updateChecker.fetchUpdate() }
        }
    }

    /** Checks whether the local ADB daemon is reachable and looks up the device's IP address. */
    fun refreshAdbStatus() {
        viewModelScope.launch {
            val (ip, status) = withContext(Dispatchers.IO) {
                AdbRebooter.localIpAddress() to rebooter.probe()
            }
            deviceIp = ip
            // A reboot that started meanwhile knows better (see [reboot]).
            if (!uiState.isBusy) adbStatus = status
        }
    }

    /**
     * The "Allow USB debugging?" prompt is a system window that takes focus from the app.
     * Once focus is back and the connection still is not authorized, the prompt was denied.
     */
    fun onWindowFocusChanged(hasFocus: Boolean) {
        if (uiState != RebootUiState.Connecting) return
        if (!hasFocus) {
            promptShown = true
            promptDismissedJob?.cancel()
        } else if (promptShown) {
            promptShown = false
            promptDismissedJob = viewModelScope.launch {
                // After "Allow" adbd answers within moments; give it a little slack.
                delay(PROMPT_ANSWER_GRACE_MS)
                if (uiState == RebootUiState.Connecting) {
                    withContext(Dispatchers.IO) { rebooter.abortAuthorization() }
                }
            }
        }
    }

    fun reboot(mode: RebootMode) {
        if (uiState.isBusy) return
        uiState = RebootUiState.Connecting
        promptShown = false
        promptDismissedJob?.cancel()

        viewModelScope.launch {
            uiState = try {
                runInterruptible(Dispatchers.IO) {
                    rebooter.reboot(mode) {
                        adbStatus = AdbStatus.READY
                        uiState = RebootUiState.Rebooting(mode)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: NotAuthorizedException) {
                adbStatus = AdbStatus.NEEDS_AUTH
                RebootUiState.Failed(R.string.error_not_authorized)
            } catch (e: NoRebootException) {
                RebootUiState.Failed(R.string.error_no_reboot)
            } catch (e: ConnectException) {
                RebootUiState.Failed(R.string.error_adb_unreachable, AdbRebooter.ADB_PORT)
            } catch (e: SocketTimeoutException) {
                RebootUiState.Failed(R.string.error_adb_unreachable, AdbRebooter.ADB_PORT)
            } catch (e: Exception) {
                Log.e(TAG, "Reboot failed", e)
                RebootUiState.Failed(R.string.error_generic, e.localizedMessage ?: e.javaClass.simpleName)
            }
            refreshAdbStatus()
        }
    }

    override fun onCleared() {
        rebooter.close()
    }

    private companion object {
        const val TAG = "RebootViewModel"
        const val PROMPT_ANSWER_GRACE_MS = 750L
    }
}

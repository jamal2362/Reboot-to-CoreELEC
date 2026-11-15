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

enum class AdbStatus { CHECKING, READY, UNREACHABLE }

class RebootViewModel(application: Application) : AndroidViewModel(application) {

    private val rebooter = AdbRebooter(application.filesDir)

    var uiState: RebootUiState by mutableStateOf(RebootUiState.Idle)
        private set

    var adbStatus: AdbStatus by mutableStateOf(AdbStatus.CHECKING)
        private set

    var deviceIp: String? by mutableStateOf(null)
        private set

    private val updateChecker = UpdateChecker(application)

    /** Version of a newer release, shown at once from the last check and then refreshed. */
    var availableUpdate: String? by mutableStateOf(updateChecker.cachedUpdate())
        private set

    init {
        viewModelScope.launch {
            availableUpdate = withContext(Dispatchers.IO) { updateChecker.fetchUpdate() }
        }
    }

    /** Checks whether the local ADB daemon is reachable and looks up the device's IP address. */
    fun refreshAdbStatus() {
        viewModelScope.launch {
            val (ip, reachable) = withContext(Dispatchers.IO) {
                AdbRebooter.localIpAddress() to rebooter.isReachable()
            }
            deviceIp = ip
            adbStatus = if (reachable) AdbStatus.READY else AdbStatus.UNREACHABLE
        }
    }

    fun reboot(mode: RebootMode) {
        if (uiState.isBusy) return
        uiState = RebootUiState.Connecting

        viewModelScope.launch {
            uiState = try {
                runInterruptible(Dispatchers.IO) {
                    rebooter.reboot(mode) { uiState = RebootUiState.Rebooting(mode) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: NotAuthorizedException) {
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
    }
}

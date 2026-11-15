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

sealed interface RebootUiState {
    data object Idle : RebootUiState
    data object Connecting : RebootUiState
    data class Rebooting(val mode: RebootMode) : RebootUiState
    data class Failed(@param:StringRes val messageRes: Int, val messageArg: Any? = null) : RebootUiState

    val isBusy: Boolean
        get() = this is Connecting || this is Rebooting
}

class RebootViewModel(application: Application) : AndroidViewModel(application) {

    private val rebooter = AdbRebooter(application.filesDir)

    var uiState: RebootUiState by mutableStateOf(RebootUiState.Idle)
        private set

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
        }
    }

    override fun onCleared() {
        rebooter.close()
    }

    private companion object {
        const val TAG = "RebootViewModel"
    }
}

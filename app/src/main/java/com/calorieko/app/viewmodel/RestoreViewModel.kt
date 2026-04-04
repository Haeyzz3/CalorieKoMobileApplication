package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.remote.CloudRestoreManager
import com.calorieko.app.data.remote.RestoreResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for orchestrating the initial cloud restore process.
 * By keeping this in a ViewModel, the coroutine survives configuration changes
 * (like screen rotation) and we can provide a retry mechanism if the restore fails.
 */
class RestoreViewModel(
    private val cloudRestoreManager: CloudRestoreManager
) : ViewModel() {

    sealed class RestoreState {
        data object Idle : RestoreState()
        data object Restoring : RestoreState()
        data object Success : RestoreState()
        data object NotNeeded : RestoreState()
        data object NoCloudData : RestoreState()
        data class Failed(val error: String) : RestoreState()
    }

    private val _state = MutableStateFlow<RestoreState>(RestoreState.Idle)
    val state: StateFlow<RestoreState> = _state.asStateFlow()

    private var lastUid: String? = null

    /**
     * Attempts to perform the cloud restore.
     * Starts the restoring state and runs the check via CloudRestoreManager.
     */
    fun restore(uid: String) {
        lastUid = uid
        _state.value = RestoreState.Restoring
        viewModelScope.launch(Dispatchers.IO) {
            val result = cloudRestoreManager.restoreIfNeeded(uid)
            _state.value = when (result) {
                is RestoreResult.Success    -> RestoreState.Success
                is RestoreResult.NotNeeded  -> RestoreState.NotNeeded
                is RestoreResult.NoCloudData -> RestoreState.NoCloudData
                is RestoreResult.Failed     -> RestoreState.Failed(result.error)
            }
        }
    }

    /**
     * Retries the restore with the last used UID, useful if a network error occurs.
     */
    fun retry() {
        lastUid?.let { restore(it) }
    }

    /**
     * Factory for creating instances of RestoreViewModel.
     */
    companion object {
        fun provideFactory(
            cloudRestoreManager: CloudRestoreManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(RestoreViewModel::class.java)) {
                    return RestoreViewModel(cloudRestoreManager) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}

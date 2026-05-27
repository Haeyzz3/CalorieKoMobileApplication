package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class VerificationViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    sealed class Event {
        data object VerificationSuccess : Event()
        data object LoggedOut : Event()
    }

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isVerified = MutableStateFlow(false)
    val isVerified: StateFlow<Boolean> = _isVerified.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _resendCooldown = MutableStateFlow(0)
    val resendCooldown: StateFlow<Int> = _resendCooldown.asStateFlow()

    fun checkVerificationStatus() {
        _isLoading.value = true
        viewModelScope.launch {
            val isVerified = authRepository.reloadUser()
            _isLoading.value = false
            if (isVerified) {
                _isVerified.value = true
                _message.value = "Success! Your email has been verified. Redirecting you shortly..."
                kotlinx.coroutines.delay(2000)
                _events.send(Event.VerificationSuccess)
            } else {
                _message.value = "Your email is not yet verified. Please check your inbox (including spam folder) and try again."
            }
        }
    }

    fun resendVerification() {
        if (_resendCooldown.value > 0) return

        _isLoading.value = true
        viewModelScope.launch {
            val success = authRepository.resendVerificationEmail()
            _isLoading.value = false
            if (success) {
                _message.value = "A new verification link has been sent to your email."
                startCooldown()
            } else {
                _message.value = "We've sent too many requests recently. For your security, please wait a minute before trying again, and remember to check your spam folder."
                startCooldown()
            }
        }
    }

    fun setInitialVerificationState(emailSent: Boolean, message: String?) {
        _message.value = message
        if (emailSent && _resendCooldown.value == 0) {
            startCooldown()
        }
    }

    private fun startCooldown() {
        viewModelScope.launch {
            _resendCooldown.value = 60
            while (_resendCooldown.value > 0) {
                kotlinx.coroutines.delay(1000)
                _resendCooldown.value -= 1
            }
        }
    }

    fun cancelRegistration() {
        authRepository.signOut()
        viewModelScope.launch {
            _events.send(Event.LoggedOut)
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(VerificationViewModel::class.java)) {
                    return VerificationViewModel(authRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}

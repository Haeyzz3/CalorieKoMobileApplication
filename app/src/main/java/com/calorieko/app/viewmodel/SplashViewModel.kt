package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for SplashScreen.
 *
 * Manages:
 * - Splash delay (2.5 seconds)
 * - Auth state check via AuthRepository
 * - Navigation event emission (AlreadyLoggedIn or NotLoggedIn)
 */
class SplashViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    // ── One-shot Events ──

    sealed class Event {
        data object AlreadyLoggedIn : Event()
        data object VerificationRequired : Event()
        data object NotLoggedIn : Event()
    }

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            // Splash delay
            delay(2500)

            // Check auth state
            val state = authRepository.checkAuthState()
            when (state) {
                is AuthRepository.AuthState.Verified -> {
                    _events.send(Event.AlreadyLoggedIn)
                }
                is AuthRepository.AuthState.Unverified -> {
                    _events.send(Event.VerificationRequired)
                }
                is AuthRepository.AuthState.NotLoggedIn -> {
                    _events.send(Event.NotLoggedIn)
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
                    return SplashViewModel(authRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}

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

/**
 * ViewModel for AuthScreen (login).
 *
 * Manages:
 * - Sign-in operation via AuthRepository
 * - Loading and error state
 *
 * Form state (email, password) stays as local composable state since
 * it's simple UI input that doesn't need to survive configuration changes
 * on a login screen.
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    // ── One-shot Events ──

    sealed class Event {
        data object LoginSuccess : Event()
    }

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // ── State ──

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    // ── Sign In ──

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Please fill in all fields"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = authRepository.signInWithEmail(email, password)
            _isLoading.value = false
            when (result) {
                is AuthRepository.AuthResult.Success -> {
                    _events.send(Event.LoginSuccess)
                }
                is AuthRepository.AuthResult.UnverifiedEmail -> {
                    _errorMessage.value = result.message
                }
                is AuthRepository.AuthResult.Error -> {
                    _errorMessage.value = result.message
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
                if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                    return AuthViewModel(authRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}

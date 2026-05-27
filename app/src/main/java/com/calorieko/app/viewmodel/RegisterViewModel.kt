package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.repository.AuthRepository
import com.calorieko.app.util.EmailValidator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for RegisterScreen (account creation).
 *
 * Manages:
 * - Account creation via AuthRepository
 * - Loading and error state
 *
 * Form state (email, password, confirmPassword) stays as local composable
 * state since it's simple UI input. Validation logic also stays in the
 * composable as it's purely derived from form state.
 */
class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    // ── One-shot Events ──

    sealed class Event {
        data class SignUpSuccess(
            val initialVerificationEmailSent: Boolean,
            val message: String?
        ) : Event()
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

    // ── Register ──

    fun register(email: String, password: String) {
        if (_isLoading.value) return

        val emailValidation = EmailValidator.validate(email)
        if (!emailValidation.isValid) {
            _errorMessage.value = emailValidation.message
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = authRepository.createAccount(emailValidation.normalizedEmail, password)
            _isLoading.value = false
            when (result) {
                is AuthRepository.RegistrationResult.AccountCreated -> {
                    _events.send(
                        Event.SignUpSuccess(
                            initialVerificationEmailSent = result.initialVerificationEmailSent,
                            message = result.message
                        )
                    )
                }
                is AuthRepository.RegistrationResult.Error -> {
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
                if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
                    return RegisterViewModel(authRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}

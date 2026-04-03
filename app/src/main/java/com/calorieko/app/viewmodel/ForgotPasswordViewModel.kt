package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for ForgotPasswordScreen.
 *
 * Manages:
 * - Password reset email via AuthRepository
 * - Loading, error, and success state
 *
 * Form state (email) stays as local composable state.
 */
class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    // ── State ──

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    // ── Reset Password ──

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _errorMessage.value = "Please enter your email address"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null

        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            _isLoading.value = false
            when (result) {
                is AuthRepository.ResetResult.Success -> {
                    _successMessage.value = result.message
                }
                is AuthRepository.ResetResult.Error -> {
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
                if (modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java)) {
                    return ForgotPasswordViewModel(authRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}

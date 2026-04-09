package com.calorieko.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
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
            _errorMessage.value = "Please enter your email address."
            return
        }

        // Basic client-side email format check before hitting Firebase
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _errorMessage.value = "Please enter a valid email address format."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null

        viewModelScope.launch {
            try {
                val result = authRepository.sendPasswordResetEmail(email.trim())
                _isLoading.value = false
                when (result) {
                    is AuthRepository.ResetResult.Success -> {
                        // Clear the email field hint — always show spam reminder
                        _successMessage.value =
                            "Reset link sent! Please check your inbox and spam folder."
                    }
                    is AuthRepository.ResetResult.Error -> {
                        // Parse Firebase error codes into user-friendly messages
                        _errorMessage.value = mapFirebaseResetError(result.message)
                    }
                }
            } catch (e: FirebaseAuthInvalidUserException) {
                _isLoading.value = false
                // Firebase throws this when the email is not registered
                _errorMessage.value = "No account found with that email address."
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _isLoading.value = false
                _errorMessage.value = "The email address format is invalid. Please check and try again."
            } catch (e: FirebaseNetworkException) {
                _isLoading.value = false
                _errorMessage.value = "No internet connection. Please check your network and try again."
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = mapFirebaseResetError(e.message ?: "An unexpected error occurred.")
            }
        }
    }

    /**
     * Maps raw Firebase error messages / codes to readable strings.
     */
    private fun mapFirebaseResetError(rawMessage: String): String {
        return when {
            "user-not-found" in rawMessage || "no user record" in rawMessage.lowercase() ->
                "No account found with that email address. Please check the email and try again."
            "invalid-email" in rawMessage ->
                "The email address format is invalid. Please enter a valid email."
            "too-many-requests" in rawMessage ->
                "Too many reset attempts. Please wait a few minutes before trying again."
            "network" in rawMessage.lowercase() ->
                "Network error. Please check your internet connection."
            else -> rawMessage.replaceFirst("ERROR_", "").replace("_", " ")
                .replaceFirstChar { it.uppercase() }
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

package com.calorieko.app.data.repository

import com.calorieko.app.util.EmailValidator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Repository wrapping all FirebaseAuth operations as suspend functions.
 *
 * Bridges Firebase's callback-based APIs to Kotlin coroutines using
 * `kotlinx.coroutines.tasks.await()`, making them cleanly consumable
 * from ViewModels.
 */
class AuthRepository(
    private val auth: FirebaseAuth
) {

    // ── Result Types ──

    sealed class AuthResult {
        data object Success : AuthResult()
        data class UnverifiedEmail(val message: String) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    sealed class RegistrationResult {
        data class AccountCreated(
            val initialVerificationEmailSent: Boolean,
            val message: String?
        ) : RegistrationResult()

        data class Error(val message: String) : RegistrationResult()
    }

    sealed class ResetResult {
        data class Success(val message: String) : ResetResult()
        data class Error(val message: String) : ResetResult()
    }

    sealed class AuthState {
        data object Verified : AuthState()
        data object Unverified : AuthState()
        data object NotLoggedIn : AuthState()
    }

    // ── Sign In ──

    /**
     * Signs in with email/password.
     * If the email is not verified, auto-resends the verification link and signs out.
     */
    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            auth.signInWithEmailAndPassword(EmailValidator.normalize(email), password).await()
            val user = auth.currentUser
            if (user != null && user.isEmailVerified) {
                AuthResult.Success
            } else {
                // Email NOT verified — resend verification & sign out
                user?.sendEmailVerification()
                auth.signOut()
                AuthResult.UnverifiedEmail(
                    "Your email is not yet verified. A new verification link has been sent to your inbox. If you don't see it, please check your spam or junk folder."
                )
            }
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            AuthResult.Error("No internet connection. Please check your network and try again.")
        } catch (e: Exception) {
            // For security and clarity, show a generic "Incorrect email or password" 
            // for all login failures (invalid user, wrong password, etc.)
            AuthResult.Error("Incorrect email or password. Please try again.")
        }
    }

    // ── Account Creation ──

    /**
     * Creates a new account with email/password and sends a verification email.
     */
    suspend fun createAccount(email: String, password: String): RegistrationResult {
        return try {
            auth.createUserWithEmailAndPassword(EmailValidator.normalize(email), password).await()
            val user = auth.currentUser
                ?: return RegistrationResult.Error("Registration failed. Please try again.")
            try {
                user.sendEmailVerification().await()
                RegistrationResult.AccountCreated(
                    initialVerificationEmailSent = true,
                    message = null
                )
            } catch (_: Exception) {
                RegistrationResult.AccountCreated(
                    initialVerificationEmailSent = false,
                    message = "Your account was created, but we couldn't send the verification email. Please use Resend Verification Email to try again."
                )
            }
        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            RegistrationResult.Error("This email is already registered. Please login instead.")
        } catch (e: com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
            RegistrationResult.Error("Password is too weak. Please use at least 8 characters.")
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            RegistrationResult.Error("Please enter a valid email address.")
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            RegistrationResult.Error("No internet connection. Please check your network and try again.")
        } catch (e: Exception) {
            RegistrationResult.Error("Registration failed. Please check your details and try again.")
        }
    }

    // ── Password Reset ──

    /**
     * Sends a password reset email.
     */
    suspend fun sendPasswordResetEmail(email: String): ResetResult {
        return try {
            val normalizedEmail = EmailValidator.normalize(email)
            auth.sendPasswordResetEmail(normalizedEmail).await()
            ResetResult.Success("A password reset link has been sent to $normalizedEmail")
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            ResetResult.Error("No account found with that email address.")
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            ResetResult.Error("No internet connection. Please try again.")
        } catch (e: Exception) {
            ResetResult.Error("Failed to send reset email. Please ensure the email is correct.")
        }
    }

    // ── Auth State Check ──

    /**
     * Checks the current authentication state.
     * Reloads the user from Firebase servers to get fresh verification status.
     * Falls back to cached value if offline.
     */
    suspend fun checkAuthState(): AuthState {
        val user = auth.currentUser ?: return AuthState.NotLoggedIn
        try {
            user.reload().await()
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            // User was deleted from Firebase (e.g., by admin panel).
            // Sign out locally and route to login screen.
            auth.signOut()
            return AuthState.NotLoggedIn
        } catch (_: Exception) {
            // Offline — use cached verification status
        }
        return if (user.isEmailVerified) {
            AuthState.Verified
        } else {
            AuthState.Unverified
        }
    }

    /**
     * Reloads the current user to refresh the email verification status.
     */
    suspend fun reloadUser(): Boolean {
        return try {
            auth.currentUser?.reload()?.await()
            auth.currentUser?.isEmailVerified ?: false
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            // User deleted from Firebase — sign out locally
            auth.signOut()
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Resends the email verification link.
     */
    suspend fun resendVerificationEmail(): Boolean {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
            true
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            // User deleted from Firebase — sign out locally
            auth.signOut()
            false
        } catch (e: Exception) {
            false
        }
    }

    // ── Sign Out ──

    fun signOut() {
        auth.signOut()
    }
}

package com.calorieko.app.data.repository

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
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = auth.currentUser
            if (user != null && user.isEmailVerified) {
                AuthResult.Success
            } else {
                // Email NOT verified — resend verification & sign out
                user?.sendEmailVerification()
                auth.signOut()
                AuthResult.UnverifiedEmail(
                    "Your email is not yet verified. A new verification link has been sent to your inbox."
                )
            }
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
    suspend fun createAccount(email: String, password: String): AuthResult {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            // Send email verification (fire-and-forget)
            auth.currentUser?.sendEmailVerification()
            AuthResult.Success
        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            AuthResult.Error("This email is already registered. Please login instead.")
        } catch (e: com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
            AuthResult.Error("Password is too weak. Please use at least 6 characters.")
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            AuthResult.Error("Please enter a valid email address.")
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            AuthResult.Error("No internet connection. Please check your network and try again.")
        } catch (e: Exception) {
            AuthResult.Error("Registration failed. Please check your details and try again.")
        }
    }

    // ── Password Reset ──

    /**
     * Sends a password reset email.
     */
    suspend fun sendPasswordResetEmail(email: String): ResetResult {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            ResetResult.Success("A password reset link has been sent to $email")
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
        } catch (_: Exception) {
            // Offline — use cached verification status
        }
        return if (user.isEmailVerified) {
            AuthState.Verified
        } else {
            auth.signOut()
            AuthState.Unverified
        }
    }

    // ── Sign Out ──

    fun signOut() {
        auth.signOut()
    }
}

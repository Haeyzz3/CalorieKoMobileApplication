package com.calorieko.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calorieko.app.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfileViewModel(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) : ViewModel() {

    // ── One-shot Events ──

    sealed class Event {
        data object SaveSuccess : Event()
        data class SaveError(val message: String) : Event()
    }

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // ── Form State ──

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _age = MutableStateFlow("")
    val age: StateFlow<String> = _age.asStateFlow()

    private val _height = MutableStateFlow("")
    val height: StateFlow<String> = _height.asStateFlow()

    private val _weight = MutableStateFlow("")
    val weight: StateFlow<String> = _weight.asStateFlow()

    private val _sex = MutableStateFlow("Male")
    val sex: StateFlow<String> = _sex.asStateFlow()

    private val _selectedGoal = MutableStateFlow("general")
    val selectedGoal: StateFlow<String> = _selectedGoal.asStateFlow()

    private val _selectedActivityLevel = MutableStateFlow("light")
    val selectedActivityLevel: StateFlow<String> = _selectedActivityLevel.asStateFlow()

    private val _existingPhotoUrl = MutableStateFlow("")
    val existingPhotoUrl: StateFlow<String> = _existingPhotoUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadExistingProfile()
    }

    // ── Field Setters ──

    fun updateName(value: String) { _name.value = value }
    fun updateAge(value: String) { _age.value = value }
    fun updateHeight(value: String) { _height.value = value }
    fun updateWeight(value: String) { _weight.value = value }
    fun updateSex(value: String) { _sex.value = value }
    fun updateGoal(value: String) { _selectedGoal.value = value }
    fun updateActivityLevel(value: String) { _selectedActivityLevel.value = value }

    // ── Data Loading ──

    private fun loadExistingProfile() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val profile = userRepository.getUserProfile(uid)
                if (profile != null) {
                    _name.value = profile.name
                    _age.value = profile.age.toString()
                    _height.value = profile.height.toString()
                    _weight.value = profile.weight.toString()
                    _sex.value = profile.sex.ifEmpty { "Male" }
                    // Map legacy IDs to NDAP IDs for backward compatibility
                    val rawLevel = profile.activityLevel.ifEmpty { "light" }
                    _selectedActivityLevel.value = when (rawLevel) {
                        "not_very_active" -> "sedentary"
                        "lightly_active"  -> "light"
                        "active"          -> "moderate"
                        "very_active"     -> "vigorous"
                        else              -> rawLevel // already an NDAP ID
                    }
                    _selectedGoal.value = profile.goal.ifEmpty { "general" }
                    _existingPhotoUrl.value = profile.photoUrl
                }
            }
        }
    }

    // ── Save ──

    /**
     * Saves the profile: compresses photo (if new), writes to Room,
     * syncs to Firestore, then emits [Event.SaveSuccess].
     *
     * @param context Needed for ContentResolver to read the selected image URI.
     *                Passed as a method parameter (not stored) to avoid ViewModel holding Context.
     * @param selectedImageUri New photo URI from gallery picker, or null if unchanged.
     */
    fun saveProfile(context: Context, selectedImageUri: Uri?) {
        val uid = auth.currentUser?.uid ?: return
        if (_isLoading.value) return

        _isLoading.value = true

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    userRepository.saveProfileFromForm(
                        context = context,
                        uid = uid,
                        email = auth.currentUser?.email ?: "",
                        name = _name.value,
                        age = _age.value,
                        weight = _weight.value,
                        height = _height.value,
                        sex = _sex.value,
                        activityLevel = _selectedActivityLevel.value,
                        goal = _selectedGoal.value,
                        selectedImageUri = selectedImageUri,
                        existingPhotoUrl = _existingPhotoUrl.value
                    )
                }
                _isLoading.value = false
                _events.send(Event.SaveSuccess)
            } catch (e: Exception) {
                _isLoading.value = false
                _events.send(Event.SaveError(e.message ?: "Save failed"))
            }
        }
    }

    companion object {
        fun provideFactory(
            auth: FirebaseAuth,
            userRepository: UserRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(EditProfileViewModel::class.java)) {
                    return EditProfileViewModel(auth, userRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}

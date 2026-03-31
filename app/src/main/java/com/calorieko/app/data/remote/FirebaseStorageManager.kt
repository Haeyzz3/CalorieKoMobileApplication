package com.calorieko.app.data.remote

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * Handles uploading files to Firebase Cloud Storage and retrieving 
 * their public HTTPS download URLs.
 */
class FirebaseStorageManager {
    companion object {
        private const val TAG = "FirebaseStorageManager"
    }

    private val storageRef = FirebaseStorage.getInstance().reference

    /**
     * Uploads a user's profile photo and returns the public download URL.
     * Replaces any existing profile photo to conserve space.
     * 
     * @return Download URL string, or null if the upload fails (e.g. offline).
     */
    suspend fun uploadProfilePhoto(uid: String, uri: Uri): String? {
        return try {
            Log.d(TAG, "Uploading profile photo for $uid...")
            val photoRef = storageRef.child("users/$uid/profile_photo.jpg")
            
            // Upload the file
            photoRef.putFile(uri).await()
            
            // Get the URL
            val downloadUrl = photoRef.downloadUrl.await()
            Log.d(TAG, "Profile photo uploaded successfully: $downloadUrl")
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload profile photo", e)
            null
        }
    }

    /**
     * Uploads a workout photo and returns the public download URL.
     * 
     * @return Download URL string, or null if the upload fails.
     */
    suspend fun uploadWorkoutPhoto(uid: String, timestamp: Long, uri: Uri): String? {
        return try {
            Log.d(TAG, "Uploading workout photo for $uid at $timestamp...")
            val photoRef = storageRef.child("users/$uid/workouts/workout_$timestamp.jpg")
            
            photoRef.putFile(uri).await()
            
            val downloadUrl = photoRef.downloadUrl.await()
            Log.d(TAG, "Workout photo uploaded successfully: $downloadUrl")
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload workout photo", e)
            null
        }
    }
}

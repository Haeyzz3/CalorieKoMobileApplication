package com.calorieko.app.data.repository

import android.content.Context
import android.net.Uri
import com.calorieko.app.data.local.ActivityLogDao
import com.calorieko.app.data.local.MealLogDao
import com.calorieko.app.data.local.UserDao
import com.calorieko.app.data.model.BadgeStats
import com.calorieko.app.data.model.UserProfile
import com.calorieko.app.data.remote.FirestoreSyncRepository
import com.calorieko.app.data.remote.ImageUtils

/**
 * Read-write repository for user profile operations.
 *
 * Encapsulates:
 * - Room read/write for UserProfile
 * - Firestore sync on writes
 * - Photo compression for profile image updates
 * - Badge stats aggregation from multiple DAOs
 */
class UserRepository(
    private val userDao: UserDao,
    private val firestoreSyncRepo: FirestoreSyncRepository
) {

    // ── Profile Read ──

    /** Fetch the user profile from Room. */
    suspend fun getUserProfile(uid: String): UserProfile? {
        return userDao.getUser(uid)
    }

    // ── Profile Write ──

    /** Save a profile to Room and sync to Firestore. */
    suspend fun saveProfile(uid: String, profile: UserProfile) {
        userDao.insertUser(profile)
        firestoreSyncRepo.syncProfile(uid, profile)
    }

    /**
     * Builds an updated [UserProfile] from form data, compressing the photo
     * if a new image URI is provided. Saves to Room and syncs to Firestore.
     *
     * @param context  Needed for ContentResolver to read the selected image URI
     * @param uid      Firebase UID
     * @param email    Firebase email (fallback for new profiles)
     * @param name     Form field
     * @param age      Form field (String, parsed to Int)
     * @param weight   Form field (String, parsed to Double)
     * @param height   Form field (String, parsed to Double)
     * @param sex      Form field
     * @param activityLevel  Form field
     * @param goal     Form field
     * @param selectedImageUri  New photo URI from gallery picker (nullable)
     * @param existingPhotoUrl  Current photo URL from the existing profile
     */
    suspend fun saveProfileFromForm(
        context: Context,
        uid: String,
        email: String,
        name: String,
        age: String,
        weight: String,
        height: String,
        sex: String,
        activityLevel: String,
        goal: String,
        selectedImageUri: Uri?,
        existingPhotoUrl: String
    ) {
        // 1. Compress and encode new photo (if selected)
        val finalPhotoUrl = selectedImageUri?.let { uri ->
            val encodedStr = ImageUtils.compressAndEncode(context, uri, maxDimension = 300, quality = 70)
            encodedStr ?: uri.toString()
        } ?: existingPhotoUrl

        // 2. Fetch existing profile to preserve unchanged fields
        val existingProfile = userDao.getUser(uid)

        // 3. Build the profile (update existing or create new)
        val updatedProfile = if (existingProfile != null) {
            existingProfile.copy(
                name = name,
                age = age.toIntOrNull() ?: existingProfile.age,
                weight = weight.toDoubleOrNull() ?: existingProfile.weight,
                height = height.toDoubleOrNull() ?: existingProfile.height,
                sex = sex,
                activityLevel = activityLevel,
                goal = goal,
                photoUrl = finalPhotoUrl
            )
        } else {
            UserProfile(
                uid = uid,
                name = name,
                email = email,
                age = age.toIntOrNull() ?: 25,
                weight = weight.toDoubleOrNull() ?: 70.0,
                height = height.toDoubleOrNull() ?: 170.0,
                sex = sex,
                activityLevel = activityLevel,
                goal = goal,
                photoUrl = finalPhotoUrl
            )
        }

        // 4. Save to Room + Firestore
        saveProfile(uid, updatedProfile)
    }

    // ── Badge Stats ──

    /**
     * Fetches aggregate counts from multiple DAOs for badge calculation.
     * Each call is wrapped in try/catch so a failing DAO doesn't crash the whole fetch.
     */
    suspend fun getBadgeStats(
        uid: String,
        mealLogDao: MealLogDao,
        activityLogDao: ActivityLogDao
    ): BadgeStats {
        val mealsCount = try { mealLogDao.getTotalMealsCount(uid) } catch (_: Exception) { 0 }
        val workoutsCount = try { activityLogDao.getTotalWorkoutsCount(uid) } catch (_: Exception) { 0 }
        val actPhotos = try { activityLogDao.getWorkoutsWithPhotoCount(uid) } catch (_: Exception) { 0 }

        return BadgeStats(
            totalMeals = mealsCount,
            totalWorkouts = workoutsCount,
            totalPhotos = actPhotos
        )
    }
}

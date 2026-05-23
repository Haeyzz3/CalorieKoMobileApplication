package com.calorieko.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.calorieko.app.data.local.ActivityLogDao
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.local.MealLogDao
import com.calorieko.app.data.model.BadgeStats
import com.calorieko.app.data.model.UserProfile
import com.calorieko.app.data.model.WeightLogEntity
import com.calorieko.app.data.remote.ImageUtils
import com.calorieko.app.data.remote.api.AutoSyncManager
import com.calorieko.app.data.remote.firestore.FirestoreAutoSyncManager
import com.calorieko.app.data.remote.firestore.FirestoreEntityType
import com.calorieko.app.data.remote.firestore.FirestorePayloadSerializer
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import kotlin.math.abs

class UserRepository(
    private val db: AppDatabase,
    private val appContext: Context
) {
    private val userDao = db.userDao()
    private val weightLogDao = db.weightLogDao()
    private val outboxDao = db.firestoreOutboxDao()

    suspend fun getUserProfile(uid: String): UserProfile? {
        return userDao.getUser(uid)
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> {
        return userDao.observeUser(uid)
    }

    suspend fun saveProfile(uid: String, profile: UserProfile) {
        db.withTransaction {
            val previousProfile = userDao.getUser(uid)
            val now = System.currentTimeMillis()
            val persistedProfile = profile.copy(uid = uid, updatedAt = now)
            userDao.insertUser(persistedProfile)
            outboxDao.insert(
                FirestorePayloadSerializer.upsert(
                    uid = uid,
                    entityType = FirestoreEntityType.USER_PROFILE,
                    entityKey = uid,
                    remotePath = FirestorePayloadSerializer.userProfilePath(uid),
                    payload = FirestorePayloadSerializer.profilePayload(persistedProfile),
                    now = now
                )
            )

            val weightLog = buildWeightLogIfChanged(uid, previousProfile?.weight, persistedProfile.weight, now)
            if (weightLog != null) {
                weightLogDao.upsertWeightLog(weightLog)
                outboxDao.insert(
                    FirestorePayloadSerializer.upsert(
                        uid = uid,
                        entityType = FirestoreEntityType.WEIGHT_LOG,
                        entityKey = weightLog.timestamp.toString(),
                        remotePath = FirestorePayloadSerializer.weightLogPath(uid, weightLog.timestamp),
                        payload = FirestorePayloadSerializer.weightLogPayload(weightLog),
                        now = now
                    )
                )
            }
        }

        triggerSync(uid)
    }

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
        val finalPhotoUrl = selectedImageUri?.let { uri ->
            val encodedStr = ImageUtils.compressAndEncode(context, uri, maxDimension = 300, quality = 70)
            encodedStr ?: uri.toString()
        } ?: existingPhotoUrl

        val existingProfile = userDao.getUser(uid)
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

        saveProfile(uid, updatedProfile)
    }

    suspend fun saveInitialProfile(uid: String, profile: UserProfile) {
        saveProfile(uid, profile)
    }

    suspend fun markOnboardingCompleted(uid: String) {
        val profile = userDao.getUser(uid) ?: return
        saveProfile(uid, profile.copy(onboardingCompleted = true))
    }

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

    private fun buildWeightLogIfChanged(
        uid: String,
        previousWeight: Double?,
        newWeight: Double,
        now: Long
    ): WeightLogEntity? {
        if (newWeight <= 0.0) return null
        if (previousWeight != null && abs(previousWeight - newWeight) < 0.05) return null
        return WeightLogEntity(
            uid = uid,
            dateEpochDay = LocalDate.now().toEpochDay(),
            weightKg = newWeight,
            timestamp = now,
            updatedAt = now,
            syncStatus = 0
        )
    }

    private fun triggerSync(uid: String) {
        if (uid.isBlank()) return
        FirestoreAutoSyncManager.triggerSync(appContext, uid)
        AutoSyncManager.triggerSync(appContext, uid)
    }
}

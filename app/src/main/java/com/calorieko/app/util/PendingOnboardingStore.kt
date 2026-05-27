package com.calorieko.app.util

import android.content.Context

data class PendingOnboardingProfile(
    val uid: String,
    val name: String,
    val email: String,
    val age: Int,
    val weight: Double,
    val height: Double,
    val sex: String,
    val activityLevel: String,
    val goal: String,
    val goalTitle: String,
    val createdAtMillis: Long,
    val initialVerificationEmailSent: Boolean,
    val initialVerificationMessage: String?
)

class PendingOnboardingStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "pending_onboarding",
        Context.MODE_PRIVATE
    )

    fun save(profile: PendingOnboardingProfile) {
        prefs.edit()
            .putString(KEY_UID, profile.uid)
            .putString(KEY_NAME, profile.name)
            .putString(KEY_EMAIL, profile.email)
            .putInt(KEY_AGE, profile.age)
            .putLong(KEY_WEIGHT, profile.weight.toBits())
            .putLong(KEY_HEIGHT, profile.height.toBits())
            .putString(KEY_SEX, profile.sex)
            .putString(KEY_ACTIVITY_LEVEL, profile.activityLevel)
            .putString(KEY_GOAL, profile.goal)
            .putString(KEY_GOAL_TITLE, profile.goalTitle)
            .putLong(KEY_CREATED_AT_MILLIS, profile.createdAtMillis)
            .putBoolean(KEY_INITIAL_VERIFICATION_EMAIL_SENT, profile.initialVerificationEmailSent)
            .putString(KEY_INITIAL_VERIFICATION_MESSAGE, profile.initialVerificationMessage)
            .apply()
    }

    fun getForUid(uid: String): PendingOnboardingProfile? {
        val storedUid = prefs.getString(KEY_UID, null) ?: return null
        if (storedUid != uid) return null

        return PendingOnboardingProfile(
            uid = storedUid,
            name = prefs.getString(KEY_NAME, null) ?: return null,
            email = prefs.getString(KEY_EMAIL, null) ?: return null,
            age = prefs.getInt(KEY_AGE, 25),
            weight = Double.fromBits(prefs.getLong(KEY_WEIGHT, 70.0.toBits())),
            height = Double.fromBits(prefs.getLong(KEY_HEIGHT, 170.0.toBits())),
            sex = prefs.getString(KEY_SEX, "") ?: "",
            activityLevel = prefs.getString(KEY_ACTIVITY_LEVEL, "") ?: "",
            goal = prefs.getString(KEY_GOAL, "") ?: "",
            goalTitle = prefs.getString(KEY_GOAL_TITLE, "General Health & Wellness")
                ?: "General Health & Wellness",
            createdAtMillis = prefs.getLong(KEY_CREATED_AT_MILLIS, System.currentTimeMillis()),
            initialVerificationEmailSent = prefs.getBoolean(KEY_INITIAL_VERIFICATION_EMAIL_SENT, true),
            initialVerificationMessage = prefs.getString(KEY_INITIAL_VERIFICATION_MESSAGE, null)
        )
    }

    fun clear(uid: String? = null) {
        val storedUid = prefs.getString(KEY_UID, null)
        if (uid == null || storedUid == uid) {
            prefs.edit().clear().apply()
        }
    }

    private companion object {
        const val KEY_UID = "uid"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_AGE = "age"
        const val KEY_WEIGHT = "weight"
        const val KEY_HEIGHT = "height"
        const val KEY_SEX = "sex"
        const val KEY_ACTIVITY_LEVEL = "activity_level"
        const val KEY_GOAL = "goal"
        const val KEY_GOAL_TITLE = "goal_title"
        const val KEY_CREATED_AT_MILLIS = "created_at_millis"
        const val KEY_INITIAL_VERIFICATION_EMAIL_SENT = "initial_verification_email_sent"
        const val KEY_INITIAL_VERIFICATION_MESSAGE = "initial_verification_message"
    }
}

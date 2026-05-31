package com.calorieko.app.data.repository

import android.util.Log
import com.calorieko.app.data.local.MealPlanDao

/**
 * Repository that centralizes meal plan status lifecycle operations.
 *
 * Handles:
 * - Skipping a meal slot (user-initiated)
 * - Marking planned meals as logged (after confirmMeal)
 * - Scheduling offline-first background sync after local status changes
 * - Building source_plan_key for provenance linking
 *
 * Status transitions: planned → logged | skipped | missed
 */
class MealPlanRepository(
    private val mealPlanDao: MealPlanDao,
    private val scheduleSync: (String) -> Unit
) {

    private companion object {
        const val TAG = "MealPlanRepository"
    }

    /**
     * Marks clearable dishes in a meal slot as "skipped" in Room and schedules background sync.
     * Called when the user taps "Skip Meal" in the MealDetailDialog.
     */
    suspend fun skipSlot(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String) {
        val updatedRows = mealPlanDao.skipSlotClearableOnly(uid, dayIndex, weekStartDate, mealSlot)
        if (updatedRows > 0) {
            scheduleSyncIfAuthenticated(uid)
        }
    }

    /**
     * Reverts skipped dishes in a slot back to "planned" in Room and schedules background sync.
     * Called when the user taps "Undo Skip" in the MealDetailDialog.
     */
    suspend fun unskipSlot(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String) {
        val updatedRows = mealPlanDao.unskipSlotSkippedOnly(uid, dayIndex, weekStartDate, mealSlot)
        if (updatedRows > 0) {
            scheduleSyncIfAuthenticated(uid)
        }
    }

    /**
     * Marks specific planned meals as "logged" after a successful meal log save.
     * Called by ManualLogViewModel after confirmMeal() when the log originated
     * from a planned meal (isPlannedQuickLog = true).
     */
    suspend fun markAsLogged(
        uid: String,
        dayIndex: Int,
        weekStartDate: String,
        mealSlot: String,
        dishLabels: List<String>
    ) {
        for (label in dishLabels) {
            mealPlanDao.updateMealStatus(uid, dayIndex, weekStartDate, mealSlot, label, "logged")
        }
        scheduleSyncIfAuthenticated(uid)
    }

    /**
     * Builds the composite source_plan_key for stamping on MealLogEntity.
     * Format matches the Firestore document ID convention.
     */
    fun buildPlanKey(dayIndex: Int, weekStartDate: String, mealSlot: String, dishLabel: String): String =
        "${dayIndex}_${weekStartDate}_${mealSlot}_${dishLabel}"

    private fun scheduleSyncIfAuthenticated(uid: String) {
        if (uid.isNotEmpty()) {
            try {
                scheduleSync(uid)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to schedule meal plan sync", e)
            }
        }
    }
}

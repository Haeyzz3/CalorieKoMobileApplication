package com.calorieko.app.data.repository

import com.calorieko.app.data.local.MealPlanDao
import com.calorieko.app.data.remote.FirestoreSyncRepository

/**
 * Repository that centralizes meal plan status lifecycle operations.
 *
 * Handles:
 * - Skipping a meal slot (user-initiated)
 * - Marking planned meals as logged (after confirmMeal)
 * - Building source_plan_key for provenance linking
 *
 * Status transitions: planned → logged | skipped | missed | partial
 */
class MealPlanRepository(
    private val mealPlanDao: MealPlanDao,
    private val firestoreSyncRepo: FirestoreSyncRepository
) {

    /**
     * Marks an entire meal slot as "skipped" in Room and syncs to Firestore.
     * Called when the user taps "Skip Meal" in the MealDetailDialog.
     */
    suspend fun skipSlot(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String) {
        mealPlanDao.updateSlotStatus(uid, dayIndex, weekStartDate, mealSlot, "skipped")
        // Sync updated status to Firestore (re-push each dish in the slot)
        val meals = mealPlanDao.getMealsForDayOneShot(uid, dayIndex, weekStartDate)
            .filter { it.mealSlot == mealSlot }
        for (meal in meals) {
            firestoreSyncRepo.syncPlannedMeal(uid, meal)
        }
    }

    /**
     * Reverts a skipped slot back to "planned" in Room and syncs to Firestore.
     * Called when the user taps "Undo Skip" in the MealDetailDialog.
     */
    suspend fun unskipSlot(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String) {
        mealPlanDao.updateSlotStatus(uid, dayIndex, weekStartDate, mealSlot, "planned")
        val meals = mealPlanDao.getMealsForDayOneShot(uid, dayIndex, weekStartDate)
            .filter { it.mealSlot == mealSlot }
        for (meal in meals) {
            firestoreSyncRepo.syncPlannedMeal(uid, meal)
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
        // Sync to Firestore
        val meals = mealPlanDao.getMealsForDayOneShot(uid, dayIndex, weekStartDate)
            .filter { it.mealSlot == mealSlot && it.dishLabel in dishLabels }
        for (meal in meals) {
            firestoreSyncRepo.syncPlannedMeal(uid, meal)
        }
    }

    /**
     * Builds the composite source_plan_key for stamping on MealLogEntity.
     * Format matches the Firestore document ID convention.
     */
    fun buildPlanKey(dayIndex: Int, weekStartDate: String, mealSlot: String, dishLabel: String): String =
        "${dayIndex}_${weekStartDate}_${mealSlot}_${dishLabel}"
}

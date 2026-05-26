package com.calorieko.app.data.repository

import android.content.Context
import com.calorieko.app.data.local.DailyNutritionSummaryDao
import com.calorieko.app.data.local.MealLogDao
import com.calorieko.app.data.local.MealLogItemDao
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.LoggedDish
import com.calorieko.app.data.model.MealLogEntity
import com.calorieko.app.data.model.MealLogItemEntity
import com.calorieko.app.data.remote.api.AutoSyncManager
import java.time.LocalDate

/**
 * Repository that abstracts meal persistence across Room and Firestore.
 *
 * Responsibilities:
 * - Insert a meal log + items into Room
 * - Upsert the daily nutrition summary
 * - Sync both to Firestore
 * - Trigger auto-sync to Laravel backend via WorkManager
 *
 * The ViewModel should call these methods within `Dispatchers.IO`.
 */
class MealRepository(
    private val mealLogDao: MealLogDao,
    private val mealLogItemDao: MealLogItemDao,
    private val dailyNutritionSummaryDao: DailyNutritionSummaryDao,
    private val appContext: Context
) {

    /**
     * Persists a complete meal to Room, updates the daily nutrition summary,
     * and syncs both to Firestore.
     *
     * @param uid       The authenticated user's UID.
     * @param mealType  One of "Breakfast", "Lunch", "Dinner", or "Snacks".
     * @param dishes    The list of logged dishes with pre-computed nutrients.
     */
    suspend fun saveMeal(uid: String, mealType: String, dishes: List<LoggedDish>, sourcePlanKey: String? = null) {
        val now = System.currentTimeMillis()

        // 1. Insert the meal log header
        val mealLogId = mealLogDao.insertMealLog(
            MealLogEntity(uid = uid, mealType = mealType, timestamp = now, sourcePlanKey = sourcePlanKey)
        )

        // 2. Insert all meal log items
        val items = dishes.map { d ->
            MealLogItemEntity(
                mealLogId = mealLogId,
                foodId = d.foodId,
                dishName = d.dishNamePh.ifBlank { d.dishNameEn },
                weightGrams = d.weightGrams,
                calories = d.calories,
                protein = d.protein,
                carbs = d.carbs,
                fiber = d.fiber,
                sugar = d.sugar,
                fat = d.fat,
                saturatedFat = d.saturatedFat,
                polyunsaturatedFat = d.polyunsaturatedFat,
                monounsaturatedFat = d.monounsaturatedFat,
                transFat = d.transFat,
                cholesterol = d.cholesterol,
                sodium = d.sodium,
                potassium = d.potassium,
                vitaminA = d.vitaminA,
                vitaminC = d.vitaminC,
                calcium = d.calcium,
                iron = d.iron
            )
        }
        mealLogItemDao.insertItems(items)

        // 3. Upsert the daily nutrition summary
        val today = LocalDate.now().toEpochDay()
        val existing = dailyNutritionSummaryDao.getSummaryForDate(uid, today)
        val mealCalories = dishes.sumOf { it.calories.toDouble() }.toFloat()

        val updated = (existing ?: DailyNutritionSummaryEntity(uid = uid, dateEpochDay = today)).let { s ->
            s.copy(
                id = s.id,
                updatedAt = System.currentTimeMillis(), // Refresh for delta sync
                totalCalories = s.totalCalories + mealCalories,
                totalProtein = s.totalProtein + dishes.sumOf { it.protein.toDouble() }.toFloat(),
                totalCarbs = s.totalCarbs + dishes.sumOf { it.carbs.toDouble() }.toFloat(),
                totalFiber = s.totalFiber + dishes.sumOf { it.fiber.toDouble() }.toFloat(),
                totalSugar = s.totalSugar + dishes.sumOf { it.sugar.toDouble() }.toFloat(),
                totalFat = s.totalFat + dishes.sumOf { it.fat.toDouble() }.toFloat(),
                totalSaturatedFat = s.totalSaturatedFat + dishes.sumOf { it.saturatedFat.toDouble() }.toFloat(),
                totalPolyunsaturatedFat = s.totalPolyunsaturatedFat + dishes.sumOf { it.polyunsaturatedFat.toDouble() }.toFloat(),
                totalMonounsaturatedFat = s.totalMonounsaturatedFat + dishes.sumOf { it.monounsaturatedFat.toDouble() }.toFloat(),
                totalTransFat = s.totalTransFat + dishes.sumOf { it.transFat.toDouble() }.toFloat(),
                totalCholesterol = s.totalCholesterol + dishes.sumOf { it.cholesterol.toDouble() }.toFloat(),
                totalSodium = s.totalSodium + dishes.sumOf { it.sodium.toDouble() }.toFloat(),
                totalPotassium = s.totalPotassium + dishes.sumOf { it.potassium.toDouble() }.toFloat(),
                totalVitaminA = s.totalVitaminA + dishes.sumOf { it.vitaminA.toDouble() }.toFloat(),
                totalVitaminC = s.totalVitaminC + dishes.sumOf { it.vitaminC.toDouble() }.toFloat(),
                totalCalcium = s.totalCalcium + dishes.sumOf { it.calcium.toDouble() }.toFloat(),
                totalIron = s.totalIron + dishes.sumOf { it.iron.toDouble() }.toFloat(),
                breakfastCalories = s.breakfastCalories + if (mealType == "Breakfast") mealCalories else 0f,
                lunchCalories = s.lunchCalories + if (mealType == "Lunch") mealCalories else 0f,
                dinnerCalories = s.dinnerCalories + if (mealType == "Dinner") mealCalories else 0f,
                snacksCalories = s.snacksCalories + if (mealType == "Snacks") mealCalories else 0f
            )
        }
        dailyNutritionSummaryDao.upsertSummary(updated)

        // 4. Skip foreground Firestore sync — let WorkManager handle it in the background.
        //    The meal stays marked as unsynced (sync_status = 0) by default.
        //    This allows confirmMeal() to return immediately without waiting for
        //    network I/O, even if connectivity is available.

        // 5. Trigger WorkManager sync (survives process death,
        //    only runs when network is available via CONNECTED constraint)
        AutoSyncManager.triggerSync(appContext, uid)
    }

    /**
     * Deletes a meal log from Room (local DB) and recalculates the daily
     * nutrition summary. DOES NOT sync to Firestore—that is handled separately
     * by the caller to avoid blocking the UI.
     *
     * Steps:
     * 1. Fetch the meal + items so we know what nutrients to subtract
     * 2. Subtract those nutrients from the DailyNutritionSummaryEntity
     * 3. Delete from Room (CASCADE deletes child MealLogItemEntity rows)
     *
     * Returns the updated [DailyNutritionSummaryEntity] so the caller can
     * sync it to Firestore alongside the meal log deletion.
     *
     * ⚠️ Firestore sync is handled by the ViewModel after UI reload to keep
     * this operation fast and responsive, especially offline.
     */
    suspend fun deleteMealLogLocally(uid: String, mealLogId: Long): DailyNutritionSummaryEntity? {
        // 1. Fetch the meal with items before deleting
        val mealWithItems = mealLogDao.getMealLogWithItems(mealLogId) ?: return null
        val items = mealWithItems.items
        val mealType = mealWithItems.mealLog.mealType

        // Compute the total nutrients of the deleted meal
        val deletedCalories = items.sumOf { it.calories.toDouble() }.toFloat()

        // 2. Determine which day this meal belongs to and update the summary
        val mealDate = java.time.Instant.ofEpochMilli(mealWithItems.mealLog.timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        val epochDay = mealDate.toEpochDay()
        val existing = dailyNutritionSummaryDao.getSummaryForDate(uid, epochDay)
        var updatedSummary: DailyNutritionSummaryEntity? = null
        if (existing != null) {
            val updated = existing.copy(
                updatedAt = System.currentTimeMillis(),
                totalCalories = (existing.totalCalories - deletedCalories).coerceAtLeast(0f),
                totalProtein = (existing.totalProtein - items.sumOf { it.protein.toDouble() }.toFloat()).coerceAtLeast(0f),
                totalCarbs = (existing.totalCarbs - items.sumOf { it.carbs.toDouble() }.toFloat()).coerceAtLeast(0f),
                totalFiber = (existing.totalFiber - items.sumOf { it.fiber.toDouble() }.toFloat()).coerceAtLeast(0f),
                totalSugar = (existing.totalSugar - items.sumOf { it.sugar.toDouble() }.toFloat()).coerceAtLeast(0f),
                totalFat = (existing.totalFat - items.sumOf { it.fat.toDouble() }.toFloat()).coerceAtLeast(0f),
                totalSaturatedFat = (existing.totalSaturatedFat - items.sumOf { it.saturatedFat.toDouble() }.toFloat()).coerceAtLeast(0f),
                totalPolyunsaturatedFat = (existing.totalPolyunsaturatedFat - items.sumOf { it.polyunsaturatedFat.toDouble() }.toFloat()).coerceAtLeast(0f),
                totalMonounsaturatedFat = (existing.totalMonounsaturatedFat - items.sumOf { it.monounsaturatedFat.toDouble() }.toFloat()).coerceAtLeast(0f),
                totalTransFat = (existing.totalTransFat - items.sumOf { it.transFat.toDouble() }.toFloat()).coerceAtLeast(0f),
                totalCholesterol = (existing.totalCholesterol - items.sumOf { it.cholesterol.toDouble() }.toFloat()).coerceAtLeast(0f),
                totalSodium = (existing.totalSodium - items.sumOf { it.sodium.toDouble() }.toFloat()).coerceAtLeast(0f),
                totalPotassium = (existing.totalPotassium - items.sumOf { it.potassium.toDouble() }.toFloat()).coerceAtLeast(0f),
                totalVitaminA = (existing.totalVitaminA - items.sumOf { it.vitaminA.toDouble() }.toFloat()).coerceAtLeast(0f),
                totalVitaminC = (existing.totalVitaminC - items.sumOf { it.vitaminC.toDouble() }.toFloat()).coerceAtLeast(0f),
                totalCalcium = (existing.totalCalcium - items.sumOf { it.calcium.toDouble() }.toFloat()).coerceAtLeast(0f),
                totalIron = (existing.totalIron - items.sumOf { it.iron.toDouble() }.toFloat()).coerceAtLeast(0f),
                breakfastCalories = if (mealType == "Breakfast") (existing.breakfastCalories - deletedCalories).coerceAtLeast(0f) else existing.breakfastCalories,
                lunchCalories = if (mealType == "Lunch") (existing.lunchCalories - deletedCalories).coerceAtLeast(0f) else existing.lunchCalories,
                dinnerCalories = if (mealType == "Dinner") (existing.dinnerCalories - deletedCalories).coerceAtLeast(0f) else existing.dinnerCalories,
                snacksCalories = if (mealType == "Snacks") (existing.snacksCalories - deletedCalories).coerceAtLeast(0f) else existing.snacksCalories
            )
            dailyNutritionSummaryDao.upsertSummary(updated)
            updatedSummary = updated
        }

        // 3. Delete from Room (CASCADE deletes child items automatically)
        mealLogDao.deleteMealLog(mealLogId)

        return updatedSummary
    }
}

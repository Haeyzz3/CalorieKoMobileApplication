package com.calorieko.app.data.repository

import android.content.Context
import com.calorieko.app.data.local.DailyNutritionSummaryDao
import com.calorieko.app.data.local.MealLogDao
import com.calorieko.app.data.local.MealLogItemDao
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.LoggedDish
import com.calorieko.app.data.model.MealLogEntity
import com.calorieko.app.data.model.MealLogItemEntity
import com.calorieko.app.data.remote.FirestoreSyncRepository
import com.calorieko.app.data.remote.api.AutoSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private val firestoreSyncRepo: FirestoreSyncRepository,
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
    suspend fun saveMeal(uid: String, mealType: String, dishes: List<LoggedDish>) {
        val now = System.currentTimeMillis()

        // 1. Insert the meal log header
        val mealLogId = mealLogDao.insertMealLog(
            MealLogEntity(uid = uid, mealType = mealType, timestamp = now)
        )

        // 2. Insert all meal log items
        val items = dishes.map { d ->
            MealLogItemEntity(
                mealLogId = mealLogId,
                foodId = d.foodId,
                dishName = d.dishNameEn,
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

        // 4. Sync to Firestore (fire-and-forget — never blocks the caller)
        //    Firestore's offline persistence queues writes automatically.
        //    Both sync methods catch exceptions internally, so this is safe.
        val mealLogEntity = MealLogEntity(mealLogId = mealLogId, uid = uid, mealType = mealType, timestamp = now)
        CoroutineScope(Dispatchers.IO).launch {
            firestoreSyncRepo.syncMealLog(uid, mealLogEntity, items)
            firestoreSyncRepo.syncDailyNutritionSummary(uid, updated)
            AutoSyncManager.triggerSync(appContext, uid)
        }
    }

    /**
     * Deletes a meal log and its child items from Firestore.
     * (For future use by DashboardScreen refactor.)
     */
    suspend fun deleteMealLog(uid: String, mealLogId: Long) {
        firestoreSyncRepo.deleteMealLog(uid, mealLogId)
    }
}

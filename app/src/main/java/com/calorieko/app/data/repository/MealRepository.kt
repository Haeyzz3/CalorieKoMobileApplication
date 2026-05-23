package com.calorieko.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.LoggedDish
import com.calorieko.app.data.model.MealLogEntity
import com.calorieko.app.data.model.MealLogItemEntity
import com.calorieko.app.data.remote.api.AutoSyncManager
import com.calorieko.app.data.remote.firestore.FirestoreAutoSyncManager
import com.calorieko.app.data.remote.firestore.FirestoreEntityType
import com.calorieko.app.data.remote.firestore.FirestorePayloadSerializer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MealRepository(
    private val db: AppDatabase,
    private val appContext: Context
) {
    private val mealLogDao = db.mealLogDao()
    private val mealLogItemDao = db.mealLogItemDao()
    private val dailyNutritionSummaryDao = db.dailyNutritionSummaryDao()
    private val outboxDao = db.firestoreOutboxDao()

    suspend fun saveMeal(uid: String, mealType: String, dishes: List<LoggedDish>) {
        val now = System.currentTimeMillis()

        db.withTransaction {
            val meal = MealLogEntity(
                uid = uid,
                mealType = mealType,
                timestamp = now,
                updatedAt = now,
                syncStatus = 0
            )
            val mealLogId = mealLogDao.insertMealLog(meal)
            val persistedMeal = meal.copy(mealLogId = mealLogId)

            val items = dishes.map { dish ->
                MealLogItemEntity(
                    mealLogId = mealLogId,
                    foodId = dish.foodId,
                    dishName = dish.dishNamePh.ifBlank { dish.dishNameEn },
                    weightGrams = dish.weightGrams,
                    calories = dish.calories,
                    protein = dish.protein,
                    carbs = dish.carbs,
                    fiber = dish.fiber,
                    sugar = dish.sugar,
                    fat = dish.fat,
                    saturatedFat = dish.saturatedFat,
                    polyunsaturatedFat = dish.polyunsaturatedFat,
                    monounsaturatedFat = dish.monounsaturatedFat,
                    transFat = dish.transFat,
                    cholesterol = dish.cholesterol,
                    sodium = dish.sodium,
                    potassium = dish.potassium,
                    vitaminA = dish.vitaminA,
                    vitaminC = dish.vitaminC,
                    calcium = dish.calcium,
                    iron = dish.iron,
                    updatedAt = now
                )
            }
            mealLogItemDao.insertItems(items)

            val today = LocalDate.now().toEpochDay()
            val existing = dailyNutritionSummaryDao.getSummaryForDate(uid, today)
            val updatedSummary = addMealToSummary(
                existing ?: DailyNutritionSummaryEntity(uid = uid, dateEpochDay = today),
                mealType,
                dishes,
                now
            )
            dailyNutritionSummaryDao.upsertSummary(updatedSummary)

            outboxDao.insert(
                FirestorePayloadSerializer.upsert(
                    uid = uid,
                    entityType = FirestoreEntityType.MEAL_LOG,
                    entityKey = persistedMeal.remoteId,
                    remotePath = FirestorePayloadSerializer.mealLogPath(uid, persistedMeal.remoteId),
                    payload = FirestorePayloadSerializer.mealPayload(persistedMeal, items),
                    now = now
                )
            )
            outboxDao.insert(
                FirestorePayloadSerializer.upsert(
                    uid = uid,
                    entityType = FirestoreEntityType.DAILY_NUTRITION_SUMMARY,
                    entityKey = updatedSummary.dateEpochDay.toString(),
                    remotePath = FirestorePayloadSerializer.dailySummaryPath(uid, updatedSummary.dateEpochDay),
                    payload = FirestorePayloadSerializer.dailySummaryPayload(updatedSummary),
                    now = now
                )
            )
        }

        triggerSync(uid)
    }

    suspend fun deleteMealLog(uid: String, mealLogId: Long): DailyNutritionSummaryEntity? {
        var updatedSummary: DailyNutritionSummaryEntity? = null
        var deleted = false

        db.withTransaction {
            val mealWithItems = mealLogDao.getMealLogWithItems(mealLogId) ?: return@withTransaction
            val meal = mealWithItems.mealLog
            val items = mealWithItems.items
            val epochDay = Instant.ofEpochMilli(meal.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toEpochDay()
            val existing = dailyNutritionSummaryDao.getSummaryForDate(uid, epochDay)
            val now = System.currentTimeMillis()

            outboxDao.insert(
                FirestorePayloadSerializer.deleteMealLogRecursive(
                    uid = uid,
                    mealRemoteId = meal.remoteId,
                    now = now
                )
            )

            if (existing != null) {
                val summary = subtractMealFromSummary(existing, meal.mealType, items, now)
                dailyNutritionSummaryDao.upsertSummary(summary)
                outboxDao.insert(
                    FirestorePayloadSerializer.upsert(
                        uid = uid,
                        entityType = FirestoreEntityType.DAILY_NUTRITION_SUMMARY,
                        entityKey = summary.dateEpochDay.toString(),
                        remotePath = FirestorePayloadSerializer.dailySummaryPath(uid, summary.dateEpochDay),
                        payload = FirestorePayloadSerializer.dailySummaryPayload(summary),
                        now = now
                    )
                )
                updatedSummary = summary
            }

            mealLogDao.deleteMealLog(mealLogId)
            deleted = true
        }

        if (deleted) triggerSync(uid)
        return updatedSummary
    }

    suspend fun deleteMealLogLocally(uid: String, mealLogId: Long): DailyNutritionSummaryEntity? {
        return deleteMealLog(uid, mealLogId)
    }

    private fun addMealToSummary(
        summary: DailyNutritionSummaryEntity,
        mealType: String,
        dishes: List<LoggedDish>,
        now: Long
    ): DailyNutritionSummaryEntity {
        val mealCalories = dishes.sumOf { it.calories.toDouble() }.toFloat()
        return summary.copy(
            updatedAt = now,
            totalCalories = summary.totalCalories + mealCalories,
            totalProtein = summary.totalProtein + dishes.sumOf { it.protein.toDouble() }.toFloat(),
            totalCarbs = summary.totalCarbs + dishes.sumOf { it.carbs.toDouble() }.toFloat(),
            totalFiber = summary.totalFiber + dishes.sumOf { it.fiber.toDouble() }.toFloat(),
            totalSugar = summary.totalSugar + dishes.sumOf { it.sugar.toDouble() }.toFloat(),
            totalFat = summary.totalFat + dishes.sumOf { it.fat.toDouble() }.toFloat(),
            totalSaturatedFat = summary.totalSaturatedFat + dishes.sumOf { it.saturatedFat.toDouble() }.toFloat(),
            totalPolyunsaturatedFat = summary.totalPolyunsaturatedFat + dishes.sumOf { it.polyunsaturatedFat.toDouble() }.toFloat(),
            totalMonounsaturatedFat = summary.totalMonounsaturatedFat + dishes.sumOf { it.monounsaturatedFat.toDouble() }.toFloat(),
            totalTransFat = summary.totalTransFat + dishes.sumOf { it.transFat.toDouble() }.toFloat(),
            totalCholesterol = summary.totalCholesterol + dishes.sumOf { it.cholesterol.toDouble() }.toFloat(),
            totalSodium = summary.totalSodium + dishes.sumOf { it.sodium.toDouble() }.toFloat(),
            totalPotassium = summary.totalPotassium + dishes.sumOf { it.potassium.toDouble() }.toFloat(),
            totalVitaminA = summary.totalVitaminA + dishes.sumOf { it.vitaminA.toDouble() }.toFloat(),
            totalVitaminC = summary.totalVitaminC + dishes.sumOf { it.vitaminC.toDouble() }.toFloat(),
            totalCalcium = summary.totalCalcium + dishes.sumOf { it.calcium.toDouble() }.toFloat(),
            totalIron = summary.totalIron + dishes.sumOf { it.iron.toDouble() }.toFloat(),
            breakfastCalories = summary.breakfastCalories + if (mealType == "Breakfast") mealCalories else 0f,
            lunchCalories = summary.lunchCalories + if (mealType == "Lunch") mealCalories else 0f,
            dinnerCalories = summary.dinnerCalories + if (mealType == "Dinner") mealCalories else 0f,
            snacksCalories = summary.snacksCalories + if (mealType == "Snacks") mealCalories else 0f
        )
    }

    private fun subtractMealFromSummary(
        summary: DailyNutritionSummaryEntity,
        mealType: String,
        items: List<MealLogItemEntity>,
        now: Long
    ): DailyNutritionSummaryEntity {
        val deletedCalories = items.sumOf { it.calories.toDouble() }.toFloat()
        return summary.copy(
            updatedAt = now,
            totalCalories = (summary.totalCalories - deletedCalories).coerceAtLeast(0f),
            totalProtein = (summary.totalProtein - items.sumOf { it.protein.toDouble() }.toFloat()).coerceAtLeast(0f),
            totalCarbs = (summary.totalCarbs - items.sumOf { it.carbs.toDouble() }.toFloat()).coerceAtLeast(0f),
            totalFiber = (summary.totalFiber - items.sumOf { it.fiber.toDouble() }.toFloat()).coerceAtLeast(0f),
            totalSugar = (summary.totalSugar - items.sumOf { it.sugar.toDouble() }.toFloat()).coerceAtLeast(0f),
            totalFat = (summary.totalFat - items.sumOf { it.fat.toDouble() }.toFloat()).coerceAtLeast(0f),
            totalSaturatedFat = (summary.totalSaturatedFat - items.sumOf { it.saturatedFat.toDouble() }.toFloat()).coerceAtLeast(0f),
            totalPolyunsaturatedFat = (summary.totalPolyunsaturatedFat - items.sumOf { it.polyunsaturatedFat.toDouble() }.toFloat()).coerceAtLeast(0f),
            totalMonounsaturatedFat = (summary.totalMonounsaturatedFat - items.sumOf { it.monounsaturatedFat.toDouble() }.toFloat()).coerceAtLeast(0f),
            totalTransFat = (summary.totalTransFat - items.sumOf { it.transFat.toDouble() }.toFloat()).coerceAtLeast(0f),
            totalCholesterol = (summary.totalCholesterol - items.sumOf { it.cholesterol.toDouble() }.toFloat()).coerceAtLeast(0f),
            totalSodium = (summary.totalSodium - items.sumOf { it.sodium.toDouble() }.toFloat()).coerceAtLeast(0f),
            totalPotassium = (summary.totalPotassium - items.sumOf { it.potassium.toDouble() }.toFloat()).coerceAtLeast(0f),
            totalVitaminA = (summary.totalVitaminA - items.sumOf { it.vitaminA.toDouble() }.toFloat()).coerceAtLeast(0f),
            totalVitaminC = (summary.totalVitaminC - items.sumOf { it.vitaminC.toDouble() }.toFloat()).coerceAtLeast(0f),
            totalCalcium = (summary.totalCalcium - items.sumOf { it.calcium.toDouble() }.toFloat()).coerceAtLeast(0f),
            totalIron = (summary.totalIron - items.sumOf { it.iron.toDouble() }.toFloat()).coerceAtLeast(0f),
            breakfastCalories = if (mealType == "Breakfast") (summary.breakfastCalories - deletedCalories).coerceAtLeast(0f) else summary.breakfastCalories,
            lunchCalories = if (mealType == "Lunch") (summary.lunchCalories - deletedCalories).coerceAtLeast(0f) else summary.lunchCalories,
            dinnerCalories = if (mealType == "Dinner") (summary.dinnerCalories - deletedCalories).coerceAtLeast(0f) else summary.dinnerCalories,
            snacksCalories = if (mealType == "Snacks") (summary.snacksCalories - deletedCalories).coerceAtLeast(0f) else summary.snacksCalories
        )
    }

    private fun triggerSync(uid: String) {
        if (uid.isBlank()) return
        FirestoreAutoSyncManager.triggerSync(appContext, uid)
        AutoSyncManager.triggerSync(appContext, uid)
    }
}

package com.calorieko.app.data.remote.firestore

import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.FirestoreOutboxEntity
import com.calorieko.app.data.model.MealLogEntity
import com.calorieko.app.data.model.MealLogItemEntity
import com.calorieko.app.data.model.PantryItem
import com.calorieko.app.data.model.PlannedMealEntity
import com.calorieko.app.data.model.UserProfile
import com.calorieko.app.data.model.WeightLogEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object FirestorePayloadSerializer {
    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, Any?>>() {}.type

    fun toJson(payload: Map<String, Any?>): String = gson.toJson(payload)

    fun fromJson(json: String): Map<String, Any?> {
        return gson.fromJson<Map<String, Any?>>(json, mapType) ?: emptyMap()
    }

    fun upsert(
        uid: String,
        entityType: String,
        entityKey: String,
        remotePath: String,
        payload: Map<String, Any?>,
        now: Long = System.currentTimeMillis()
    ): FirestoreOutboxEntity =
        FirestoreOutboxEntity(
            uid = uid,
            entityType = entityType,
            entityKey = entityKey,
            operation = FirestoreSyncOperation.UPSERT_DOCUMENT,
            remotePath = remotePath,
            payloadJson = toJson(payload),
            createdAt = now,
            updatedAt = now
        )

    fun deleteDocument(
        uid: String,
        entityType: String,
        entityKey: String,
        remotePath: String,
        now: Long = System.currentTimeMillis()
    ): FirestoreOutboxEntity =
        FirestoreOutboxEntity(
            uid = uid,
            entityType = entityType,
            entityKey = entityKey,
            operation = FirestoreSyncOperation.DELETE_DOCUMENT,
            remotePath = remotePath,
            payloadJson = null,
            createdAt = now,
            updatedAt = now
        )

    fun deleteMealLogRecursive(
        uid: String,
        mealRemoteId: String,
        now: Long = System.currentTimeMillis()
    ): FirestoreOutboxEntity =
        FirestoreOutboxEntity(
            uid = uid,
            entityType = FirestoreEntityType.MEAL_LOG,
            entityKey = mealRemoteId,
            operation = FirestoreSyncOperation.DELETE_MEAL_LOG_RECURSIVE,
            remotePath = mealLogPath(uid, mealRemoteId),
            payloadJson = null,
            createdAt = now,
            updatedAt = now
        )

    fun clearCollection(
        uid: String,
        collectionKey: String,
        remotePath: String,
        filters: Map<String, Any?> = emptyMap(),
        now: Long = System.currentTimeMillis()
    ): FirestoreOutboxEntity =
        FirestoreOutboxEntity(
            uid = uid,
            entityType = FirestoreEntityType.COLLECTION,
            entityKey = collectionKey,
            operation = FirestoreSyncOperation.CLEAR_COLLECTION,
            remotePath = remotePath,
            payloadJson = toJson(mapOf("filters" to filters)),
            createdAt = now,
            updatedAt = now
        )

    fun userProfilePath(uid: String): String = "users/$uid"
    fun weightLogPath(uid: String, timestamp: Long): String = "users/$uid/weightLogs/$timestamp"
    fun activityPath(uid: String, remoteId: String): String = "users/$uid/activityLogs/$remoteId"
    fun mealLogPath(uid: String, remoteId: String): String = "users/$uid/mealLogs/$remoteId"
    fun dailySummaryPath(uid: String, dateEpochDay: Long): String = "users/$uid/dailyNutritionSummaries/$dateEpochDay"
    fun pantryItemPath(uid: String, ingredientName: String): String = "users/$uid/pantryItems/$ingredientName"
    fun pantryCollectionPath(uid: String): String = "users/$uid/pantryItems"
    fun plannedMealCollectionPath(uid: String): String = "users/$uid/plannedMeals"
    fun plannedMealPath(uid: String, meal: PlannedMealEntity): String = "users/$uid/plannedMeals/${plannedMealDocumentId(meal)}"

    fun plannedMealDocumentId(meal: PlannedMealEntity): String =
        "${meal.dayIndex}_${meal.weekStartDate}_${meal.mealSlot}_${meal.dishLabel}"

    fun profilePayload(profile: UserProfile): Map<String, Any?> =
        mapOf(
            "name" to profile.name,
            "email" to profile.email,
            "age" to profile.age,
            "weight" to profile.weight,
            "height" to profile.height,
            "sex" to profile.sex,
            "activityLevel" to profile.activityLevel,
            "goal" to profile.goal,
            "streak" to profile.streak,
            "level" to profile.level,
            "globalXp" to profile.globalXp,
            "milestonesTier" to profile.milestonesTier,
            "photoUrl" to profile.photoUrl,
            "onboardingCompleted" to profile.onboardingCompleted,
            "updatedAt" to profile.updatedAt
        )

    fun weightLogPayload(log: WeightLogEntity): Map<String, Any?> =
        mapOf(
            "dateEpochDay" to log.dateEpochDay,
            "weightKg" to log.weightKg,
            "timestamp" to log.timestamp,
            "updatedAt" to log.updatedAt
        )

    fun activityPayload(log: ActivityLogEntity): Map<String, Any?> =
        mapOf(
            "remoteId" to log.remoteId,
            "type" to log.type,
            "name" to log.name,
            "timeString" to log.timeString,
            "weightOrDuration" to log.weightOrDuration,
            "calories" to log.calories,
            "protein" to log.protein,
            "carbs" to log.carbs,
            "fats" to log.fats,
            "sodium" to log.sodium,
            "timestamp" to log.timestamp,
            "distanceKm" to log.distanceKm,
            "pace" to log.pace,
            "movingTimeSeconds" to log.movingTimeSeconds,
            "steps" to log.steps,
            "mapType" to log.mapType,
            "notes" to log.notes,
            "activityTag" to log.activityTag,
            "photoUri" to log.photoUri,
            "encodedPath" to log.encodedPath,
            "updatedAt" to log.updatedAt
        )

    fun mealPayload(meal: MealLogEntity, items: List<MealLogItemEntity>): Map<String, Any?> =
        mapOf(
            "meal" to mapOf(
                "remoteId" to meal.remoteId,
                "mealType" to meal.mealType,
                "timestamp" to meal.timestamp,
                "notes" to meal.notes,
                "updatedAt" to meal.updatedAt
            ),
            "items" to items.map { item ->
                mapOf(
                    "remoteId" to item.remoteId,
                    "payload" to mealItemPayload(item)
                )
            }
        )

    fun mealItemPayload(item: MealLogItemEntity): Map<String, Any?> =
        mapOf(
            "remoteId" to item.remoteId,
            "foodId" to item.foodId,
            "dishName" to item.dishName,
            "weightGrams" to item.weightGrams,
            "calories" to item.calories,
            "protein" to item.protein,
            "carbs" to item.carbs,
            "fiber" to item.fiber,
            "sugar" to item.sugar,
            "fat" to item.fat,
            "saturatedFat" to item.saturatedFat,
            "polyunsaturatedFat" to item.polyunsaturatedFat,
            "monounsaturatedFat" to item.monounsaturatedFat,
            "transFat" to item.transFat,
            "cholesterol" to item.cholesterol,
            "sodium" to item.sodium,
            "potassium" to item.potassium,
            "vitaminA" to item.vitaminA,
            "vitaminC" to item.vitaminC,
            "calcium" to item.calcium,
            "iron" to item.iron,
            "updatedAt" to item.updatedAt
        )

    fun dailySummaryPayload(summary: DailyNutritionSummaryEntity): Map<String, Any?> =
        mapOf(
            "dateEpochDay" to summary.dateEpochDay,
            "totalCalories" to summary.totalCalories,
            "totalProtein" to summary.totalProtein,
            "totalCarbs" to summary.totalCarbs,
            "totalFiber" to summary.totalFiber,
            "totalSugar" to summary.totalSugar,
            "totalFat" to summary.totalFat,
            "totalSaturatedFat" to summary.totalSaturatedFat,
            "totalPolyunsaturatedFat" to summary.totalPolyunsaturatedFat,
            "totalMonounsaturatedFat" to summary.totalMonounsaturatedFat,
            "totalTransFat" to summary.totalTransFat,
            "totalCholesterol" to summary.totalCholesterol,
            "totalSodium" to summary.totalSodium,
            "totalPotassium" to summary.totalPotassium,
            "totalVitaminA" to summary.totalVitaminA,
            "totalVitaminC" to summary.totalVitaminC,
            "totalCalcium" to summary.totalCalcium,
            "totalIron" to summary.totalIron,
            "breakfastCalories" to summary.breakfastCalories,
            "lunchCalories" to summary.lunchCalories,
            "dinnerCalories" to summary.dinnerCalories,
            "snacksCalories" to summary.snacksCalories,
            "updatedAt" to summary.updatedAt
        )

    fun pantryPayload(item: PantryItem, now: Long = System.currentTimeMillis()): Map<String, Any?> =
        mapOf(
            "ingredientName" to item.ingredientName,
            "addedAt" to now
        )

    fun plannedMealPayload(meal: PlannedMealEntity): Map<String, Any?> =
        mapOf(
            "dayIndex" to meal.dayIndex,
            "dishLabel" to meal.dishLabel,
            "weekStartDate" to meal.weekStartDate,
            "mealSlot" to meal.mealSlot,
            "substitutionsJson" to meal.substitutionsJson,
            "scaledServings" to meal.scaledServings,
            "tweaksJson" to meal.tweaksJson
        )
}

package com.calorieko.app.data.remote

import android.util.Log
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.MealLogEntity
import com.calorieko.app.data.model.MealLogItemEntity
import com.calorieko.app.data.model.PlannedMealEntity
import com.calorieko.app.data.model.UserProfile
import com.calorieko.app.data.model.WeightLogEntity
import com.calorieko.app.data.remote.firestore.FirestorePayloadSerializer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreSyncRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "FirestoreSync"
        private const val USERS_COLLECTION = "users"
    }

    suspend fun syncProfile(uid: String, profile: UserProfile) {
        userDocument(uid)
            .set(FirestorePayloadSerializer.profilePayload(profile), SetOptions.merge())
            .await()
    }

    suspend fun syncWeightLog(uid: String, log: WeightLogEntity) {
        userDocument(uid)
            .collection("weightLogs")
            .document(log.timestamp.toString())
            .set(FirestorePayloadSerializer.weightLogPayload(log))
            .await()
    }

    suspend fun syncWeightLogsBatch(uid: String, logs: List<WeightLogEntity>) {
        val collection = userDocument(uid).collection("weightLogs")
        logs.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { log ->
                batch.set(collection.document(log.timestamp.toString()), FirestorePayloadSerializer.weightLogPayload(log))
            }
            batch.commit().await()
        }
    }

    suspend fun syncActivityLog(uid: String, log: ActivityLogEntity) {
        userDocument(uid)
            .collection("activityLogs")
            .document(log.remoteId)
            .set(FirestorePayloadSerializer.activityPayload(log))
            .await()
    }

    suspend fun syncActivityLogsBatch(uid: String, logs: List<ActivityLogEntity>) {
        val collection = userDocument(uid).collection("activityLogs")
        logs.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { log ->
                batch.set(collection.document(log.remoteId), FirestorePayloadSerializer.activityPayload(log))
            }
            batch.commit().await()
        }
    }

    suspend fun syncMealLog(uid: String, mealLog: MealLogEntity, items: List<MealLogItemEntity>) {
        val mealDocument = userDocument(uid).collection("mealLogs").document(mealLog.remoteId)
        val payload = FirestorePayloadSerializer.mealPayload(mealLog, items)
        val mealPayload = payload["meal"] as Map<*, *>
        val itemPayloads = payload["items"] as List<*>

        if (items.size + 1 <= 500) {
            val batch = firestore.batch()
            batch.set(mealDocument, mealPayload)
            itemPayloads.forEach { item ->
                val itemMap = item as Map<*, *>
                val remoteId = itemMap["remoteId"].toString()
                val itemData = itemMap["payload"] as Map<*, *>
                batch.set(mealDocument.collection("items").document(remoteId), itemData)
            }
            batch.commit().await()
            return
        }

        mealDocument.set(mealPayload).await()
        itemPayloads.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { item ->
                val itemMap = item as Map<*, *>
                val remoteId = itemMap["remoteId"].toString()
                val itemData = itemMap["payload"] as Map<*, *>
                batch.set(mealDocument.collection("items").document(remoteId), itemData)
            }
            batch.commit().await()
        }
    }

    suspend fun deleteMealLog(uid: String, mealLogId: Long) {
        deleteMealLogRecursive(uid, mealLogId.toString())
    }

    suspend fun deleteMealLogRecursive(uid: String, mealRemoteId: String) {
        val mealDocument = userDocument(uid).collection("mealLogs").document(mealRemoteId)
        val itemSnapshot = mealDocument.collection("items").get().await()
        itemSnapshot.documents.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
        mealDocument.delete().await()
    }

    suspend fun syncDailyNutritionSummary(uid: String, summary: DailyNutritionSummaryEntity) {
        userDocument(uid)
            .collection("dailyNutritionSummaries")
            .document(summary.dateEpochDay.toString())
            .set(FirestorePayloadSerializer.dailySummaryPayload(summary))
            .await()
    }

    suspend fun syncDailyNutritionSummariesBatch(uid: String, summaries: List<DailyNutritionSummaryEntity>) {
        val collection = userDocument(uid).collection("dailyNutritionSummaries")
        summaries.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { summary ->
                batch.set(collection.document(summary.dateEpochDay.toString()), FirestorePayloadSerializer.dailySummaryPayload(summary))
            }
            batch.commit().await()
        }
    }

    suspend fun syncPantryItem(uid: String, ingredientName: String) {
        userDocument(uid)
            .collection("pantryItems")
            .document(ingredientName)
            .set(mapOf("ingredientName" to ingredientName, "addedAt" to System.currentTimeMillis()))
            .await()
    }

    suspend fun syncPantryItemsBatch(uid: String, itemNames: List<String>) {
        val collection = userDocument(uid).collection("pantryItems")
        val now = System.currentTimeMillis()
        itemNames.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { name ->
                batch.set(collection.document(name), mapOf("ingredientName" to name, "addedAt" to now))
            }
            batch.commit().await()
        }
    }

    suspend fun deletePantryItem(uid: String, ingredientName: String) {
        userDocument(uid)
            .collection("pantryItems")
            .document(ingredientName)
            .delete()
            .await()
    }

    suspend fun syncPlannedMeal(uid: String, meal: PlannedMealEntity) {
        userDocument(uid)
            .collection("plannedMeals")
            .document(FirestorePayloadSerializer.plannedMealDocumentId(meal))
            .set(FirestorePayloadSerializer.plannedMealPayload(meal))
            .await()
    }

    suspend fun syncPlannedMealsBatch(uid: String, meals: List<PlannedMealEntity>) {
        val collection = userDocument(uid).collection("plannedMeals")
        meals.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { meal ->
                batch.set(
                    collection.document(FirestorePayloadSerializer.plannedMealDocumentId(meal)),
                    FirestorePayloadSerializer.plannedMealPayload(meal)
                )
            }
            batch.commit().await()
        }
    }

    suspend fun deletePlannedMeal(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String, dishLabel: String) {
        val meal = PlannedMealEntity(dayIndex = dayIndex, dishLabel = dishLabel, weekStartDate = weekStartDate, mealSlot = mealSlot)
        userDocument(uid)
            .collection("plannedMeals")
            .document(FirestorePayloadSerializer.plannedMealDocumentId(meal))
            .delete()
            .await()
    }

    suspend fun deletePlannedMealSlot(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String) {
        clearQuery(
            userDocument(uid).collection("plannedMeals")
                .whereEqualTo("dayIndex", dayIndex)
                .whereEqualTo("weekStartDate", weekStartDate)
                .whereEqualTo("mealSlot", mealSlot)
        )
    }

    suspend fun clearWeekPlannedMeals(uid: String, weekStartDate: String) {
        clearQuery(userDocument(uid).collection("plannedMeals").whereEqualTo("weekStartDate", weekStartDate))
    }

    suspend fun clearDayPlannedMeals(uid: String, dayIndex: Int, weekStartDate: String) {
        clearQuery(
            userDocument(uid).collection("plannedMeals")
                .whereEqualTo("dayIndex", dayIndex)
                .whereEqualTo("weekStartDate", weekStartDate)
        )
    }

    suspend fun clearPantryItems(uid: String) {
        clearQuery(userDocument(uid).collection("pantryItems"))
    }

    suspend fun clearAllPlannedMeals(uid: String) {
        clearQuery(userDocument(uid).collection("plannedMeals"))
    }

    suspend fun wipeAllUserData(uid: String) {
        val userDoc = userDocument(uid)
        clearQuery(userDoc.collection("activityLogs"))
        clearMealLogs(uid)
        clearQuery(userDoc.collection("dailyNutritionSummaries"))
        clearQuery(userDoc.collection("pantryItems"))
        clearQuery(userDoc.collection("plannedMeals"))
        clearQuery(userDoc.collection("weightLogs"))
    }

    suspend fun deleteUserAccount(uid: String) {
        wipeAllUserData(uid)
        userDocument(uid).delete().await()
    }

    suspend fun fetchProfile(uid: String): UserProfile? {
        val doc = userDocument(uid).get().await()
        if (!doc.exists()) return null
        return UserProfile(
            uid = uid,
            name = doc.getString("name") ?: "",
            email = doc.getString("email") ?: "",
            age = (doc.get("age") as? Number)?.toInt() ?: 25,
            weight = (doc.get("weight") as? Number)?.toDouble() ?: 70.0,
            height = (doc.get("height") as? Number)?.toDouble() ?: 170.0,
            sex = doc.getString("sex") ?: "",
            activityLevel = doc.getString("activityLevel") ?: "",
            goal = doc.getString("goal") ?: "general",
            streak = (doc.get("streak") as? Number)?.toInt() ?: 0,
            level = (doc.get("level") as? Number)?.toInt() ?: 1,
            globalXp = (doc.get("globalXp") as? Number)?.toInt() ?: 0,
            milestonesTier = (doc.get("milestonesTier") as? Number)?.toInt() ?: 1,
            photoUrl = doc.getString("photoUrl") ?: "",
            onboardingCompleted = doc.getBoolean("onboardingCompleted") ?: false,
            updatedAt = (doc.get("updatedAt") as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    suspend fun fetchActivityLogs(uid: String): List<ActivityLogEntity> {
        val snapshot = userDocument(uid).collection("activityLogs").get().await()
        return snapshot.documents.mapNotNull { doc ->
            runCatching {
                ActivityLogEntity(
                    id = 0,
                    uid = uid,
                    remoteId = doc.id,
                    type = doc.getString("type") ?: "workout",
                    name = doc.getString("name") ?: "",
                    timeString = doc.getString("timeString") ?: "",
                    weightOrDuration = doc.getString("weightOrDuration") ?: "",
                    calories = (doc.get("calories") as? Number)?.toInt() ?: 0,
                    protein = (doc.get("protein") as? Number)?.toInt() ?: 0,
                    carbs = (doc.get("carbs") as? Number)?.toInt() ?: 0,
                    fats = (doc.get("fats") as? Number)?.toInt() ?: 0,
                    sodium = (doc.get("sodium") as? Number)?.toInt() ?: 0,
                    timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: 0L,
                    distanceKm = (doc.get("distanceKm") as? Number)?.toDouble(),
                    pace = (doc.get("pace") as? Number)?.toDouble(),
                    movingTimeSeconds = (doc.get("movingTimeSeconds") as? Number)?.toLong(),
                    steps = (doc.get("steps") as? Number)?.toInt(),
                    mapType = doc.getString("mapType"),
                    notes = doc.getString("notes"),
                    activityTag = doc.getString("activityTag"),
                    encodedPath = doc.getString("encodedPath"),
                    photoUri = doc.getString("photoUri"),
                    updatedAt = (doc.get("updatedAt") as? Number)?.toLong() ?: 0L,
                    syncStatus = 1
                )
            }.onFailure { Log.w(TAG, "Skipping malformed activity log ${doc.id}", it) }.getOrNull()
        }
    }

    suspend fun fetchWeightLogs(uid: String): List<WeightLogEntity> {
        val snapshot = userDocument(uid).collection("weightLogs").get().await()
        return snapshot.documents.mapNotNull { doc ->
            runCatching {
                val epochDay = (doc.get("dateEpochDay") as? Number)?.toLong()
                    ?: doc.id.toLongOrNull()
                    ?: return@mapNotNull null
                val documentTimestamp = doc.id.toLongOrNull()?.takeIf { it > 1_000_000_000_000L }
                val timestamp = (doc.get("timestamp") as? Number)?.toLong()
                    ?: documentTimestamp
                    ?: epochDay * 86_400_000L
                WeightLogEntity(
                    uid = uid,
                    dateEpochDay = epochDay,
                    weightKg = (doc.get("weightKg") as? Number)?.toDouble()
                        ?: (doc.get("weight_kg") as? Number)?.toDouble()
                        ?: return@mapNotNull null,
                    timestamp = timestamp,
                    updatedAt = (doc.get("updatedAt") as? Number)?.toLong()
                        ?: (doc.get("updated_at") as? Number)?.toLong()
                        ?: 0L,
                    syncStatus = 1
                )
            }.onFailure { Log.w(TAG, "Skipping malformed weight log ${doc.id}", it) }.getOrNull()
        }
    }

    suspend fun fetchMealLogs(uid: String): List<Pair<MealLogEntity, List<MealLogItemEntity>>> {
        val mealSnapshot = userDocument(uid).collection("mealLogs").get().await()
        val results = mutableListOf<Pair<MealLogEntity, List<MealLogItemEntity>>>()

        for (mealDoc in mealSnapshot.documents) {
            val mealLog = runCatching {
                MealLogEntity(
                    mealLogId = 0,
                    uid = uid,
                    remoteId = mealDoc.id,
                    mealType = mealDoc.getString("mealType") ?: "Snacks",
                    timestamp = (mealDoc.get("timestamp") as? Number)?.toLong() ?: 0L,
                    notes = mealDoc.getString("notes"),
                    updatedAt = (mealDoc.get("updatedAt") as? Number)?.toLong() ?: 0L,
                    syncStatus = 1
                )
            }.onFailure { Log.w(TAG, "Skipping malformed meal log ${mealDoc.id}", it) }.getOrNull() ?: continue

            val itemsSnapshot = mealDoc.reference.collection("items").get().await()
            val items = itemsSnapshot.documents.mapNotNull { itemDoc ->
                runCatching {
                    MealLogItemEntity(
                        mealLogItemId = 0,
                        mealLogId = 0,
                        remoteId = itemDoc.id,
                        foodId = (itemDoc.get("foodId") as? Number)?.toInt() ?: 0,
                        dishName = itemDoc.getString("dishName") ?: "",
                        weightGrams = (itemDoc.get("weightGrams") as? Number)?.toFloat() ?: 0f,
                        calories = (itemDoc.get("calories") as? Number)?.toFloat() ?: 0f,
                        protein = (itemDoc.get("protein") as? Number)?.toFloat() ?: 0f,
                        carbs = (itemDoc.get("carbs") as? Number)?.toFloat() ?: 0f,
                        fiber = (itemDoc.get("fiber") as? Number)?.toFloat() ?: 0f,
                        sugar = (itemDoc.get("sugar") as? Number)?.toFloat() ?: 0f,
                        fat = (itemDoc.get("fat") as? Number)?.toFloat() ?: 0f,
                        saturatedFat = (itemDoc.get("saturatedFat") as? Number)?.toFloat() ?: 0f,
                        polyunsaturatedFat = (itemDoc.get("polyunsaturatedFat") as? Number)?.toFloat() ?: 0f,
                        monounsaturatedFat = (itemDoc.get("monounsaturatedFat") as? Number)?.toFloat() ?: 0f,
                        transFat = (itemDoc.get("transFat") as? Number)?.toFloat() ?: 0f,
                        cholesterol = (itemDoc.get("cholesterol") as? Number)?.toFloat() ?: 0f,
                        sodium = (itemDoc.get("sodium") as? Number)?.toFloat() ?: 0f,
                        potassium = (itemDoc.get("potassium") as? Number)?.toFloat() ?: 0f,
                        vitaminA = (itemDoc.get("vitaminA") as? Number)?.toFloat() ?: 0f,
                        vitaminC = (itemDoc.get("vitaminC") as? Number)?.toFloat() ?: 0f,
                        calcium = (itemDoc.get("calcium") as? Number)?.toFloat() ?: 0f,
                        iron = (itemDoc.get("iron") as? Number)?.toFloat() ?: 0f,
                        updatedAt = (itemDoc.get("updatedAt") as? Number)?.toLong() ?: 0L
                    )
                }.onFailure { Log.w(TAG, "Skipping malformed meal item ${itemDoc.id}", it) }.getOrNull()
            }

            results.add(mealLog to items)
        }

        return results
    }

    suspend fun fetchDailyNutritionSummaries(uid: String): List<DailyNutritionSummaryEntity> {
        val snapshot = userDocument(uid).collection("dailyNutritionSummaries").get().await()
        return snapshot.documents.mapNotNull { doc ->
            runCatching {
                DailyNutritionSummaryEntity(
                    id = 0,
                    uid = uid,
                    dateEpochDay = (doc.get("dateEpochDay") as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: return@mapNotNull null,
                    totalCalories = (doc.get("totalCalories") as? Number)?.toFloat() ?: 0f,
                    totalProtein = (doc.get("totalProtein") as? Number)?.toFloat() ?: 0f,
                    totalCarbs = (doc.get("totalCarbs") as? Number)?.toFloat() ?: 0f,
                    totalFiber = (doc.get("totalFiber") as? Number)?.toFloat() ?: 0f,
                    totalSugar = (doc.get("totalSugar") as? Number)?.toFloat() ?: 0f,
                    totalFat = (doc.get("totalFat") as? Number)?.toFloat() ?: 0f,
                    totalSaturatedFat = (doc.get("totalSaturatedFat") as? Number)?.toFloat() ?: 0f,
                    totalPolyunsaturatedFat = (doc.get("totalPolyunsaturatedFat") as? Number)?.toFloat() ?: 0f,
                    totalMonounsaturatedFat = (doc.get("totalMonounsaturatedFat") as? Number)?.toFloat() ?: 0f,
                    totalTransFat = (doc.get("totalTransFat") as? Number)?.toFloat() ?: 0f,
                    totalCholesterol = (doc.get("totalCholesterol") as? Number)?.toFloat() ?: 0f,
                    totalSodium = (doc.get("totalSodium") as? Number)?.toFloat() ?: 0f,
                    totalPotassium = (doc.get("totalPotassium") as? Number)?.toFloat() ?: 0f,
                    totalVitaminA = (doc.get("totalVitaminA") as? Number)?.toFloat() ?: 0f,
                    totalVitaminC = (doc.get("totalVitaminC") as? Number)?.toFloat() ?: 0f,
                    totalCalcium = (doc.get("totalCalcium") as? Number)?.toFloat() ?: 0f,
                    totalIron = (doc.get("totalIron") as? Number)?.toFloat() ?: 0f,
                    breakfastCalories = (doc.get("breakfastCalories") as? Number)?.toFloat() ?: 0f,
                    lunchCalories = (doc.get("lunchCalories") as? Number)?.toFloat() ?: 0f,
                    dinnerCalories = (doc.get("dinnerCalories") as? Number)?.toFloat() ?: 0f,
                    snacksCalories = (doc.get("snacksCalories") as? Number)?.toFloat() ?: 0f,
                    updatedAt = (doc.get("updatedAt") as? Number)?.toLong() ?: 0L
                )
            }.onFailure { Log.w(TAG, "Skipping malformed nutrition summary ${doc.id}", it) }.getOrNull()
        }
    }

    suspend fun fetchPantryItems(uid: String): List<String> {
        return userDocument(uid).collection("pantryItems").get().await().documents.map { it.id }
    }

    suspend fun fetchPlannedMeals(uid: String): List<PlannedMealEntity> {
        val snapshot = userDocument(uid).collection("plannedMeals").get().await()
        return snapshot.documents.mapNotNull { doc ->
            runCatching {
                PlannedMealEntity(
                    dayIndex = (doc.get("dayIndex") as? Number)?.toInt() ?: return@mapNotNull null,
                    dishLabel = doc.getString("dishLabel") ?: return@mapNotNull null,
                    weekStartDate = doc.getString("weekStartDate") ?: return@mapNotNull null,
                    mealSlot = doc.getString("mealSlot") ?: "Lunch",
                    substitutionsJson = doc.getString("substitutionsJson") ?: "",
                    scaledServings = (doc.get("scaledServings") as? Number)?.toInt() ?: 0,
                    tweaksJson = doc.getString("tweaksJson") ?: ""
                )
            }.onFailure { Log.w(TAG, "Skipping malformed planned meal ${doc.id}", it) }.getOrNull()
        }
    }

    private fun userDocument(uid: String) = firestore.collection(USERS_COLLECTION).document(uid)

    private suspend fun clearMealLogs(uid: String) {
        val mealLogs = userDocument(uid).collection("mealLogs").get().await()
        for (mealDoc in mealLogs.documents) {
            deleteMealLogRecursive(uid, mealDoc.id)
        }
    }

    private suspend fun clearQuery(query: Query) {
        val snapshot = query.get().await()
        snapshot.documents.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }
}

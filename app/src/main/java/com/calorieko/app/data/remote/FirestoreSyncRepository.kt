package com.calorieko.app.data.remote

import android.util.Log
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.MealLogEntity
import com.calorieko.app.data.model.MealLogItemEntity
import com.calorieko.app.data.model.PlannedMealEntity
import com.calorieko.app.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Central repository for pushing local Room data to Cloud Firestore.
 *
 * Design principles:
 * - **Write-through cache:** Every local Room write is mirrored to Firestore.
 * - **Fire-and-forget:** All methods catch exceptions internally.
 *   A failed Firestore write never crashes the app or blocks the UI.
 * - **Room stays the source of truth** for reads. Firestore is write-only in this phase.
 *
 * Document structure:
 * ```
 * users/{uid}                          → profile fields
 * users/{uid}/activityLogs/{id}        → workout/activity data
 * users/{uid}/mealLogs/{mealLogId}     → meal log header
 * users/{uid}/mealLogs/{mealLogId}/items/{itemId}  → individual dishes
 * users/{uid}/dailyNutritionSummaries/{dateEpochDay} → daily aggregate
 * users/{uid}/pantryItems/{ingredientName}           → pantry entry
 * users/{uid}/plannedMeals/{dayIndex_weekStartDate}  → meal plan entry
 * ```
 */
class FirestoreSyncRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "FirestoreSync"
        private const val USERS_COLLECTION = "users"
    }

    // ════════════════════════════════════════════════════════════
    //  USER PROFILE
    // ════════════════════════════════════════════════════════════

    /**
     * Syncs the user profile to Firestore using merge-set so that
     * partial updates don't wipe existing fields (e.g. future fields
     * added on the console side).
     */
    suspend fun syncProfile(uid: String, profile: UserProfile) {
        try {
            val data = hashMapOf(
                "name" to profile.name,
                "email" to profile.email,
                "age" to profile.age,
                "weight" to profile.weight,
                "height" to profile.height,
                "sex" to profile.sex,
                "activityLevel" to profile.activityLevel,
                "goal" to profile.goal,
                "streak" to profile.streak,
                "level" to profile.level
            )
            db.collection(USERS_COLLECTION)
                .document(uid)
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Profile synced for $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync profile for $uid", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    //  ACTIVITY LOGS (Workouts)
    // ════════════════════════════════════════════════════════════

    /**
     * Syncs a single activity log entry to Firestore.
     * Intentionally excludes `encodedPath` (GPS coordinates can be very large).
     */
    suspend fun syncActivityLog(uid: String, log: ActivityLogEntity) {
        try {
            val data = hashMapOf<String, Any?>(
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
                "mapType" to log.mapType,
                "notes" to log.notes,
                "activityTag" to log.activityTag,
                "photoUri" to log.photoUri
            )
            db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("activityLogs")
                .document(log.id.toString())
                .set(data)
                .await()
            Log.d(TAG, "Activity log ${log.id} synced for $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync activity log ${log.id}", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    //  MEAL LOGS
    // ════════════════════════════════════════════════════════════

    /**
     * Syncs a meal log header and all its child items to Firestore.
     * Items are stored as a sub-collection under the meal log document.
     */
    suspend fun syncMealLog(uid: String, mealLog: MealLogEntity, items: List<MealLogItemEntity>) {
        try {
            val mealDocRef = db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("mealLogs")
                .document(mealLog.mealLogId.toString())

            // 1. Write the meal log header
            val mealData = hashMapOf<String, Any?>(
                "mealType" to mealLog.mealType,
                "timestamp" to mealLog.timestamp,
                "notes" to mealLog.notes
            )
            mealDocRef.set(mealData).await()

            // 2. Write each item as a sub-document
            for (item in items) {
                val itemData = hashMapOf<String, Any?>(
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
                    "iron" to item.iron
                )
                mealDocRef.collection("items")
                    .document(item.mealLogItemId.toString())
                    .set(itemData)
                    .await()
            }
            Log.d(TAG, "Meal log ${mealLog.mealLogId} synced with ${items.size} items for $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync meal log ${mealLog.mealLogId}", e)
        }
    }

    /**
     * Deletes a meal log and all its child item documents from Firestore.
     */
    suspend fun deleteMealLog(uid: String, mealLogId: Long) {
        try {
            val mealDocRef = db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("mealLogs")
                .document(mealLogId.toString())

            // Delete all items in the sub-collection first
            val itemSnapshots = mealDocRef.collection("items").get().await()
            for (doc in itemSnapshots.documents) {
                doc.reference.delete().await()
            }

            // Delete the parent meal log document
            mealDocRef.delete().await()
            Log.d(TAG, "Meal log $mealLogId deleted from Firestore for $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete meal log $mealLogId", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    //  DAILY NUTRITION SUMMARIES
    // ════════════════════════════════════════════════════════════

    /**
     * Syncs (upserts) the daily nutrition summary for a given date.
     * Uses `dateEpochDay` as document ID for natural uniqueness.
     */
    suspend fun syncDailyNutritionSummary(uid: String, summary: DailyNutritionSummaryEntity) {
        try {
            val data = hashMapOf<String, Any?>(
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
                "snacksCalories" to summary.snacksCalories
            )
            db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("dailyNutritionSummaries")
                .document(summary.dateEpochDay.toString())
                .set(data)
                .await()
            Log.d(TAG, "Daily nutrition summary synced for day ${summary.dateEpochDay}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync daily nutrition summary", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    //  PANTRY ITEMS
    // ════════════════════════════════════════════════════════════

    /**
     * Syncs a pantry item to Firestore. The document body is empty
     * because the ingredient name IS the document ID.
     */
    suspend fun syncPantryItem(uid: String, ingredientName: String) {
        try {
            db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("pantryItems")
                .document(ingredientName)
                .set(hashMapOf("addedAt" to System.currentTimeMillis()))
                .await()
            Log.d(TAG, "Pantry item '$ingredientName' synced for $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync pantry item '$ingredientName'", e)
        }
    }

    /**
     * Deletes a pantry item document from Firestore.
     */
    suspend fun deletePantryItem(uid: String, ingredientName: String) {
        try {
            db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("pantryItems")
                .document(ingredientName)
                .delete()
                .await()
            Log.d(TAG, "Pantry item '$ingredientName' deleted from Firestore for $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete pantry item '$ingredientName'", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    //  PLANNED MEALS
    // ════════════════════════════════════════════════════════════

    /**
     * Syncs a planned meal to Firestore.
     * Document ID is a composite key: `{dayIndex}_{weekStartDate}`.
     */
    suspend fun syncPlannedMeal(uid: String, meal: PlannedMealEntity) {
        try {
            val docId = "${meal.dayIndex}_${meal.weekStartDate}"
            val data = hashMapOf<String, Any?>(
                "dayIndex" to meal.dayIndex,
                "dishLabel" to meal.dishLabel,
                "weekStartDate" to meal.weekStartDate
            )
            db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("plannedMeals")
                .document(docId)
                .set(data)
                .await()
            Log.d(TAG, "Planned meal '$docId' synced for $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync planned meal", e)
        }
    }

    /**
     * Deletes a single planned meal document from Firestore.
     */
    suspend fun deletePlannedMeal(uid: String, dayIndex: Int, weekStartDate: String) {
        try {
            val docId = "${dayIndex}_${weekStartDate}"
            db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("plannedMeals")
                .document(docId)
                .delete()
                .await()
            Log.d(TAG, "Planned meal '$docId' deleted from Firestore for $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete planned meal", e)
        }
    }

    /**
     * Batch-deletes all planned meals for a given week from Firestore.
     * Called when the user triggers `clearWeek()` in the meal plan.
     */
    suspend fun clearWeekPlannedMeals(uid: String, weekStartDate: String) {
        try {
            val snapshot = db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("plannedMeals")
                .whereEqualTo("weekStartDate", weekStartDate)
                .get()
                .await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
            Log.d(TAG, "Cleared ${snapshot.size()} planned meals for week $weekStartDate")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear week planned meals", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    //  WIPE ALL USER DATA
    // ════════════════════════════════════════════════════════════

    /**
     * Deletes all sub-collections under a user document.
     * Called when the user chooses "Wipe Profile Data" in Settings.
     *
     * Note: Firestore doesn't support recursive deletes natively on the client.
     * We iterate each known sub-collection and delete all documents.
     * The user profile document itself is preserved (just cleared).
     */
    suspend fun wipeAllUserData(uid: String) {
        try {
            val userDocRef = db.collection(USERS_COLLECTION).document(uid)
            val subCollections = listOf(
                "activityLogs",
                "mealLogs",
                "dailyNutritionSummaries",
                "pantryItems",
                "plannedMeals"
            )

            for (collectionName in subCollections) {
                val snapshot = userDocRef.collection(collectionName).get().await()
                for (doc in snapshot.documents) {
                    // For mealLogs, also delete the nested 'items' sub-collection
                    if (collectionName == "mealLogs") {
                        val itemsSnapshot = doc.reference.collection("items").get().await()
                        for (itemDoc in itemsSnapshot.documents) {
                            itemDoc.reference.delete().await()
                        }
                    }
                    doc.reference.delete().await()
                }
            }
            Log.d(TAG, "All user data wiped from Firestore for $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to wipe user data from Firestore", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    //  FETCH METHODS (Pull from Cloud)
    // ════════════════════════════════════════════════════════════

    /**
     * Fetches the user profile from Firestore.
     * Returns null if the document doesn't exist or on error.
     */
    suspend fun fetchProfile(uid: String): UserProfile? {
        return try {
            val doc = db.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()

            if (!doc.exists()) {
                Log.d(TAG, "No Firestore profile found for $uid")
                return null
            }

            UserProfile(
                uid = uid,
                name = doc.getString("name") ?: "",
                email = doc.getString("email") ?: "",
                age = (doc.getLong("age") ?: 25).toInt(),
                weight = doc.getDouble("weight") ?: 70.0,
                height = doc.getDouble("height") ?: 170.0,
                sex = doc.getString("sex") ?: "",
                activityLevel = doc.getString("activityLevel") ?: "",
                goal = doc.getString("goal") ?: "general",
                streak = (doc.getLong("streak") ?: 0).toInt(),
                level = (doc.getLong("level") ?: 1).toInt()
            ).also { Log.d(TAG, "Profile fetched for $uid") }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch profile for $uid", e)
            null
        }
    }

    /**
     * Fetches all activity logs from Firestore for the given user.
     * Note: `encodedPath` was intentionally not synced,
     * so it will be null/default in restored entries.
     */
    suspend fun fetchActivityLogs(uid: String): List<ActivityLogEntity> {
        return try {
            val snapshot = db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("activityLogs")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    ActivityLogEntity(
                        id = 0, // Room will auto-generate a new ID
                        uid = uid,
                        type = doc.getString("type") ?: "workout",
                        name = doc.getString("name") ?: "",
                        timeString = doc.getString("timeString") ?: "",
                        weightOrDuration = doc.getString("weightOrDuration") ?: "",
                        calories = (doc.getLong("calories") ?: 0).toInt(),
                        protein = (doc.getLong("protein") ?: 0).toInt(),
                        carbs = (doc.getLong("carbs") ?: 0).toInt(),
                        fats = (doc.getLong("fats") ?: 0).toInt(),
                        sodium = (doc.getLong("sodium") ?: 0).toInt(),
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        distanceKm = doc.getDouble("distanceKm"),
                        pace = doc.getDouble("pace"),
                        movingTimeSeconds = doc.getLong("movingTimeSeconds"),
                        mapType = doc.getString("mapType"),
                        notes = doc.getString("notes"),
                        activityTag = doc.getString("activityTag")
                        // encodedPath intentionally excluded
                        photoUri = doc.getString("photoUri")
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping malformed activity log doc ${doc.id}", e)
                    null
                }
            }.also { Log.d(TAG, "Fetched ${it.size} activity logs for $uid") }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch activity logs for $uid", e)
            emptyList()
        }
    }

    /**
     * Fetches all meal logs and their child items from Firestore.
     * Returns a list of pairs: (MealLogEntity, List<MealLogItemEntity>).
     *
     * The mealLogId on the returned entities is set to 0 — Room will
     * auto-generate new IDs. The caller (CloudRestoreManager) is
     * responsible for re-mapping item foreign keys.
     */
    suspend fun fetchMealLogs(uid: String): List<Pair<MealLogEntity, List<MealLogItemEntity>>> {
        return try {
            val mealSnapshot = db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("mealLogs")
                .get()
                .await()

            val results = mutableListOf<Pair<MealLogEntity, List<MealLogItemEntity>>>()

            for (mealDoc in mealSnapshot.documents) {
                try {
                    val mealLog = MealLogEntity(
                        mealLogId = 0, // Will be re-assigned by Room
                        uid = uid,
                        mealType = mealDoc.getString("mealType") ?: "Snacks",
                        timestamp = mealDoc.getLong("timestamp") ?: 0L,
                        notes = mealDoc.getString("notes")
                    )

                    // Fetch child items
                    val itemsSnapshot = mealDoc.reference.collection("items")
                        .get()
                        .await()

                    val items = itemsSnapshot.documents.mapNotNull { itemDoc ->
                        try {
                            MealLogItemEntity(
                                mealLogItemId = 0, // Will be auto-generated
                                mealLogId = 0,     // Placeholder — remapped by CloudRestoreManager
                                foodId = (itemDoc.getLong("foodId") ?: 0).toInt(),
                                dishName = itemDoc.getString("dishName") ?: "",
                                weightGrams = (itemDoc.getDouble("weightGrams") ?: 0.0).toFloat(),
                                calories = (itemDoc.getDouble("calories") ?: 0.0).toFloat(),
                                protein = (itemDoc.getDouble("protein") ?: 0.0).toFloat(),
                                carbs = (itemDoc.getDouble("carbs") ?: 0.0).toFloat(),
                                fiber = (itemDoc.getDouble("fiber") ?: 0.0).toFloat(),
                                sugar = (itemDoc.getDouble("sugar") ?: 0.0).toFloat(),
                                fat = (itemDoc.getDouble("fat") ?: 0.0).toFloat(),
                                saturatedFat = (itemDoc.getDouble("saturatedFat") ?: 0.0).toFloat(),
                                polyunsaturatedFat = (itemDoc.getDouble("polyunsaturatedFat") ?: 0.0).toFloat(),
                                monounsaturatedFat = (itemDoc.getDouble("monounsaturatedFat") ?: 0.0).toFloat(),
                                transFat = (itemDoc.getDouble("transFat") ?: 0.0).toFloat(),
                                cholesterol = (itemDoc.getDouble("cholesterol") ?: 0.0).toFloat(),
                                sodium = (itemDoc.getDouble("sodium") ?: 0.0).toFloat(),
                                potassium = (itemDoc.getDouble("potassium") ?: 0.0).toFloat(),
                                vitaminA = (itemDoc.getDouble("vitaminA") ?: 0.0).toFloat(),
                                vitaminC = (itemDoc.getDouble("vitaminC") ?: 0.0).toFloat(),
                                calcium = (itemDoc.getDouble("calcium") ?: 0.0).toFloat(),
                                iron = (itemDoc.getDouble("iron") ?: 0.0).toFloat()
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Skipping malformed meal log item ${itemDoc.id}", e)
                            null
                        }
                    }

                    results.add(Pair(mealLog, items))
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping malformed meal log ${mealDoc.id}", e)
                }
            }

            Log.d(TAG, "Fetched ${results.size} meal logs for $uid")
            results
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch meal logs for $uid", e)
            emptyList()
        }
    }

    /**
     * Fetches all daily nutrition summaries from Firestore.
     */
    suspend fun fetchDailyNutritionSummaries(uid: String): List<DailyNutritionSummaryEntity> {
        return try {
            val snapshot = db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("dailyNutritionSummaries")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    DailyNutritionSummaryEntity(
                        id = 0, // Auto-generated by Room
                        uid = uid,
                        dateEpochDay = doc.getLong("dateEpochDay") ?: return@mapNotNull null,
                        totalCalories = (doc.getDouble("totalCalories") ?: 0.0).toFloat(),
                        totalProtein = (doc.getDouble("totalProtein") ?: 0.0).toFloat(),
                        totalCarbs = (doc.getDouble("totalCarbs") ?: 0.0).toFloat(),
                        totalFiber = (doc.getDouble("totalFiber") ?: 0.0).toFloat(),
                        totalSugar = (doc.getDouble("totalSugar") ?: 0.0).toFloat(),
                        totalFat = (doc.getDouble("totalFat") ?: 0.0).toFloat(),
                        totalSaturatedFat = (doc.getDouble("totalSaturatedFat") ?: 0.0).toFloat(),
                        totalPolyunsaturatedFat = (doc.getDouble("totalPolyunsaturatedFat") ?: 0.0).toFloat(),
                        totalMonounsaturatedFat = (doc.getDouble("totalMonounsaturatedFat") ?: 0.0).toFloat(),
                        totalTransFat = (doc.getDouble("totalTransFat") ?: 0.0).toFloat(),
                        totalCholesterol = (doc.getDouble("totalCholesterol") ?: 0.0).toFloat(),
                        totalSodium = (doc.getDouble("totalSodium") ?: 0.0).toFloat(),
                        totalPotassium = (doc.getDouble("totalPotassium") ?: 0.0).toFloat(),
                        totalVitaminA = (doc.getDouble("totalVitaminA") ?: 0.0).toFloat(),
                        totalVitaminC = (doc.getDouble("totalVitaminC") ?: 0.0).toFloat(),
                        totalCalcium = (doc.getDouble("totalCalcium") ?: 0.0).toFloat(),
                        totalIron = (doc.getDouble("totalIron") ?: 0.0).toFloat(),
                        breakfastCalories = (doc.getDouble("breakfastCalories") ?: 0.0).toFloat(),
                        lunchCalories = (doc.getDouble("lunchCalories") ?: 0.0).toFloat(),
                        dinnerCalories = (doc.getDouble("dinnerCalories") ?: 0.0).toFloat(),
                        snacksCalories = (doc.getDouble("snacksCalories") ?: 0.0).toFloat()
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping malformed nutrition summary ${doc.id}", e)
                    null
                }
            }.also { Log.d(TAG, "Fetched ${it.size} nutrition summaries for $uid") }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch nutrition summaries for $uid", e)
            emptyList()
        }
    }

    /**
     * Fetches all pantry item names from Firestore.
     * The document ID is the ingredient name itself.
     */
    suspend fun fetchPantryItems(uid: String): List<String> {
        return try {
            val snapshot = db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("pantryItems")
                .get()
                .await()

            snapshot.documents.map { it.id }
                .also { Log.d(TAG, "Fetched ${it.size} pantry items for $uid") }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch pantry items for $uid", e)
            emptyList()
        }
    }

    /**
     * Fetches all planned meals from Firestore.
     */
    suspend fun fetchPlannedMeals(uid: String): List<PlannedMealEntity> {
        return try {
            val snapshot = db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("plannedMeals")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    PlannedMealEntity(
                        dayIndex = (doc.getLong("dayIndex") ?: return@mapNotNull null).toInt(),
                        dishLabel = doc.getString("dishLabel") ?: return@mapNotNull null,
                        weekStartDate = doc.getString("weekStartDate") ?: return@mapNotNull null
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping malformed planned meal ${doc.id}", e)
                    null
                }
            }.also { Log.d(TAG, "Fetched ${it.size} planned meals for $uid") }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch planned meals for $uid", e)
            emptyList()
        }
    }
}

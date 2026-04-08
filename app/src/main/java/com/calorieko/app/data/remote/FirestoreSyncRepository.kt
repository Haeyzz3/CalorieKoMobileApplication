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
 * users/{uid}/plannedMeals/{dayIndex_weekStartDate_mealSlot}  → meal plan entry
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
                "level" to profile.level,
                "photoUrl" to profile.photoUrl,
                "updatedAt" to profile.updatedAt
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
                "photoUri" to log.photoUri,
                "encodedPath" to log.encodedPath,
                "updatedAt" to log.updatedAt
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

    /**
     * Batch-syncs a list of activity logs using [WriteBatch].
     * Used by the full-sync path in SettingsViewModel.
     *
     * Respects the Firestore 500-operation-per-batch limit via [chunked].
     * The single-record [syncActivityLog] is still used for real-time
     * write-through sync from repositories.
     */
    suspend fun syncActivityLogsBatch(uid: String, logs: List<ActivityLogEntity>) {
        if (logs.isEmpty()) return
        try {
            val userRef = db.collection(USERS_COLLECTION).document(uid)
            logs.chunked(500).forEach { chunk ->
                val batch = db.batch()
                for (log in chunk) {
                    val docRef = userRef.collection("activityLogs").document(log.id.toString())
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
                        "photoUri" to log.photoUri,
                        "encodedPath" to log.encodedPath,
                        "updatedAt" to log.updatedAt
                    )
                    batch.set(docRef, data)
                }
                batch.commit().await()
            }
            Log.d(TAG, "${logs.size} activity logs batch-synced for $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to batch-sync activity logs", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    //  MEAL LOGS
    // ════════════════════════════════════════════════════════════

    /**
     * Syncs a meal log header and all its child items to Firestore
     * using a single atomic [WriteBatch].
     *
     * This replaces the previous N+1 sequential writes with one
     * network call that either fully succeeds or fully fails.
     */
    suspend fun syncMealLog(uid: String, mealLog: MealLogEntity, items: List<MealLogItemEntity>) {
        try {
            val mealDocRef = db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("mealLogs")
                .document(mealLog.mealLogId.toString())

            val batch = db.batch()

            // 1. Meal log header
            val mealData = hashMapOf<String, Any?>(
                "mealType" to mealLog.mealType,
                "timestamp" to mealLog.timestamp,
                "notes" to mealLog.notes,
                "updatedAt" to mealLog.updatedAt
            )
            batch.set(mealDocRef, mealData)

            // 2. All items in the same batch
            for (item in items) {
                val itemRef = mealDocRef.collection("items")
                    .document(item.mealLogItemId.toString())
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
                    "iron" to item.iron,
                    "updatedAt" to item.updatedAt
                )
                batch.set(itemRef, itemData)
            }

            // 3. Commit all writes atomically
            batch.commit().await()
            Log.d(TAG, "Meal log ${mealLog.mealLogId} synced with ${items.size} items for $uid (batched)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync meal log ${mealLog.mealLogId}", e)
        }
    }

    /**
     * Deletes a meal log and all its child item documents from Firestore
     * using a single atomic [WriteBatch].
     */
    suspend fun deleteMealLog(uid: String, mealLogId: Long) {
        try {
            val mealDocRef = db.collection(USERS_COLLECTION)
                .document(uid)
                .collection("mealLogs")
                .document(mealLogId.toString())

            val batch = db.batch()

            // Queue all item deletes
            val itemSnapshots = mealDocRef.collection("items").get().await()
            for (doc in itemSnapshots.documents) {
                batch.delete(doc.reference)
            }

            // Queue the parent meal log delete
            batch.delete(mealDocRef)

            // Commit all deletes atomically
            batch.commit().await()
            Log.d(TAG, "Meal log $mealLogId deleted from Firestore for $uid (batched)")
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
                "snacksCalories" to summary.snacksCalories,
                "updatedAt" to summary.updatedAt
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

    /**
     * Batch-syncs a list of daily nutrition summaries using [WriteBatch].
     * Used by the full-sync path in SettingsViewModel.
     */
    suspend fun syncDailyNutritionSummariesBatch(uid: String, summaries: List<DailyNutritionSummaryEntity>) {
        if (summaries.isEmpty()) return
        try {
            val userRef = db.collection(USERS_COLLECTION).document(uid)
            summaries.chunked(500).forEach { chunk ->
                val batch = db.batch()
                for (summary in chunk) {
                    val docRef = userRef.collection("dailyNutritionSummaries")
                        .document(summary.dateEpochDay.toString())
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
                        "snacksCalories" to summary.snacksCalories,
                        "updatedAt" to summary.updatedAt
                    )
                    batch.set(docRef, data)
                }
                batch.commit().await()
            }
            Log.d(TAG, "${summaries.size} nutrition summaries batch-synced for $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to batch-sync nutrition summaries", e)
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
     * Batch-syncs a list of pantry item names using [WriteBatch].
     * Used by the full-sync path in SettingsViewModel.
     */
    suspend fun syncPantryItemsBatch(uid: String, itemNames: List<String>) {
        if (itemNames.isEmpty()) return
        try {
            val userRef = db.collection(USERS_COLLECTION).document(uid)
            val now = System.currentTimeMillis()
            itemNames.chunked(500).forEach { chunk ->
                val batch = db.batch()
                for (name in chunk) {
                    val docRef = userRef.collection("pantryItems").document(name)
                    batch.set(docRef, hashMapOf("addedAt" to now))
                }
                batch.commit().await()
            }
            Log.d(TAG, "${itemNames.size} pantry items batch-synced for $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to batch-sync pantry items", e)
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
     * Document ID is a composite key: `{dayIndex}_{weekStartDate}_{mealSlot}`.
     */
    suspend fun syncPlannedMeal(uid: String, meal: PlannedMealEntity) {
        try {
            val docId = "${meal.dayIndex}_${meal.weekStartDate}_${meal.mealSlot}"
            val data = hashMapOf<String, Any?>(
                "dayIndex" to meal.dayIndex,
                "dishLabel" to meal.dishLabel,
                "weekStartDate" to meal.weekStartDate,
                "mealSlot" to meal.mealSlot
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
     * Batch-syncs a list of planned meals using [WriteBatch].
     * Used by the full-sync path in SettingsViewModel.
     */
    suspend fun syncPlannedMealsBatch(uid: String, meals: List<PlannedMealEntity>) {
        if (meals.isEmpty()) return
        try {
            val userRef = db.collection(USERS_COLLECTION).document(uid)
            meals.chunked(500).forEach { chunk ->
                val batch = db.batch()
                for (meal in chunk) {
                    val docId = "${meal.dayIndex}_${meal.weekStartDate}_${meal.mealSlot}"
                    val docRef = userRef.collection("plannedMeals").document(docId)
                    val data = hashMapOf<String, Any?>(
                        "dayIndex" to meal.dayIndex,
                        "dishLabel" to meal.dishLabel,
                        "weekStartDate" to meal.weekStartDate,
                        "mealSlot" to meal.mealSlot
                    )
                    batch.set(docRef, data)
                }
                batch.commit().await()
            }
            Log.d(TAG, "${meals.size} planned meals batch-synced for $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to batch-sync planned meals", e)
        }
    }

    /**
     * Deletes a single planned meal document from Firestore.
     */
    suspend fun deletePlannedMeal(uid: String, dayIndex: Int, weekStartDate: String, mealSlot: String) {
        try {
            val docId = "${dayIndex}_${weekStartDate}_${mealSlot}"
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

            if (snapshot.isEmpty) return

            val batch = db.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Log.d(TAG, "Cleared ${snapshot.size()} planned meals for week $weekStartDate (batched)")
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
     * We iterate each known sub-collection, collect all document references
     * (including nested meal log items), then commit in batches of 500
     * (Firestore's WriteBatch limit).
     *
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

            // Phase 1: Collect ALL document references to delete
            val allRefs = mutableListOf<com.google.firebase.firestore.DocumentReference>()

            for (collectionName in subCollections) {
                val snapshot = userDocRef.collection(collectionName).get().await()
                for (doc in snapshot.documents) {
                    // For mealLogs, also collect the nested 'items' sub-collection
                    if (collectionName == "mealLogs") {
                        val itemsSnapshot = doc.reference.collection("items").get().await()
                        allRefs.addAll(itemsSnapshot.documents.map { it.reference })
                    }
                    allRefs.add(doc.reference)
                }
            }

            // Phase 2: Commit deletes in batches of 500 (WriteBatch limit)
            allRefs.chunked(500).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { ref -> batch.delete(ref) }
                batch.commit().await()
            }

            Log.d(TAG, "All user data wiped from Firestore for $uid (${allRefs.size} docs, batched)")
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
                level = (doc.getLong("level") ?: 1).toInt(),
                photoUrl = doc.getString("photoUrl") ?: ""
            ).also { Log.d(TAG, "Profile fetched for $uid") }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch profile for $uid", e)
            null
        }
    }

    /**
     * Fetches all activity logs from Firestore for the given user.
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
                        calories = (doc.get("calories") as? Number)?.toInt() ?: 0,
                        protein = (doc.get("protein") as? Number)?.toInt() ?: 0,
                        carbs = (doc.get("carbs") as? Number)?.toInt() ?: 0,
                        fats = (doc.get("fats") as? Number)?.toInt() ?: 0,
                        sodium = (doc.get("sodium") as? Number)?.toInt() ?: 0,
                        timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: 0L,
                        distanceKm = (doc.get("distanceKm") as? Number)?.toDouble(),
                        pace = (doc.get("pace") as? Number)?.toDouble(),
                        movingTimeSeconds = (doc.get("movingTimeSeconds") as? Number)?.toLong(),
                        mapType = doc.getString("mapType"),
                        notes = doc.getString("notes"),
                        activityTag = doc.getString("activityTag"),
                        encodedPath = doc.getString("encodedPath"),
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
                                iron = (itemDoc.get("iron") as? Number)?.toFloat() ?: 0f
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
                        snacksCalories = (doc.get("snacksCalories") as? Number)?.toFloat() ?: 0f
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
                        weekStartDate = doc.getString("weekStartDate") ?: return@mapNotNull null,
                        mealSlot = doc.getString("mealSlot") ?: "Lunch"
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

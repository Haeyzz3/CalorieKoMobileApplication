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
     * Intentionally excludes `encodedPath` (GPS coordinates can be very large)
     * and `photoUri` (local file path, not useful across devices).
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
                "activityTag" to log.activityTag
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
}

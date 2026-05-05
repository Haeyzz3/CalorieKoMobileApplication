package com.calorieko.app.data.remote.api

import com.google.gson.annotations.SerializedName

/**
 * Data classes matching the Laravel backend's SyncFullRequest schema.
 * These are serialized to JSON by Gson and sent to POST /api/sync/full.
 *
 * @see sample_sync_payload.json in the admin-api repo for reference.
 */

// ── Top-level payload ──

data class SyncFullPayload(
    @SerializedName("uid") val uid: String,
    @SerializedName("last_sync_timestamp") val lastSyncTimestamp: Long,
    @SerializedName("profile") val profile: SyncProfile?,
    @SerializedName("meals") val meals: List<SyncMeal>,
    @SerializedName("activities") val activities: List<SyncActivity>,
    @SerializedName("nutrition_summaries") val nutritionSummaries: List<SyncNutritionSummary>
)

// ── Profile ──

data class SyncProfile(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("age") val age: Int,
    @SerializedName("weight") val weight: Double,
    @SerializedName("height") val height: Double,
    @SerializedName("sex") val sex: String,
    @SerializedName("activityLevel") val activityLevel: String,
    @SerializedName("goal") val goal: String,
    @SerializedName("streak") val streak: Int,
    @SerializedName("level") val level: Int,
    @SerializedName("updated_at") val updatedAt: Long
)

// ── Meal Log (parent + items) ──

data class SyncMeal(
    @SerializedName("uid") val uid: String,
    @SerializedName("meal_type") val mealType: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("notes") val notes: String?,
    @SerializedName("updated_at") val updatedAt: Long,
    @SerializedName("items") val items: List<SyncMealItem>
)

data class SyncMealItem(
    @SerializedName("food_id") val foodId: Int,
    @SerializedName("dish_name") val dishName: String,
    @SerializedName("weight_grams") val weightGrams: Float,
    @SerializedName("calories") val calories: Float,
    @SerializedName("protein") val protein: Float,
    @SerializedName("carbs") val carbs: Float,
    @SerializedName("fat") val fat: Float,
    @SerializedName("fiber") val fiber: Float,
    @SerializedName("sugar") val sugar: Float,
    @SerializedName("saturated_fat") val saturatedFat: Float,
    @SerializedName("polyunsaturated_fat") val polyunsaturatedFat: Float,
    @SerializedName("monounsaturated_fat") val monounsaturatedFat: Float,
    @SerializedName("trans_fat") val transFat: Float,
    @SerializedName("cholesterol") val cholesterol: Float,
    @SerializedName("sodium") val sodium: Float,
    @SerializedName("potassium") val potassium: Float,
    @SerializedName("vitamin_a") val vitaminA: Float,
    @SerializedName("vitamin_c") val vitaminC: Float,
    @SerializedName("calcium") val calcium: Float,
    @SerializedName("iron") val iron: Float,
    @SerializedName("updated_at") val updatedAt: Long
)

// ── Activity Log ──

data class SyncActivity(
    @SerializedName("uid") val uid: String,
    @SerializedName("type") val type: String,
    @SerializedName("name") val name: String,
    @SerializedName("timeString") val timeString: String,
    @SerializedName("weightOrDuration") val weightOrDuration: String,
    @SerializedName("calories") val calories: Int,
    @SerializedName("protein") val protein: Int,
    @SerializedName("carbs") val carbs: Int,
    @SerializedName("fats") val fats: Int,
    @SerializedName("sodium") val sodium: Int,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("distanceKm") val distanceKm: Double?,
    @SerializedName("pace") val pace: Double?,
    @SerializedName("movingTimeSeconds") val movingTimeSeconds: Long?,
    @SerializedName("steps") val steps: Int?,
    @SerializedName("mapType") val mapType: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("activityTag") val activityTag: String?,
    @SerializedName("updated_at") val updatedAt: Long
)

// ── Daily Nutrition Summary ──

data class SyncNutritionSummary(
    @SerializedName("uid") val uid: String,
    @SerializedName("date_epoch_day") val dateEpochDay: Long,
    @SerializedName("total_calories") val totalCalories: Float,
    @SerializedName("total_protein") val totalProtein: Float,
    @SerializedName("total_carbs") val totalCarbs: Float,
    @SerializedName("total_fiber") val totalFiber: Float,
    @SerializedName("total_sugar") val totalSugar: Float,
    @SerializedName("total_fat") val totalFat: Float,
    @SerializedName("total_saturated_fat") val totalSaturatedFat: Float,
    @SerializedName("total_polyunsaturated_fat") val totalPolyunsaturatedFat: Float,
    @SerializedName("total_monounsaturated_fat") val totalMonounsaturatedFat: Float,
    @SerializedName("total_trans_fat") val totalTransFat: Float,
    @SerializedName("total_cholesterol") val totalCholesterol: Float,
    @SerializedName("total_sodium") val totalSodium: Float,
    @SerializedName("total_potassium") val totalPotassium: Float,
    @SerializedName("total_vitamin_a") val totalVitaminA: Float,
    @SerializedName("total_vitamin_c") val totalVitaminC: Float,
    @SerializedName("total_calcium") val totalCalcium: Float,
    @SerializedName("total_iron") val totalIron: Float,
    @SerializedName("breakfast_calories") val breakfastCalories: Float,
    @SerializedName("lunch_calories") val lunchCalories: Float,
    @SerializedName("dinner_calories") val dinnerCalories: Float,
    @SerializedName("snacks_calories") val snacksCalories: Float,
    @SerializedName("updated_at") val updatedAt: Long
)

// ── Server Response ──

data class SyncFullResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("last_successful_sync") val lastSuccessfulSync: Long?,
    @SerializedName("error") val error: String?,
    @SerializedName("conflicts") val conflicts: List<SyncConflict>? = null
)

/**
 * Represents a single entity that was rejected by the server
 * because the server's record was newer (admin override).
 */
data class SyncConflict(
    @SerializedName("entity_type") val entityType: String,
    @SerializedName("entity_id") val entityId: String,
    @SerializedName("reason") val reason: String
)

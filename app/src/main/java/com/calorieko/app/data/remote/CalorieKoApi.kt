package com.calorieko.app.data.remote

import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.UserProfile
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.Part

// ─────────────────────────────────────────────────────────────
// DTOs — match the exact JSON keys the Laravel API expects
// ─────────────────────────────────────────────────────────────

/**
 * Meal-log sync request.  The parent + child items are sent together.
 */
data class MealLogSyncRequest(
    val uid: String,
    @SerializedName("meal_type") val mealType: String,
    val timestamp: Long,
    val notes: String? = null,
    val items: List<MealLogItemDto>
    
)

data class MealLogItemDto(
    @SerializedName("food_id")      val foodId: Int,
    @SerializedName("dish_name")    val dishName: String,
    @SerializedName("weight_grams") val weightGrams: Float,
    val calories: Float = 0f,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val fat: Float = 0f,
    @SerializedName("saturated_fat")       val saturatedFat: Float = 0f,
    @SerializedName("polyunsaturated_fat") val polyunsaturatedFat: Float = 0f,
    @SerializedName("monounsaturated_fat") val monounsaturatedFat: Float = 0f,
    @SerializedName("trans_fat")           val transFat: Float = 0f,
    val cholesterol: Float = 0f,
    val sodium: Float = 0f,
    val potassium: Float = 0f,
    @SerializedName("vitamin_a") val vitaminA: Float = 0f,
    @SerializedName("vitamin_c") val vitaminC: Float = 0f,
    val calcium: Float = 0f,
    val iron: Float = 0f
)

data class DailyNutritionSyncRequest(
    val uid: String,
    @SerializedName("date_epoch_day")              val dateEpochDay: Long,
    @SerializedName("total_calories")              val totalCalories: Float = 0f,
    @SerializedName("total_protein")               val totalProtein: Float = 0f,
    @SerializedName("total_carbs")                 val totalCarbs: Float = 0f,
    @SerializedName("total_fiber")                 val totalFiber: Float = 0f,
    @SerializedName("total_sugar")                 val totalSugar: Float = 0f,
    @SerializedName("total_fat")                   val totalFat: Float = 0f,
    @SerializedName("total_saturated_fat")         val totalSaturatedFat: Float = 0f,
    @SerializedName("total_polyunsaturated_fat")   val totalPolyunsaturatedFat: Float = 0f,
    @SerializedName("total_monounsaturated_fat")   val totalMonounsaturatedFat: Float = 0f,
    @SerializedName("total_trans_fat")             val totalTransFat: Float = 0f,
    @SerializedName("total_cholesterol")           val totalCholesterol: Float = 0f,
    @SerializedName("total_sodium")                val totalSodium: Float = 0f,
    @SerializedName("total_potassium")             val totalPotassium: Float = 0f,
    @SerializedName("total_vitamin_a")             val totalVitaminA: Float = 0f,
    @SerializedName("total_vitamin_c")             val totalVitaminC: Float = 0f,
    @SerializedName("total_calcium")               val totalCalcium: Float = 0f,
    @SerializedName("total_iron")                  val totalIron: Float = 0f,
    @SerializedName("breakfast_calories")           val breakfastCalories: Float = 0f,
    @SerializedName("lunch_calories")              val lunchCalories: Float = 0f,
    @SerializedName("dinner_calories")             val dinnerCalories: Float = 0f,
    @SerializedName("snacks_calories")             val snacksCalories: Float = 0f
)

data class FoodItemSyncRequest(
    @SerializedName("food_id")                      val foodId: Int? = null,
    @SerializedName("name_en")                      val nameEn: String,
    @SerializedName("name_ph")                      val namePh: String,
    val category: String,
    @SerializedName("calories_per_100g")            val caloriesPer100g: Float = 0f,
    @SerializedName("protein_per_100g")             val proteinPer100g: Float = 0f,
    @SerializedName("carbs_per_100g")               val carbsPer100g: Float = 0f,
    @SerializedName("fiber_per_100g")               val fiberPer100g: Float = 0f,
    @SerializedName("sugar_per_100g")               val sugarPer100g: Float = 0f,
    @SerializedName("fat_per_100g")                 val fatPer100g: Float = 0f,
    @SerializedName("saturated_fat_per_100g")       val saturatedFatPer100g: Float = 0f,
    @SerializedName("polyunsaturated_fat_per_100g") val polyunsaturatedFatPer100g: Float = 0f,
    @SerializedName("monounsaturated_fat_per_100g") val monounsaturatedFatPer100g: Float = 0f,
    @SerializedName("trans_fat_per_100g")           val transFatPer100g: Float = 0f,
    @SerializedName("cholesterol_per_100g")         val cholesterolPer100g: Float = 0f,
    @SerializedName("sodium_per_100g")              val sodiumPer100g: Float = 0f,
    @SerializedName("potassium_per_100g")           val potassiumPer100g: Float = 0f,
    @SerializedName("vitamin_a_per_100g")           val vitaminAPer100g: Float = 0f,
    @SerializedName("vitamin_c_per_100g")           val vitaminCPer100g: Float = 0f,
    @SerializedName("calcium_per_100g")             val calciumPer100g: Float = 0f,
    @SerializedName("iron_per_100g")                val ironPer100g: Float = 0f
)

/** Batch wrapper used by POST /api/sync/activity-log/batch */
data class ActivityLogBatchRequest(
    val entries: List<ActivityLogEntity>
)

// ─────────────────────────────────────────────────────────────
// Retrofit API Interface
// ─────────────────────────────────────────────────────────────

interface CalorieKoApi {

    // ── Mobile Sync Endpoints ───────────────────────────────

    /** Upsert the current user's profile */
    @POST("sync/profile")
    suspend fun syncProfile(@Body profile: UserProfile): Response<UserProfile>

    /** Sync a single food item */
    @POST("sync/food")
    suspend fun syncFood(@Body food: FoodItemSyncRequest): Response<Any>

    /** Sync a single activity log entry */
    @POST("sync/activity-log")
    suspend fun syncActivityLog(@Body log: ActivityLogEntity): Response<Any>

    /** Batch-sync multiple activity log entries at once */
    @POST("sync/activity-log/batch")
    suspend fun syncActivityLogBatch(@Body batch: ActivityLogBatchRequest): Response<Any>

    /** Sync a meal log with its child items */
    @POST("sync/meal-log")
    suspend fun syncMealLog(@Body mealLog: MealLogSyncRequest): Response<Any>

    /** Sync a daily nutrition summary */
    @POST("sync/nutrition-summary")
    suspend fun syncNutritionSummary(@Body summary: DailyNutritionSyncRequest): Response<Any>


    @Multipart
    @POST("sync/profile/photo")
    suspend fun uploadProfilePhoto(
        @Part("uid") uid: RequestBody,
        @Part photo: MultipartBody.Part
    ): Response<PhotoUploadResponse>
}

/** Response from the photo upload endpoint */
data class PhotoUploadResponse(
    val message: String,
    @SerializedName("photo_url") val photoUrl: String
)

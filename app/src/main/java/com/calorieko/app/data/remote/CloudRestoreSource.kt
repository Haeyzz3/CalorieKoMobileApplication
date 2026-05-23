package com.calorieko.app.data.remote

import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.MealLogEntity
import com.calorieko.app.data.model.MealLogItemEntity
import com.calorieko.app.data.model.PlannedMealEntity
import com.calorieko.app.data.model.UserProfile
import com.calorieko.app.data.model.WeightLogEntity

interface CloudRestoreSource {
    suspend fun fetchProfile(uid: String): UserProfile?
    suspend fun fetchActivityLogs(uid: String): List<ActivityLogEntity>
    suspend fun fetchMealLogs(uid: String): List<Pair<MealLogEntity, List<MealLogItemEntity>>>
    suspend fun fetchDailyNutritionSummaries(uid: String): List<DailyNutritionSummaryEntity>
    suspend fun fetchPantryItems(uid: String): List<String>
    suspend fun fetchPlannedMeals(uid: String): List<PlannedMealEntity>
    suspend fun fetchWeightLogs(uid: String): List<WeightLogEntity>
}

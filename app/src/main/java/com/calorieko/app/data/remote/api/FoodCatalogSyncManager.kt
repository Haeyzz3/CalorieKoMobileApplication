package com.calorieko.app.data.remote.api

import android.content.Context
import android.util.Log
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.local.FoodDatabaseCallback
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Handles the server-to-mobile synchronization of the food catalog.
 * Pulls admin-managed food items and integrates them into the local Room database,
 * while protecting USDA-verified dishes from being overwritten.
 */
class FoodCatalogSyncManager(private val context: Context, private val apiService: CalorieKoApiService) {
    companion object {
        private const val TAG = "FoodCatalogSyncManager"
        private const val SYNC_PREFS = "sync_prefs"
        private const val SEED_PREFS = "reference_data_seed"
        private const val KEY_LAST_FOOD_CATALOG_SYNC = "last_food_catalog_sync_ms"
    }

    /**
     * Pulls the food catalog from the server and updates the local database.
     * Returns true if successful, false otherwise.
     */
    suspend fun pullFoodCatalog(): Boolean {
        return try {
            val user = FirebaseAuth.getInstance().currentUser ?: return false
            val token = user.getIdToken(true).await().token ?: return false
            
            val response = apiService.getFoodCatalog("Bearer $token")
            if (response.isSuccessful && response.body()?.success == true) {
                val serverFoods = response.body()!!.foods
                val app = context.applicationContext as com.calorieko.app.CalorieKoApplication
                val db = app.database
                
                // 1. Get USDA-protected labels (dishes with System B recipes)
                val protectedLabels = db.dishRecipeDao().getAllDishLabels().toSet()
                
                // 2. Perform atomic sync in FoodDao
                db.foodDao().syncFromServer(
                    serverFoods.map { it.toFoodItem() },
                    protectedLabels
                )
                
                // 3. Mark as synced to prevent CSV re-seeding
                context.getSharedPreferences(SEED_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(FoodDatabaseCallback.KEY_FOOD_CATALOG_SYNCED, true)
                    .apply()
                
                // 4. Update last-synced timestamp
                context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(KEY_LAST_FOOD_CATALOG_SYNC, System.currentTimeMillis())
                    .apply()
                
                val adminCount = serverFoods.count { it.mlLabel !in protectedLabels }
                Log.d(TAG, "Food catalog pull SUCCESS: ${serverFoods.size} total, $adminCount admin-added (${protectedLabels.size} USDA-protected skipped).")
                true
            } else {
                Log.w(TAG, "Food catalog pull FAILED: ${response.code()} ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Food catalog pull ERROR", e)
            false
        }
    }
}

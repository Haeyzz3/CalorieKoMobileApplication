package com.calorieko.app.di

import android.app.Application
import com.calorieko.app.data.local.ActivityLogDao
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.local.DailyNutritionSummaryDao
import com.calorieko.app.data.local.FoodDao
import com.calorieko.app.data.local.MealLogDao
import com.calorieko.app.data.local.MealLogItemDao
import com.calorieko.app.data.local.MealPlanDao
import com.calorieko.app.data.local.PantryDao
import com.calorieko.app.data.local.UserDao
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        application: Application,
        @ApplicationScope scope: CoroutineScope
    ): AppDatabase {
        return AppDatabase.getDatabase(application, scope)
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideFoodDao(database: AppDatabase): FoodDao = database.foodDao()

    @Provides
    fun provideActivityLogDao(database: AppDatabase): ActivityLogDao = database.activityLogDao()

    @Provides
    fun provideMealLogDao(database: AppDatabase): MealLogDao = database.mealLogDao()

    @Provides
    fun provideMealLogItemDao(database: AppDatabase): MealLogItemDao = database.mealLogItemDao()

    @Provides
    fun provideDailyNutritionSummaryDao(database: AppDatabase): DailyNutritionSummaryDao = database.dailyNutritionSummaryDao()

    @Provides
    fun providePantryDao(database: AppDatabase): PantryDao = database.pantryDao()

    @Provides
    fun provideMealPlanDao(database: AppDatabase): MealPlanDao = database.mealPlanDao()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}

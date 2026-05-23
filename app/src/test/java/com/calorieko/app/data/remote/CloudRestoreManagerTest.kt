package com.calorieko.app.data.remote

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.DailyNutritionSummaryEntity
import com.calorieko.app.data.model.MealLogEntity
import com.calorieko.app.data.model.MealLogItemEntity
import com.calorieko.app.data.model.PlannedMealEntity
import com.calorieko.app.data.model.UserProfile
import com.calorieko.app.data.model.WeightLogEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class CloudRestoreManagerTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun restorePreservesRemoteIdsSyncedStateAndCreatesNoOutboxRows() = runBlocking {
        val uid = "uid-restore"
        val source = FakeRestoreSource(uid)
        val manager = manager(source)

        val result = manager.restoreIfNeeded(uid)

        val activity = db.activityLogDao().getAllLogsForUser(uid).single()
        val mealWithItems = db.mealLogDao().getAllMealLogsWithItems(uid).single()
        val weight = db.weightLogDao().getAllWeightLogsForUser(uid).single()

        assertTrue(result is RestoreResult.Success)
        assertEquals("activity-doc", activity.remoteId)
        assertEquals(1, activity.syncStatus)
        assertEquals("meal-doc", mealWithItems.mealLog.remoteId)
        assertEquals(1, mealWithItems.mealLog.syncStatus)
        assertEquals("item-doc", mealWithItems.items.single().remoteId)
        assertEquals(mealWithItems.mealLog.mealLogId, mealWithItems.items.single().mealLogId)
        assertEquals(1, weight.syncStatus)
        assertEquals(0, db.firestoreOutboxDao().pendingCount(uid))
    }

    @Test
    fun failedSubcollectionFetchRollsBackLocalRestore() = runBlocking {
        val uid = "uid-failed-restore"
        val source = FakeRestoreSource(uid, failMealFetch = true)
        val manager = manager(source)

        val result = manager.restoreIfNeeded(uid)

        assertTrue(result is RestoreResult.Failed)
        assertEquals(null, db.userDao().getUser(uid))
        assertTrue(db.activityLogDao().getAllLogsForUser(uid).isEmpty())
        assertTrue(db.mealLogDao().getAllMealLogsWithItems(uid).isEmpty())
        assertEquals(0, db.firestoreOutboxDao().pendingCount(uid))
    }

    private fun manager(source: CloudRestoreSource): CloudRestoreManager =
        CloudRestoreManager(
            db = db,
            restoreSource = source,
            userDao = db.userDao(),
            activityLogDao = db.activityLogDao(),
            mealLogDao = db.mealLogDao(),
            mealLogItemDao = db.mealLogItemDao(),
            dailyNutritionSummaryDao = db.dailyNutritionSummaryDao(),
            pantryDao = db.pantryDao(),
            mealPlanDao = db.mealPlanDao(),
            weightLogDao = db.weightLogDao()
        )

    private class FakeRestoreSource(
        private val uid: String,
        private val failMealFetch: Boolean = false
    ) : CloudRestoreSource {
        override suspend fun fetchProfile(uid: String): UserProfile? =
            UserProfile(
                uid = uid,
                name = "Restored User",
                email = "restore@example.com",
                age = 30,
                weight = 70.0,
                height = 170.0,
                goal = "maintain",
                onboardingCompleted = true,
                updatedAt = 1000L
            )

        override suspend fun fetchActivityLogs(uid: String): List<ActivityLogEntity> =
            listOf(
                ActivityLogEntity(
                    uid = uid,
                    remoteId = "activity-doc",
                    type = "workout",
                    name = "Run",
                    timeString = "30 min",
                    weightOrDuration = "30 min",
                    calories = 200,
                    timestamp = 1_700_000_000_000L,
                    syncStatus = 1
                )
            )

        override suspend fun fetchMealLogs(uid: String): List<Pair<MealLogEntity, List<MealLogItemEntity>>> {
            if (failMealFetch) error("meal fetch failed")
            return listOf(
                MealLogEntity(
                    uid = uid,
                    remoteId = "meal-doc",
                    mealType = "Lunch",
                    timestamp = 1_700_000_000_000L,
                    syncStatus = 1
                ) to listOf(
                    MealLogItemEntity(
                        mealLogId = 0,
                        remoteId = "item-doc",
                        foodId = 1,
                        dishName = "Adobo",
                        weightGrams = 150f,
                        calories = 250f
                    )
                )
            )
        }

        override suspend fun fetchDailyNutritionSummaries(uid: String): List<DailyNutritionSummaryEntity> =
            listOf(
                DailyNutritionSummaryEntity(
                    uid = uid,
                    dateEpochDay = LocalDate.of(2026, 5, 23).toEpochDay(),
                    totalCalories = 250f
                )
            )

        override suspend fun fetchPantryItems(uid: String): List<String> =
            listOf("rice")

        override suspend fun fetchPlannedMeals(uid: String): List<PlannedMealEntity> =
            listOf(
                PlannedMealEntity(
                    dayIndex = 0,
                    dishLabel = "adobo",
                    weekStartDate = "2026-05-18",
                    mealSlot = "Lunch"
                )
            )

        override suspend fun fetchWeightLogs(uid: String): List<WeightLogEntity> =
            listOf(
                WeightLogEntity(
                    uid = uid,
                    dateEpochDay = LocalDate.of(2026, 5, 23).toEpochDay(),
                    weightKg = 70.0,
                    timestamp = 2222L,
                    syncStatus = 1
                )
            )
    }
}

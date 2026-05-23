package com.calorieko.app.data.repository

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.LoggedDish
import com.calorieko.app.data.model.PlannedMealEntity
import com.calorieko.app.data.model.UserProfile
import com.calorieko.app.data.remote.firestore.FirestoreEntityType
import com.calorieko.app.data.remote.firestore.FirestorePayloadSerializer
import com.calorieko.app.data.remote.firestore.FirestoreSyncOperation
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class FirestoreOutboxRepositoryTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun saveProfileCreatesProfileWeightAndOutboxRows() = runBlocking {
        val uid = "uid-profile"
        val repository = UserRepository(db, context)

        repository.saveProfile(uid, profile(uid, weight = 72.5))

        val savedProfile = db.userDao().getUser(uid)
        val weightLogs = db.weightLogDao().getAllWeightLogsForUser(uid)
        val outbox = db.firestoreOutboxDao().getPending(uid, 10)

        assertNotNull(savedProfile)
        assertEquals(1, weightLogs.size)
        assertEquals(2, outbox.size)
        assertEquals(listOf(FirestoreEntityType.USER_PROFILE, FirestoreEntityType.WEIGHT_LOG), outbox.map { it.entityType })
        assertEquals(listOf(FirestoreSyncOperation.UPSERT_DOCUMENT, FirestoreSyncOperation.UPSERT_DOCUMENT), outbox.map { it.operation })
        assertEquals("users/$uid", outbox[0].remotePath)
        assertTrue(outbox[1].remotePath.startsWith("users/$uid/weightLogs/"))
    }

    @Test
    fun activityCreateAndDeleteUseStableRemoteIdOutbox() = runBlocking {
        val uid = "uid-activity"
        val repository = ActivityRepository(db, context)

        val id = repository.insertWorkoutLog(uid, activity(uid = "ignored")).toInt()
        val inserted = db.activityLogDao().getLogById(id)
        val createOutbox = db.firestoreOutboxDao().getPending(uid, 10)

        assertNotNull(inserted)
        assertEquals(uid, inserted?.uid)
        assertEquals(1, createOutbox.size)
        assertEquals(FirestoreSyncOperation.UPSERT_DOCUMENT, createOutbox[0].operation)
        assertEquals(inserted?.remoteId, createOutbox[0].entityKey)
        assertEquals("users/$uid/activityLogs/${inserted?.remoteId}", createOutbox[0].remotePath)

        repository.deleteWorkoutLog(uid, id)
        val deleteOutbox = db.firestoreOutboxDao().getPending(uid, 10)

        assertNull(db.activityLogDao().getLogById(id))
        assertEquals(2, deleteOutbox.size)
        assertEquals(FirestoreSyncOperation.DELETE_DOCUMENT, deleteOutbox[1].operation)
        assertEquals(inserted?.remoteId, deleteOutbox[1].entityKey)
    }

    @Test
    fun mealSaveAndDeleteCreateOrderedOutboxRows() = runBlocking {
        val uid = "uid-meal"
        val repository = MealRepository(db, context)

        repository.saveMeal(uid, "Lunch", listOf(loggedDish("Chicken Adobo", 250f)))
        val savedMeal = db.mealLogDao().getAllMealLogsWithItems(uid).single()
        val createOutbox = db.firestoreOutboxDao().getPending(uid, 10)
        val mealPayload = FirestorePayloadSerializer.fromJson(createOutbox[0].payloadJson!!)
        val itemPayloads = mealPayload["items"] as List<*>
        val firstItem = itemPayloads.single() as Map<*, *>

        assertEquals(2, createOutbox.size)
        assertEquals(listOf(FirestoreEntityType.MEAL_LOG, FirestoreEntityType.DAILY_NUTRITION_SUMMARY), createOutbox.map { it.entityType })
        assertEquals(savedMeal.items.single().remoteId, firstItem["remoteId"])

        repository.deleteMealLog(uid, savedMeal.mealLog.mealLogId)
        val deleteOutbox = db.firestoreOutboxDao().getPending(uid, 10)

        assertTrue(db.mealLogDao().getAllMealLogsWithItems(uid).isEmpty())
        assertEquals(
            listOf(
                FirestoreSyncOperation.UPSERT_DOCUMENT,
                FirestoreSyncOperation.UPSERT_DOCUMENT,
                FirestoreSyncOperation.DELETE_MEAL_LOG_RECURSIVE,
                FirestoreSyncOperation.UPSERT_DOCUMENT
            ),
            deleteOutbox.map { it.operation }
        )
        assertEquals(savedMeal.mealLog.remoteId, deleteOutbox[2].entityKey)
        assertEquals(FirestoreEntityType.DAILY_NUTRITION_SUMMARY, deleteOutbox[3].entityType)
    }

    @Test
    fun pantryClearThenAddKeepsOrderedBarrier() = runBlocking {
        val uid = "uid-pantry"
        val repository = PantryRepository(db, context)

        repository.addIngredients(uid, listOf("rice", "egg"))
        repository.clearAll(uid)
        repository.addIngredient(uid, "tomato")

        val outbox = db.firestoreOutboxDao().getPending(uid, 10)

        assertEquals(listOf("tomato"), db.pantryDao().getAllItemsList())
        assertEquals(
            listOf(
                FirestoreSyncOperation.UPSERT_DOCUMENT,
                FirestoreSyncOperation.UPSERT_DOCUMENT,
                FirestoreSyncOperation.CLEAR_COLLECTION,
                FirestoreSyncOperation.UPSERT_DOCUMENT
            ),
            outbox.map { it.operation }
        )
        assertEquals("users/$uid/pantryItems", outbox[2].remotePath)
    }

    @Test
    fun pantryApplySelectionCreatesAddAndDeleteDiffOperations() = runBlocking {
        val uid = "uid-pantry-diff"
        val repository = PantryRepository(db, context)

        repository.addIngredients(uid, listOf("rice", "egg"))
        val diff = repository.applySelection(uid, setOf("egg", "tomato"))

        val outbox = db.firestoreOutboxDao().getPending(uid, 10)

        assertEquals(1 to 1, diff)
        assertEquals(listOf("egg", "tomato"), db.pantryDao().getAllItemsList())
        assertEquals(FirestoreSyncOperation.UPSERT_DOCUMENT, outbox[2].operation)
        assertEquals("tomato", outbox[2].entityKey)
        assertEquals(FirestoreSyncOperation.DELETE_DOCUMENT, outbox[3].operation)
        assertEquals("rice", outbox[3].entityKey)
    }

    @Test
    fun plannedMealReplaceWeekWritesClearBeforeUpserts() = runBlocking {
        val uid = "uid-plan"
        val weekStart = "2026-05-18"
        val repository = MealPlanRepository(db, context)

        repository.upsertMeal(uid, plannedMeal(weekStart, "old_dish"))
        repository.replaceWeek(
            uid = uid,
            weekStartDate = weekStart,
            meals = listOf(
                plannedMeal(weekStart, "new_breakfast", mealSlot = "Breakfast"),
                plannedMeal(weekStart, "new_lunch", mealSlot = "Lunch")
            )
        )

        val outbox = db.firestoreOutboxDao().getPending(uid, 10)
        val savedMeals = db.mealPlanDao().getMealsForWeekOneShot(weekStart)
        val clearPayload = FirestorePayloadSerializer.fromJson(outbox[1].payloadJson!!)
        val filters = clearPayload["filters"] as Map<*, *>

        assertEquals(listOf("new_breakfast", "new_lunch"), savedMeals.map { it.dishLabel }.sorted())
        assertEquals(
            listOf(
                FirestoreSyncOperation.UPSERT_DOCUMENT,
                FirestoreSyncOperation.CLEAR_COLLECTION,
                FirestoreSyncOperation.UPSERT_DOCUMENT,
                FirestoreSyncOperation.UPSERT_DOCUMENT
            ),
            outbox.map { it.operation }
        )
        assertEquals("plannedMeals:$weekStart", outbox[1].entityKey)
        assertEquals(weekStart, filters["weekStartDate"])
    }

    private fun profile(uid: String, weight: Double): UserProfile =
        UserProfile(
            uid = uid,
            name = "Test User",
            email = "test@example.com",
            age = 30,
            weight = weight,
            height = 170.0,
            sex = "Female",
            activityLevel = "moderate",
            goal = "maintain"
        )

    private fun activity(uid: String): ActivityLogEntity =
        ActivityLogEntity(
            uid = uid,
            type = "workout",
            name = "Run",
            timeString = "30 min",
            weightOrDuration = "30 min",
            calories = 220,
            timestamp = 1_700_000_000_000L
        )

    private fun loggedDish(name: String, calories: Float): LoggedDish =
        LoggedDish(
            dishNameEn = name,
            dishNamePh = name,
            weightGrams = 150f,
            confidence = 1f,
            foodId = 1,
            dishLabel = name.lowercase().replace(" ", "_"),
            calories = calories,
            protein = 20f,
            carbs = 12f,
            fat = 8f,
            fiber = 2f,
            sugar = 1f,
            saturatedFat = 1f,
            polyunsaturatedFat = 1f,
            monounsaturatedFat = 2f,
            transFat = 0f,
            cholesterol = 30f,
            sodium = 500f,
            potassium = 250f,
            vitaminA = 0f,
            vitaminC = 0f,
            calcium = 0f,
            iron = 2f
        )

    private fun plannedMeal(
        weekStartDate: String,
        dishLabel: String,
        mealSlot: String = "Lunch"
    ): PlannedMealEntity =
        PlannedMealEntity(
            dayIndex = 0,
            dishLabel = dishLabel,
            weekStartDate = weekStartDate,
            mealSlot = mealSlot
        )
}

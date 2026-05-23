package com.calorieko.app.data.remote.firestore

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.ActivityLogEntity
import com.calorieko.app.data.model.FirestoreOutboxEntity
import com.calorieko.app.data.model.MealLogEntity
import com.calorieko.app.data.model.MealLogItemEntity
import com.calorieko.app.data.model.WeightLogEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class FirestoreOperationExecutorTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var remoteClient: FakeRemoteClient
    private lateinit var executor: FirestoreOperationExecutor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        remoteClient = FakeRemoteClient()
        executor = FirestoreOperationExecutor(db, remoteClient)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun nullPayloadBackfillsActivityMealAndWeightFromRoom() = runBlocking {
        val uid = "uid-backfill"
        db.activityLogDao().insertLog(activity(uid, "activity-1"))
        val mealId = db.mealLogDao().insertMealLog(meal(uid, "meal-1"))
        db.mealLogItemDao().insertItems(listOf(mealItem(mealId, "item-1")))
        db.weightLogDao().upsertWeightLog(weight(uid, timestamp = 1111L))

        val activityOp = insertOutbox(backfillOp(uid, FirestoreEntityType.ACTIVITY_LOG, "activity-1", "users/$uid/activityLogs/activity-1"))
        val mealOp = insertOutbox(backfillOp(uid, FirestoreEntityType.MEAL_LOG, "meal-1", "users/$uid/mealLogs/meal-1"))
        val weightOp = insertOutbox(backfillOp(uid, FirestoreEntityType.WEIGHT_LOG, "1111", "users/$uid/weightLogs/1111"))

        assertEquals(FirestoreOperationResult.REMOTE_CONFIRMED, executor.execute(activityOp))
        assertEquals(FirestoreOperationResult.REMOTE_CONFIRMED, executor.execute(mealOp))
        assertEquals(FirestoreOperationResult.REMOTE_CONFIRMED, executor.execute(weightOp))

        assertEquals(listOf("users/$uid/activityLogs/activity-1", "users/$uid/weightLogs/1111"), remoteClient.setDocuments.map { it.path })
        assertEquals("users/$uid/mealLogs/meal-1", remoteClient.mealLogs.single().path)
        assertEquals("item-1", remoteClient.mealLogs.single().items.single().remoteId)
    }

    @Test
    fun missingBackfillWithoutLaterDeleteFailsAndProcessorKeepsRowPending() = runBlocking {
        val uid = "uid-missing"
        val operation = insertOutbox(backfillOp(uid, FirestoreEntityType.ACTIVITY_LOG, "missing", "users/$uid/activityLogs/missing"))
        val processor = FirestoreOutboxProcessor(db.firestoreOutboxDao(), executor, batchLimit = 10) { 2000L }

        val result = processor.process(uid)
        val pending = db.firestoreOutboxDao().getPending(uid, 10)

        assertEquals(FirestoreOutboxProcessResult.Retry, result)
        assertEquals(1, pending.size)
        assertEquals(operation.id, pending.single().id)
        assertEquals(1, pending.single().attemptCount)
        assertTrue(pending.single().lastError.orEmpty().contains("Unable to resolve Firestore payload"))
    }

    @Test
    fun missingBackfillWithLaterDeleteIsSupersededThenDeleteRuns() = runBlocking {
        val uid = "uid-superseded"
        val path = "users/$uid/activityLogs/deleted"
        insertOutbox(backfillOp(uid, FirestoreEntityType.ACTIVITY_LOG, "deleted", path, createdAt = 10))
        insertOutbox(
            FirestoreOutboxEntity(
                uid = uid,
                entityType = FirestoreEntityType.ACTIVITY_LOG,
                entityKey = "deleted",
                operation = FirestoreSyncOperation.DELETE_DOCUMENT,
                remotePath = path,
                payloadJson = null,
                createdAt = 20,
                updatedAt = 20
            )
        )
        val processor = FirestoreOutboxProcessor(db.firestoreOutboxDao(), executor, batchLimit = 10)

        val result = processor.process(uid)

        assertEquals(FirestoreOutboxProcessResult.Success(false), result)
        assertEquals(0, db.firestoreOutboxDao().pendingCount(uid))
        assertTrue(remoteClient.setDocuments.isEmpty())
        assertEquals(listOf(path), remoteClient.deletedDocuments)
    }

    @Test
    fun malformedMealPayloadFailsInsteadOfSkippingChildWrites() {
        runBlocking {
            val uid = "uid-malformed"
            val payload = FirestorePayloadSerializer.toJson(
                mapOf(
                    "meal" to mapOf("remoteId" to "meal-1"),
                    "items" to listOf(mapOf("payload" to mapOf("dishName" to "Broken")))
                )
            )
            val operation = FirestoreOutboxEntity(
                uid = uid,
                entityType = FirestoreEntityType.MEAL_LOG,
                entityKey = "meal-1",
                operation = FirestoreSyncOperation.UPSERT_DOCUMENT,
                remotePath = "users/$uid/mealLogs/meal-1",
                payloadJson = payload,
                createdAt = 1,
                updatedAt = 1
            )

            try {
                executor.execute(operation)
                fail("Expected malformed meal payload to fail.")
            } catch (e: IllegalStateException) {
                assertTrue(e.message.orEmpty().contains("missing remoteId"))
            }
        }
    }

    private suspend fun insertOutbox(operation: FirestoreOutboxEntity): FirestoreOutboxEntity {
        val id = db.firestoreOutboxDao().insert(operation)
        return db.firestoreOutboxDao().getPending(operation.uid, 100).first { it.id == id }
    }

    private fun backfillOp(
        uid: String,
        entityType: String,
        entityKey: String,
        remotePath: String,
        createdAt: Long = 1
    ): FirestoreOutboxEntity =
        FirestoreOutboxEntity(
            uid = uid,
            entityType = entityType,
            entityKey = entityKey,
            operation = FirestoreSyncOperation.UPSERT_DOCUMENT,
            remotePath = remotePath,
            payloadJson = null,
            createdAt = createdAt,
            updatedAt = createdAt
        )

    private fun activity(uid: String, remoteId: String): ActivityLogEntity =
        ActivityLogEntity(
            uid = uid,
            remoteId = remoteId,
            type = "workout",
            name = "Run",
            timeString = "30 min",
            weightOrDuration = "30 min",
            calories = 200,
            timestamp = 1_700_000_000_000L
        )

    private fun meal(uid: String, remoteId: String): MealLogEntity =
        MealLogEntity(
            uid = uid,
            remoteId = remoteId,
            mealType = "Lunch",
            timestamp = 1_700_000_000_000L
        )

    private fun mealItem(mealId: Long, remoteId: String): MealLogItemEntity =
        MealLogItemEntity(
            mealLogId = mealId,
            remoteId = remoteId,
            foodId = 1,
            dishName = "Adobo",
            weightGrams = 150f,
            calories = 250f,
            protein = 20f,
            carbs = 12f,
            fat = 8f
        )

    private fun weight(uid: String, timestamp: Long): WeightLogEntity =
        WeightLogEntity(
            uid = uid,
            dateEpochDay = LocalDate.of(2026, 5, 23).toEpochDay(),
            weightKg = 70.5,
            timestamp = timestamp
        )

    private class FakeRemoteClient : FirestoreRemoteClient {
        data class SetDocument(val path: String, val payload: Map<String, Any?>, val merge: Boolean)
        data class MealLogWrite(val path: String, val mealPayload: Map<String, Any?>, val items: List<FirestoreMealItemPayload>)

        val setDocuments = mutableListOf<SetDocument>()
        val deletedDocuments = mutableListOf<String>()
        val mealLogs = mutableListOf<MealLogWrite>()
        val deletedMealLogs = mutableListOf<String>()
        val clears = mutableListOf<Pair<String, Map<String, Any?>>>()

        override suspend fun setDocument(path: String, payload: Map<String, Any?>, merge: Boolean) {
            setDocuments += SetDocument(path, payload, merge)
        }

        override suspend fun deleteDocument(path: String) {
            deletedDocuments += path
        }

        override suspend fun setMealLog(path: String, mealPayload: Map<String, Any?>, items: List<FirestoreMealItemPayload>) {
            mealLogs += MealLogWrite(path, mealPayload, items)
        }

        override suspend fun deleteMealLogRecursive(path: String) {
            deletedMealLogs += path
        }

        override suspend fun clearCollection(path: String, filters: Map<String, Any?>) {
            clears += path to filters
        }
    }
}

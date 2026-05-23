package com.calorieko.app.data.remote.firestore

import com.calorieko.app.data.local.FirestoreOutboxDao
import com.calorieko.app.data.model.FirestoreOutboxEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreOutboxProcessorTest {
    @Test
    fun successDeletesRowsOnlyAfterExecutorConfirms() = runBlocking {
        val dao = FakeOutboxDao(listOf(operation(id = 1), operation(id = 2)))
        val executor = RecordingExecutor()
        val processor = FirestoreOutboxProcessor(dao, executor, batchLimit = 10)

        val result = processor.process(UID)

        assertEquals(FirestoreOutboxProcessResult.Success(false), result)
        assertEquals(listOf(1L, 2L), executor.executedIds)
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun firstFailureRecordsErrorAndStopsProcessingLaterRows() = runBlocking {
        val dao = FakeOutboxDao(listOf(operation(id = 1), operation(id = 2)))
        val executor = RecordingExecutor(failId = 1)
        val processor = FirestoreOutboxProcessor(dao, executor, batchLimit = 10) { 1234L }

        val result = processor.process(UID)

        assertEquals(FirestoreOutboxProcessResult.Retry, result)
        assertEquals(listOf(1L), executor.executedIds)
        assertEquals(2, dao.rows.size)
        assertEquals(1, dao.rows.first { it.id == 1L }.attemptCount)
        assertEquals("boom-1", dao.rows.first { it.id == 1L }.lastError)
    }

    @Test
    fun processesRowsInCreatedAtThenIdOrder() = runBlocking {
        val dao = FakeOutboxDao(
            listOf(
                operation(id = 3, createdAt = 20),
                operation(id = 2, createdAt = 10),
                operation(id = 1, createdAt = 10)
            )
        )
        val executor = RecordingExecutor()
        val processor = FirestoreOutboxProcessor(dao, executor, batchLimit = 10)

        processor.process(UID)

        assertEquals(listOf(1L, 2L, 3L), executor.executedIds)
    }

    @Test
    fun remainingRowsAfterBatchRequestFollowUp() = runBlocking {
        val dao = FakeOutboxDao(listOf(operation(id = 1), operation(id = 2), operation(id = 3)))
        val executor = RecordingExecutor()
        val processor = FirestoreOutboxProcessor(dao, executor, batchLimit = 2)

        val result = processor.process(UID)

        assertEquals(FirestoreOutboxProcessResult.Success(true), result)
        assertEquals(listOf(1L, 2L), executor.executedIds)
        assertEquals(listOf(3L), dao.rows.map { it.id })
    }

    private class RecordingExecutor(
        private val failId: Long? = null
    ) : FirestoreOutboxOperationExecutor {
        val executedIds = mutableListOf<Long>()

        override suspend fun execute(operation: FirestoreOutboxEntity): FirestoreOperationResult {
            executedIds += operation.id
            if (operation.id == failId) error("boom-${operation.id}")
            return FirestoreOperationResult.REMOTE_CONFIRMED
        }
    }

    private class FakeOutboxDao(initialRows: List<FirestoreOutboxEntity>) : FirestoreOutboxDao {
        val rows = initialRows.toMutableList()

        override suspend fun getPending(uid: String, limit: Int): List<FirestoreOutboxEntity> =
            rows.filter { it.uid == uid && it.state == "PENDING" }
                .sortedWith(compareBy<FirestoreOutboxEntity> { it.createdAt }.thenBy { it.id })
                .take(limit)

        override suspend fun insert(operation: FirestoreOutboxEntity): Long {
            rows += operation
            return operation.id
        }

        override suspend fun insertAll(operations: List<FirestoreOutboxEntity>) {
            rows += operations
        }

        override suspend fun deleteById(id: Long) {
            rows.removeAll { it.id == id }
        }

        override suspend fun recordFailure(id: Long, error: String, now: Long) {
            val index = rows.indexOfFirst { it.id == id }
            if (index >= 0) {
                val row = rows[index]
                rows[index] = row.copy(
                    attemptCount = row.attemptCount + 1,
                    lastError = error,
                    updatedAt = now
                )
            }
        }

        override suspend fun pendingCount(uid: String): Int =
            rows.count { it.uid == uid && it.state == "PENDING" }

        override suspend fun hasLaterPendingDelete(
            uid: String,
            remotePath: String,
            createdAt: Long,
            id: Long
        ): Boolean =
            rows.any {
                it.uid == uid &&
                    it.state == "PENDING" &&
                    it.remotePath == remotePath &&
                    it.operation in setOf(
                        FirestoreSyncOperation.DELETE_DOCUMENT,
                        FirestoreSyncOperation.DELETE_MEAL_LOG_RECURSIVE
                    ) &&
                    (it.createdAt > createdAt || (it.createdAt == createdAt && it.id > id))
            }

        override suspend fun deleteAllForUid(uid: String) {
            rows.removeAll { it.uid == uid }
        }

        override suspend fun deleteAll() {
            rows.clear()
        }
    }

    private companion object {
        const val UID = "uid-processor"

        fun operation(id: Long, createdAt: Long = id): FirestoreOutboxEntity =
            FirestoreOutboxEntity(
                id = id,
                uid = UID,
                entityType = FirestoreEntityType.ACTIVITY_LOG,
                entityKey = "entity-$id",
                operation = FirestoreSyncOperation.UPSERT_DOCUMENT,
                remotePath = "users/$UID/activityLogs/entity-$id",
                payloadJson = "{}",
                createdAt = createdAt,
                updatedAt = createdAt
            )
    }
}

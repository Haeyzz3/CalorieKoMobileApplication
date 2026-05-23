package com.calorieko.app.data.remote.firestore

import com.calorieko.app.data.local.FirestoreOutboxDao

sealed class FirestoreOutboxProcessResult {
    data class Success(val shouldEnqueueFollowUp: Boolean) : FirestoreOutboxProcessResult()
    data object Retry : FirestoreOutboxProcessResult()
}

class FirestoreOutboxProcessor(
    private val outboxDao: FirestoreOutboxDao,
    private val executor: FirestoreOutboxOperationExecutor,
    private val batchLimit: Int = 100,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun process(uid: String): FirestoreOutboxProcessResult {
        val pending = outboxDao.getPending(uid, batchLimit)
        if (pending.isEmpty()) {
            return FirestoreOutboxProcessResult.Success(shouldEnqueueFollowUp = false)
        }

        for (operation in pending) {
            try {
                executor.execute(operation)
                outboxDao.deleteById(operation.id)
            } catch (e: Exception) {
                outboxDao.recordFailure(
                    id = operation.id,
                    error = e.message ?: e::class.java.simpleName,
                    now = now()
                )
                return FirestoreOutboxProcessResult.Retry
            }
        }

        return FirestoreOutboxProcessResult.Success(
            shouldEnqueueFollowUp = outboxDao.pendingCount(uid) > 0
        )
    }
}

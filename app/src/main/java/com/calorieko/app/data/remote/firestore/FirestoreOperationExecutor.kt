package com.calorieko.app.data.remote.firestore

import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.FirestoreOutboxEntity

class FirestoreOperationExecutor(
    private val appDatabase: AppDatabase,
    private val remoteClient: FirestoreRemoteClient = FirebaseFirestoreRemoteClient()
) : FirestoreOutboxOperationExecutor {
    override suspend fun execute(operation: FirestoreOutboxEntity): FirestoreOperationResult {
        return when (operation.operation) {
            FirestoreSyncOperation.UPSERT_DOCUMENT -> upsertDocument(operation)
            FirestoreSyncOperation.DELETE_DOCUMENT -> {
                remoteClient.deleteDocument(operation.remotePath)
                FirestoreOperationResult.REMOTE_CONFIRMED
            }
            FirestoreSyncOperation.DELETE_MEAL_LOG_RECURSIVE -> {
                remoteClient.deleteMealLogRecursive(operation.remotePath)
                FirestoreOperationResult.REMOTE_CONFIRMED
            }
            FirestoreSyncOperation.CLEAR_COLLECTION -> clearCollection(operation)
            else -> error("Unsupported Firestore outbox operation: ${operation.operation}")
        }
    }

    private suspend fun upsertDocument(operation: FirestoreOutboxEntity): FirestoreOperationResult {
        val payload = operation.payloadJson
            ?.let(FirestorePayloadSerializer::fromJson)
            ?: resolveBackfillPayload(operation)
            ?: return handleMissingBackfillPayload(operation)

        if (operation.entityType == FirestoreEntityType.MEAL_LOG) {
            upsertMealLog(operation.remotePath, payload)
            return FirestoreOperationResult.REMOTE_CONFIRMED
        }

        remoteClient.setDocument(
            path = operation.remotePath,
            payload = payload,
            merge = operation.entityType == FirestoreEntityType.USER_PROFILE
        )
        return FirestoreOperationResult.REMOTE_CONFIRMED
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun upsertMealLog(remotePath: String, payload: Map<String, Any?>) {
        val mealPayload = payload["meal"] as? Map<String, Any?>
            ?: error("Meal payload missing parent document data for $remotePath")
        val items = payload["items"] as? List<*>
            ?: error("Meal payload missing item list for $remotePath")
        val itemPayloads = items.mapIndexed { index, item ->
            val itemMap = item as? Map<String, Any?>
                ?: error("Meal item payload at index $index is not a map for $remotePath")
            val remoteId = itemMap["remoteId"] as? String
                ?: error("Meal item payload at index $index missing remoteId for $remotePath")
            val itemPayload = itemMap["payload"] as? Map<String, Any?>
                ?: error("Meal item payload at index $index missing payload map for $remotePath")
            FirestoreMealItemPayload(remoteId, itemPayload)
        }

        remoteClient.setMealLog(remotePath, mealPayload, itemPayloads)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun clearCollection(operation: FirestoreOutboxEntity): FirestoreOperationResult {
        val payload = operation.payloadJson
            ?.let(FirestorePayloadSerializer::fromJson)
            ?: emptyMap()
        val filters = payload["filters"] as? Map<String, Any?> ?: emptyMap()
        val normalizedFilters = filters.mapValues { (field, value) ->
            normalizeFilterValue(field, value)
        }
        remoteClient.clearCollection(operation.remotePath, normalizedFilters)
        return FirestoreOperationResult.REMOTE_CONFIRMED
    }

    private fun normalizeFilterValue(field: String, value: Any?): Any? {
        return when (field) {
            "dayIndex" -> (value as? Number)?.toInt() ?: value
            else -> value
        }
    }

    private suspend fun resolveBackfillPayload(operation: FirestoreOutboxEntity): Map<String, Any?>? {
        return when (operation.entityType) {
            FirestoreEntityType.ACTIVITY_LOG -> {
                appDatabase.activityLogDao()
                    .getLogByRemoteId(operation.uid, operation.entityKey)
                    ?.let(FirestorePayloadSerializer::activityPayload)
            }
            FirestoreEntityType.MEAL_LOG -> {
                appDatabase.mealLogDao()
                    .getMealLogWithItemsByRemoteId(operation.uid, operation.entityKey)
                    ?.let { FirestorePayloadSerializer.mealPayload(it.mealLog, it.items) }
            }
            FirestoreEntityType.WEIGHT_LOG -> {
                operation.entityKey.toLongOrNull()
                    ?.let { appDatabase.weightLogDao().getWeightLogByTimestamp(operation.uid, it) }
                    ?.let(FirestorePayloadSerializer::weightLogPayload)
            }
            else -> null
        }
    }

    private suspend fun handleMissingBackfillPayload(operation: FirestoreOutboxEntity): FirestoreOperationResult {
        val hasLaterDelete = appDatabase.firestoreOutboxDao().hasLaterPendingDelete(
            uid = operation.uid,
            remotePath = operation.remotePath,
            createdAt = operation.createdAt,
            id = operation.id
        )
        if (hasLaterDelete) {
            return FirestoreOperationResult.SUPERSEDED_NO_REMOTE_WORK
        }

        throw IllegalStateException(
            "Unable to resolve Firestore payload for pending ${operation.entityType} ${operation.entityKey} at ${operation.remotePath}"
        )
    }
}

package com.calorieko.app.data.remote.firestore

import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.FirestoreOutboxEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreOperationExecutor(
    private val appDatabase: AppDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun execute(operation: FirestoreOutboxEntity) {
        when (operation.operation) {
            FirestoreSyncOperation.UPSERT_DOCUMENT -> upsertDocument(operation)
            FirestoreSyncOperation.DELETE_DOCUMENT -> firestore.document(operation.remotePath).delete().await()
            FirestoreSyncOperation.DELETE_MEAL_LOG_RECURSIVE -> deleteMealLogRecursive(operation.remotePath)
            FirestoreSyncOperation.CLEAR_COLLECTION -> clearCollection(operation)
            else -> error("Unsupported Firestore outbox operation: ${operation.operation}")
        }
    }

    private suspend fun upsertDocument(operation: FirestoreOutboxEntity) {
        val payload = operation.payloadJson
            ?.let(FirestorePayloadSerializer::fromJson)
            ?: resolveBackfillPayload(operation)
            ?: return

        if (operation.entityType == FirestoreEntityType.MEAL_LOG) {
            upsertMealLog(operation.remotePath, payload)
            return
        }

        val document = firestore.document(operation.remotePath)
        if (operation.entityType == FirestoreEntityType.USER_PROFILE) {
            document.set(payload, SetOptions.merge()).await()
        } else {
            document.set(payload).await()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun upsertMealLog(remotePath: String, payload: Map<String, Any?>) {
        val mealPayload = payload["meal"] as? Map<String, Any?> ?: payload
        val items = payload["items"] as? List<*> ?: emptyList<Any>()
        val mealDocument = firestore.document(remotePath)

        if (items.size + 1 <= 500) {
            val batch = firestore.batch()
            batch.set(mealDocument, mealPayload)
            for (item in items) {
                val itemMap = item as? Map<String, Any?> ?: continue
                val remoteId = itemMap["remoteId"]?.toString() ?: continue
                val itemPayload = itemMap["payload"] as? Map<String, Any?> ?: continue
                batch.set(mealDocument.collection("items").document(remoteId), itemPayload)
            }
            batch.commit().await()
            return
        }

        mealDocument.set(mealPayload).await()
        items.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            for (item in chunk) {
                val itemMap = item as? Map<String, Any?> ?: continue
                val remoteId = itemMap["remoteId"]?.toString() ?: continue
                val itemPayload = itemMap["payload"] as? Map<String, Any?> ?: continue
                batch.set(mealDocument.collection("items").document(remoteId), itemPayload)
            }
            batch.commit().await()
        }
    }

    private suspend fun deleteMealLogRecursive(remotePath: String) {
        val mealDocument = firestore.document(remotePath)
        val itemSnapshot = mealDocument.collection("items").get().await()
        itemSnapshot.documents.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
        mealDocument.delete().await()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun clearCollection(operation: FirestoreOutboxEntity) {
        val payload = operation.payloadJson
            ?.let(FirestorePayloadSerializer::fromJson)
            ?: emptyMap()
        val filters = payload["filters"] as? Map<String, Any?> ?: emptyMap()

        var query: Query = firestore.collection(operation.remotePath)
        for ((field, value) in filters) {
            query = query.whereEqualTo(field, normalizeFilterValue(field, value))
        }

        val snapshot = query.get().await()
        snapshot.documents.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
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
}

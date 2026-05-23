package com.calorieko.app.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class FirestoreMealItemPayload(
    val remoteId: String,
    val payload: Map<String, Any?>
)

interface FirestoreRemoteClient {
    suspend fun setDocument(path: String, payload: Map<String, Any?>, merge: Boolean = false)
    suspend fun deleteDocument(path: String)
    suspend fun setMealLog(path: String, mealPayload: Map<String, Any?>, items: List<FirestoreMealItemPayload>)
    suspend fun deleteMealLogRecursive(path: String)
    suspend fun clearCollection(path: String, filters: Map<String, Any?>)
}

class FirebaseFirestoreRemoteClient(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FirestoreRemoteClient {
    override suspend fun setDocument(path: String, payload: Map<String, Any?>, merge: Boolean) {
        val document = firestore.document(path)
        if (merge) {
            document.set(payload, SetOptions.merge()).await()
        } else {
            document.set(payload).await()
        }
    }

    override suspend fun deleteDocument(path: String) {
        firestore.document(path).delete().await()
    }

    override suspend fun setMealLog(
        path: String,
        mealPayload: Map<String, Any?>,
        items: List<FirestoreMealItemPayload>
    ) {
        val mealDocument = firestore.document(path)
        if (items.size + 1 <= 500) {
            val batch = firestore.batch()
            batch.set(mealDocument, mealPayload)
            for (item in items) {
                batch.set(mealDocument.collection("items").document(item.remoteId), item.payload)
            }
            batch.commit().await()
            return
        }

        mealDocument.set(mealPayload).await()
        items.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            for (item in chunk) {
                batch.set(mealDocument.collection("items").document(item.remoteId), item.payload)
            }
            batch.commit().await()
        }
    }

    override suspend fun deleteMealLogRecursive(path: String) {
        val mealDocument = firestore.document(path)
        val itemSnapshot = mealDocument.collection("items").get().await()
        itemSnapshot.documents.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
        mealDocument.delete().await()
    }

    override suspend fun clearCollection(path: String, filters: Map<String, Any?>) {
        var query: Query = firestore.collection(path)
        for ((field, value) in filters) {
            query = query.whereEqualTo(field, value)
        }

        val snapshot = query.get().await()
        snapshot.documents.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }
}

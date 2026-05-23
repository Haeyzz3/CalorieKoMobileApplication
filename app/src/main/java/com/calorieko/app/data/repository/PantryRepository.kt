package com.calorieko.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.data.model.PantryItem
import com.calorieko.app.data.remote.api.AutoSyncManager
import com.calorieko.app.data.remote.firestore.FirestoreAutoSyncManager
import com.calorieko.app.data.remote.firestore.FirestoreEntityType
import com.calorieko.app.data.remote.firestore.FirestorePayloadSerializer

class PantryRepository(
    private val db: AppDatabase,
    private val appContext: Context
) {
    private val pantryDao = db.pantryDao()
    private val outboxDao = db.firestoreOutboxDao()

    suspend fun addIngredient(uid: String, ingredientName: String) {
        val normalized = ingredientName.trim().lowercase()
        if (normalized.isBlank()) return
        addIngredients(uid, listOf(normalized))
    }

    suspend fun addIngredients(uid: String, ingredientNames: List<String>) {
        val normalized = ingredientNames.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
        if (normalized.isEmpty()) return
        if (uid.isBlank()) {
            pantryDao.insertAll(normalized.map { PantryItem(ingredientName = it) })
            return
        }
        val now = System.currentTimeMillis()

        db.withTransaction {
            pantryDao.insertAll(normalized.map { PantryItem(ingredientName = it) })
            outboxDao.insertAll(
                normalized.map { name ->
                    val item = PantryItem(ingredientName = name)
                    FirestorePayloadSerializer.upsert(
                        uid = uid,
                        entityType = FirestoreEntityType.PANTRY_ITEM,
                        entityKey = name,
                        remotePath = FirestorePayloadSerializer.pantryItemPath(uid, name),
                        payload = FirestorePayloadSerializer.pantryPayload(item, now),
                        now = now
                    )
                }
            )
        }

        triggerSync(uid)
    }

    suspend fun removeIngredient(uid: String, ingredientName: String) {
        removeIngredients(uid, listOf(ingredientName))
    }

    suspend fun removeIngredients(uid: String, ingredientNames: List<String>) {
        val normalized = ingredientNames.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
        if (normalized.isEmpty()) return
        if (uid.isBlank()) {
            pantryDao.deleteItems(normalized)
            return
        }
        val now = System.currentTimeMillis()

        db.withTransaction {
            pantryDao.deleteItems(normalized)
            outboxDao.insertAll(
                normalized.map { name ->
                    FirestorePayloadSerializer.deleteDocument(
                        uid = uid,
                        entityType = FirestoreEntityType.PANTRY_ITEM,
                        entityKey = name,
                        remotePath = FirestorePayloadSerializer.pantryItemPath(uid, name),
                        now = now
                    )
                }
            )
        }

        triggerSync(uid)
    }

    suspend fun applySelection(uid: String, selectedKeys: Set<String>): Pair<Int, Int> {
        val selected = selectedKeys.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        val currentPantry = pantryDao.getAllItemsList().map { it.trim().lowercase() }.toSet()
        val toAdd = selected - currentPantry
        val toRemove = currentPantry - selected
        if (toAdd.isEmpty() && toRemove.isEmpty()) return 0 to 0
        if (uid.isBlank()) {
            if (toAdd.isNotEmpty()) pantryDao.insertAll(toAdd.map { PantryItem(ingredientName = it) })
            if (toRemove.isNotEmpty()) pantryDao.deleteItems(toRemove.toList())
            return toAdd.size to toRemove.size
        }

        val now = System.currentTimeMillis()
        db.withTransaction {
            if (toAdd.isNotEmpty()) {
                pantryDao.insertAll(toAdd.map { PantryItem(ingredientName = it) })
            }
            if (toRemove.isNotEmpty()) {
                pantryDao.deleteItems(toRemove.toList())
            }

            val operations = buildList {
                addAll(
                    toAdd.map { name ->
                        val item = PantryItem(ingredientName = name)
                        FirestorePayloadSerializer.upsert(
                            uid = uid,
                            entityType = FirestoreEntityType.PANTRY_ITEM,
                            entityKey = name,
                            remotePath = FirestorePayloadSerializer.pantryItemPath(uid, name),
                            payload = FirestorePayloadSerializer.pantryPayload(item, now),
                            now = now
                        )
                    }
                )
                addAll(
                    toRemove.map { name ->
                        FirestorePayloadSerializer.deleteDocument(
                            uid = uid,
                            entityType = FirestoreEntityType.PANTRY_ITEM,
                            entityKey = name,
                            remotePath = FirestorePayloadSerializer.pantryItemPath(uid, name),
                            now = now
                        )
                    }
                )
            }
            outboxDao.insertAll(operations)
        }

        triggerSync(uid)
        return toAdd.size to toRemove.size
    }

    suspend fun clearAll(uid: String) {
        if (uid.isBlank()) {
            pantryDao.clearAllItems()
            return
        }
        val now = System.currentTimeMillis()
        db.withTransaction {
            outboxDao.insert(
                FirestorePayloadSerializer.clearCollection(
                    uid = uid,
                    collectionKey = "pantryItems",
                    remotePath = FirestorePayloadSerializer.pantryCollectionPath(uid),
                    now = now
                )
            )
            pantryDao.clearAllItems()
        }

        triggerSync(uid)
    }

    private fun triggerSync(uid: String) {
        if (uid.isBlank()) return
        FirestoreAutoSyncManager.triggerSync(appContext, uid)
        AutoSyncManager.triggerSync(appContext, uid)
    }
}

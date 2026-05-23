package com.calorieko.app.data.remote.firestore

import com.calorieko.app.data.model.FirestoreOutboxEntity

enum class FirestoreOperationResult {
    REMOTE_CONFIRMED,
    SUPERSEDED_NO_REMOTE_WORK
}

interface FirestoreOutboxOperationExecutor {
    suspend fun execute(operation: FirestoreOutboxEntity): FirestoreOperationResult
}

package com.calorieko.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "firestore_outbox",
    indices = [
        Index(value = ["uid", "state", "created_at"]),
        Index(value = ["uid", "entity_type", "entity_key"])
    ]
)
data class FirestoreOutboxEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "uid")
    val uid: String,

    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "entity_key")
    val entityKey: String,

    @ColumnInfo(name = "operation")
    val operation: String,

    @ColumnInfo(name = "remote_path")
    val remotePath: String,

    @ColumnInfo(name = "payload_json")
    val payloadJson: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "attempt_count", defaultValue = "0")
    val attemptCount: Int = 0,

    @ColumnInfo(name = "last_error")
    val lastError: String? = null,

    @ColumnInfo(name = "state", defaultValue = "'PENDING'")
    val state: String = "PENDING"
)

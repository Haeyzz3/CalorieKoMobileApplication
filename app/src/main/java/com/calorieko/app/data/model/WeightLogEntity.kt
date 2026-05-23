package com.calorieko.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "weight_log_table",
    primaryKeys = ["uid", "timestamp"]
)
data class WeightLogEntity(
    val uid: String,
    @ColumnInfo(name = "date_epoch_day") val dateEpochDay: Long,
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
    val timestamp: Long,

    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    // Legacy sync-status metadata used by API delta/restore flows.
    // Firestore retry and durability are governed by firestore_outbox.
    @ColumnInfo(name = "sync_status") val syncStatus: Int = 0
)

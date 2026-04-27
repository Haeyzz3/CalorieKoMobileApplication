package com.calorieko.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_log_table")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uid: String,
    val type: String,
    val name: String,
    val timeString: String,
    val weightOrDuration: String,

    // Nutrition / Burn Data
    val calories: Int,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fats: Int = 0,
    val sodium: Int = 0,

    val timestamp: Long,

    // --- NEW GPS WORKOUT FIELDS ---
    val distanceKm: Double? = null,
    val pace: Double? = null,
    val movingTimeSeconds: Long? = null,
    val steps: Int? = null,
    val encodedPath: String? = null, // Coordinate string: "lat,lng|lat,lng"
    val mapType: String? = null,
    val photoUri: String? = null,
    val notes: String? = null,
    val activityTag: String? = null,

    // --- DELTA SYNC: Last-modified timestamp (epoch millis) ---
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),

    // --- OFFLINE-FIRST: Sync status flag ---
    // 0 = PENDING (saved locally, not yet synced)
    // 1 = SYNCED  (successfully pushed to Firestore + Laravel)
    @ColumnInfo(name = "sync_status") val syncStatus: Int = 0
)
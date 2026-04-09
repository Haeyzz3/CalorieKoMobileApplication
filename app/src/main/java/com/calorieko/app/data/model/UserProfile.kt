package com.calorieko.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val uid: String, // The Firebase UID
    val name: String,
    val email: String,
    val age: Int,
    val weight: Double,
    val height: Double,
    val sex: String = "",
    val activityLevel: String = "", // "not_very_active", "lightly_active", "active", "very_active"
    val goal: String,
    val streak: Int = 0,         // Current streak counter
    val level: Int = 1,          // Global user level (increments when milestones tier is maxed)
    val globalXp: Int = 0,       // Accumulated XP from all badge unlocks
    val milestonesTier: Int = 1, // Current difficulty tier for badges (scales thresholds)
    val photoUrl: String = "",   // Profile photo URL from backend

    // --- DELTA SYNC: Last-modified timestamp (epoch millis) ---
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

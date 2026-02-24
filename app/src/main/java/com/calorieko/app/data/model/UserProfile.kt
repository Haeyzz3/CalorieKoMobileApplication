package com.calorieko.app.data.model

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
    val goal: String
)
package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calorieko.app.data.model.UserProfile

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserProfile): Long

    // Checks if the user's physical profile is saved locally
    @Query("SELECT * FROM user_profile WHERE uid = :firebaseUid LIMIT 1")
    suspend fun getUserProfile(firebaseUid: String): UserProfile?

    @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
    suspend fun getUser(uid: String): UserProfile?
}

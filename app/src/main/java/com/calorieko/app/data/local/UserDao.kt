package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calorieko.app.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserProfile): Long

    // Checks if the user's physical profile is saved locally
    @Query("SELECT * FROM user_profile WHERE uid = :firebaseUid LIMIT 1")
    suspend fun getUserProfile(firebaseUid: String): UserProfile?

    @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
    suspend fun getUser(uid: String): UserProfile?

    /** Observe user profile reactively (emits on every Room change). */
    @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
    fun observeUser(uid: String): Flow<UserProfile?>

    /** Deletes all user profiles. Used during logout to clear user data only. */
    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
}

package com.calorieko.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.calorieko.app.data.model.FirestoreOutboxEntity

@Dao
interface FirestoreOutboxDao {
    @Query(
        """
        SELECT * FROM firestore_outbox
        WHERE uid = :uid AND state = 'PENDING'
        ORDER BY created_at ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getPending(uid: String, limit: Int): List<FirestoreOutboxEntity>

    @Insert
    suspend fun insert(operation: FirestoreOutboxEntity): Long

    @Insert
    suspend fun insertAll(operations: List<FirestoreOutboxEntity>)

    @Query("DELETE FROM firestore_outbox WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        UPDATE firestore_outbox
        SET attempt_count = attempt_count + 1,
            last_error = :error,
            updated_at = :now
        WHERE id = :id
        """
    )
    suspend fun recordFailure(id: Long, error: String, now: Long)

    @Query("SELECT COUNT(*) FROM firestore_outbox WHERE uid = :uid AND state = 'PENDING'")
    suspend fun pendingCount(uid: String): Int

    @Query("DELETE FROM firestore_outbox WHERE uid = :uid")
    suspend fun deleteAllForUid(uid: String)

    @Query("DELETE FROM firestore_outbox")
    suspend fun deleteAll()
}

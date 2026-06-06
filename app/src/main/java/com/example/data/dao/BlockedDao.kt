package com.example.data.dao

import androidx.room.*
import com.example.data.entity.BlockedNumber
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedDao {
    @Query("SELECT * FROM blocked_numbers ORDER BY timestamp DESC")
    fun getAllBlockedFlow(): Flow<List<BlockedNumber>>

    @Query("SELECT * FROM blocked_numbers ORDER BY timestamp DESC")
    suspend fun getAllBlocked(): List<BlockedNumber>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE phoneNumber = :number LIMIT 1)")
    fun isNumberBlockedFlow(number: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE phoneNumber = :number LIMIT 1)")
    suspend fun isNumberBlocked(number: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocked(blockedNumber: BlockedNumber): Long

    @Query("DELETE FROM blocked_numbers WHERE phoneNumber = :number")
    suspend fun deleteBlockedByNumber(number: String)

    @Delete
    suspend fun deleteBlocked(blockedNumber: BlockedNumber)
}

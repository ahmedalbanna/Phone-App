package com.example.data.dao

import androidx.room.*
import com.example.data.entity.CallRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {
    @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
    fun getAllRecordsFlow(): Flow<List<CallRecord>>

    @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
    suspend fun getAllRecords(): List<CallRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: CallRecord): Long

    @Delete
    suspend fun deleteRecord(record: CallRecord)

    @Query("DELETE FROM call_records")
    suspend fun deleteAllRecords()
}

package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_records")
data class CallRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String,
    val callerName: String?,
    val type: String, // "INCOMING", "OUTGOING", "MISSED"
    val timestamp: Long,
    val duration: Long // in seconds
)

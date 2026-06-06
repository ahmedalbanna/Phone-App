package com.example.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.dao.BlockedDao
import com.example.data.dao.CallDao
import com.example.data.entity.BlockedNumber
import com.example.data.entity.CallRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CallRepository(
    private val context: Context,
    private val callDao: CallDao,
    private val blockedDao: BlockedDao
) {
    // Flow of custom database call records
    val localCallRecordsFlow: Flow<List<CallRecord>> = callDao.getAllRecordsFlow().flowOn(Dispatchers.IO)

    // Flow of blocked numbers
    val blockedNumbersFlow: Flow<List<BlockedNumber>> = blockedDao.getAllBlockedFlow().flowOn(Dispatchers.IO)

    suspend fun insertCallRecord(record: CallRecord) = withContext(Dispatchers.IO) {
        callDao.insertRecord(record)
    }

    suspend fun deleteCallRecord(record: CallRecord) = withContext(Dispatchers.IO) {
        callDao.deleteRecord(record)
    }

    suspend fun clearAllLocalRecords() = withContext(Dispatchers.IO) {
        callDao.deleteAllRecords()
    }

    suspend fun blockNumber(number: String, reason: String?) = withContext(Dispatchers.IO) {
        val cleanNumber = number.trim()
        if (cleanNumber.isNotEmpty()) {
            blockedDao.insertBlocked(BlockedNumber(phoneNumber = cleanNumber, blockReason = reason))
        }
    }

    suspend fun unblockNumber(number: String) = withContext(Dispatchers.IO) {
        blockedDao.deleteBlockedByNumber(number.trim())
    }

    suspend fun isBlocked(number: String): Boolean = withContext(Dispatchers.IO) {
        blockedDao.isNumberBlocked(number.trim())
    }

    fun isBlockedFlow(number: String): Flow<Boolean> = blockedDao.isNumberBlockedFlow(number.trim()).flowOn(Dispatchers.IO)

    // Query system call log if permission is granted
    suspend fun getSystemCallLog(): List<CallRecord> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CallRecord>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return@withContext emptyList<CallRecord>()
        }

        val cursor: Cursor? = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls._ID
            ),
            null,
            null,
            "${CallLog.Calls.DATE} DESC LIMIT 50"
        )

        cursor?.use {
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
            val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
            val idIndex = it.getColumnIndex(CallLog.Calls._ID)

            while (it.moveToNext()) {
                val number = if (numberIndex != -1) it.getString(numberIndex) ?: "غير معروف" else "غير معروف"
                val name = if (nameIndex != -1) it.getString(nameIndex) else null
                val rawType = if (typeIndex != -1) it.getInt(typeIndex) else CallLog.Calls.OUTGOING_TYPE
                val date = if (dateIndex != -1) it.getLong(dateIndex) else System.currentTimeMillis()
                val duration = if (durationIndex != -1) it.getLong(durationIndex) else 0L
                val recordId = if (idIndex != -1) it.getInt(idIndex) else 0

                val finalType = when (rawType) {
                    CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                    CallLog.Calls.MISSED_TYPE -> "MISSED"
                    else -> "OUTGOING"
                }

                list.add(
                    CallRecord(
                        id = recordId,
                        phoneNumber = number,
                        callerName = name,
                        type = finalType,
                        timestamp = date,
                        duration = duration
                    )
                )
            }
        }
        return@withContext list
    }
}

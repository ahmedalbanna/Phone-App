package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.BlockedNumber
import com.example.data.entity.CallRecord
import com.example.data.model.ContactItem
import com.example.data.repository.CallRepository
import com.example.data.repository.ContactsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CallSessionState {
    object Idle : CallSessionState
    data class Dialing(
        val number: String,
        val contactName: String?,
        val isIncoming: Boolean = false
    ) : CallSessionState
    
    data class Active(
        val number: String,
        val contactName: String?,
        val durationSeconds: Int = 0,
        val isMuted: Boolean = false,
        val isSpeakerOn: Boolean = false
    ) : CallSessionState
}

class CallViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val callRepository = CallRepository(context, database.callDao(), database.blockedDao())
    private val contactsRepository = ContactsRepository(context)

    // Current phone number typed in dialer
    private val _dialerNumber = MutableStateFlow("")
    val dialerNumber: StateFlow<String> = _dialerNumber.asStateFlow()

    // Search query for contacts tab
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active call session state
    private val _activeCallState = MutableStateFlow<CallSessionState>(CallSessionState.Idle)
    val activeCallState: StateFlow<CallSessionState> = _activeCallState.asStateFlow()

    // List of contacts
    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts: StateFlow<List<ContactItem>> = _contacts.asStateFlow()

    // List of unified call history (Local database + system calls)
    private val _callHistory = MutableStateFlow<List<CallRecord>>(emptyList())
    val callHistory: StateFlow<List<CallRecord>> = _callHistory.asStateFlow()

    // Flow of blocked numbers from database
    val blockedNumbers: StateFlow<List<BlockedNumber>> = callRepository.blockedNumbersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var timerJob: Job? = null

    init {
        // Hydrate list on creation
        loadContacts()
        observeLocalCallsAndSync()
    }

    fun onDialDigit(digit: Char) {
        if (_dialerNumber.value.length < 20) {
            _dialerNumber.value += digit
        }
    }

    fun onBackspace() {
        if (_dialerNumber.value.isNotEmpty()) {
            _dialerNumber.value = _dialerNumber.value.dropLast(1)
        }
    }

    fun clearDialer() {
        _dialerNumber.value = ""
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        loadContacts(query)
    }

    fun loadContacts(query: String = "") {
        viewModelScope.launch {
            _contacts.value = contactsRepository.getContacts(query)
        }
    }

    private fun observeLocalCallsAndSync() {
        viewModelScope.launch {
            callRepository.localCallRecordsFlow.collectLatest { localList ->
                // Try to load system calls and merge
                val systemCalls = try {
                    callRepository.getSystemCallLog()
                } catch (e: Exception) {
                    emptyList()
                }
                
                // Prepopulate with a few default mock calls if both lists are empty (first run UX)
                val baseLocalList = if (localList.isEmpty() && systemCalls.isEmpty()) {
                    createDefaultPrepopulatedLogs()
                } else {
                    localList
                }

                // Merge and remove duplicates (by number + timestamp)
                val merged = (baseLocalList + systemCalls)
                    .distinctBy { "${it.phoneNumber}_${it.timestamp}_${it.type}" }
                    .sortedByDescending { it.timestamp }

                _callHistory.value = merged
            }
        }
    }

    private suspend fun createDefaultPrepopulatedLogs(): List<CallRecord> {
        val now = System.currentTimeMillis()
        val HOUR_MS = 3600000L
        val DAY_MS = 86400000L
        
        val defaultList = listOf(
            CallRecord(
                phoneNumber = "0501234567",
                callerName = "أحمد البنا",
                type = "INCOMING",
                timestamp = now - 2 * HOUR_MS,
                duration = 142
            ),
            CallRecord(
                phoneNumber = "0559876543",
                callerName = "أمل الحربي",
                type = "MISSED",
                timestamp = now - 5 * HOUR_MS,
                duration = 0
            ),
            CallRecord(
                phoneNumber = "0543210987",
                callerName = "خالد العتيبي",
                type = "OUTGOING",
                timestamp = now - 1 * DAY_MS,
                duration = 310
            )
        )
        
        // Write defaults to DB so they persist
        for (record in defaultList) {
            callRepository.insertCallRecord(record)
        }
        return defaultList
    }

    // Triggered when user attempts to initiate a call
    fun initiateCall(number: String) {
        val cleanNumber = number.trim()
        if (cleanNumber.isEmpty()) return

        viewModelScope.launch {
            // 1. Check if number is blocked
            if (callRepository.isBlocked(cleanNumber)) {
                Toast.makeText(context, "🚫 المكالمة محظورة لأن الرقم في قائمة الحظر!", Toast.LENGTH_LONG).show()
                return@launch
            }

            // 2. Perform background actual Android intent (if they are on a real phone)
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
            } catch (e: Exception) {
                Log.e("CallViewModel", "Unable to launch dial intent", e)
            }

            // 3. Initiate our beautiful in-app active call simulator!
            // Look up the name from local contacts dictionary
            val contactName = _contacts.value.find { it.phoneNumber == cleanNumber }?.name
            _activeCallState.value = CallSessionState.Dialing(cleanNumber, contactName, isIncoming = false)

            // Simulate call connection after 2 seconds
            delay(2000)
            if (_activeCallState.value is CallSessionState.Dialing) {
                startActiveCallTimer(cleanNumber, contactName)
            }
        }
    }

    // Call acceptance / Simulation trigger for Incoming Calls
    fun simulateIncomingCall(number: String) {
        val cleanNumber = number.trim()
        if (cleanNumber.isEmpty()) return

        viewModelScope.launch {
            if (callRepository.isBlocked(cleanNumber)) {
                Toast.makeText(context, "🛡️ تم قمع مكالمة واردة من الرقم المحظور: $cleanNumber", Toast.LENGTH_LONG).show()
                // Save a muted log
                callRepository.insertCallRecord(
                    CallRecord(
                        phoneNumber = cleanNumber,
                        callerName = null,
                        type = "MISSED",
                        timestamp = System.currentTimeMillis(),
                        duration = 0
                    )
                )
                return@launch
            }

            val contactName = _contacts.value.find { it.phoneNumber == cleanNumber }?.name
            _activeCallState.value = CallSessionState.Dialing(cleanNumber, contactName, isIncoming = true)
        }
    }

    fun acceptIncomingCall() {
        val currentState = _activeCallState.value
        if (currentState is CallSessionState.Dialing && currentState.isIncoming) {
            startActiveCallTimer(currentState.number, currentState.contactName)
        }
    }

    fun rejectIncomingCall() {
        val currentState = _activeCallState.value
        if (currentState is CallSessionState.Dialing) {
            val record = CallRecord(
                phoneNumber = currentState.number,
                callerName = currentState.contactName,
                type = "MISSED",
                timestamp = System.currentTimeMillis(),
                duration = 0
            )
            viewModelScope.launch {
                callRepository.insertCallRecord(record)
            }
            _activeCallState.value = CallSessionState.Idle
        }
    }

    private fun startActiveCallTimer(number: String, contactName: String?) {
        timerJob?.cancel()
        _activeCallState.value = CallSessionState.Active(
            number = number,
            contactName = contactName,
            durationSeconds = 0,
            isMuted = false,
            isSpeakerOn = false
        )

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _activeCallState.value
                if (current is CallSessionState.Active) {
                    _activeCallState.value = current.copy(durationSeconds = current.durationSeconds + 1)
                } else {
                    break
                }
            }
        }
    }

    fun toggleMute() {
        val current = _activeCallState.value
        if (current is CallSessionState.Active) {
            _activeCallState.value = current.copy(isMuted = !current.isMuted)
        }
    }

    fun toggleSpeaker() {
        val current = _activeCallState.value
        if (current is CallSessionState.Active) {
            _activeCallState.value = current.copy(isSpeakerOn = !current.isSpeakerOn)
        }
    }

    fun endCall() {
        timerJob?.cancel()
        timerJob = null

        val current = _activeCallState.value
        if (current is CallSessionState.Active) {
            val record = CallRecord(
                phoneNumber = current.number,
                callerName = current.contactName,
                type = "OUTGOING", // custom simulated outbound call
                timestamp = System.currentTimeMillis(),
                duration = current.durationSeconds.toLong()
            )
            viewModelScope.launch {
                callRepository.insertCallRecord(record)
            }
        }
        _activeCallState.value = CallSessionState.Idle
        clearDialer()
    }

    // Blacklist Operations
    fun blockNewNumber(number: String, reason: String? = null) {
        viewModelScope.launch {
            callRepository.blockNumber(number, reason)
            Toast.makeText(context, "🔒 تم إضافة $number إلى القائمة السوداء", Toast.LENGTH_SHORT).show()
        }
    }

    fun unblockNumber(number: String) {
        viewModelScope.launch {
            callRepository.unblockNumber(number)
            Toast.makeText(context, "🔓 تم إزالة $number من القائمة السوداء", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteCallHistoryRecord(record: CallRecord) {
        viewModelScope.launch {
            callRepository.deleteCallRecord(record)
            Toast.makeText(context, "🗑️ تم حذف السجل", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            callRepository.clearAllLocalRecords()
            Toast.makeText(context, "🧹 تم تنظيف السجل بالكامل", Toast.LENGTH_SHORT).show()
        }
    }
}

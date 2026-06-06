package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.entity.BlockedNumber
import com.example.data.entity.CallRecord
import com.example.data.model.ContactItem
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BorderGray
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import com.example.ui.theme.OrangeGold
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SoftBlueActive
import com.example.ui.viewmodel.CallSessionState
import com.example.ui.viewmodel.CallViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDialerApp(
    viewModel: CallViewModel = viewModel()
) {
    // Force RTL local layout direction for consistent Arabic look and feel
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var currentTab by remember { mutableStateOf(0) } // 0 = سِجل المكالمات, 1 = جهات الاتصال, 2 = المحظورون
        var isDialerVisible by remember { mutableStateOf(false) }

        val dialerNumber by viewModel.dialerNumber.collectAsStateWithLifecycle()
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
        val activeCallState by viewModel.activeCallState.collectAsStateWithLifecycle()
        val contacts by viewModel.contacts.collectAsStateWithLifecycle()
        val callHistory by viewModel.callHistory.collectAsStateWithLifecycle()
        val blockedNumbers by viewModel.blockedNumbers.collectAsStateWithLifecycle()

        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                "هاتفي",
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite,
                                modifier = Modifier.testTag("app_title")
                            )
                            Text(
                                "لوحة اتصال ذكية تدير مكالماتك ومحظوريك بأمان",
                                fontSize = 12.sp,
                                color = TextGray
                            )
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = DarkBg,
                        titleContentColor = TextWhite
                    ),
                    actions = {
                        IconButton(
                            onClick = { viewModel.clearAllHistory() },
                            modifier = Modifier.testTag("clear_history_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "تنظيف الكل",
                                tint = TextGray
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        label = { Text("السجل", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.History, contentDescription = "سجل المكالمات") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavyDark,
                            selectedTextColor = NavyDark,
                            indicatorColor = SoftBlueActive,
                            unselectedIconColor = TextGray,
                            unselectedTextColor = TextGray
                        ),
                        modifier = Modifier.testTag("tab_history")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        label = { Text("الجهات", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.People, contentDescription = "جهات الاتصال") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavyDark,
                            selectedTextColor = NavyDark,
                            indicatorColor = SoftBlueActive,
                            unselectedIconColor = TextGray,
                            unselectedTextColor = TextGray
                        ),
                        modifier = Modifier.testTag("tab_contacts")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        label = { Text("المحظورون", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Block, contentDescription = "الأرقام المحظورة") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavyDark,
                            selectedTextColor = NavyDark,
                            indicatorColor = SoftBlueActive,
                            unselectedIconColor = TextGray,
                            unselectedTextColor = TextGray
                        ),
                        modifier = Modifier.testTag("tab_blocked")
                    )
                }
            },
            floatingActionButton = {
                // Dialer trigger button shown on first 2 tabs
                if (currentTab != 2) {
                    ExtendedFloatingActionButton(
                        onClick = { isDialerVisible = !isDialerVisible },
                        containerColor = PrimaryEmerald,
                        contentColor = Color.White,
                        icon = { Icon(Icons.Default.Dialpad, contentDescription = "لوحة الأرقام") },
                        text = { Text("لوحة الاتصال", fontWeight = FontWeight.Bold) },
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .testTag("toggle_dialer_fab")
                    )
                }
            },
            containerColor = DarkBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Tab Contents
                Crossfade(targetState = currentTab, label = "tabTransition") { tab ->
                    when (tab) {
                        0 -> HistoryTabContent(
                            callHistory = callHistory,
                            onDial = { viewModel.initiateCall(it) },
                            onDelete = { viewModel.deleteCallHistoryRecord(it) },
                            onBlock = { viewModel.blockNewNumber(it, "حظر من السجل") }
                        )
                        1 -> ContactsTabContent(
                            contacts = contacts,
                            searchQuery = searchQuery,
                            onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                            onCall = { viewModel.initiateCall(it) },
                            onSimulateCall = { viewModel.simulateIncomingCall(it) },
                            onBlock = { viewModel.blockNewNumber(it, "حظر من جهات الاتصال") }
                        )
                        2 -> BlockedTabContent(
                            blockedNumbers = blockedNumbers,
                            onBlock = { number, reason -> viewModel.blockNewNumber(number, reason) },
                            onUnblock = { viewModel.unblockNumber(it) }
                        )
                    }
                }

                // Smooth Sliding Dialer Plate at the bottom
                AnimatedVisibility(
                    visible = isDialerVisible,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    DialerPad(
                        dialerNumber = dialerNumber,
                        onDigitClick = { viewModel.onDialDigit(it) },
                        onBackspace = { viewModel.onBackspace() },
                        onClear = { viewModel.clearDialer() },
                        onCall = {
                            viewModel.initiateCall(dialerNumber)
                            isDialerVisible = false
                        },
                        onClose = { isDialerVisible = false }
                    )
                }

                // Full-Screen Immersive calling Screen (Toll-free simulation overlay)
                if (activeCallState != CallSessionState.Idle) {
                    InCallOverlayScreen(
                        state = activeCallState,
                        onMuteToggle = { viewModel.toggleMute() },
                        onSpeakerToggle = { viewModel.toggleSpeaker() },
                        onAccept = { viewModel.acceptIncomingCall() },
                        onReject = { viewModel.rejectIncomingCall() },
                        onEndCall = { viewModel.endCall() }
                    )
                }
            }
        }
    }
}

// --------------------- TABS IMPLEMENTATIONS ---------------------

@Composable
fun HistoryTabContent(
    callHistory: List<CallRecord>,
    onDial: (String) -> Unit,
    onDelete: (CallRecord) -> Unit,
    onBlock: (String) -> Unit
) {
    if (callHistory.isEmpty()) {
        EmptyStateLayout(
            title = "لا توجد مكالمات مؤخرًا",
            subtitle = "أجرِ مكالماتك الأولى لتراها معروضة هنا بتنظيم فائق.",
            icon = Icons.Default.Call
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            items(callHistory) { record ->
                CallHistoryItemRow(
                    record = record,
                    onDial = { onDial(record.phoneNumber) },
                    onDelete = { onDelete(record) },
                    onBlock = { onBlock(record.phoneNumber) }
                )
            }
        }
    }
}

@Composable
fun CallHistoryItemRow(
    record: CallRecord,
    onDial: () -> Unit,
    onDelete: () -> Unit,
    onBlock: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Colored icon representing log type
                val (color, icon) = when (record.type) {
                    "INCOMING" -> Pair(PrimaryEmerald, Icons.AutoMirrored.Filled.CallReceived)
                    "MISSED" -> Pair(AccentRed, Icons.AutoMirrored.Filled.CallReceived)
                    else -> Pair(OrangeGold, Icons.AutoMirrored.Filled.CallMade)
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = record.type,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = record.callerName ?: record.phoneNumber,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 16.sp
                    )
                    
                    if (record.callerName != null) {
                        Text(
                            text = record.phoneNumber,
                            color = TextGray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        val sdf = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar"))
                        val dateFormatted = sdf.format(Date(record.timestamp))
                        Text(
                            text = dateFormatted,
                            color = TextGray,
                            fontSize = 11.sp
                        )
                        
                        if (record.duration > 0) {
                            Text(
                                text = "  •  ${record.duration} ثانية",
                                color = PrimaryEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (record.type == "MISSED") {
                            Text(
                                text = "  •  فائتة",
                                color = AccentRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick actions side row
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDial) {
                    Icon(Icons.Default.Call, contentDescription = "اتصال سريع", tint = PrimaryEmerald)
                }
                var expandedMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "المزيد", tint = TextGray)
                    }
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false },
                        containerColor = DarkSurfaceVariant
                    ) {
                        DropdownMenuItem(
                            text = { Text("حظر الرقم", color = AccentRed) },
                            leadingIcon = { Icon(Icons.Default.Block, null, tint = AccentRed) },
                            onClick = {
                                onBlock()
                                expandedMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف السجل", color = TextWhite) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = TextGray) },
                            onClick = {
                                onDelete()
                                expandedMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsTabContent(
    contacts: List<ContactItem>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onCall: (String) -> Unit,
    onSimulateCall: (String) -> Unit,
    onBlock: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // High-fidelity search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("ابحث بالاسم أو برقم الهاتف...", color = TextGray) },
            prefix = {
                Icon(
                    Icons.Default.Search,
                    null,
                    tint = PrimaryEmerald,
                    modifier = Modifier.padding(end = 8.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Clear, "مسح", tint = TextGray)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = PrimaryEmerald,
                unfocusedBorderColor = BorderGray,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("contact_search_input")
        )

        if (contacts.isEmpty()) {
            EmptyStateLayout(
                title = "لا توجد جهات اتصال مطابقة",
                subtitle = "جرب البحث بكلمات أخرى أو أرقام مختلفة.",
                icon = Icons.Default.SearchOff
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(contacts) { contact ->
                    ContactItemRow(
                        contact = contact,
                        onCall = { onCall(contact.phoneNumber) },
                        onSim = { onSimulateCall(contact.phoneNumber) },
                        onBlock = { onBlock(contact.phoneNumber) }
                    )
                }
            }
        }
    }
}

@Composable
fun ContactItemRow(
    contact: ContactItem,
    onCall: () -> Unit,
    onSim: () -> Unit,
    onBlock: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Initials Avatar with consistent color derived from contact's name hashes
                val avatarColor = remember(contact.name) {
                    val hash = contact.name.hashCode()
                    val hues = listOf(0xFF00E676, 0xFF00BFA5, 0xFF00B0FF, 0xFFD500F9, 0xFFFFA000, 0xFFDD2C00)
                    Color(hues[kotlin.math.abs(hash % hues.size)])
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(avatarColor.copy(alpha = 0.2f))
                        .border(1.5.dp, avatarColor, CircleShape)
                ) {
                    Text(
                        text = contact.initials,
                        color = avatarColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite,
                        fontSize = 16.sp
                    )
                    Text(
                        text = contact.phoneNumber,
                        color = TextGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    if (contact.email != null) {
                        Text(
                            text = contact.email,
                            color = PrimaryEmerald.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Quick actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Interactive call simulators
                Button(
                    onClick = onSim,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryEmerald.copy(alpha = 0.15f),
                        contentColor = PrimaryEmerald
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Icon(Icons.Default.PhoneCallback, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("وارد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onCall,
                    modifier = Modifier
                        .background(PrimaryEmerald, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "اتصال",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                var expandedMenu by remember { mutableStateOf(false) }
                Box(modifier = Modifier.padding(start = 4.dp)) {
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(Icons.Default.MoreVert, "مزيد", tint = TextGray)
                    }
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false },
                        containerColor = DarkSurfaceVariant
                    ) {
                        DropdownMenuItem(
                            text = { Text("إضافة للقائمة السوداء", color = AccentRed) },
                            leadingIcon = { Icon(Icons.Default.Block, null, tint = AccentRed) },
                            onClick = {
                                onBlock()
                                expandedMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedTabContent(
    blockedNumbers: List<BlockedNumber>,
    onBlock: (String, String) -> Unit,
    onUnblock: (String) -> Unit
) {
    var blockInputNumber by remember { mutableStateOf("") }
    var blockInputReason by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Adding block card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "حظر رقم يدويًا (القائمة السوداء)",
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    fontSize = 15.sp
                )
                
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = blockInputNumber,
                    onValueChange = { blockInputNumber = it },
                    placeholder = { Text("أدخل رقم الهاتف للحظر...", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = BorderGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("blacklist_num_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = blockInputReason,
                    onValueChange = { blockInputReason = it },
                    placeholder = { Text("سبب الحظر (مثال: سبام، شركة إعلانية)...", color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryEmerald,
                        unfocusedBorderColor = BorderGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (blockInputNumber.isNotBlank()) {
                            onBlock(blockInputNumber, blockInputReason.ifBlank { "بلا سبب محدد" })
                            blockInputNumber = ""
                            blockInputReason = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_block_btn")
                ) {
                    Icon(Icons.Default.Lock, null, tint = TextWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حظر هذا الرقم الآن", fontWeight = FontWeight.Bold, color = TextWhite)
                }
            }
        }

        // List Header
        Text(
            "الأرقام المحظورة حاليًا (${blockedNumbers.size})",
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (blockedNumbers.isEmpty()) {
            EmptyStateLayout(
                title = "قائمة الحظر فارغة",
                subtitle = "الرقم المحظور لن يتمكن من إزعاجك مجددًا وستُكتم مكالماته تلقائيًا.",
                icon = Icons.Default.VerifiedUser
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(blockedNumbers) { blocked ->
                    BlockedNumberRow(blocked = blocked, onUnblock = onUnblock)
                }
            }
        }
    }
}

@Composable
fun BlockedNumberRow(
    blocked: BlockedNumber,
    onUnblock: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        border = BorderStroke(0.5.dp, BorderGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    blocked.phoneNumber,
                    fontWeight = FontWeight.ExtraBold,
                    color = AccentRed,
                    fontSize = 16.sp
                )
                Text(
                    "السبب: ${blocked.blockReason ?: "غير محدد"}",
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Button(
                onClick = { onUnblock(blocked.phoneNumber) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryEmerald.copy(alpha = 0.12f),
                    contentColor = PrimaryEmerald
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إلغاء الحظر", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --------------------- SHARED ELEMENTS ---------------------

@Composable
fun EmptyStateLayout(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PrimaryEmerald.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryEmerald,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = TextWhite,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = TextGray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

// --------------------- SLIDING DIALER CONTROLS ---------------------

@Composable
fun DialerPad(
    dialerNumber: String,
    onDigitClick: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onCall: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, BorderGray), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp, start = 24.dp, end = 24.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle / exit dialer row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, "إخفاء", tint = TextWhite, modifier = Modifier.size(30.dp))
                }
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(BorderGray)
                )
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.DeleteOutline, "تفريغ", tint = TextGray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Number Screen Container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dialer letters or dummy sub text
                Text(
                    text = dialerNumber,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    maxLines = 1,
                    textAlign = TextAlign.Left,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dialer_number_display")
                )
                
                if (dialerNumber.isNotEmpty()) {
                    IconButton(
                        onClick = onBackspace,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("backspace_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "حذف",
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Keyboard grid elements
            val keyLetters = listOf(
                Pair('1', " "), Pair('2', "أ ب ت"), Pair('3', "ج ح خ"),
                Pair('4', "د ذ ر ز"), Pair('5', "س ش ص"), Pair('6', "ض ط ظ"),
                Pair('7', "ع غ ف"), Pair('8', "ق ك ل"), Pair('9', "م ن هـ"),
                Pair('*', " "), Pair('0', "+"), Pair('#', " ")
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (r in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (c in 0 until 3) {
                            val element = keyLetters[r * 3 + c]
                            DialerButton(
                                digit = element.first,
                                subtext = element.second,
                                onClick = { onDigitClick(element.first) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Call Action Button
            IconButton(
                onClick = onCall,
                modifier = Modifier
                    .size(68.dp)
                    .background(PrimaryEmerald, CircleShape)
                    .testTag("dialer_call_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "اتصل",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun DialerButton(
    digit: Char,
    subtext: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .aspectRatio(1.2f)
            .testTag("dialer_key_$digit")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit.toString(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = TextWhite
            )
            if (subtext.isNotBlank()) {
                Text(
                    text = subtext,
                    fontSize = 11.sp,
                    color = TextGray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --------------------- IN-CALL FULL SCREEN OVERLAY ---------------------

@Composable
fun InCallOverlayScreen(
    state: CallSessionState,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onEndCall: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAnimation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        color = Color(0xFF090E17),
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false) {}, // Intercept touch events to protect system screen
        tonalElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header information
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = when (state) {
                        is CallSessionState.Dialing -> if (state.isIncoming) "مكالمة واردة..." else "جاري الاتصال..."
                        is CallSessionState.Active -> "مكالمة جارية"
                        else -> ""
                    },
                    color = if (state is CallSessionState.Dialing && state.isIncoming) OrangeGold else PrimaryEmerald,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Pulsing Avatar container representing premium phone connection
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.graphicsLayer(
                        scaleX = if (state is CallSessionState.Dialing) pulseScale else 1f,
                        scaleY = if (state is CallSessionState.Dialing) pulseScale else 1f
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        PrimaryEmerald.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .border(2.dp, PrimaryEmerald, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PrimaryEmerald,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val callerName = when (state) {
                    is CallSessionState.Dialing -> state.contactName
                    is CallSessionState.Active -> state.contactName
                    else -> null
                }
                val callerNumber = when (state) {
                    is CallSessionState.Dialing -> state.number
                    is CallSessionState.Active -> state.number
                    else -> "مجهول"
                }

                Text(
                    text = callerName ?: "مكالمة مجهولة",
                    color = TextWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                )

                if (callerName != null) {
                    Text(
                        text = callerNumber,
                        color = TextGray,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timer ticking or calling indicators
                if (state is CallSessionState.Active) {
                    val minutes = state.durationSeconds / 60
                    val seconds = state.durationSeconds % 60
                    val timerFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                    
                    Text(
                        text = timerFormatted,
                        color = TextWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.testTag("incall_timer")
                    )
                } else if (state is CallSessionState.Dialing && state.isIncoming) {
                    Text(
                        text = callerNumber,
                        color = OrangeGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }

            // Controls suite
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                if (state is CallSessionState.Active) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 44.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = onMuteToggle,
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        if (state.isMuted) PrimaryEmerald else Color.White.copy(alpha = 0.08f),
                                        CircleShape
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "كتم الصوت",
                                    tint = if (state.isMuted) NavyDark else TextWhite
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("كتم الصوت", color = TextGray, fontSize = 12.sp)
                        }

                        // Speaker button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = onSpeakerToggle,
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        if (state.isSpeakerOn) PrimaryEmerald else Color.White.copy(alpha = 0.08f),
                                        CircleShape
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (state.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                    contentDescription = "مكبر الصوت",
                                    tint = if (state.isSpeakerOn) NavyDark else TextWhite
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("مكبر الصوت", color = TextGray, fontSize = 12.sp)
                        }
                    }
                }

                // Call disconnect / accept button suite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state is CallSessionState.Dialing && state.isIncoming) {
                        // Accept and Decline Buttons side-by-side
                        IconButton(
                            onClick = onReject,
                            modifier = Modifier
                                .size(68.dp)
                                .background(AccentRed, CircleShape)
                                .testTag("decline_call_btn")
                        ) {
                            Icon(Icons.Default.CallEnd, "Decline", tint = TextWhite, modifier = Modifier.size(30.dp))
                        }

                        Spacer(modifier = Modifier.width(48.dp))

                        IconButton(
                            onClick = onAccept,
                            modifier = Modifier
                                .size(68.dp)
                                .background(PrimaryEmerald, CircleShape)
                                .testTag("accept_call_btn")
                        ) {
                            Icon(Icons.Default.Call, "Accept", tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                    } else {
                        // Center End Call
                        IconButton(
                            onClick = onEndCall,
                            modifier = Modifier
                                .size(72.dp)
                                .background(AccentRed, CircleShape)
                                .testTag("end_call_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "إنهاء المكالمة",
                                tint = TextWhite,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Mess
import com.example.data.model.Room
import com.example.ui.MessViewModel
import com.example.ui.dialogs.AssignMemberRoomDialog
import com.example.ui.dialogs.CreateOrEditRoomDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MessViewModel,
    mess: Mess,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val rooms by viewModel.rooms.collectAsState()
    val currentUserId = viewModel.currentUserId

    var selectedTab by remember { mutableIntStateOf(0) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // Dialog states
    var showAddRoomDialog by remember { mutableStateOf(false) }
    var presetRoomType by remember { mutableStateOf("Single") }
    var editingRoom by remember { mutableStateOf<Room?>(null) }
    var roomToAssignMember by remember { mutableStateOf<Room?>(null) }
    var roomToDelete by remember { mutableStateOf<Room?>(null) }
    var memberToRemove by remember { mutableStateOf<Pair<String, String>?>(null) }
    var memberToAssignRoom by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Calculations
    val totalCapacity = rooms.sumOf { it.capacity }
    val totalOccupied = rooms.sumOf { it.assignedMemberUids.size }
    val totalAvailable = (totalCapacity - totalOccupied).coerceAtLeast(0)
    val singleRoomsCount = rooms.count { it.type.equals("Single", ignoreCase = true) }
    val doubleRoomsCount = rooms.count { it.type.equals("Double", ignoreCase = true) }
    val adminCount = mess.adminUids.size.coerceAtLeast(1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "অ্যাডমিন কন্ট্রোল সেন্টার",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = "👑 ADMIN",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${mess.name} • কোড: ${mess.joinCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "সাহায্য ও পারমিশন নির্দেশিকা"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Metrics Overview Banner
            AdminMetricsBanner(
                totalRooms = rooms.size,
                singleRooms = singleRoomsCount,
                doubleRooms = doubleRoomsCount,
                totalSeats = totalCapacity,
                availableSeats = totalAvailable,
                totalMembers = mess.memberUids.size,
                adminCount = adminCount
            )

            // Tabs for Rooms & Permissions
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("রুম ব্যবস্থাপনা (${rooms.size})", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier.testTag("tab_admin_rooms")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("সদস্য ও পারমিশন (${mess.memberUids.size})", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier.testTag("tab_admin_permissions")
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    AdminRoomsTabContent(
                        rooms = rooms,
                        mess = mess,
                        onAddNewRoom = { type ->
                            presetRoomType = type
                            editingRoom = null
                            showAddRoomDialog = true
                        },
                        onEditRoom = { room ->
                            editingRoom = room
                            showAddRoomDialog = true
                        },
                        onDeleteRoom = { room ->
                            roomToDelete = room
                        },
                        onAssignMemberToRoom = { room ->
                            roomToAssignMember = room
                        },
                        onRemoveMemberFromRoom = { room, memberUid ->
                            viewModel.leaveRoom(room, memberUid)
                        }
                    )
                }
                1 -> {
                    AdminPermissionsTabContent(
                        mess = mess,
                        rooms = rooms,
                        currentUserId = currentUserId,
                        onToggleAdmin = { uid, name ->
                            viewModel.toggleAdminRole(uid, name)
                        },
                        onRemoveMember = { uid, name ->
                            memberToRemove = uid to name
                        },
                        onAssignMemberRoom = { uid, name ->
                            memberToAssignRoom = uid to name
                        }
                    )
                }
            }
        }
    }

    // Dialog: Create or Edit Room
    if (showAddRoomDialog) {
        CreateOrEditRoomDialog(
            initialRoom = editingRoom,
            onDismiss = {
                showAddRoomDialog = false
                editingRoom = null
            },
            onConfirm = { roomNumber, type, capacity, rent ->
                viewModel.createOrUpdateRoom(
                    roomNumber = roomNumber,
                    type = type,
                    capacity = capacity,
                    rent = rent,
                    existingRoomId = editingRoom?.id
                ) { success ->
                    if (success) {
                        showAddRoomDialog = false
                        editingRoom = null
                    }
                }
            }
        )
    }

    // Dialog: Assign Member to Room
    if (roomToAssignMember != null) {
        AssignMemberRoomDialog(
            mess = mess,
            room = roomToAssignMember!!,
            onDismiss = { roomToAssignMember = null },
            onAssign = { memberUid, memberName ->
                viewModel.chooseRoom(roomToAssignMember!!, memberUid, memberName)
                roomToAssignMember = null
            }
        )
    }

    // Dialog: Member Room Selector from Member List
    if (memberToAssignRoom != null) {
        val (memberUid, memberName) = memberToAssignRoom!!
        MemberRoomAssignmentDialog(
            memberName = memberName,
            memberUid = memberUid,
            rooms = rooms,
            onDismiss = { memberToAssignRoom = null },
            onSelectRoom = { selectedRoom ->
                viewModel.chooseRoom(selectedRoom, memberUid, memberName)
                memberToAssignRoom = null
            },
            onLeaveCurrentRoom = { currentRoom ->
                viewModel.leaveRoom(currentRoom, memberUid)
                memberToAssignRoom = null
            }
        )
    }

    // Dialog: Delete Room Confirmation
    if (roomToDelete != null) {
        AlertDialog(
            onDismissRequest = { roomToDelete = null },
            title = { Text("রুম মুছে ফেলতে চান?") },
            text = {
                Text("আপনি কি নিশ্চিত যে রুম ${roomToDelete?.roomNumber} মুছে ফেলতে চান? এই রুমে যারা আছেন তারা রুমহীন হয়ে যাবেন।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        roomToDelete?.id?.let { viewModel.deleteRoom(it) }
                        roomToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_room_button")
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { roomToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Dialog: Remove Member Confirmation
    if (memberToRemove != null) {
        val (uid, name) = memberToRemove!!
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("সদস্য অপসারণ নিশ্চিত করুন") },
            text = { Text("আপনি কি নিশ্চিত যে $name-কে এই মেস থেকে বাদ দিতে চান? তার সব ডাটা সংরক্ষিত থাকবে কিন্তু তিনি মেসে আর প্রবেশ করতে পারবেন না।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeMember(uid, name)
                        memberToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_remove_member_button")
                ) {
                    Text("অপসারণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Dialog: Help & Permission Guide
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("অ্যাডমিন পারমিশন ও অধিকার")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "👑 অ্যাডমিন হিসেবে আপনার পূর্ণ নিয়ন্ত্রণ রয়েছে:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("১. আগে থেকেই সিঙ্গেল (১ সিট) বা ডাবল (২ সিট) রুম যুক্ত ও এডিট করতে পারবেন।")
                    Text("২. যেকোনো সদস্যকে নির্দিষ্ট রুমে আসন বরাদ্দ দিতে পারবেন বা সিট পরিবর্তন করতে পারবেন।")
                    Text("৩. অন্য সাধারণ সদস্যদের অ্যাডমিন বা কো-অ্যাডমিন পারমিশন প্রদান ও প্রত্যাহার করতে পারবেন।")
                    Text("৪. মেস থেকে অবাঞ্ছিত সদস্য অপসারণ করতে পারবেন।")
                    HorizontalDivider()
                    Text(
                        text = "👤 সাধারণ সদস্য অধিকার:",
                        fontWeight = FontWeight.Bold
                    )
                    Text("• মেসের খালি থাকা সিঙ্গেল বা ডাবল রুমে নিজে উঠতে বা ছাড়তে পারবেন।")
                    Text("• দৈনন্দিন মিল ও বাজার খরচ এন্ট্রি দিতে পারবেন।")
                    Text("• সার্বিক হিসাব ও অন্যান্য খরচ দেখতে পারবেন।")
                }
            },
            confirmButton = {
                Button(onClick = { showHelpDialog = false }) {
                    Text("বুঝেছি")
                }
            }
        )
    }
}

@Composable
fun AdminMetricsBanner(
    totalRooms: Int,
    singleRooms: Int,
    doubleRooms: Int,
    totalSeats: Int,
    availableSeats: Int,
    totalMembers: Int,
    adminCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "মেস ওভারভিউ ও পরিসংখান",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "👑 অ্যাডমিন: $adminCount জন",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricItem(
                    label = "মোট রুম",
                    value = "$totalRooms টি",
                    subtext = "সিঙ্গেল: $singleRooms | ডাবল: $doubleRooms",
                    icon = Icons.Default.MeetingRoom,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "সিট সংখ্যা",
                    value = "$totalSeats টি",
                    subtext = "খালি: $availableSeats টি",
                    icon = Icons.Default.Bed,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "মোট সদস্য",
                    value = "$totalMembers জন",
                    subtext = "যুক্ত আছেন",
                    icon = Icons.Default.Group,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminRoomsTabContent(
    rooms: List<Room>,
    mess: Mess,
    onAddNewRoom: (type: String) -> Unit,
    onEditRoom: (Room) -> Unit,
    onDeleteRoom: (Room) -> Unit,
    onAssignMemberToRoom: (Room) -> Unit,
    onRemoveMemberFromRoom: (Room, memberUid: String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredRooms = when (selectedFilter) {
        "Single" -> rooms.filter { it.type.equals("Single", ignoreCase = true) }
        "Double" -> rooms.filter { it.type.equals("Double", ignoreCase = true) }
        "Vacant" -> rooms.filter { it.assignedMemberUids.size < it.capacity }
        else -> rooms
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Room Add Actions & Presets
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "নতুন রুম যোগ করুন (Pre-add Rooms)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "সিঙ্গেল বা ডাবল রুম যুক্ত করে রাখুন, মেম্বাররা পছন্দমতো সিটে উঠতে পারবে।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAddNewRoom("Single") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_add_single_room_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("সিঙ্গেল রুম (১ সিট)")
                        }
                        FilledTonalButton(
                            onClick = { onAddNewRoom("Double") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_add_double_room_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ডাবল রুম (২ সিট)")
                        }
                    }
                }
            }
        }

        // Filter chips
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "All",
                    onClick = { selectedFilter = "All" },
                    label = { Text("সব রুম (${rooms.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "Single",
                    onClick = { selectedFilter = "Single" },
                    label = { Text("সিঙ্গেল (Single)") }
                )
                FilterChip(
                    selected = selectedFilter == "Double",
                    onClick = { selectedFilter = "Double" },
                    label = { Text("ডাবল (Double)") }
                )
                FilterChip(
                    selected = selectedFilter == "Vacant",
                    onClick = { selectedFilter = "Vacant" },
                    label = { Text("🟢 খালি সিট আছে") }
                )
            }
        }

        if (filteredRooms.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (rooms.isEmpty()) "এখনো কোনো রুম যোগ করা হয়নি!" else "এই ফিল্টারে কোনো রুম পাওয়া যায়নি",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "উপরে 'সিঙ্গেল রুম' বা 'ডাবল রুম' বোতামে চাপ দিয়ে মেসের জন্য রুম তৈরি করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredRooms) { room ->
                AdminRoomCard(
                    room = room,
                    mess = mess,
                    onEdit = { onEditRoom(room) },
                    onDelete = { onDeleteRoom(room) },
                    onAssignMember = { onAssignMemberToRoom(room) },
                    onRemoveMember = { uid -> onRemoveMemberFromRoom(room, uid) }
                )
            }
        }
    }
}

@Composable
fun AdminRoomCard(
    room: Room,
    mess: Mess,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAssignMember: () -> Unit,
    onRemoveMember: (uid: String) -> Unit
) {
    val occupiedCount = room.assignedMemberUids.size
    val isFull = occupiedCount >= room.capacity
    val availableSeats = (room.capacity - occupiedCount).coerceAtLeast(0)

    val typeColor = when (room.type.lowercase()) {
        "single" -> MaterialTheme.colorScheme.tertiary
        "double" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Room Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = typeColor.copy(alpha = 0.15f),
                        contentColor = typeColor
                    ) {
                        Text(
                            text = "রুম ${room.roomNumber}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = when (room.type) {
                                "Single" -> "সিঙ্গেল (১ সিট)"
                                "Double" -> "ডাবল (২ সিট)"
                                "Triple" -> "ট্রিপল (৩ সিট)"
                                else -> "${room.type} (${room.capacity} সিট)"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Edit / Delete Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp).testTag("edit_room_${room.roomNumber}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "রুম এডিট",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_room_${room.roomNumber}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "রুম ডিলিট",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rent and Seat status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (room.rent > 0) {
                    Text(
                        text = "ভাড়া: ৳ ${room.rent.toInt()} / মাস",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "ভাড়া নির্ধারিত হয়নি",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isFull) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (isFull) "পূর্ণ (${occupiedCount}/${room.capacity})" else "খালি $availableSeats টি সিট (${occupiedCount}/${room.capacity})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isFull) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Occupants & Visual Seats
            Text(
                text = "আসন বিন্যাস ও সদস্যগণ:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Occupied seats
                room.assignedMemberUids.forEachIndexed { index, uid ->
                    val memberName = room.assignedMemberNames.getOrNull(index)
                        ?: mess.getMemberName(uid)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (index + 1).toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = memberName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                if (mess.isAdmin(uid)) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "👑",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            // Unseat button
                            TextButton(
                                onClick = { onRemoveMember(uid) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonRemove,
                                    contentDescription = "সিট খালি করুন",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "সিট ছাড়ুন",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Available / Empty seats
                for (emptySeatIndex in (occupiedCount until room.capacity)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.outlineVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (emptySeatIndex + 1).toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "খালি আসন (সিট ${emptySeatIndex + 1})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Assign Member Button
                            FilledTonalButton(
                                onClick = onAssignMember,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "সদস্য বরাদ্দ দিন",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPermissionsTabContent(
    mess: Mess,
    rooms: List<Room>,
    currentUserId: String,
    onToggleAdmin: (memberUid: String, memberName: String) -> Unit,
    onRemoveMember: (memberUid: String, memberName: String) -> Unit,
    onAssignMemberRoom: (memberUid: String, memberName: String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val members = mess.memberUids.mapIndexed { index, uid ->
        val name = mess.memberNames.getOrNull(index) ?: "Member"
        Triple(uid, name, mess.isAdmin(uid))
    }.filter { (_, name, _) ->
        name.contains(searchQuery.trim(), ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mess Code Share Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "মেস কোড শেয়ার করুন",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "সদস্যদের এই কোড দিয়ে মেসে যুক্ত হতে বলুন: ${mess.joinCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Mess Code", mess.joinCode))
                                Toast.makeText(context, "কোড কপি হয়েছে: ${mess.joinCode}", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "কপি করুন", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "আমাদের '${mess.name}' মেসে যোগ দিতে কোডটি ব্যবহার করুন: ${mess.joinCode}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "মেস কোড শেয়ার"))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "শেয়ার করুন", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("সদস্য খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Permissions Instruction Note
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "অ্যাডমিন পাওয়ার দিয়ে সদস্যদের কো-অ্যাডমিন করতে পারেন অথবা প্রয়োজন না থাকলে বাদ দিতে পারেন।",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Member Cards
        items(members) { (uid, name, isAdmin) ->
            val isSelf = uid == currentUserId
            val isCreator = uid == mess.creatorId
            val memberRoom = rooms.find { it.assignedMemberUids.contains(uid) }

            MemberPermissionCard(
                name = name,
                uid = uid,
                isAdmin = isAdmin,
                isCreator = isCreator,
                isSelf = isSelf,
                memberRoom = memberRoom,
                onToggleAdmin = { onToggleAdmin(uid, name) },
                onRemove = { onRemoveMember(uid, name) },
                onAssignRoom = { onAssignMemberRoom(uid, name) }
            )
        }
    }
}

@Composable
fun MemberPermissionCard(
    name: String,
    uid: String,
    isAdmin: Boolean,
    isCreator: Boolean,
    isSelf: Boolean,
    memberRoom: Room?,
    onToggleAdmin: () -> Unit,
    onRemove: () -> Unit,
    onAssignRoom: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdmin)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar circle
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.firstOrNull()?.uppercase() ?: "U",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isAdmin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (isSelf) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(আপনি)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when {
                                    isCreator -> MaterialTheme.colorScheme.primary
                                    isAdmin -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = when {
                                    isCreator -> MaterialTheme.colorScheme.onPrimary
                                    isAdmin -> MaterialTheme.colorScheme.onTertiary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ) {
                                Text(
                                    text = when {
                                        isCreator -> "👑 প্রতিষ্ঠাতা অ্যাডমিন"
                                        isAdmin -> "🛡️ কো-অ্যাডমিন"
                                        else -> "👤 সাধারণ সদস্য"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = if (memberRoom != null) "🏠 রুম ${memberRoom.roomNumber} (${memberRoom.type})" else "⚠️ কোনো রুমে নেই",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (memberRoom != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Delete member button (cannot remove self or creator)
                if (!isSelf && !isCreator) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "মেস থেকে অপসারণ",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: Admin Toggle & Room Allocation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Assign Room Button
                OutlinedButton(
                    onClick = onAssignRoom,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MeetingRoom,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (memberRoom != null) "রুম পরিবর্তন" else "রুমে আসন দিন",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Toggle Admin Role Button (Cannot modify self if only one admin, cannot modify creator)
                if (!isCreator && !isSelf) {
                    FilledTonalButton(
                        onClick = onToggleAdmin,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isAdmin) Icons.Default.Shield else Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAdmin) "অ্যাডমিন বাতিল" else "অ্যাডমিন বানান",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemberRoomAssignmentDialog(
    memberName: String,
    memberUid: String,
    rooms: List<Room>,
    onDismiss: () -> Unit,
    onSelectRoom: (Room) -> Unit,
    onLeaveCurrentRoom: (Room) -> Unit
) {
    val currentRoom = rooms.find { it.assignedMemberUids.contains(memberUid) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "$memberName-এর রুম নির্ধারণ",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (currentRoom != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "বর্তমান: রুম ${currentRoom.roomNumber} (${currentRoom.type})",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            TextButton(onClick = { onLeaveCurrentRoom(currentRoom) }) {
                                Text("রুম ছাড়ুন", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "এই সদস্য বর্তমানে কোনো রুমে নেই। নিচের তালিকা থেকে খালি সিট আছে এমন রুম নির্বাচন করুন:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                if (rooms.isEmpty()) {
                    Text(
                        text = "মেসে কোনো রুম তৈরি করা নেই। আগে 'রুম ব্যবস্থাপনা' থেকে রুম তৈরি করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(rooms) { room ->
                            val isCurrent = room.id == currentRoom?.id
                            val isFull = room.assignedMemberUids.size >= room.capacity && !isCurrent
                            val available = (room.capacity - room.assignedMemberUids.size).coerceAtLeast(0)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isFull && !isCurrent) {
                                        onSelectRoom(room)
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else if (isFull)
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "রুম ${room.roomNumber} (${room.type})",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = if (room.rent > 0) "ভাড়া ৳ ${room.rent.toInt()}" else "ভাড়া নির্ধারিত হয়নি",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else if (isFull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                    ) {
                                        Text(
                                            text = if (isCurrent) "বর্তমান রুম" else if (isFull) "পূর্ণ" else "খালি: $available টি",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}

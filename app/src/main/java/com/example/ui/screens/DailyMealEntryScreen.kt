package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Meal
import com.example.data.model.Mess
import com.example.ui.MessViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyMealEntryScreen(
    viewModel: MessViewModel,
    mess: Mess,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUserId = viewModel.currentUserId
    val currentDate by viewModel.selectedDate.collectAsState()
    val meals by viewModel.meals.collectAsState()

    BackHandler {
        onNavigateBack()
    }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayStr = remember { sdf.format(Date()) }

    // Find index of current user or default to 0
    val defaultUserIndex = remember(mess, currentUserId) {
        val idx = mess.memberUids.indexOf(currentUserId)
        if (idx >= 0) idx else 0
    }

    var selectedMemberIndex by remember { mutableIntStateOf(defaultUserIndex) }
    var expandedMemberMenu by remember { mutableStateOf(false) }

    val activeMemberUid = mess.memberUids.getOrNull(selectedMemberIndex) ?: currentUserId
    val activeMemberName = mess.memberNames.getOrNull(selectedMemberIndex) ?: "Member"

    // Existing meal for selected user on selected date
    val existingMealForUser = remember(meals, currentDate, activeMemberUid) {
        meals.find { it.date == currentDate && it.userId == activeMemberUid }
    }

    var breakfast by remember { mutableDoubleStateOf(existingMealForUser?.breakfast ?: 0.0) }
    var lunch by remember { mutableDoubleStateOf(existingMealForUser?.lunch ?: 1.0) }
    var dinner by remember { mutableDoubleStateOf(existingMealForUser?.dinner ?: 1.0) }
    var isSaving by remember { mutableStateOf(false) }

    // Update counters whenever selected member or date changes
    LaunchedEffect(currentDate, activeMemberUid, existingMealForUser) {
        breakfast = existingMealForUser?.breakfast ?: 0.0
        lunch = existingMealForUser?.lunch ?: 1.0
        dinner = existingMealForUser?.dinner ?: 1.0
    }

    val totalMeals = breakfast + lunch + dinner
    val mealsOnDate = meals.filter { it.date == currentDate }
    val totalMessMealsOnDate = mealsOnDate.sumOf { it.totalMeals }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "দৈনিক মিল এন্ট্রি",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = mess.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("meal_entry_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentDate != todayStr) {
                        OutlinedButton(
                            onClick = { viewModel.setDate(todayStr) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("আজ", fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Date Navigation Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                try {
                                    val d = sdf.parse(currentDate) ?: Date()
                                    val prev = Date(d.time - 24 * 60 * 60 * 1000)
                                    viewModel.setDate(sdf.format(prev))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.testTag("prev_day_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentDate,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = if (currentDate == todayStr) "আজকের তারিখ • মোট মিল: ${String.format(Locale.US, "%.1f", totalMessMealsOnDate)} টি"
                                else "মোট মিল: ${String.format(Locale.US, "%.1f", totalMessMealsOnDate)} টি",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                try {
                                    val d = sdf.parse(currentDate) ?: Date()
                                    val next = Date(d.time + 24 * 60 * 60 * 1000)
                                    viewModel.setDate(sdf.format(next))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.testTag("next_day_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                        }
                    }
                }
            }

            // Member Selector Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "কার মিল এন্ট্রি করতে চান?",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        ExposedDropdownMenuBox(
                            expanded = expandedMemberMenu,
                            onExpandedChange = { expandedMemberMenu = !expandedMemberMenu }
                        ) {
                            OutlinedTextField(
                                value = if (activeMemberUid == currentUserId) "$activeMemberName (আমি)" else activeMemberName,
                                onValueChange = {},
                                readOnly = true,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMemberMenu) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("member_selector_field")
                            )

                            ExposedDropdownMenu(
                                expanded = expandedMemberMenu,
                                onDismissRequest = { expandedMemberMenu = false }
                            ) {
                                mess.memberUids.forEachIndexed { idx, uid ->
                                    val name = mess.memberNames.getOrNull(idx) ?: "Member $idx"
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (uid == currentUserId) "$name (আমি)" else name,
                                                    fontWeight = if (idx == selectedMemberIndex) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (idx == selectedMemberIndex) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedMemberIndex = idx
                                            expandedMemberMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        if (existingMealForUser != null) {
                            Text(
                                text = "ℹ️ এই সদস্যের আজকের পূর্বের এন্ট্রি লোড হয়েছে। মান পরিবর্তন করে সংরক্ষণ করতে পারেন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Meal Counters Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "খাবারের মিল পরিমাণ নির্ধারণ করুন:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        // 1. Breakfast
                        MealInputBlock(
                            title = "সকালের নাস্তা (Breakfast)",
                            icon = Icons.Default.WbTwilight,
                            value = breakfast,
                            quickOptions = listOf(0.0, 0.5, 1.0, 1.5),
                            onValueChange = { breakfast = it },
                            testTagPrefix = "breakfast"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // 2. Lunch
                        MealInputBlock(
                            title = "দুপুরের খাবার (Lunch)",
                            icon = Icons.Default.WbSunny,
                            value = lunch,
                            quickOptions = listOf(0.0, 1.0, 1.5, 2.0),
                            onValueChange = { lunch = it },
                            testTagPrefix = "lunch"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // 3. Dinner
                        MealInputBlock(
                            title = "রাতের খাবার (Dinner)",
                            icon = Icons.Default.NightlightRound,
                            value = dinner,
                            quickOptions = listOf(0.0, 1.0, 1.5, 2.0),
                            onValueChange = { dinner = it },
                            testTagPrefix = "dinner"
                        )

                        // Total Indicator Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "এই দিনের মোট মিল",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "$activeMemberName-এর জন্য হিসাব",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f টি", totalMeals),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Save Button
                        Button(
                            onClick = {
                                isSaving = true
                                viewModel.recordMeal(
                                    targetUserId = activeMemberUid,
                                    targetUserName = activeMemberName,
                                    date = currentDate,
                                    breakfast = breakfast,
                                    lunch = lunch,
                                    dinner = dinner,
                                    existingMealId = existingMealForUser?.id
                                ) { success ->
                                    isSaving = false
                                    if (success) {
                                        Toast.makeText(
                                            context,
                                            "$activeMemberName-এর মিল সফলভাবে সংরক্ষণ করা হয়েছে!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("save_meal_button"),
                            shape = RoundedCornerShape(26.dp),
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Firestore-এ সংরক্ষণ করুন",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Summary List of all members on this date
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "এই দিনের সব সদস্যের মিল ($currentDate):",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "মোট: ${String.format(Locale.US, "%.1f", totalMessMealsOnDate)} টি",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            items(mess.memberUids) { uid ->
                val idx = mess.memberUids.indexOf(uid)
                val memberName = mess.memberNames.getOrNull(idx) ?: "Member"
                val mealEntry = mealsOnDate.find { it.userId == uid }
                val isSelected = uid == activeMemberUid

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedMemberIndex = idx
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (uid == currentUserId) "$memberName (আমি)" else memberName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "সিলেক্টেড",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (mealEntry != null) {
                                Text(
                                    text = "সকাল: ${mealEntry.breakfast} | দুপুর: ${mealEntry.lunch} | রাত: ${mealEntry.dinner}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "কোনো এন্ট্রি দেওয়া হয়নি (০)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${mealEntry?.totalMeals ?: 0.0} টি",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { selectedMemberIndex = idx }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Member Meal",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MealInputBlock(
    title: String,
    icon: ImageVector,
    value: Double,
    quickOptions: List<Double>,
    onValueChange: (Double) -> Unit,
    testTagPrefix: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            // Stepper controls (- / +)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(enabled = value > 0.0) {
                            val newVal = (value - 0.5).coerceAtLeast(0.0)
                            onValueChange(newVal)
                        }
                        .testTag("${testTagPrefix}_decrement"),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = String.format(Locale.US, "%.1f", value),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .testTag("${testTagPrefix}_value")
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable {
                            val newVal = (value + 0.5).coerceAtMost(10.0)
                            onValueChange(newVal)
                        }
                        .testTag("${testTagPrefix}_increment"),
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Preset Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickOptions.forEach { opt ->
                val isSelected = (value == opt)
                FilterChip(
                    selected = isSelected,
                    onClick = { onValueChange(opt) },
                    label = {
                        Text(
                            text = if (opt == 0.0) "০ (বন্ধ)" else "$opt টি",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("${testTagPrefix}_chip_$opt")
                )
            }
        }
    }
}

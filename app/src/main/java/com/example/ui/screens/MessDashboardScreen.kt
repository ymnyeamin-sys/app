package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BazarExpense
import com.example.data.model.Deposit
import com.example.data.model.Meal
import com.example.data.model.MemberSummary
import com.example.data.model.Mess
import com.example.data.model.MessSummary
import com.example.data.model.OtherExpense
import com.example.ui.MessViewModel
import com.example.ui.dialogs.RecordBazarDialog
import com.example.ui.dialogs.RecordDepositDialog
import com.example.ui.dialogs.RecordMealDialog
import com.example.ui.dialogs.RecordOtherExpenseDialog
import com.example.ui.theme.DueCardBackground
import com.example.ui.theme.DueCardText
import com.example.ui.theme.RefundCardBackground
import com.example.ui.theme.RefundCardText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessDashboardScreen(
    viewModel: MessViewModel,
    mess: Mess,
    onSwitchMess: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val uiMessage by viewModel.uiMessage.collectAsState()

    val currentMonth by viewModel.selectedMonth.collectAsState()
    val currentDate by viewModel.selectedDate.collectAsState()

    val meals by viewModel.meals.collectAsState()
    val deposits by viewModel.deposits.collectAsState()
    val bazarExpenses by viewModel.bazarExpenses.collectAsState()
    val otherExpenses by viewModel.otherExpenses.collectAsState()
    val messSummary by viewModel.messSummary.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showTopMenu by remember { mutableStateOf(false) }

    // Dialog controllers
    var showRecordMealDialog by remember { mutableStateOf(false) }
    var editingMeal by remember { mutableStateOf<Meal?>(null) }
    var showRecordDepositDialog by remember { mutableStateOf(false) }
    var showRecordBazarDialog by remember { mutableStateOf(false) }
    var showRecordOtherDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = mess.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Mess Code", mess.joinCode))
                                    Toast.makeText(context, "কোড কপি হয়েছে: ${mess.joinCode}", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(
                                text = "কোড: ${mess.joinCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Code",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Month changer arrows
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }
                    Text(
                        text = currentMonth,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                    }

                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("মেস পরিবর্তন") },
                                onClick = {
                                    showTopMenu = false
                                    onSwitchMess()
                                },
                                leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("অন্যান্য / ফিক্সড খরচ") },
                                onClick = {
                                    showTopMenu = false
                                    showRecordOtherDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("লগআউট") },
                                onClick = {
                                    showTopMenu = false
                                    onSignOut()
                                },
                                leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dining, contentDescription = null) },
                    label = { Text("মিল") },
                    modifier = Modifier.testTag("tab_meals")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    label = { Text("বাজার") },
                    modifier = Modifier.testTag("tab_bazar")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("জমা") },
                    modifier = Modifier.testTag("tab_deposits")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null) },
                    label = { Text("মাসিক হিসাব") },
                    modifier = Modifier.testTag("tab_summary")
                )
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> {
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingMeal = null
                            showRecordMealDialog = true
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("মিল যোগ") },
                        modifier = Modifier.testTag("fab_add_meal")
                    )
                }
                1 -> {
                    ExtendedFloatingActionButton(
                        onClick = { showRecordBazarDialog = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("বাজার খরচ") },
                        modifier = Modifier.testTag("fab_add_bazar")
                    )
                }
                2 -> {
                    ExtendedFloatingActionButton(
                        onClick = { showRecordDepositDialog = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("টাকা জমা") },
                        modifier = Modifier.testTag("fab_add_deposit")
                    )
                }
                3 -> {
                    FloatingActionButton(
                        onClick = {
                            shareMonthSummary(context, mess, currentMonth, messSummary)
                        },
                        modifier = Modifier.testTag("fab_share_summary")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // High-priority Live Meal Rate & Top Overview
            DashboardTopCard(summary = messSummary)

            // Dynamic Tab Content
            when (selectedTab) {
                0 -> DailyMealsTab(
                    mess = mess,
                    meals = meals,
                    currentDate = currentDate,
                    onDateChanged = { viewModel.setDate(it) },
                    onEditMeal = { meal ->
                        editingMeal = meal
                        showRecordMealDialog = true
                    },
                    onDeleteMeal = { mealId -> viewModel.deleteMeal(mealId) }
                )
                1 -> BazarExpensesTab(
                    bazarExpenses = bazarExpenses,
                    onDeleteExpense = { viewModel.deleteBazar(it) }
                )
                2 -> DepositsTab(
                    deposits = deposits,
                    onDeleteDeposit = { viewModel.deleteDeposit(it) }
                )
                3 -> MonthEndSummaryTab(
                    mess = mess,
                    month = currentMonth,
                    summary = messSummary,
                    otherExpenses = otherExpenses,
                    onAddOtherExpense = { showRecordOtherDialog = true },
                    onDeleteOtherExpense = { viewModel.deleteOtherExpense(it) }
                )
            }
        }
    }

    // Dialog Renderers
    if (showRecordMealDialog) {
        RecordMealDialog(
            mess = mess,
            initialMeal = editingMeal,
            selectedDate = currentDate,
            onDismiss = { showRecordMealDialog = false },
            onConfirm = { userId, userName, date, bf, lunch, din, mealId ->
                viewModel.recordMeal(userId, userName, date, bf, lunch, din, mealId)
                showRecordMealDialog = false
            }
        )
    }

    if (showRecordDepositDialog) {
        RecordDepositDialog(
            mess = mess,
            onDismiss = { showRecordDepositDialog = false },
            onConfirm = { userId, userName, amount, date, note ->
                viewModel.recordDeposit(userId, userName, amount, date, note)
                showRecordDepositDialog = false
            }
        )
    }

    if (showRecordBazarDialog) {
        RecordBazarDialog(
            mess = mess,
            onDismiss = { showRecordBazarDialog = false },
            onConfirm = { title, amount, date, shopperId, shopperName ->
                viewModel.recordBazar(title, amount, date, shopperId, shopperName)
                showRecordBazarDialog = false
            }
        )
    }

    if (showRecordOtherDialog) {
        RecordOtherExpenseDialog(
            onDismiss = { showRecordOtherDialog = false },
            onConfirm = { title, amount, date ->
                viewModel.recordOtherExpense(title, amount, date)
                showRecordOtherDialog = false
            }
        )
    }
}

@Composable
private fun DashboardTopCard(summary: MessSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Meal Rate Highlight
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "বর্তমান মিল রেট (Meal Rate)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = String.format(Locale.US, "৳ %.2f", summary.mealRate),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "হাতে নগদ",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = String.format(Locale.US, "৳ %.0f", summary.cashInHand),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            // 3-Metric Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "মোট মিল",
                    value = String.format(Locale.US, "%.1f টি", summary.totalMeals)
                )
                MetricItem(
                    label = "মোট বাজার",
                    value = String.format(Locale.US, "৳ %.0f", summary.totalBazar)
                )
                MetricItem(
                    label = "মোট জমা",
                    value = String.format(Locale.US, "৳ %.0f", summary.totalDeposit)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// --- Tab 1: Daily Meals ---
@Composable
private fun DailyMealsTab(
    mess: Mess,
    meals: List<Meal>,
    currentDate: String,
    onDateChanged: (String) -> Unit,
    onEditMeal: (Meal) -> Unit,
    onDeleteMeal: (String) -> Unit
) {
    val mealsOnDate = meals.filter { it.date == currentDate }
    val totalMealsOnDate = mealsOnDate.sumOf { it.totalMeals }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Date Header bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "তারিখ: $currentDate",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "এই দিনের মোট মিল: ${String.format(Locale.US, "%.1f", totalMealsOnDate)} টি",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row {
                    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
                    IconButton(
                        onClick = {
                            try {
                                val d = sdf.parse(currentDate) ?: Date()
                                val prev = Date(d.time - 24 * 60 * 60 * 1000)
                                onDateChanged(sdf.format(prev))
                            } catch (_: Exception) {}
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Day")
                    }
                    IconButton(
                        onClick = {
                            try {
                                val d = sdf.parse(currentDate) ?: Date()
                                val next = Date(d.time + 24 * 60 * 60 * 1000)
                                onDateChanged(sdf.format(next))
                            } catch (_: Exception) {}
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                    }
                }
            }
        }

        Text(
            text = "সদস্যদের মিল তালিকা:",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mess.memberUids) { uid ->
                val index = mess.memberUids.indexOf(uid)
                val memberName = mess.memberNames.getOrNull(index) ?: "Member"
                val existingMeal = mealsOnDate.find { it.userId == uid }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (existingMeal != null && existingMeal.totalMeals > 0)
                            MaterialTheme.colorScheme.surface
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                            Text(
                                text = memberName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (existingMeal != null) {
                                Text(
                                    text = "সকাল: ${existingMeal.breakfast} | দুপুর: ${existingMeal.lunch} | রাত: ${existingMeal.dinner}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "কোনো মিল এন্ট্রি নেই (০)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${existingMeal?.totalMeals ?: 0.0} টি",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    val toEdit = existingMeal ?: Meal(
                                        messId = mess.id,
                                        userId = uid,
                                        userName = memberName,
                                        memberUids = mess.memberUids,
                                        date = currentDate,
                                        month = currentDate.take(7)
                                    )
                                    onEditMeal(toEdit)
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Meal", tint = MaterialTheme.colorScheme.primary)
                            }
                            if (existingMeal != null) {
                                IconButton(onClick = { onDeleteMeal(existingMeal.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Tab 2: Bazar Expenses ---
@Composable
private fun BazarExpensesTab(
    bazarExpenses: List<BazarExpense>,
    onDeleteExpense: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "বাজার খরচের তালিকা (${bazarExpenses.size} টি):",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        if (bazarExpenses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "এই মাসে কোনো বাজার খরচ যোগ করা হয়নি।\nনিচের '+ বাজার খরচ' বাটনে চাপুন।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bazarExpenses) { exp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                    text = exp.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "তারিখ: ${exp.date} • বাজার করেছেন: ${exp.shopperName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format(Locale.US, "৳ %.0f", exp.amount),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                IconButton(onClick = { onDeleteExpense(exp.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Tab 3: Deposits ---
@Composable
private fun DepositsTab(
    deposits: List<Deposit>,
    onDeleteDeposit: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "টাকা জমার বিবরণ (${deposits.size} টি):",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        if (deposits.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "এই মাসে কোনো টাকা জমার রেকর্ড নেই।\nনিচের '+ টাকা জমা' বাটনে চাপুন।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(deposits) { dep ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                    text = dep.userName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "তারিখ: ${dep.date}${if (!dep.note.isNullOrBlank()) " • " + dep.note else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format(Locale.US, "৳ %.0f", dep.amount),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                )
                                IconButton(onClick = { onDeleteDeposit(dep.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Tab 4: Month-End Summary ---
@Composable
private fun MonthEndSummaryTab(
    mess: Mess,
    month: String,
    summary: MessSummary,
    otherExpenses: List<OtherExpense>,
    onAddOtherExpense: () -> Unit,
    onDeleteOtherExpense: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "মাস শেষের চূড়ান্ত হিসাব বিবরণী ($month)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Member Summary Cards
        items(summary.memberSummaries) { mem ->
            val isRefund = mem.netBalance >= 0
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRefund) RefundCardBackground else DueCardBackground
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mem.userName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isRefund) RefundCardText else DueCardText,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = if (isRefund)
                                    "ফেরত পাবে: ৳ ${String.format(Locale.US, "%.0f", mem.netBalance)}"
                                else
                                    "বকেয়া/দেবে: ৳ ${String.format(Locale.US, "%.0f", -mem.netBalance)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("মোট মিল: ${String.format(Locale.US, "%.1f", mem.totalMeals)} টি", style = MaterialTheme.typography.bodySmall)
                            Text("মিল খরচ: ৳ ${String.format(Locale.US, "%.1f", mem.mealCost)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Column {
                            Text("অন্যান্য খরচ: ৳ ${String.format(Locale.US, "%.1f", mem.sharedOtherCost)}", style = MaterialTheme.typography.bodySmall)
                            Text("মোট জমা: ৳ ${String.format(Locale.US, "%.0f", mem.totalDeposit)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Shared / Fixed Other Expenses section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "অন্যান্য ফিক্সড খরচ (খালা, গ্যাস, ওয়াইফাই):",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onAddOtherExpense) {
                    Icon(Icons.Default.Add, contentDescription = "Add Other Expense", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (otherExpenses.isEmpty()) {
            item {
                Text(
                    text = "কোনো অতিরিক্ত খরচ নেই (বুয়া, গ্যাস ইত্যাদি যোগ করতে '+' চাপুন)।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(otherExpenses) { oth ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${oth.title} (${oth.date})", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format(Locale.US, "৳ %.0f", oth.amount),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            IconButton(onClick = { onDeleteOtherExpense(oth.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

private fun shareMonthSummary(
    context: Context,
    mess: Mess,
    month: String,
    summary: MessSummary
) {
    val sb = StringBuilder()
    sb.append("📊 মেসের মাস শেষের হিসাব 📊\n")
    sb.append("মেস: ${mess.name}\n")
    sb.append("মাস: $month\n")
    sb.append("-----------------------------\n")
    sb.append("🍲 মোট মিল: ${String.format(Locale.US, "%.1f", summary.totalMeals)} টি\n")
    sb.append("🛒 মোট বাজার খরচ: ৳ ${String.format(Locale.US, "%.0f", summary.totalBazar)}\n")
    sb.append("💡 অন্যান্য খরচ: ৳ ${String.format(Locale.US, "%.0f", summary.totalOther)}\n")
    sb.append("💵 মোট জমা: ৳ ${String.format(Locale.US, "%.0f", summary.totalDeposit)}\n")
    sb.append("⚡ মিল রেট: ৳ ${String.format(Locale.US, "%.2f", summary.mealRate)}\n")
    sb.append("💰 অবশিষ্ট ক্যাশ: ৳ ${String.format(Locale.US, "%.0f", summary.cashInHand)}\n")
    sb.append("-----------------------------\n")
    sb.append("সদস্যদের হিসাব:\n")
    summary.memberSummaries.forEach { mem ->
        val status = if (mem.netBalance >= 0)
            "ফেরত পাবে: +৳ ${String.format(Locale.US, "%.0f", mem.netBalance)}"
        else
            "বকেয়া/দেবে: -৳ ${String.format(Locale.US, "%.0f", -mem.netBalance)}"

        sb.append("• ${mem.userName}: মিল ${String.format(Locale.US, "%.1f", mem.totalMeals)} টি | জমা ৳ ${String.format(Locale.US, "%.0f", mem.totalDeposit)} | মোট খরচ ৳ ${String.format(Locale.US, "%.0f", mem.totalCost)} => $status\n")
    }
    sb.append("-----------------------------\n")
    sb.append("তৈরি হয়েছে Mess Meal Manager অ্যাপ দিয়ে")

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, sb.toString())
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "মেসের হিসাব শেয়ার করুন")
    context.startActivity(shareIntent)
}

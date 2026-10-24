package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Meal
import com.example.data.model.Mess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CreateMessDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন মেস তৈরি করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "মেসের নাম দিন। রুমমেটদের যোগ করার জন্য একটি কোড শেয়ার করতে পারবেন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("মেসের নাম * (উদা: স্বপ্ননীড়)") },
                    modifier = Modifier.fillMaxWidth().testTag("mess_name_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("ঐচ্ছিক কোড (উদা: MESS99)") },
                    placeholder = { Text("ফাঁকা রাখলে স্বয়ংক্রিয় তৈরি হবে") },
                    modifier = Modifier.fillMaxWidth().testTag("mess_code_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), code.trim().takeIf { it.isNotBlank() })
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("create_mess_submit")
            ) {
                Text("তৈরি করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun JoinMessDialog(
    onDismiss: () -> Unit,
    onConfirm: (code: String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("মেসে যোগ দিন", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "আপনার মেস ম্যানেজারের দেওয়া জয়েন কোডটি লিখুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("মেস জয়েন কোড (উদা: MESS01)") },
                    modifier = Modifier.fillMaxWidth().testTag("join_code_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank()) {
                        onConfirm(code.trim())
                    }
                },
                enabled = code.isNotBlank(),
                modifier = Modifier.testTag("join_mess_submit")
            ) {
                Text("যুক্ত হোন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordMealDialog(
    mess: Mess,
    initialMeal: Meal? = null,
    selectedDate: String,
    onDismiss: () -> Unit,
    onConfirm: (userId: String, userName: String, date: String, breakfast: Double, lunch: Double, dinner: Double, mealId: String?) -> Unit
) {
    var selectedIndex by remember {
        val initialIdx = if (initialMeal != null) {
            mess.memberUids.indexOf(initialMeal.userId).takeIf { it >= 0 } ?: 0
        } else 0
        mutableStateOf(initialIdx)
    }
    var expandedMemberDropdown by remember { mutableStateOf(false) }

    var dateText by remember { mutableStateOf(initialMeal?.date ?: selectedDate) }
    var breakfast by remember { mutableDoubleStateOf(initialMeal?.breakfast ?: 0.0) }
    var lunch by remember { mutableDoubleStateOf(initialMeal?.lunch ?: 1.0) }
    var dinner by remember { mutableDoubleStateOf(initialMeal?.dinner ?: 1.0) }

    val total = breakfast + lunch + dinner
    val memberName = mess.memberNames.getOrNull(selectedIndex) ?: "Member"
    val memberUid = mess.memberUids.getOrNull(selectedIndex) ?: ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialMeal == null) "মিল এন্ট্রি / হিসাব যোগ করুন" else "মিল সম্পাদনা করুন",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Member Selector
                ExposedDropdownMenuBox(
                    expanded = expandedMemberDropdown,
                    onExpandedChange = { expandedMemberDropdown = !expandedMemberDropdown }
                ) {
                    OutlinedTextField(
                        value = memberName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("সদস্য নির্বাচন করুন") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMemberDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMemberDropdown,
                        onDismissRequest = { expandedMemberDropdown = false }
                    ) {
                        mess.memberUids.forEachIndexed { idx, _ ->
                            val name = mess.memberNames.getOrNull(idx) ?: "Member $idx"
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedIndex = idx
                                    expandedMemberDropdown = false
                                }
                            )
                        }
                    }
                }

                // Date Picker field
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("তারিখ (YYYY-MM-DD)") },
                    trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Meal counters: Breakfast, Lunch, Dinner
                MealCounterRow(
                    label = "সকালের নাস্তা (Breakfast):",
                    value = breakfast,
                    step = 0.5,
                    onValueChange = { breakfast = it }
                )
                MealCounterRow(
                    label = "দুপুরের খাবার (Lunch):",
                    value = lunch,
                    step = 0.5,
                    onValueChange = { lunch = it }
                )
                MealCounterRow(
                    label = "রাতের খাবার (Dinner):",
                    value = dinner,
                    step = 0.5,
                    onValueChange = { dinner = it }
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "দিনের মোট মিল:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f টি", total),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (memberUid.isNotBlank() && dateText.length == 10) {
                        onConfirm(
                            memberUid,
                            memberName,
                            dateText,
                            breakfast,
                            lunch,
                            dinner,
                            initialMeal?.id
                        )
                    }
                },
                modifier = Modifier.testTag("save_meal_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
private fun MealCounterRow(
    label: String,
    value: Double,
    step: Double,
    onValueChange: (Double) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = {
                    val next = (value - step).coerceAtLeast(0.0)
                    onValueChange(next)
                },
                enabled = value > 0.0
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }
            Text(
                text = String.format(Locale.US, "%.1f", value),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            FilledTonalIconButton(
                onClick = {
                    val next = (value + step).coerceAtMost(10.0)
                    onValueChange(next)
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDepositDialog(
    mess: Mess,
    onDismiss: () -> Unit,
    onConfirm: (userId: String, userName: String, amount: Double, date: String, note: String?) -> Unit
) {
    var selectedIndex by remember { mutableStateOf(0) }
    var expandedMemberDropdown by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var dateText by remember { mutableStateOf(today) }

    val memberName = mess.memberNames.getOrNull(selectedIndex) ?: "Member"
    val memberUid = mess.memberUids.getOrNull(selectedIndex) ?: ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("টাকা জমা / ডিপোজিট যোগ করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expandedMemberDropdown,
                    onExpandedChange = { expandedMemberDropdown = !expandedMemberDropdown }
                ) {
                    OutlinedTextField(
                        value = memberName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("কে জমা দিয়েছে?") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMemberDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMemberDropdown,
                        onDismissRequest = { expandedMemberDropdown = false }
                    ) {
                        mess.memberUids.forEachIndexed { idx, _ ->
                            val name = mess.memberNames.getOrNull(idx) ?: "Member $idx"
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedIndex = idx
                                    expandedMemberDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("জমার পরিমাণ (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("deposit_amount_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("তারিখ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("নোট (উদা: বিকাশ, নগদ, ব্যাংক)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            val amount = amountText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    if (amount > 0 && memberUid.isNotBlank()) {
                        onConfirm(memberUid, memberName, amount, dateText, noteText.takeIf { it.isNotBlank() })
                    }
                },
                enabled = amount > 0,
                modifier = Modifier.testTag("save_deposit_button")
            ) {
                Text("জমা করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordBazarDialog(
    mess: Mess,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, date: String, shopperId: String, shopperName: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var dateText by remember { mutableStateOf(today) }

    var selectedIndex by remember { mutableStateOf(0) }
    var expandedMemberDropdown by remember { mutableStateOf(false) }

    val shopperName = mess.memberNames.getOrNull(selectedIndex) ?: "Shopper"
    val shopperId = mess.memberUids.getOrNull(selectedIndex) ?: ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("বাজার খরচ যোগ করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("বাজারের বিবরণ * (উদা: মাছ, চাল, তেল)") },
                    modifier = Modifier.fillMaxWidth().testTag("bazar_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("খরচের পরিমাণ (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("bazar_amount_input"),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = expandedMemberDropdown,
                    onExpandedChange = { expandedMemberDropdown = !expandedMemberDropdown }
                ) {
                    OutlinedTextField(
                        value = shopperName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("কে বাজার করেছেন?") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMemberDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMemberDropdown,
                        onDismissRequest = { expandedMemberDropdown = false }
                    ) {
                        mess.memberUids.forEachIndexed { idx, _ ->
                            val name = mess.memberNames.getOrNull(idx) ?: "Member $idx"
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedIndex = idx
                                    expandedMemberDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("তারিখ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            val amount = amountText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    if (title.isNotBlank() && amount > 0) {
                        onConfirm(title.trim(), amount, dateText, shopperId, shopperName)
                    }
                },
                enabled = title.isNotBlank() && amount > 0,
                modifier = Modifier.testTag("save_bazar_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
fun RecordOtherExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, date: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var dateText by remember { mutableStateOf(today) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("অন্যান্য / ফিক্সড খরচ যোগ করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "খালা/বুয়া বিল, গ্যাস, ওয়াইফাই, বিদ্যুৎ বিল যা মেসের সবার মাঝে সমানভাবে ভাগ হবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("খরচের নাম * (উদা: বুয়া বিল, গ্যাস বিল)") },
                    modifier = Modifier.fillMaxWidth().testTag("other_title_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("টাকার পরিমাণ (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("other_amount_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("তারিখ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            val amount = amountText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    if (title.isNotBlank() && amount > 0) {
                        onConfirm(title.trim(), amount, dateText)
                    }
                },
                enabled = title.isNotBlank() && amount > 0,
                modifier = Modifier.testTag("save_other_expense_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrEditRoomDialog(
    initialRoom: com.example.data.model.Room? = null,
    onDismiss: () -> Unit,
    onConfirm: (roomNumber: String, type: String, capacity: Int, rent: Double) -> Unit
) {
    var roomNumber by remember { mutableStateOf(initialRoom?.roomNumber ?: "") }
    var selectedType by remember { mutableStateOf(initialRoom?.type ?: "Double") }
    var expandedTypeDropdown by remember { mutableStateOf(false) }
    var capacityText by remember { mutableStateOf(initialRoom?.capacity?.toString() ?: if (selectedType == "Single") "1" else "2") }
    var rentText by remember { mutableStateOf(initialRoom?.rent?.takeIf { it > 0 }?.toInt()?.toString() ?: "") }

    val roomTypes = listOf("Single", "Double", "Triple", "Shared")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialRoom == null) "নতুন রুম যোগ করুন (Admin)" else "রুম সম্পাদনা করুন",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "মেসে আগে থেকেই রুম তৈরি করে রাখুন (সিঙ্গেল বা ডাবল)। পরে মেম্বাররা এই রুমে উঠতে পারবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text("রুম নম্বর / নাম * (উদা: 101, 202)") },
                    modifier = Modifier.fillMaxWidth().testTag("room_number_input"),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = expandedTypeDropdown,
                    onExpandedChange = { expandedTypeDropdown = !expandedTypeDropdown }
                ) {
                    OutlinedTextField(
                        value = when (selectedType) {
                            "Single" -> "সিঙ্গেল রুম (Single - ১ সিট)"
                            "Double" -> "ডাবল রুম (Double - ২ সিট)"
                            "Triple" -> "ট্রিপল রুম (Triple - ৩ সিট)"
                            else -> "শেয়ার্ড রুম (Shared)"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("রুমের ধরন (Type) *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTypeDropdown,
                        onDismissRequest = { expandedTypeDropdown = false }
                    ) {
                        roomTypes.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (type) {
                                            "Single" -> "সিঙ্গেল রুম (Single - ১ সিট)"
                                            "Double" -> "ডাবল রুম (Double - ২ সিট)"
                                            "Triple" -> "ট্রিপল রুম (Triple - ৩ সিট)"
                                            else -> "শেয়ার্ড রুম (Shared)"
                                        }
                                    )
                                },
                                onClick = {
                                    selectedType = type
                                    capacityText = when (type) {
                                        "Single" -> "1"
                                        "Double" -> "2"
                                        "Triple" -> "3"
                                        else -> "4"
                                    }
                                    expandedTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it.filter { c -> c.isDigit() } },
                    label = { Text("সিট ধারণক্ষমতা (Capacity) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("room_capacity_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = rentText,
                    onValueChange = { rentText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("ভাড়া ৳ (ঐচ্ছিক সিট বা রুম ভাড়া)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("room_rent_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            val capacity = capacityText.toIntOrNull() ?: 1
            val rent = rentText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    if (roomNumber.isNotBlank() && capacity >= 1) {
                        onConfirm(roomNumber.trim(), selectedType, capacity, rent)
                    }
                },
                enabled = roomNumber.isNotBlank() && capacity >= 1,
                modifier = Modifier.testTag("save_room_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignMemberRoomDialog(
    mess: Mess,
    room: com.example.data.model.Room,
    onDismiss: () -> Unit,
    onAssign: (memberUid: String, memberName: String) -> Unit
) {
    var selectedIndex by remember { mutableStateOf(0) }
    var expandedDropdown by remember { mutableStateOf(false) }

    val memberName = mess.memberNames.getOrNull(selectedIndex) ?: "Member"
    val memberUid = mess.memberUids.getOrNull(selectedIndex) ?: ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("রুম ${room.roomNumber}-এ সদস্য যুক্ত করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "কোন সদস্যকে এই রুমে বরাদ্দ দিতে চান নির্বাচন করুন:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = memberName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("সদস্য নির্বাচন") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        mess.memberUids.forEachIndexed { idx, uid ->
                            val name = mess.memberNames.getOrNull(idx) ?: "Member $idx"
                            val isAlreadyInThisRoom = room.assignedMemberUids.contains(uid)
                            DropdownMenuItem(
                                text = {
                                    Text(if (isAlreadyInThisRoom) "$name (ইতিমধ্যেই এই রুমে আছেন)" else name)
                                },
                                onClick = {
                                    selectedIndex = idx
                                    expandedDropdown = false
                                },
                                enabled = !isAlreadyInThisRoom
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (memberUid.isNotBlank()) {
                        onAssign(memberUid, memberName)
                    }
                },
                enabled = memberUid.isNotBlank() && !room.assignedMemberUids.contains(memberUid)
            ) {
                Text("রুমে দিন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
fun AdminManagementDialog(
    mess: Mess,
    rooms: List<com.example.data.model.Room>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onToggleAdmin: (memberUid: String, memberName: String) -> Unit,
    onRemoveMember: (memberUid: String, memberName: String) -> Unit
) {
    var memberToRemove by remember { mutableStateOf<Pair<String, String>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "মেস কন্ট্রোল ও সদস্য তালিকা", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "মেসের সব সদস্যদের তালিকা, অ্যাডমিন ক্ষমতা ও রুম অবস্থান নিয়ন্ত্রণ করুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mess.memberUids) { uid ->
                        val index = mess.memberUids.indexOf(uid)
                        val name = mess.memberNames.getOrNull(index) ?: "Member"
                        val isUserAdmin = mess.isAdmin(uid)
                        val isSelf = uid == currentUserId
                        val memberRoom = rooms.find { it.assignedMemberUids.contains(uid) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUserAdmin)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
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
                                            if (isUserAdmin) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ) {
                                                    Text(
                                                        text = "👑 অ্যাডমিন",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }

                                            Text(
                                                text = if (memberRoom != null) "🏠 রুম ${memberRoom.roomNumber}" else "🏠 রুম নেই",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Action buttons
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Toggle admin role button
                                        IconButton(
                                            onClick = { onToggleAdmin(uid, name) },
                                            enabled = !isSelf || mess.adminUids.size > 1
                                        ) {
                                            Icon(
                                                imageVector = if (isUserAdmin) Icons.Default.Security else Icons.Default.AdminPanelSettings,
                                                contentDescription = if (isUserAdmin) "অ্যাডমিন পদ বাতিল" else "অ্যাডমিন বানান",
                                                tint = if (isUserAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Remove member button
                                        if (!isSelf) {
                                            IconButton(onClick = { memberToRemove = uid to name }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "সদস্য অপসারণ",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("ঠিক আছে")
            }
        }
    )

    // Remove Confirmation Alert
    if (memberToRemove != null) {
        val (uid, name) = memberToRemove!!
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("সদস্য অপসারণ নিশ্চিত করুন") },
            text = { Text("আপনি কি নিশ্চিত যে $name-কে এই মেস থেকে বাদ দিতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveMember(uid, name)
                        memberToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
}


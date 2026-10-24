package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BazarExpense
import com.example.data.model.Deposit
import com.example.data.model.Meal
import com.example.data.model.MemberSummary
import com.example.data.model.Mess
import com.example.data.model.MessSummary
import com.example.data.model.OtherExpense
import com.example.data.model.Room
import com.example.data.model.UserProfile
import com.example.data.repository.MessRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MessViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MessRepository(application)
    val auth = Firebase.auth

    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _selectedMonth = MutableStateFlow(monthFormat.format(Date()))
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(dateFormat.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _userMesses = MutableStateFlow<List<Mess>>(emptyList())
    val userMesses: StateFlow<List<Mess>> = _userMesses.asStateFlow()

    private val _activeMess = MutableStateFlow<Mess?>(null)
    val activeMess: StateFlow<Mess?> = _activeMess.asStateFlow()

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    val isAdmin: StateFlow<Boolean> = _activeMess.map { mess ->
        val uid = auth.currentUser?.uid ?: return@map false
        mess?.isAdmin(uid) == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    private val _meals = MutableStateFlow<List<Meal>>(emptyList())
    val meals: StateFlow<List<Meal>> = _meals.asStateFlow()

    private val _deposits = MutableStateFlow<List<Deposit>>(emptyList())
    val deposits: StateFlow<List<Deposit>> = _deposits.asStateFlow()

    private val _bazarExpenses = MutableStateFlow<List<BazarExpense>>(emptyList())
    val bazarExpenses: StateFlow<List<BazarExpense>> = _bazarExpenses.asStateFlow()

    private val _otherExpenses = MutableStateFlow<List<OtherExpense>>(emptyList())
    val otherExpenses: StateFlow<List<OtherExpense>> = _otherExpenses.asStateFlow()

    private val _messSummary = MutableStateFlow(MessSummary())
    val messSummary: StateFlow<MessSummary> = _messSummary.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private var observeJob: Job? = null

    init {
        loadInitialData()
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun loadInitialData() {
        auth.currentUser ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profile = repository.getOrCreateUserProfile()
                _userProfile.value = profile

                repository.observeUserMesses().catch { e ->
                    _uiMessage.value = "Error loading messes: ${e.message}"
                }.collect { messes ->
                    _userMesses.value = messes
                    if (messes.isNotEmpty()) {
                        val currentActiveId = _activeMess.value?.id ?: profile.activeMessId
                        val match = messes.find { it.id == currentActiveId } ?: messes.first()
                        if (_activeMess.value?.id != match.id) {
                            setActiveMess(match)
                        } else {
                            _activeMess.value = match
                            recalculateSummary()
                        }
                    } else {
                        _activeMess.value = null
                        _rooms.value = emptyList()
                        _meals.value = emptyList()
                        _deposits.value = emptyList()
                        _bazarExpenses.value = emptyList()
                        _otherExpenses.value = emptyList()
                        _messSummary.value = MessSummary()
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _uiMessage.value = e.message ?: "Failed to initialize"
            }
        }
    }

    fun setActiveMess(mess: Mess) {
        _activeMess.value = mess
        viewModelScope.launch {
            repository.updateActiveMess(mess.id)
        }
        startObservingMessData(mess.id, _selectedMonth.value)
    }

    fun setMonth(newMonth: String) {
        _selectedMonth.value = newMonth
        val messId = _activeMess.value?.id ?: return
        startObservingMessData(messId, newMonth)
    }

    fun previousMonth() {
        try {
            val cal = Calendar.getInstance()
            cal.time = monthFormat.parse(_selectedMonth.value) ?: Date()
            cal.add(Calendar.MONTH, -1)
            setMonth(monthFormat.format(cal.time))
        } catch (_: Exception) {}
    }

    fun nextMonth() {
        try {
            val cal = Calendar.getInstance()
            cal.time = monthFormat.parse(_selectedMonth.value) ?: Date()
            cal.add(Calendar.MONTH, 1)
            setMonth(monthFormat.format(cal.time))
        } catch (_: Exception) {}
    }

    fun setDate(newDate: String) {
        _selectedDate.value = newDate
        val monthOfDate = newDate.take(7)
        if (monthOfDate != _selectedMonth.value) {
            setMonth(monthOfDate)
        }
    }

    private fun startObservingMessData(messId: String, month: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                repository.observeMeals(messId, month),
                repository.observeDeposits(messId, month),
                repository.observeBazarExpenses(messId, month),
                repository.observeOtherExpenses(messId, month),
                repository.observeRooms(messId)
            ) { mealsList, depositsList, bazarList, otherList, roomsList ->
                _meals.value = mealsList
                _deposits.value = depositsList
                _bazarExpenses.value = bazarList
                _otherExpenses.value = otherList
                _rooms.value = roomsList
                computeSummary(mealsList, depositsList, bazarList, otherList, roomsList, _activeMess.value)
            }.catch { error ->
                _uiMessage.value = "Error syncing mess data: ${error.message}"
            }.collect { summary ->
                _messSummary.value = summary
            }
        }
    }

    private fun recalculateSummary() {
        _messSummary.value = computeSummary(
            _meals.value,
            _deposits.value,
            _bazarExpenses.value,
            _otherExpenses.value,
            _rooms.value,
            _activeMess.value
        )
    }

    private fun computeSummary(
        meals: List<Meal>,
        deposits: List<Deposit>,
        bazar: List<BazarExpense>,
        other: List<OtherExpense>,
        rooms: List<Room>,
        mess: Mess?
    ): MessSummary {
        val totalMeals = meals.sumOf { it.totalMeals }
        val totalBazar = bazar.sumOf { it.amount }
        val totalOther = other.sumOf { it.amount }
        val totalDeposit = deposits.sumOf { it.amount }

        val mealRate = if (totalMeals > 0.0) totalBazar / totalMeals else 0.0
        val memberCount = mess?.memberUids?.size?.coerceAtLeast(1) ?: 1
        val sharedOtherPerMember = totalOther / memberCount.toDouble()

        val memberSummaries = if (mess != null) {
            mess.memberUids.mapIndexed { index, uid ->
                val name = mess.memberNames.getOrNull(index) ?: "Member"
                val memberMeals = meals.filter { it.userId == uid }.sumOf { it.totalMeals }
                val memberDeposit = deposits.filter { it.userId == uid }.sumOf { it.amount }
                val memberMealCost = memberMeals * mealRate
                val totalMemberCost = memberMealCost + sharedOtherPerMember
                val balance = memberDeposit - totalMemberCost

                val room = rooms.find { it.assignedMemberUids.contains(uid) }
                val roomNumber = room?.let { "রুম ${it.roomNumber}" } ?: "অনির্ধারিত"
                val roomType = room?.type ?: ""

                MemberSummary(
                    userId = uid,
                    userName = name,
                    roomNumber = roomNumber,
                    roomType = roomType,
                    totalMeals = memberMeals,
                    totalDeposit = memberDeposit,
                    mealCost = memberMealCost,
                    sharedOtherCost = sharedOtherPerMember,
                    totalCost = totalMemberCost,
                    netBalance = balance
                )
            }
        } else {
            emptyList()
        }

        val cashInHand = totalDeposit - (totalBazar + totalOther)

        return MessSummary(
            totalMeals = totalMeals,
            totalBazar = totalBazar,
            totalOther = totalOther,
            totalDeposit = totalDeposit,
            mealRate = mealRate,
            cashInHand = cashInHand,
            memberSummaries = memberSummaries
        )
    }

    // --- Mess Actions ---
    fun createMess(name: String, joinCode: String? = null, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.createMess(name, joinCode)
            _isLoading.value = false
            if (result.isSuccess) {
                _uiMessage.value = "মেস '${name}' সফলভাবে তৈরি হয়েছে! আপনি এই মেসের অ্যাডমিন।"
                setActiveMess(result.getOrThrow())
                onComplete(true)
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "মেস তৈরি ব্যর্থ হয়েছে"
                onComplete(false)
            }
        }
    }

    fun joinMess(code: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.joinMessByCode(code)
            _isLoading.value = false
            if (result.isSuccess) {
                _uiMessage.value = "মেসে যুক্ত হয়েছেন! এখন রুম ও সিট বেছে নিতে পারেন।"
                setActiveMess(result.getOrThrow())
                onComplete(true)
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "মেসে যুক্ত হওয়া ব্যর্থ হয়েছে"
                onComplete(false)
            }
        }
    }

    fun removeMember(memberUid: String, memberName: String) {
        val currentMess = _activeMess.value ?: return
        viewModelScope.launch {
            val result = repository.removeMemberFromMess(currentMess.id, memberUid)
            if (result.isSuccess) {
                _uiMessage.value = "$memberName-কে মেস থেকে অপসারণ করা হয়েছে"
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "সদস্য অপসারণে ব্যর্থ"
            }
        }
    }

    fun toggleAdminRole(memberUid: String, memberName: String) {
        val currentMess = _activeMess.value ?: return
        viewModelScope.launch {
            val result = repository.toggleAdminRole(currentMess.id, memberUid)
            if (result.isSuccess) {
                _uiMessage.value = "$memberName-এর অ্যাডমিন রোল আপডেট হয়েছে"
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "অ্যাডমিন রোল পরিবর্তনে ব্যর্থ"
            }
        }
    }

    // --- Room Actions ---
    fun createOrUpdateRoom(
        roomNumber: String,
        type: String,
        capacity: Int,
        rent: Double,
        existingRoomId: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val currentMess = _activeMess.value ?: return
        viewModelScope.launch {
            val result = repository.createOrUpdateRoom(
                messId = currentMess.id,
                memberUids = currentMess.memberUids,
                roomNumber = roomNumber,
                type = type,
                capacity = capacity,
                rent = rent,
                existingRoomId = existingRoomId
            )
            if (result.isSuccess) {
                _uiMessage.value = "রুম $roomNumber সফলভাবে সংরক্ষিত হয়েছে!"
                onComplete(true)
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "রুম সেভ ব্যর্থ"
                onComplete(false)
            }
        }
    }

    fun deleteRoom(roomId: String) {
        val currentMess = _activeMess.value ?: return
        viewModelScope.launch {
            val result = repository.deleteRoom(currentMess.id, roomId)
            if (result.isSuccess) {
                _uiMessage.value = "রুম মুছে ফেলা হয়েছে"
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "রুম মুছতে ব্যর্থ"
            }
        }
    }

    fun chooseRoom(room: Room, targetUid: String? = null, targetName: String? = null) {
        val currentMess = _activeMess.value ?: return
        val uid = targetUid ?: currentUserId
        val name = targetName ?: (auth.currentUser?.displayName ?: "User")
        viewModelScope.launch {
            val result = repository.assignMemberToRoom(currentMess.id, room.id, uid, name)
            if (result.isSuccess) {
                _uiMessage.value = "$name রুম ${room.roomNumber}-এ উঠেছেন!"
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "রুমে উঠতে ব্যর্থ"
            }
        }
    }

    fun leaveRoom(room: Room, targetUid: String? = null) {
        val currentMess = _activeMess.value ?: return
        val uid = targetUid ?: currentUserId
        viewModelScope.launch {
            val result = repository.leaveRoom(currentMess.id, room.id, uid)
            if (result.isSuccess) {
                _uiMessage.value = "রুম ত্যাগ সম্পন্ন হয়েছে"
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "রুম ছাড়তে ব্যর্থ"
            }
        }
    }

    // --- Meals ---
    fun recordMeal(
        targetUserId: String,
        targetUserName: String,
        date: String,
        breakfast: Double,
        lunch: Double,
        dinner: Double,
        existingMealId: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val currentMess = _activeMess.value ?: return
        viewModelScope.launch {
            val result = repository.recordMeal(
                messId = currentMess.id,
                memberUids = currentMess.memberUids,
                targetUserId = targetUserId,
                targetUserName = targetUserName,
                date = date,
                breakfast = breakfast,
                lunch = lunch,
                dinner = dinner,
                existingMealId = existingMealId
            )
            if (result.isSuccess) {
                _uiMessage.value = "${targetUserName}-এর মিল সংরক্ষণ হয়েছে"
                onComplete(true)
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "মিল সেভ ব্যর্থ হয়েছে"
                onComplete(false)
            }
        }
    }

    fun deleteMeal(mealId: String) {
        val currentMess = _activeMess.value ?: return
        viewModelScope.launch {
            val result = repository.deleteMeal(currentMess.id, mealId)
            if (result.isSuccess) {
                _uiMessage.value = "মিল রেকর্ড মুছে ফেলা হয়েছে"
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "মুছতে ব্যর্থ"
            }
        }
    }

    // --- Deposits ---
    fun recordDeposit(
        targetUserId: String,
        targetUserName: String,
        amount: Double,
        date: String,
        note: String?,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val currentMess = _activeMess.value ?: return
        viewModelScope.launch {
            val result = repository.recordDeposit(
                messId = currentMess.id,
                memberUids = currentMess.memberUids,
                targetUserId = targetUserId,
                targetUserName = targetUserName,
                amount = amount,
                date = date,
                note = note
            )
            if (result.isSuccess) {
                _uiMessage.value = "৳ ${amount.toInt()} জমা সংরক্ষণ হয়েছে"
                onComplete(true)
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "জমা সেভ ব্যর্থ"
                onComplete(false)
            }
        }
    }

    fun deleteDeposit(depositId: String) {
        val currentMess = _activeMess.value ?: return
        viewModelScope.launch {
            val result = repository.deleteDeposit(currentMess.id, depositId)
            if (result.isSuccess) {
                _uiMessage.value = "জমা রেকর্ড মুছে ফেলা হয়েছে"
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "মুছতে ব্যর্থ"
            }
        }
    }

    // --- Bazar ---
    fun recordBazar(
        title: String,
        amount: Double,
        date: String,
        shopperId: String,
        shopperName: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val currentMess = _activeMess.value ?: return
        viewModelScope.launch {
            val result = repository.recordBazarExpense(
                messId = currentMess.id,
                memberUids = currentMess.memberUids,
                title = title,
                amount = amount,
                date = date,
                shopperId = shopperId,
                shopperName = shopperName
            )
            if (result.isSuccess) {
                _uiMessage.value = "বাজার খরচ ৳ ${amount.toInt()} সংরক্ষণ হয়েছে"
                onComplete(true)
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "বাজার খরচ সেভ ব্যর্থ"
                onComplete(false)
            }
        }
    }

    fun deleteBazar(expenseId: String) {
        val currentMess = _activeMess.value ?: return
        viewModelScope.launch {
            val result = repository.deleteBazarExpense(currentMess.id, expenseId)
            if (result.isSuccess) {
                _uiMessage.value = "বাজার খরচ মুছে ফেলা হয়েছে"
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "মুছতে ব্যর্থ"
            }
        }
    }

    // --- Other Expenses ---
    fun recordOtherExpense(
        title: String,
        amount: Double,
        date: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val currentMess = _activeMess.value ?: return
        viewModelScope.launch {
            val result = repository.recordOtherExpense(
                messId = currentMess.id,
                memberUids = currentMess.memberUids,
                title = title,
                amount = amount,
                date = date
            )
            if (result.isSuccess) {
                _uiMessage.value = "অন্যান্য খরচ ৳ ${amount.toInt()} সংরক্ষণ হয়েছে"
                onComplete(true)
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "অন্যান্য খরচ সেভ ব্যর্থ"
                onComplete(false)
            }
        }
    }

    fun deleteOtherExpense(expenseId: String) {
        val currentMess = _activeMess.value ?: return
        viewModelScope.launch {
            val result = repository.deleteOtherExpense(currentMess.id, expenseId)
            if (result.isSuccess) {
                _uiMessage.value = "অন্যান্য খরচ মুছে ফেলা হয়েছে"
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "মুছতে ব্যর্থ"
            }
        }
    }
}

package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val activeMessId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class JoinCode(
    val id: String = "",
    val code: String = "",
    val messId: String = "",
    val messName: String = "",
    val creatorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Mess(
    val id: String = "",
    val name: String = "",
    val joinCode: String = "",
    val managerId: String = "",
    val adminUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    val memberNames: List<String> = emptyList(),
    val activeMonth: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isAdmin(userId: String): Boolean = (userId == managerId || adminUids.contains(userId))
    val creatorId: String get() = managerId
    fun getMemberName(userId: String): String {
        val idx = memberUids.indexOf(userId)
        return if (idx >= 0 && idx < memberNames.size) memberNames[idx] else "Member"
    }
}

@IgnoreExtraProperties
data class Room(
    val id: String = "",
    val messId: String = "",
    val roomNumber: String = "",
    val type: String = "Double", // Single, Double, Triple, Shared
    val capacity: Int = 2,
    val rent: Double = 0.0,
    val assignedMemberUids: List<String> = emptyList(),
    val assignedMemberNames: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val availableSeats: Int get() = (capacity - assignedMemberUids.size).coerceAtLeast(0)
    val isFull: Boolean get() = availableSeats <= 0
}

@IgnoreExtraProperties
data class Meal(
    val id: String = "",
    val messId: String = "",
    val userId: String = "",
    val userName: String = "",
    val memberUids: List<String> = emptyList(),
    val date: String = "",
    val month: String = "",
    val breakfast: Double = 0.0,
    val lunch: Double = 0.0,
    val dinner: Double = 0.0,
    val totalMeals: Double = 0.0,
    val recordedBy: String = "",
    val recordedByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Deposit(
    val id: String = "",
    val messId: String = "",
    val userId: String = "",
    val userName: String = "",
    val memberUids: List<String> = emptyList(),
    val amount: Double = 0.0,
    val date: String = "",
    val month: String = "",
    val note: String? = null,
    val recordedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BazarExpense(
    val id: String = "",
    val messId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val month: String = "",
    val shopperId: String = "",
    val shopperName: String = "",
    val memberUids: List<String> = emptyList(),
    val recordedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OtherExpense(
    val id: String = "",
    val messId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val month: String = "",
    val memberUids: List<String> = emptyList(),
    val recordedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class MemberSummary(
    val userId: String,
    val userName: String,
    val roomNumber: String = "",
    val roomType: String = "",
    val totalMeals: Double,
    val totalDeposit: Double,
    val mealCost: Double,
    val sharedOtherCost: Double,
    val totalCost: Double,
    val netBalance: Double // Positive = Refundable (পাবেন), Negative = Due (দেবেন)
)

data class MessSummary(
    val totalMeals: Double = 0.0,
    val totalBazar: Double = 0.0,
    val totalOther: Double = 0.0,
    val totalDeposit: Double = 0.0,
    val mealRate: Double = 0.0,
    val cashInHand: Double = 0.0,
    val memberSummaries: List<MemberSummary> = emptyList()
)

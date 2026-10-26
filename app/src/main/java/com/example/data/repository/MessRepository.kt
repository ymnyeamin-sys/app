package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BazarExpense
import com.example.data.model.Deposit
import com.example.data.model.JoinCode
import com.example.data.model.Meal
import com.example.data.model.Mess
import com.example.data.model.OtherExpense
import com.example.data.model.Room
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

private fun createFirestoreInstance(context: Context): FirebaseFirestore {
    val resId = context.applicationContext.resources.getIdentifier(
        "firestore_database_id",
        "string",
        context.applicationContext.packageName
    )
    val dbId = if (resId != 0) {
        try { context.applicationContext.getString(resId) } catch (e: Exception) { null }
    } else null
    return if (!dbId.isNullOrBlank()) {
        FirebaseFirestore.getInstance(dbId)
    } else {
        FirebaseFirestore.getInstance()
    }
}

class MessRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(createFirestoreInstance(context))

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun getCurrentUserName(): String {
        return auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.email?.substringBefore("@")
            ?: "User"
    }

    // --- User Profile ---
    suspend fun getOrCreateUserProfile(): UserProfile {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                snapshot.toObject(UserProfile::class.java) ?: UserProfile(id = uid, userId = uid)
            } else {
                val email = auth.currentUser?.email ?: ""
                val name = getCurrentUserName()
                val payload = mapOf(
                    "userId" to uid,
                    "name" to name,
                    "email" to email,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                UserProfile(id = uid, userId = uid, name = name, email = email)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun updateActiveMess(messId: String?) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "activeMessId" to messId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val email = auth.currentUser?.email ?: ""
                val name = getCurrentUserName()
                val payload = mapOf(
                    "userId" to uid,
                    "name" to name,
                    "email" to email,
                    "activeMessId" to messId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    // --- Messes ---
    fun observeUserMesses(): Flow<List<Mess>> {
        val uid = requireUserId()
        val query = db.collection("messes").whereArrayContains("memberUids", uid)
        return query.snapshots()
            .map { it.toObjects(Mess::class.java) }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "messes")
                }
                throw error
            }
    }

    fun observeMess(messId: String): Flow<Mess?> {
        val docRef = db.collection("messes").document(messId)
        return docRef.snapshots()
            .map { it.toObject(Mess::class.java) }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    suspend fun createMess(name: String, customJoinCode: String? = null): Result<Mess> {
        val uid = requireUserId()
        val userName = getCurrentUserName()
        val messId = "mess_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val code = (customJoinCode?.trim()?.uppercase()?.takeIf { it.length in 4..12 }
            ?: "MESS${(1000..9999).random()}")

        val codeRef = db.collection("join_codes").document(code)
        val messRef = db.collection("messes").document(messId)

        return try {
            val codeSnap = codeRef.get().await()
            if (codeSnap.exists()) {
                return Result.failure(IllegalArgumentException("Join code '$code' is already in use. Please try another."))
            }

            val codePayload = mapOf(
                "code" to code,
                "messId" to messId,
                "messName" to name,
                "creatorId" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
            codeRef.set(codePayload).await()

            val currentMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date())
            val messPayload = mapOf(
                "id" to messId,
                "name" to name,
                "joinCode" to code,
                "managerId" to uid,
                "adminUids" to listOf(uid),
                "memberUids" to listOf(uid),
                "memberNames" to listOf(userName),
                "activeMonth" to currentMonth,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            messRef.set(messPayload).await()

            updateActiveMess(messId)

            Result.success(
                Mess(
                    id = messId,
                    name = name,
                    joinCode = code,
                    managerId = uid,
                    adminUids = listOf(uid),
                    memberUids = listOf(uid),
                    memberNames = listOf(userName),
                    activeMonth = currentMonth
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, messRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinMessByCode(code: String): Result<Mess> {
        val uid = requireUserId()
        val userName = getCurrentUserName()
        val normalizedCode = code.trim().uppercase()
        val codeRef = db.collection("join_codes").document(normalizedCode)

        return try {
            val codeSnap = codeRef.get().await()
            if (!codeSnap.exists()) {
                return Result.failure(IllegalArgumentException("Invalid code. No mess found for '$normalizedCode'."))
            }
            val joinCode = codeSnap.toObject(JoinCode::class.java)
                ?: return Result.failure(IllegalStateException("Could not parse mess join code."))

            val messRef = db.collection("messes").document(joinCode.messId)
            val messSnap = messRef.get().await()
            if (!messSnap.exists()) {
                return Result.failure(IllegalStateException("Mess not found."))
            }
            val mess = messSnap.toObject(Mess::class.java)
                ?: return Result.failure(IllegalStateException("Could not parse mess data."))

            if (!mess.memberUids.contains(uid)) {
                val updatedUids = mess.memberUids + uid
                val updatedNames = mess.memberNames + userName
                messRef.update(
                    mapOf(
                        "memberUids" to updatedUids,
                        "memberNames" to updatedNames,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            updateActiveMess(mess.id)
            Result.success(mess)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, codeRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleAdminRole(messId: String, targetUid: String): Result<Unit> {
        val messRef = db.collection("messes").document(messId)
        return try {
            val snap = messRef.get().await()
            val mess = snap.toObject(Mess::class.java) ?: return Result.failure(IllegalStateException("Mess not found"))
            val currentAdmins = mess.adminUids.toMutableList()
            if (currentAdmins.contains(targetUid)) {
                if (targetUid != mess.managerId) {
                    currentAdmins.remove(targetUid)
                }
            } else {
                currentAdmins.add(targetUid)
            }
            messRef.update(
                mapOf(
                    "adminUids" to currentAdmins,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, messRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeMemberFromMess(messId: String, targetUid: String): Result<Unit> {
        val messRef = db.collection("messes").document(messId)
        return try {
            val snap = messRef.get().await()
            val mess = snap.toObject(Mess::class.java) ?: return Result.failure(IllegalStateException("Mess not found"))
            val idx = mess.memberUids.indexOf(targetUid)
            if (idx >= 0) {
                val updatedUids = mess.memberUids.toMutableList().apply { removeAt(idx) }
                val updatedNames = mess.memberNames.toMutableList().apply { removeAt(idx) }
                val updatedAdmins = mess.adminUids.filter { it != targetUid }
                messRef.update(
                    mapOf(
                        "memberUids" to updatedUids,
                        "memberNames" to updatedNames,
                        "adminUids" to updatedAdmins,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                // Remove member from any room they occupy
                val roomsSnap = db.collection("messes").document(messId).collection("rooms").get().await()
                for (doc in roomsSnap.documents) {
                    val room = doc.toObject(Room::class.java) ?: continue
                    if (room.assignedMemberUids.contains(targetUid)) {
                        val mIdx = room.assignedMemberUids.indexOf(targetUid)
                        val rUids = room.assignedMemberUids.toMutableList().apply { removeAt(mIdx) }
                        val rNames = room.assignedMemberNames.toMutableList().apply { removeAt(mIdx) }
                        doc.reference.update(
                            mapOf(
                                "assignedMemberUids" to rUids,
                                "assignedMemberNames" to rNames,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, messRef.path)
            Result.failure(e)
        }
    }

    // --- Rooms (Admin room management & member room occupation) ---
    fun observeRooms(messId: String): Flow<List<Room>> {
        val uid = requireUserId()
        val query = db.collection("messes").document(messId).collection("rooms")
            .whereArrayContains("memberUids", uid)
        return query.snapshots()
            .map { snap ->
                snap.toObjects(Room::class.java).sortedBy { it.roomNumber }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "messes/$messId/rooms")
                }
                throw error
            }
    }

    suspend fun createOrUpdateRoom(
        messId: String,
        memberUids: List<String>,
        roomNumber: String,
        type: String, // Single, Double, Triple, Shared
        capacity: Int,
        rent: Double,
        existingRoomId: String? = null
    ): Result<Unit> {
        val roomId = existingRoomId?.takeIf { it.isNotBlank() }
            ?: "room_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("messes").document(messId).collection("rooms").document(roomId)

        return try {
            val payload = mutableMapOf<String, Any>(
                "id" to roomId,
                "messId" to messId,
                "roomNumber" to roomNumber.trim(),
                "type" to type,
                "capacity" to capacity,
                "rent" to rent,
                "memberUids" to memberUids,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingRoomId.isNullOrBlank()) {
                payload["assignedMemberUids"] = emptyList<String>()
                payload["assignedMemberNames"] = emptyList<String>()
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteRoom(messId: String, roomId: String): Result<Unit> {
        val docRef = db.collection("messes").document(messId).collection("rooms").document(roomId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun assignMemberToRoom(
        messId: String,
        targetRoomId: String,
        memberUid: String,
        memberName: String
    ): Result<Unit> {
        return try {
            // First remove member from any other room in the mess
            val roomsQuery = db.collection("messes").document(messId).collection("rooms").get().await()
            for (doc in roomsQuery.documents) {
                val room = doc.toObject(Room::class.java) ?: continue
                if (room.assignedMemberUids.contains(memberUid) && room.id != targetRoomId) {
                    val idx = room.assignedMemberUids.indexOf(memberUid)
                    val updatedUids = room.assignedMemberUids.toMutableList().apply { removeAt(idx) }
                    val updatedNames = room.assignedMemberNames.toMutableList().apply { removeAt(idx) }
                    doc.reference.update(
                        mapOf(
                            "assignedMemberUids" to updatedUids,
                            "assignedMemberNames" to updatedNames,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            // Add member to target room
            val targetRef = db.collection("messes").document(messId).collection("rooms").document(targetRoomId)
            val targetSnap = targetRef.get().await()
            val targetRoom = targetSnap.toObject(Room::class.java)
                ?: return Result.failure(IllegalStateException("Target room not found"))

            if (targetRoom.assignedMemberUids.contains(memberUid)) {
                return Result.success(Unit) // Already in this room
            }
            if (targetRoom.isFull) {
                return Result.failure(IllegalStateException("এই রুমে কোনো সিট খালি নেই! (Room is full)"))
            }

            val updatedUids = targetRoom.assignedMemberUids + memberUid
            val updatedNames = targetRoom.assignedMemberNames + memberName
            targetRef.update(
                mapOf(
                    "assignedMemberUids" to updatedUids,
                    "assignedMemberNames" to updatedNames,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "messes/$messId/rooms/$targetRoomId")
            Result.failure(e)
        }
    }

    suspend fun leaveRoom(messId: String, roomId: String, memberUid: String): Result<Unit> {
        val docRef = db.collection("messes").document(messId).collection("rooms").document(roomId)
        return try {
            val snap = docRef.get().await()
            val room = snap.toObject(Room::class.java)
                ?: return Result.failure(IllegalStateException("Room not found"))
            if (room.assignedMemberUids.contains(memberUid)) {
                val idx = room.assignedMemberUids.indexOf(memberUid)
                val updatedUids = room.assignedMemberUids.toMutableList().apply { removeAt(idx) }
                val updatedNames = room.assignedMemberNames.toMutableList().apply { removeAt(idx) }
                docRef.update(
                    mapOf(
                        "assignedMemberUids" to updatedUids,
                        "assignedMemberNames" to updatedNames,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Meals ---
    fun observeMeals(messId: String, month: String): Flow<List<Meal>> {
        val uid = requireUserId()
        val query = db.collection("messes").document(messId).collection("meals")
            .whereArrayContains("memberUids", uid)
        return query.snapshots()
            .map { snap ->
                snap.toObjects(Meal::class.java)
                    .filter { it.month == month }
                    .sortedByDescending { it.date }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "messes/$messId/meals")
                }
                throw error
            }
    }

    suspend fun recordMeal(
        messId: String,
        memberUids: List<String>,
        targetUserId: String,
        targetUserName: String,
        date: String,
        breakfast: Double,
        lunch: Double,
        dinner: Double,
        existingMealId: String? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val recorderName = getCurrentUserName()
        val month = date.take(7) // YYYY-MM
        val totalMeals = breakfast + lunch + dinner
        val mealId = existingMealId?.takeIf { it.isNotBlank() }
            ?: "meal_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("messes").document(messId).collection("meals").document(mealId)

        return try {
            val payload = mutableMapOf<String, Any>(
                "id" to mealId,
                "messId" to messId,
                "userId" to targetUserId,
                "userName" to targetUserName,
                "memberUids" to memberUids,
                "date" to date,
                "month" to month,
                "breakfast" to breakfast,
                "lunch" to lunch,
                "dinner" to dinner,
                "totalMeals" to totalMeals,
                "recordedBy" to uid,
                "recordedByName" to recorderName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (existingMealId.isNullOrBlank()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMeal(messId: String, mealId: String): Result<Unit> {
        val docRef = db.collection("messes").document(messId).collection("meals").document(mealId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Deposits ---
    fun observeDeposits(messId: String, month: String): Flow<List<Deposit>> {
        val uid = requireUserId()
        val query = db.collection("messes").document(messId).collection("deposits")
            .whereArrayContains("memberUids", uid)
        return query.snapshots()
            .map { snap ->
                snap.toObjects(Deposit::class.java)
                    .filter { it.month == month }
                    .sortedByDescending { it.date }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "messes/$messId/deposits")
                }
                throw error
            }
    }

    suspend fun recordDeposit(
        messId: String,
        memberUids: List<String>,
        targetUserId: String,
        targetUserName: String,
        amount: Double,
        date: String,
        note: String?
    ): Result<Unit> {
        val uid = requireUserId()
        val month = date.take(7)
        val depositId = "dep_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("messes").document(messId).collection("deposits").document(depositId)

        return try {
            val payload = mutableMapOf<String, Any>(
                "id" to depositId,
                "messId" to messId,
                "userId" to targetUserId,
                "userName" to targetUserName,
                "memberUids" to memberUids,
                "amount" to amount,
                "date" to date,
                "month" to month,
                "recordedBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!note.isNullOrBlank()) {
                payload["note"] = note.trim()
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteDeposit(messId: String, depositId: String): Result<Unit> {
        val docRef = db.collection("messes").document(messId).collection("deposits").document(depositId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Bazar Expenses ---
    fun observeBazarExpenses(messId: String, month: String): Flow<List<BazarExpense>> {
        val uid = requireUserId()
        val query = db.collection("messes").document(messId).collection("bazar_expenses")
            .whereArrayContains("memberUids", uid)
        return query.snapshots()
            .map { snap ->
                snap.toObjects(BazarExpense::class.java)
                    .filter { it.month == month }
                    .sortedByDescending { it.date }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "messes/$messId/bazar_expenses")
                }
                throw error
            }
    }

    suspend fun recordBazarExpense(
        messId: String,
        memberUids: List<String>,
        title: String,
        amount: Double,
        date: String,
        shopperId: String,
        shopperName: String
    ): Result<Unit> {
        val uid = requireUserId()
        val month = date.take(7)
        val expenseId = "baz_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("messes").document(messId).collection("bazar_expenses").document(expenseId)

        return try {
            val payload = mapOf(
                "id" to expenseId,
                "messId" to messId,
                "title" to title.trim(),
                "amount" to amount,
                "date" to date,
                "month" to month,
                "shopperId" to shopperId,
                "shopperName" to shopperName,
                "memberUids" to memberUids,
                "recordedBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteBazarExpense(messId: String, expenseId: String): Result<Unit> {
        val docRef = db.collection("messes").document(messId).collection("bazar_expenses").document(expenseId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Other Expenses ---
    fun observeOtherExpenses(messId: String, month: String): Flow<List<OtherExpense>> {
        val uid = requireUserId()
        val query = db.collection("messes").document(messId).collection("other_expenses")
            .whereArrayContains("memberUids", uid)
        return query.snapshots()
            .map { snap ->
                snap.toObjects(OtherExpense::class.java)
                    .filter { it.month == month }
                    .sortedByDescending { it.date }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "messes/$messId/other_expenses")
                }
                throw error
            }
    }

    suspend fun recordOtherExpense(
        messId: String,
        memberUids: List<String>,
        title: String,
        amount: Double,
        date: String
    ): Result<Unit> {
        val uid = requireUserId()
        val month = date.take(7)
        val expenseId = "oth_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("messes").document(messId).collection("other_expenses").document(expenseId)

        return try {
            val payload = mapOf(
                "id" to expenseId,
                "messId" to messId,
                "title" to title.trim(),
                "amount" to amount,
                "date" to date,
                "month" to month,
                "memberUids" to memberUids,
                "recordedBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteOtherExpense(messId: String, expenseId: String): Result<Unit> {
        val docRef = db.collection("messes").document(messId).collection("other_expenses").document(expenseId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}

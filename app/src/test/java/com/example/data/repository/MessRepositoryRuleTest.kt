package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MessRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createMess_authenticatedUser_success() = runBlocking<Unit> {
        signInTestUser(ALICE_EMAIL)
        val repository = MessRepository(firestore)

        val messName = "Padma Mess"
        val code = "PADMA${(100..999).random()}"
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createMess(messName, code)
        }

        assertTrue(result.exceptionOrNull()?.message ?: "failed", result.isSuccess)
        val mess = result.getOrThrow()
        assertEquals(messName, mess.name)
        assertEquals(code, mess.joinCode)
    }

    @Test
    fun recordMealAndObserve_authenticatedMember_emitsMeal() = runBlocking<Unit> {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = MessRepository(firestore)

        val messResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createMess("Meghna Mess", "MEGH${(100..999).random()}")
        }
        val mess = messResult.getOrThrow()

        val recordResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordMeal(
                messId = mess.id,
                memberUids = mess.memberUids,
                targetUserId = aliceUid,
                targetUserName = "Alice",
                date = "2026-10-24",
                breakfast = 1.0,
                lunch = 1.0,
                dinner = 1.0
            )
        }
        assertTrue(recordResult.exceptionOrNull()?.message ?: "failed", recordResult.isSuccess)

        val meals = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMeals(mess.id, "2026-10").first()
        }
        assertTrue(meals.isNotEmpty())
        assertEquals(3.0, meals.first().totalMeals, 0.001)
    }

    @Test
    fun observeMeals_unauthenticatedUser_fails() = runBlocking<Unit> {
        auth.signOut()
        val repository = MessRepository(firestore)

        val exception = assertThrows(Exception::class.java) {
            runBlocking<Unit> {
                withTimeout(FLOW_TIMEOUT_MS) {
                    repository.observeMeals("mess_any", "2026-10").first()
                }
            }
        }
        assertNotNull(exception)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 6000L
    }
}

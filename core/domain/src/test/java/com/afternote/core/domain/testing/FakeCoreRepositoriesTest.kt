package com.afternote.core.domain.testing

import com.afternote.core.model.TokenBundle
import com.afternote.core.model.delivery.ConditionState
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.User
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeCoreRepositoriesTest {
    @Test
    fun `updateTokens와 rotateToken 성공은 토큰과 로그인 상태를 함께 갱신한다`() {
        val repository =
            FakeAuthRepository(
                loggedIn = false,
                rotatedTokens = TokenBundle("rotated-access", "rotated-refresh"),
            )

        val updateResult = runBlocking { repository.updateTokens("updated-access", "updated-refresh") }

        assertTrue(updateResult.isSuccess)
        assertEquals("updated-access", repository.accessToken)
        assertEquals("updated-refresh", repository.refreshToken)
        assertTrue(repository.loggedIn)

        repository.loggedIn = false
        val rotateResult = runBlocking { repository.rotateToken() }

        assertTrue(rotateResult.isSuccess)
        assertEquals("rotated-access", repository.accessToken)
        assertEquals("rotated-refresh", repository.refreshToken)
        assertTrue(repository.loggedIn)
    }

    @Test
    fun `rotateToken 기본 경로는 refresh가 없거나 새 access가 비어 있으면 실패한다`() {
        val missingRefresh = FakeAuthRepository(loggedIn = false)

        val missingRefreshResult = runBlocking { missingRefresh.rotateToken() }

        assertTrue(missingRefreshResult.isFailure)
        assertNull(missingRefresh.accessToken)
        assertNull(missingRefresh.refreshToken)
        assertFalse(missingRefresh.loggedIn)

        val emptyAccess =
            FakeAuthRepository(
                loggedIn = false,
                refreshToken = "current-refresh",
                rotatedTokens = TokenBundle("", "rotated-refresh"),
            )

        val emptyAccessResult = runBlocking { emptyAccess.rotateToken() }

        assertTrue(emptyAccessResult.isFailure)
        assertNull(emptyAccess.accessToken)
        assertEquals("current-refresh", emptyAccess.refreshToken)
        assertFalse(emptyAccess.loggedIn)
    }

    @Test
    fun `Auth onX는 기록 외 기본 메모리 동작을 완전히 대체한다`() {
        val updateRepository =
            FakeAuthRepository(
                loggedIn = false,
                accessToken = "old-access",
                refreshToken = "old-refresh",
                onUpdateTokens = { _, _ -> Result.success(Unit) },
            )

        val updateResult = runBlocking { updateRepository.updateTokens("new-access", "new-refresh") }

        assertTrue(updateResult.isSuccess)
        assertEquals(listOf("new-access" to "new-refresh"), updateRepository.updatedTokens)
        assertEquals("old-access", updateRepository.accessToken)
        assertEquals("old-refresh", updateRepository.refreshToken)
        assertFalse(updateRepository.loggedIn)

        val rotateRepository =
            FakeAuthRepository(
                loggedIn = false,
                accessToken = "old-access",
                refreshToken = "old-refresh",
                onRotateToken = { Result.success(TokenBundle("new-access", "new-refresh")) },
            )

        val rotateResult = runBlocking { rotateRepository.rotateToken() }

        assertTrue(rotateResult.isSuccess)
        assertEquals(1, rotateRepository.rotateTokenCalls)
        assertEquals("old-access", rotateRepository.accessToken)
        assertEquals("old-refresh", rotateRepository.refreshToken)
        assertFalse(rotateRepository.loggedIn)
    }

    @Test
    fun `기본 receiver detail은 합성되고 수정 뒤 재조회에도 모든 필드가 남는다`() {
        val receiver = Receiver(7L, "수신자", "가족", "auth-7")
        val initialReceivers = mutableListOf(receiver)
        val synthesizedRepository = FakeUserRepository(receivers = initialReceivers)
        initialReceivers.clear()

        val synthesized = runBlocking { synthesizedRepository.getReceiverDetail(receiver.receiverId) }

        assertEquals(receiver.receiverId, synthesized.receiverId)
        assertEquals(receiver.name, synthesized.name)
        assertEquals(receiver.relation, synthesized.relation)
        assertEquals(receiver.authCode, synthesized.authCode)
        assertNull(synthesized.phone)
        assertNull(synthesized.email)
        assertNull(synthesized.message)

        val updatedRepository = FakeUserRepository(receivers = listOf(receiver))
        runBlocking {
            updatedRepository.updateReceiver(receiver.receiverId, "새 이름", "01012345678", "친구", "new@test.local")
            updatedRepository.updateReceiverMessage(receiver.receiverId, "남길 메시지")
        }

        val updated = runBlocking { updatedRepository.getReceiverDetail(receiver.receiverId) }

        assertEquals("새 이름", updated.name)
        assertEquals("친구", updated.relation)
        assertEquals("01012345678", updated.phone)
        assertEquals("new@test.local", updated.email)
        assertEquals("남길 메시지", updated.message)
        assertEquals("auth-7", updated.authCode)
    }

    @Test
    fun `delivery conditions는 입력 list를 기록과 상태에 snapshot한다`() {
        val condition =
            DeliveryConditionItem(
                contentType = DeliveryContentType.TIME_LETTER,
                conditionType = DeliveryConditionType.INACTIVITY,
                inactivityPeriod = InactivityPeriod.THREE_MONTHS,
                state = ConditionState.ACTIVE,
                fulfilled = false,
                gracePeriodStartedAt = null,
                fulfilledAt = null,
            )
        val input = mutableListOf(condition)
        val repository = FakeUserRepository()

        val returned = runBlocking { repository.updateReceiverDeliveryConditions(7L, input) }
        input.clear()
        val reloaded = runBlocking { repository.getReceiverDeliveryConditions(7L) }

        assertEquals(listOf(condition), repository.deliveryUpdateCalls.single().conditions)
        assertEquals(listOf(condition), returned.conditions)
        assertEquals(listOf(condition), reloaded.conditions)
        assertEquals(listOf(condition), repository.deliveryConditions.getValue(7L).conditions)
    }

    /**
     * 합본 fake 가 책임별 fake 에 위임해도(#1282) 생성 이후에 갈아끼운 `onX` 와 상태 쓰기가
     * 그대로 먹혀야 한다 — 소비자 다수가 `strict()` 로 만든 뒤 필요한 멤버만 열어 쓴다.
     */
    @Test
    fun `합본 fake 의 생성 후 onX 교체와 상태 쓰기는 위임 대상에 그대로 닿는다`() {
        val repository = FakeUserRepository.strict()

        repository.onGetReceivers = null
        repository.onGetMyProfile = null
        repository.receiverState.value = listOf(Receiver(9L, "나중 수신자", "가족", "auth-9"))
        repository.profile = User("나중 사용자", "later@afternote.local", null, null)

        assertEquals(listOf(Receiver(9L, "나중 수신자", "가족", "auth-9")), runBlocking { repository.getReceivers() })
        assertEquals("나중 사용자", runBlocking { repository.getMyProfile() }.name)
        assertEquals(1, repository.getReceiversCalls)
        assertEquals(1, repository.getProfileCalls)
    }

    /** 좁은 fake 는 합본을 거치지 않고 곧장 쓸 수 있어야 한다 (#1282). */
    @Test
    fun `좁은 fake 는 자기 책임 상태만 갖고 단독으로 동작한다`() {
        val receiverRepository = FakeUserReceiverRepository(receivers = listOf(Receiver(3L, "수신자", "가족", "auth-3")))
        val profileRepository = FakeMyProfileRepository()

        val receivers = runBlocking { receiverRepository.getReceivers() }
        val updated = runBlocking { profileRepository.updateMyProfile("바뀐 이름", null, null) }

        assertEquals(listOf(Receiver(3L, "수신자", "가족", "auth-3")), receivers)
        assertEquals(1, receiverRepository.getReceiversCalls)
        assertEquals("바뀐 이름", updated.name)
        assertEquals(
            listOf(FakeUserRepository.ProfileUpdateCall("바뀐 이름", null, null)),
            profileRepository.profileUpdateCalls.toList(),
        )
    }
}

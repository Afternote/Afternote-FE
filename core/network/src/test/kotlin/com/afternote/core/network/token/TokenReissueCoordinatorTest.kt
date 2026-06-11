package com.afternote.core.network.token

import com.afternote.core.model.TokenBundle
import com.afternote.core.network.FakeAuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [TokenReissueCoordinator] 단일 비행 계약 회귀 가드 (#408).
 * 핵심 계약 — "관찰 토큰 vs 저장 토큰" 재확인으로 늦게 진입한 경로는 회전을 생략하고,
 * 회전 시도(성공/실패 무관)는 항상 deadline 을 폐기한다.
 */
class TokenReissueCoordinatorTest {
    private var nowElapsedMillis = 0L
    private val tracker = AccessTokenExpiryTracker { nowElapsedMillis }

    @Test
    fun `저장 토큰이 관찰 토큰과 다름 - 회전 생략하고 갱신된 토큰 반환`() {
        val repository = FakeAuthRepository(accessToken = "refreshed-by-other-path")
        val coordinator = TokenReissueCoordinator({ repository }, tracker)

        val outcome = coordinator.reissue(observedAccessToken = "old-token")

        assertEquals(0, repository.rotateCallCount)
        assertEquals(
            TokenReissueCoordinator.Outcome.AlreadyRefreshed("refreshed-by-other-path"),
            outcome,
        )
    }

    @Test
    fun `저장 토큰이 관찰 토큰과 동일 - 회전 수행 후 새 토큰 반환, deadline 폐기`() {
        tracker.record(expiresInSeconds = 30)
        val repository =
            FakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = {
                    accessToken = "fresh-token"
                    Result.success(TokenBundle(accessToken = "fresh-token", refreshToken = "r"))
                },
            )
        val coordinator = TokenReissueCoordinator({ repository }, tracker)

        val outcome = coordinator.reissue(observedAccessToken = "old-token")

        assertEquals(1, repository.rotateCallCount)
        assertEquals(TokenReissueCoordinator.Outcome.Rotated("fresh-token"), outcome)
        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `회전 실패 - Failed 반환, deadline 폐기 (재시도 폭주 방지)`() {
        tracker.record(expiresInSeconds = 30)
        val repository =
            FakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = { Result.failure(IllegalStateException("리프레시 만료")) },
            )
        val coordinator = TokenReissueCoordinator({ repository }, tracker)

        val outcome = coordinator.reissue(observedAccessToken = "old-token")

        assertTrue(outcome is TokenReissueCoordinator.Outcome.Failed)
        assertFalse(tracker.isExpiringSoon())
        // 후처리는 호출자 몫 — 코디네이터가 임의로 세션을 지우지 않는다 (Fake 의 clearSession error 가드)
        assertEquals(0, repository.clearSessionCallCount)
    }

    @Test
    fun `저장 토큰이 빈 값 - AlreadyRefreshed 가 아니라 회전 시도로 진행`() {
        val repository =
            FakeAuthRepository(
                accessToken = null,
                onRotateToken = { Result.failure(IllegalStateException("리프레시 토큰이 존재하지 않습니다.")) },
            )
        val coordinator = TokenReissueCoordinator({ repository }, tracker)

        val outcome = coordinator.reissue(observedAccessToken = "old-token")

        assertEquals(1, repository.rotateCallCount)
        assertTrue(outcome is TokenReissueCoordinator.Outcome.Failed)
    }
}

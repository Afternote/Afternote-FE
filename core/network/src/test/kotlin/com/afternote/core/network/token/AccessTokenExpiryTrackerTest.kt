package com.afternote.core.network.token

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 선제 reissue 판단 [AccessTokenExpiryTracker] 의 deadline 산식·경계 회귀 가드 (#408).
 * 시계는 가짜 주입 — 임계값 60초 경계에서 "정확히 60초 남음"은 아직 선제 갱신 대상이 아니다.
 */
class AccessTokenExpiryTrackerTest {
    private var nowElapsedMillis = 0L
    private val tracker = AccessTokenExpiryTracker { nowElapsedMillis }

    @Test
    fun `기록된 deadline 없음 - 선제 갱신 대상 아님`() {
        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `잔여 수명이 임계값 초과 - 선제 갱신 대상 아님`() {
        tracker.record(expiresInSeconds = 3599)

        nowElapsedMillis = 3_599_000L - 60_001L

        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `잔여 수명이 정확히 임계값 - 미만이 아니므로 대상 아님`() {
        tracker.record(expiresInSeconds = 3599)

        nowElapsedMillis = 3_599_000L - 60_000L

        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `잔여 수명이 임계값 미만 - 선제 갱신 대상`() {
        tracker.record(expiresInSeconds = 3599)

        nowElapsedMillis = 3_599_000L - 59_999L

        assertTrue(tracker.isExpiringSoon())
    }

    @Test
    fun `deadline 이 이미 지난 경우 - 선제 갱신 대상`() {
        tracker.record(expiresInSeconds = 10)

        nowElapsedMillis = 100_000L

        assertTrue(tracker.isExpiringSoon())
    }

    @Test
    fun `clear 후 - 기록 없음 상태로 복귀`() {
        tracker.record(expiresInSeconds = 10)
        nowElapsedMillis = 100_000L

        tracker.clear()

        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `record 재호출 - 최신 수신 기준으로 deadline 연장`() {
        tracker.record(expiresInSeconds = 10)
        nowElapsedMillis = 9_000L

        tracker.record(expiresInSeconds = 3599)

        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `expiresIn 0 - 즉시 임박 판정 (무시 가드 아님)`() {
        // 0/음수는 서버가 "이미 만료"를 알린 것 — 다음 요청에서 1회 회전 후 clear 로 수렴해야 한다
        tracker.record(expiresInSeconds = 0)

        assertTrue(tracker.isExpiringSoon())
    }

    @Test
    fun `expiresIn 음수 - 즉시 임박 판정`() {
        tracker.record(expiresInSeconds = -1)

        assertTrue(tracker.isExpiringSoon())
    }
}

package com.afternote.core.network.token

import android.os.SystemClock
import com.afternote.core.network.token.AccessTokenExpiryTracker.Companion.PREEMPTIVE_REISSUE_THRESHOLD_MILLIS
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 발급 응답의 `expiresIn`으로 액세스 토큰 만료 시점을 프로세스 메모리에 추적한다 (#408, #410).
 *
 * `AuthInterceptor`는 요청 전에 [isExpiringSoon]으로 선제 재발급 여부를 판단한다.
 * 만료 시점이 없으면 선제 재발급을 건너뛰고, 실제 만료는 401 대응 경로가 처리한다.
 *
 * 벽시계 변경의 영향을 받지 않고 deep sleep을 포함하는 [SystemClock.elapsedRealtime]을 사용한다.
 * 이 시계는 재부팅 시 초기화되므로 만료 시점은 영속화하지 않는다.
 */
@Singleton
class AccessTokenExpiryTracker internal constructor(
    private val elapsedRealtimeMillis: () -> Long,
) {
    @Inject
    constructor() : this(SystemClock::elapsedRealtime)

    @Volatile
    private var deadlineElapsedMillis: Long? = null

    /**
     * 현재 시점부터 [expiresInSeconds]초 뒤를 만료 시점으로 기록한다.
     * 0 이하도 그대로 기록되어 즉시 만료 임박으로 판정된다.
     */
    fun record(expiresInSeconds: Long) {
        deadlineElapsedMillis = elapsedRealtimeMillis() + expiresInSeconds * MILLIS_PER_SECOND
    }

    /**
     * 잔여 수명이 [PREEMPTIVE_REISSUE_THRESHOLD_MILLIS] 미만이면 `true`.
     * 기록이 없으면 `false`.
     */
    fun isExpiringSoon(): Boolean {
        val deadline = deadlineElapsedMillis ?: return false
        return deadline - elapsedRealtimeMillis() < PREEMPTIVE_REISSUE_THRESHOLD_MILLIS
    }

    /** 다음 [record]까지 선제 재발급을 건너뛰도록 만료 시점을 비운다. */
    fun clear() {
        deadlineElapsedMillis = null
    }

    companion object {
        private const val MILLIS_PER_SECOND = 1_000L

        /** 잔여 수명이 이 값 미만이면 다음 요청 전 선제 재발급한다. */
        private const val PREEMPTIVE_REISSUE_THRESHOLD_MILLIS = 60_000L
    }
}

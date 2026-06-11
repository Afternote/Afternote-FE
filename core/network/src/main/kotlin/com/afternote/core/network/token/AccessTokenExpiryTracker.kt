package com.afternote.core.network.token

import android.os.SystemClock
import com.afternote.core.network.token.AccessTokenExpiryTracker.Companion.PREEMPTIVE_REISSUE_THRESHOLD_MILLIS
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 액세스 토큰 만료 시점(deadline)을 메모리에만 보관하는 추적기 (#408).
 *
 * 서버는 `@IncludeAccessTokenExpiresIn` 이 붙은 목록 endpoint 응답 봉투에만 `expiresIn`
 * (잔여 수명 초)을 내려준다 — `AuthInterceptor` 가 응답에서 이를 [record] 로 기록하고,
 * 다음 요청 전 [isExpiringSoon] 으로 선제 reissue 여부를 판단한다.
 *
 * 의도적으로 영속화(DataStore)하지 않는다 — deadline 은 "수신 시각 + N초" 기반 휘발 정보라
 * 앱 재시작 후엔 어차피 stale 이고, 모르는 상태(null)면 선제 갱신만 건너뛸 뿐
 * 만료 토큰은 기존 401 사후 대응(`TokenAuthenticator`)이 안전망으로 처리한다.
 *
 * 시계는 [SystemClock.elapsedRealtime] 기준 — SystemClock 공식 문서가 범용 interval 측정의
 * 권장 기준("the recommend basis for general purpose interval timing")으로 명시한 단조 시계로,
 * deep sleep 중에도 흐른다. 벽시계(`System.currentTimeMillis`)는 사용자 시간 변경에 deadline 이
 * 왜곡되어 같은 문서가 interval 측정에 쓰지 말라고 지시한다. 주 생성자는 유닛 테스트의
 * 가짜 시계 주입용(internal)이고, 프로덕션은 `@Inject` 부 생성자로 실제 시계를 쓴다 —
 * 이 `@Inject` 생성자 자체가 Hilt 바인딩이라 모듈(@Provides/@Binds) 등록 없이 주입된다
 * (모듈은 인터페이스·서드파티처럼 생성자 주입이 불가능한 타입 전용).
 */
@Singleton
class AccessTokenExpiryTracker internal constructor(
    // "지금 시각(부팅 후 경과 ms)" 공급자 — record/isExpiringSoon 이 시각이 필요할 때마다 호출.
    // 프로덕션 = SystemClock::elapsedRealtime, 테스트 = 가짜 시계 람다.
    private val elapsedRealtimeMillis: () -> Long,
) {
    @Inject
    constructor() : this(SystemClock::elapsedRealtime)

    @Volatile
    private var deadlineElapsedMillis: Long? = null

    /** 응답 봉투의 `expiresIn`(초) 수신 시점 기준으로 deadline 을 갱신한다. */
    fun record(expiresInSeconds: Long) {
        deadlineElapsedMillis = elapsedRealtimeMillis() + expiresInSeconds * MILLIS_PER_SECOND
    }

    /**
     * 잔여 수명이 [PREEMPTIVE_REISSUE_THRESHOLD_MILLIS] 미만이면 true.
     * 기록된 deadline 이 없으면(null) false — 선제 갱신은 기록이 있을 때만 동작한다.
     */
    fun isExpiringSoon(): Boolean {
        val deadline = deadlineElapsedMillis ?: return false
        return deadline - elapsedRealtimeMillis() < PREEMPTIVE_REISSUE_THRESHOLD_MILLIS
    }

    /**
     * 기록된 deadline 을 비운다(null).
     * 토큰이 교체(reissue)·소멸(로그아웃)되면 기존 deadline 은 이전 토큰 기준 stale 이므로,
     * 다음 목록 응답이 새 deadline 을 기록할 때까지 선제 갱신을 쉬게 한다 — reissue 실패 시에도
     * clear 해 만료 deadline 이 남아 매 요청마다 reissue 를 재시도(폭주)하는 것을 막는다.
     */
    fun clear() {
        deadlineElapsedMillis = null
    }

    companion object {
        private const val MILLIS_PER_SECOND = 1_000L

        /** 잔여 수명이 이 값 미만이면 다음 요청 전 선제 reissue (#408 — 60초). */
        private const val PREEMPTIVE_REISSUE_THRESHOLD_MILLIS = 60_000L
    }
}

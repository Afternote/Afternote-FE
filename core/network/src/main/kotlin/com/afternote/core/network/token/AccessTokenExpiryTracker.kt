package com.afternote.core.network.token

import android.os.SystemClock
import com.afternote.core.network.token.AccessTokenExpiryTracker.Companion.PREEMPTIVE_REISSUE_THRESHOLD_MILLIS
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 액세스 토큰 만료 시점(deadline)을 메모리에만 보관하는 추적기 (#408).
 *
 * 서버는 발급 응답(로그인·reissue)의 `data` 안에 `expiresIn`(잔여 수명 초)을 내려준다(#410) —
 * 토큰을 발급/회전하는 곳(`AuthRepositoryImpl`·`TokenReissuer`)이 이를 [record] 로 기록하고,
 * 다음 요청 전 `AuthInterceptor` 가 [isExpiringSoon] 으로 선제 reissue 여부를 판단한다.
 * 발급/회전을 한 번도 거치지 않은 상태(앱 첫 진입 등)면 기록이 없어 선제 갱신이 조용히 쉬고,
 * 만료는 401 안전망(`TokenAuthenticator`)이 받는다.
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

    /**
     * 발급 응답의 `expiresIn`(초) 수신 시점 기준으로 deadline 을 갱신한다 —
     * `deadline = 수신 시각 + expiresIn`. 이후 [isExpiringSoon] 이 이 deadline 과 현재 시각을
     * 비교해 "곧 만료"(선제 reissue 트리거) 여부를 판정한다. 토큰 클레임을 매번 디코드하지 않고
     * 발급 때 한 번 계산해 둔 시각만 쓰는 게 핵심 — 그래서 expiresIn 미수신 시 판정 근거가 없다.
     */
    fun record(expiresInSeconds: Long) {
        deadlineElapsedMillis = elapsedRealtimeMillis() + expiresInSeconds * MILLIS_PER_SECOND
    }

    /**
     * 잔여 수명이 [PREEMPTIVE_REISSUE_THRESHOLD_MILLIS] 미만이면 true.
     * 기록된 deadline 이 없으면(null) false — 선제 갱신은 기록이 있을 때만 동작한다.
     *
     * 주의: null 은 "곧 만료"가 아니라 **"만료 시각 모름"** 이다. 모를 땐 선제 reissue 를 *건너뛰고*
     * (시도 자체를 안 함) 토큰이 실제 만료되면 401 → `TokenAuthenticator` 사후 대응이 받는다.
     * 모름을 임박으로 취급하면 deadline 이 빈 동안 매 요청 reissue 가 폭주하므로, 보수적 false 가 옳다 —
     * deadline 은 "미리 갱신" 최적화 스위치일 뿐 인증을 지키는 장치가 아니다(그건 401 경로 몫).
     */
    fun isExpiringSoon(): Boolean {
        val deadline = deadlineElapsedMillis ?: return false
        return deadline - elapsedRealtimeMillis() < PREEMPTIVE_REISSUE_THRESHOLD_MILLIS
    }

    /**
     * 기록된 deadline 을 비운다(null).
     * 토큰이 교체(reissue)·소멸(로그아웃)되면 기존 deadline 은 이전 토큰 기준 stale 이므로,
     * 다음 발급/회전이 새 deadline 을 기록할 때까지 선제 갱신을 쉬게 한다 — reissue 실패 시에도
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

package com.afternote.core.network.token

import com.afternote.core.domain.repository.auth.AuthRepository
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 토큰 reissue 의 단일 비행(single-flight) 지점 (#408).
 *
 * reissue 호출 경로는 둘이다 — 선제 갱신(`AuthInterceptor`)과 401 사후 대응(`TokenAuthenticator`).
 * 각자 자기 인스턴스를 잠그면 모니터가 달라 서로를 배제하지 못하고, 토큰 만료 시점엔 두 경로가
 * 동시에 깨어나는 게 기본 시나리오라 같은 refresh token 으로 rotateToken 이 이중 실행된다.
 * 현재 BE 는 사용된 refresh 재사용을 허용해(2026-06-11 실측: 동일 refresh 로 reissue 2연속 200)
 * 이중 실행의 실해가 "낭비 회전 1회"에 그치지만, BE 가 보안 권고(RFC 9700)대로 rotation
 * (사용된 토큰 즉시 무효화)을 도입하는 순간 늦은 쪽이 실패해 멀쩡한 세션이 강제 로그아웃되는
 * 재현 어려운 버그가 된다. 그래서 두 경로 모두 이 @Singleton 의 단일 락을 경유한다 —
 * "내부 상태 때문에 앱 어디서든 같은 인스턴스가 필요한 경우 SingletonComponent 스코프가
 * 적절하다(appropriate)"는 Hilt 공식 가이드(Component scopes)의 승인 케이스다.
 *
 * 락 진입 후 재확인은 deadline 이 아니라 **"호출자가 기대한 토큰 vs 현재 저장 토큰" 비교** —
 * 락 대기 중 다른 경로가 회전을 끝냈으면 저장 토큰이 달라져 있으므로, 늦게 진입한 쪽은
 * 회전을 생략하고 새 토큰만 받아 간다([Outcome.TokenAlreadyChanged]). "문자열이 달라짐 = 갱신 완료"
 * 가 성립하는 근거: 저장 토큰이 바뀌는 경로는 회전 성공·재로그인뿐이고 항상 더 신선한 발급분이
 * 저장된다 (같은 값으로 되돌아오는 경우는 "서버가 동일 토큰 재발급"뿐 — `TokenAuthenticator`
 * 의 동일-토큰 가드가 별도 차단). develop 의 기존 `TokenAuthenticator` 락 내 재확인과 같은 원리.
 *
 * 실패 후처리(clearSession 등)는 의도적으로 호출자 몫 — 401 확정 상황(`TokenAuthenticator`)과
 * best-effort 선제 경로(`AuthInterceptor`)의 실패 의미가 다르기 때문.
 */
@Singleton
class TokenReissuer
    @Inject
    constructor(
        private val authRepository: dagger.Lazy<AuthRepository>,
        private val expiryTracker: AccessTokenExpiryTracker,
    ) {
        sealed interface Outcome {
            /**
             * 락 대기 중 저장 토큰이 이미 교체됨 — 회전 생략, 현재 저장 토큰 반환.
             * 교체 원인은 대부분 다른 경로의 회전이지만 재로그인일 수도 있다 — 어느 쪽이든
             * "호출자가 갈아 끼우려던 그 토큰은 더 이상 현역이 아니므로 회전 불필요"는 동일.
             */
            data class TokenAlreadyChanged(
                val accessToken: String,
            ) : Outcome

            /** 이번 호출이 회전을 수행 — 서버가 발급한 새 액세스 토큰. */
            data class Rotated(
                val accessToken: String,
            ) : Outcome

            /** rotateToken 실패 (refresh 만료·네트워크 오류 등). 후처리는 호출자 판단. */
            data object Failed : Outcome
        }

        /**
         * @param expectedAccessToken 호출자가 "낡았다"고 판단한 근거가 된 바로 그 토큰 —
         *   새로 받고 싶은 토큰이 아니라 **바꿔치우려는 대상**이다 (선제 경로 = 만료 임박으로
         *   읽힌 저장 토큰, 401 경로 = 거절당한 요청 헤더의 토큰). 락 안에서 "저장소에 아직
         *   이 토큰이 있나"를 재확인해, 이미 달라졌으면 회전을 생략하고 현재 토큰을 돌려준다.
         */
        fun reissue(expectedAccessToken: String): Outcome {
            synchronized(this) {
                val currentToken = runBlocking { authRepository.get().getAccessToken() }.getOrNull()
                if (!currentToken.isNullOrEmpty() && currentToken != expectedAccessToken) {
                    return Outcome.TokenAlreadyChanged(currentToken)
                }

                val newAccessToken =
                    runBlocking { authRepository.get().rotateToken() }
                        .getOrNull()
                        ?.accessToken
                // 회전 시도 자체가 기존 deadline 을 무효화한다 — 새 수명은 다음 목록 응답에서 다시 기록되고,
                // 실패 시에도 비워 만료 deadline 잔존으로 인한 요청마다 재시도(폭주)를 막는다.
                expiryTracker.clear()

                return if (newAccessToken.isNullOrEmpty()) {
                    Outcome.Failed
                } else {
                    Outcome.Rotated(newAccessToken)
                }
            }
        }
    }

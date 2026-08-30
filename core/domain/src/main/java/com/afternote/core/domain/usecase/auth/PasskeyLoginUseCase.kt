package com.afternote.core.domain.usecase.auth

import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.repository.auth.PasskeyRepository
import com.afternote.core.model.PasskeyAuthenticationOptions
import javax.inject.Inject

/**
 * 패스키 로그인. 다른 로그인과 달리 **한 번에 끝나지 않는다** — 중간에 시스템(Credential Manager)이
 * 사용자에게 자격을 고르게 하는 단계가 끼어들고, 그 호출은 Activity 를 쥐고 있어야 하므로
 * UI 계층 몫이다. 그래서 이 UseCase 는 그 앞뒤 두 토막을 각각 제공한다.
 *
 * ```
 * requestOptions()  →  (UI: Credential Manager getCredential)  →  invoke(assertionJson)
 * ```
 *
 * 플랫폼 호출을 람다로 받아 한 함수로 합치지 않은 이유 — 그러면 Activity 를 캡처한 람다가
 * `viewModelScope` 안에 머무르게 되고, 구성 변경으로 화면이 갈려도 시스템 시트가 닫힐 때까지
 * 옛 Activity 가 붙잡힌다. 두 토막으로 두면 플랫폼 호출이 컴포지션 수명 안에서 끝난다.
 */
class PasskeyLoginUseCase
    @Inject
    constructor(
        private val passkeyRepository: PasskeyRepository,
        private val authRepository: AuthRepository,
    ) {
        /** 1단계 — 서버에서 인증 옵션을 받는다. 반환 원문을 그대로 Credential Manager 에 넣는다. */
        suspend fun requestOptions(): Result<PasskeyAuthenticationOptions> = passkeyRepository.authenticationOptions()

        /**
         * 2단계 — assertion 을 서버에 검증시키고, 성공하면 **기존 로그인과 같은 저장 경로**로 세션을 남긴다.
         *
         * 검증이 실패하면 그 실패를 그대로 돌려주고 세션은 건드리지 않는다 — 이미 로그인해 둔
         * 세션이 있는 상태에서 패스키 시도가 깨져도 그 세션이 지워지면 안 된다.
         */
        suspend operator fun invoke(assertionJson: String): Result<Unit> {
            val session =
                passkeyRepository.authenticate(assertionJson).getOrElse { exception ->
                    return Result.failure(exception)
                }
            return authRepository.saveSession(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
            )
        }
    }

package com.afternote.core.network

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.unexpectedCall
import com.afternote.core.model.TokenBundle
import com.afternote.core.domain.testing.FakeAuthRepository as CoreFakeAuthRepository

/**
 * 토큰 갱신 경로(#408)의 시나리오 설정. 정본은 core:domain testFixtures 에 있고, 여기서는
 * 저장 토큰 조회만 열고 rotate·clear 동작을 호출부 람다로 연결한다.
 */
internal fun networkFakeAuthRepository(
    accessToken: String? = null,
    onRotateToken: suspend CoreFakeAuthRepository.() -> Result<TokenBundle> = {
        unexpectedCall("AuthRepository.rotateToken")
    },
    onClearSession: suspend CoreFakeAuthRepository.() -> Result<Unit> = {
        unexpectedCall("AuthRepository.clearSession")
    },
): CoreFakeAuthRepository =
    CoreFakeAuthRepository.strict(accessToken = accessToken).apply {
        onGetAccessToken = null
        this.onRotateToken = onRotateToken
        this.onClearSession = { onClearSession(this) }
    }

internal class FakeErrorReporter : ErrorReporter {
    val writtenFailures = mutableListOf<Pair<Throwable, Map<String, String>>>()

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        writtenFailures += throwable to attributes
    }
}

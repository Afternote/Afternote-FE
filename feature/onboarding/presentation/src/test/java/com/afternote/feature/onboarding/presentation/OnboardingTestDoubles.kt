package com.afternote.feature.onboarding.presentation

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.model.AccountRegistration
import com.afternote.core.model.FoundAccount

/** 계측을 보지 않는 테스트용. */
internal object NoopErrorReporter : ErrorReporter {
    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) = Unit
}

/**
 * 리듀서 테스트가 쓰는 저장소. **어느 경로도 부르지 않는다** — 순수 전이만 보는 층이라
 * 호출이 일어났다면 그것이 결함이고, 그 자리에서 드러나야 한다(core:data 의 Fake 들과 같은 규칙).
 */
internal object UnusedAccountRepository : AccountRepository {
    override suspend fun sendEmailCode(email: String): Result<Unit> = error("sendEmailCode 는 리듀서 경로에서 호출되면 안 됨")

    override suspend fun verifyEmail(
        email: String,
        certificateCode: String,
    ): Result<Unit> = error("verifyEmail 은 리듀서 경로에서 호출되면 안 됨")

    override suspend fun sendFindCode(email: String): Result<Unit> = error("sendFindCode 는 리듀서 경로에서 호출되면 안 됨")

    override suspend fun findAccount(
        email: String,
        certificateCode: String,
    ): Result<FoundAccount> = error("findAccount 는 리듀서 경로에서 호출되면 안 됨")

    override suspend fun signUp(
        email: String,
        password: String,
        name: String,
        profileUrl: String?,
    ): Result<AccountRegistration> = error("signUp 은 리듀서 경로에서 호출되면 안 됨")

    override suspend fun passwordChange(
        currentPassword: String,
        newPassword: String,
    ): Result<Unit> = error("passwordChange 는 리듀서 경로에서 호출되면 안 됨")
}

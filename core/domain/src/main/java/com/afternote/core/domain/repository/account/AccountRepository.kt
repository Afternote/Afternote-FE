package com.afternote.core.domain.repository.account

import com.afternote.core.model.AccountRegistration
import com.afternote.core.model.FoundAccount

interface AccountRepository {
    suspend fun sendEmailCode(email: String): Result<Unit>

    suspend fun verifyEmail(
        email: String,
        certificateCode: String,
    ): Result<Unit>

    /** 아이디/비밀번호 찾기용 인증번호 발송. 회원가입용 [sendEmailCode] 와 엔드포인트가 다르다. */
    suspend fun sendFindCode(email: String): Result<Unit>

    /**
     * 아이디 찾기 — 인증번호 검증 후 가입 계정을 돌려받는다 (서버 계약명은 `auth/email/find`,
     * 이 앱의 로그인 아이디가 곧 이메일이라 서버가 "아이디 찾기" 를 이렇게 부른다).
     */
    suspend fun findAccount(
        email: String,
        certificateCode: String,
    ): Result<FoundAccount>

    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        profileUrl: String?,
    ): Result<AccountRegistration>

    suspend fun passwordChange(
        currentPassword: String,
        newPassword: String,
    ): Result<Unit>
}

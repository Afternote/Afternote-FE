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

    /**
     * 비밀번호 찾기 — 인증번호 검증과 새 비밀번호 반영을 한 요청으로 끝낸다
     * (서버 계약명은 `auth/password/find`).
     *
     * [findAccount] 와 달리 인증번호를 미리 소비하지 않는다. 서버는 인증번호를 **검증하면서
     * 삭제**하므로(BE `EmailService.verifyAndDeleteCode`), 화면에서 먼저 확인해 버리면 여기서
     * 쓸 코드가 남지 않는다 — 그래서 이 흐름의 인증 화면에는 인라인 "확인" 이 없고
     * [certificateCode] 를 비밀번호 변경 화면까지 들고 와 여기서 한 번에 낸다.
     *
     * [confirmPassword] 는 클라가 이미 일치를 강제하지만 서버도 독립적으로 검사한다
     * (BE `AuthService.findPassword` — 불일치는 code 1218). 계약 필드라 그대로 싣는다.
     */
    suspend fun resetPassword(
        email: String,
        certificateCode: String,
        newPassword: String,
        confirmPassword: String,
    ): Result<Unit>

    suspend fun passwordChange(
        currentPassword: String,
        newPassword: String,
    ): Result<Unit>
}

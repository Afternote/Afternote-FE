package com.afternote.core.data.repoimpl.account

import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.network.dto.EmailFindDto
import com.afternote.core.network.dto.EmailFindRequestDto
import com.afternote.core.network.dto.FindSendCodeDto
import com.afternote.core.network.dto.FindSendCodeRequestDto
import com.afternote.core.network.dto.PasswordChangeRequestDto
import com.afternote.core.network.dto.PasswordFindRequestDto
import com.afternote.core.network.dto.SendEmailCodeRequestDto
import com.afternote.core.network.dto.SignUpDto
import com.afternote.core.network.dto.SignUpRequestDto
import com.afternote.core.network.dto.VerifyEmailRequestDto
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.service.AccountApiService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [AccountRepositoryImpl] 의 예외 번역 계약 회귀 가드 (#472 · #457).
 *
 * 계약 — 서버가 인증번호 무효(code 1207)로 거절하면 [CoreAuthFailure.EmailVerification] 으로
 * 번역해 Presentation 이 타입으로 분기(인라인 표시)할 수 있게 하고, 그 외 실패
 * (네트워크·서버 오류 등)는 원본 예외를 유지해 기존 스낵바 경로로 흐르게 한다.
 *
 * 비밀번호 찾기(#457)가 더한 두 코드도 같은 규칙을 따른다 — 소셜 가입 계정(1702)은 화면이
 * 차단 팝업으로, 새 비밀번호가 기존과 같음(1206)은 스낵바 전용 문구로 가른다. 둘 다 타입으로만
 * 갈리므로 여기서 번역이 끊기면 화면이 조용히 폴백 문구를 낸다.
 */
class AccountRepositoryImplTest {
    private fun repository(accountApiService: AccountApiService) = AccountRepositoryImpl(accountApiService)

    @Test
    fun `verifyEmail - 인증번호 무효(1207)는 EmailVerification 으로 번역`() {
        val repository =
            repository(
                FakeAccountApiService(
                    onVerifyEmail = {
                        throw ApiException(
                            status = 400,
                            code = 1207,
                            serverMessage = "인증번호가 유효하지 않습니다.",
                            fallbackMessage = "인증번호가 유효하지 않습니다.",
                        )
                    },
                ),
            )

        val result = runBlocking { repository.verifyEmail("user@example.com", "000000") }

        val error = result.exceptionOrNull()
        assertTrue(error is CoreAuthFailure.EmailVerification)
    }

    @Test
    fun `verifyEmail - 그 외 code 실패는 원본 ApiException 유지 (스낵바 경로)`() {
        val repository =
            repository(
                FakeAccountApiService(
                    onVerifyEmail = {
                        throw ApiException(status = 500, code = 500, serverMessage = null, fallbackMessage = "서버 오류")
                    },
                ),
            )

        val result = runBlocking { repository.verifyEmail("user@example.com", "000000") }

        val error = result.exceptionOrNull()
        assertTrue(error is ApiException)
        assertEquals(500, (error as ApiException).code)
    }

    @Test
    fun `verifyEmail - 성공 시 Result_success`() {
        val repository = repository(FakeAccountApiService(onVerifyEmail = { success(Unit) }))

        val result = runBlocking { repository.verifyEmail("user@example.com", "123456") }

        assertTrue(result.isSuccess)
    }

    @Test
    fun `resetPassword - 소셜 가입 계정(1702)은 SocialSignUpAccount 로 번역`() {
        val repository =
            repository(
                FakeAccountApiService(
                    onFindPassword = {
                        throw ApiException(
                            status = 400,
                            code = 1702,
                            serverMessage = "소셜 로그인으로 가입한 계정입니다. 소셜 로그인을 이용해주세요.",
                            fallbackMessage = "소셜 로그인으로 가입한 계정입니다.",
                        )
                    },
                ),
            )

        val result = runBlocking { repository.resetPassword(EMAIL, CODE, NEW_PASSWORD, NEW_PASSWORD) }

        assertTrue(result.exceptionOrNull() is CoreAuthFailure.SocialSignUpAccount)
    }

    @Test
    fun `resetPassword - 기존과 같은 비밀번호(1206)는 PasswordUnchanged 로 번역`() {
        val repository =
            repository(
                FakeAccountApiService(
                    onFindPassword = {
                        throw ApiException(
                            status = 400,
                            code = 1206,
                            serverMessage = "새 비밀번호와 같습니다.",
                            fallbackMessage = "새 비밀번호와 같습니다.",
                        )
                    },
                ),
            )

        val result = runBlocking { repository.resetPassword(EMAIL, CODE, NEW_PASSWORD, NEW_PASSWORD) }

        assertTrue(result.exceptionOrNull() is CoreAuthFailure.PasswordUnchanged)
    }

    @Test
    fun `resetPassword - 인증번호 무효(1207)는 EmailVerification 으로 번역`() {
        val repository =
            repository(
                FakeAccountApiService(
                    onFindPassword = {
                        throw ApiException(
                            status = 400,
                            code = 1207,
                            serverMessage = "인증번호가 유효하지 않습니다.",
                            fallbackMessage = "인증번호가 유효하지 않습니다.",
                        )
                    },
                ),
            )

        val result = runBlocking { repository.resetPassword(EMAIL, CODE, NEW_PASSWORD, NEW_PASSWORD) }

        assertTrue(result.exceptionOrNull() is CoreAuthFailure.EmailVerification)
    }

    @Test
    fun `resetPassword - 그 외 code 실패는 원본 ApiException 유지 (스낵바 경로)`() {
        val repository =
            repository(
                FakeAccountApiService(
                    onFindPassword = {
                        throw ApiException(status = 400, code = 1219, serverMessage = null, fallbackMessage = "가입되지 않은 이메일입니다.")
                    },
                ),
            )

        val result = runBlocking { repository.resetPassword(EMAIL, CODE, NEW_PASSWORD, NEW_PASSWORD) }

        val error = result.exceptionOrNull()
        assertTrue(error is ApiException)
        assertEquals(1219, (error as ApiException).code)
    }

    @Test
    fun `resetPassword - 네 필드를 서버 요청 그대로 싣는다`() {
        var sent: PasswordFindRequestDto? = null
        val repository =
            repository(
                FakeAccountApiService(
                    onFindPassword = { body ->
                        sent = body
                        success(Unit)
                    },
                ),
            )

        val result = runBlocking { repository.resetPassword(EMAIL, CODE, NEW_PASSWORD, "Confirm1!") }

        assertTrue(result.isSuccess)
        assertEquals(PasswordFindRequestDto(EMAIL, CODE, NEW_PASSWORD, "Confirm1!"), sent)
    }

    private companion object {
        const val EMAIL = "user@example.com"
        const val CODE = "123456"
        const val NEW_PASSWORD = "NewPass1!"
    }
}

private fun <T> success(data: T) = BaseResponse(status = 200, code = 200, message = "성공", data = data)

/**
 * [AccountApiService] 테스트 공용 가짜 — 미지정 경로 호출은 error 로 드러낸다
 * (core:data 의 FakeAuthApiService 와 같은 규칙).
 */
private class FakeAccountApiService(
    private val onVerifyEmail: () -> BaseResponse<Unit> = {
        error("verifyEmail 은 이 시나리오에서 호출되면 안 됨")
    },
    private val onFindPassword: (PasswordFindRequestDto) -> BaseResponse<Unit> = {
        error("findPassword 는 이 시나리오에서 호출되면 안 됨")
    },
) : AccountApiService {
    override suspend fun sendEmailCode(body: SendEmailCodeRequestDto): BaseResponse<Unit> = error("sendEmailCode 는 이 시나리오에서 호출되면 안 됨")

    override suspend fun sendFindCode(body: FindSendCodeRequestDto): BaseResponse<FindSendCodeDto> =
        error("sendFindCode 는 이 시나리오에서 호출되면 안 됨")

    override suspend fun findEmail(body: EmailFindRequestDto): BaseResponse<EmailFindDto> = error("findEmail 은 이 시나리오에서 호출되면 안 됨")

    override suspend fun findPassword(body: PasswordFindRequestDto): BaseResponse<Unit> = onFindPassword(body)

    override suspend fun verifyEmail(body: VerifyEmailRequestDto): BaseResponse<Unit> = onVerifyEmail()

    override suspend fun signUp(body: SignUpRequestDto): BaseResponse<SignUpDto> = error("signUp 은 이 시나리오에서 호출되면 안 됨")

    override suspend fun passwordChange(body: PasswordChangeRequestDto): BaseResponse<Unit> = error("passwordChange 는 이 시나리오에서 호출되면 안 됨")
}

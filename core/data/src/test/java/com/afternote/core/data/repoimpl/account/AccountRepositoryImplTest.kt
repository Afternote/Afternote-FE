package com.afternote.core.data.repoimpl.account

import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.network.dto.EmailFindDto
import com.afternote.core.network.dto.EmailFindRequestDto
import com.afternote.core.network.dto.FindSendCodeRequestDto
import com.afternote.core.network.dto.PasswordChangeRequestDto
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
 * [AccountRepositoryImpl.verifyEmail] 의 예외 번역 계약 회귀 가드 (#472).
 *
 * 계약 — 서버가 인증번호 무효(code 1207)로 거절하면 [CoreAuthFailure.EmailVerification] 으로
 * 번역해 Presentation 이 타입으로 분기(인라인 표시)할 수 있게 하고, 그 외 실패
 * (네트워크·서버 오류 등)는 원본 예외를 유지해 기존 스낵바 경로로 흐르게 한다.
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
                            message = "인증번호가 유효하지 않습니다.",
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
                        throw ApiException(status = 500, code = 500, serverMessage = null, message = "서버 오류")
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
) : AccountApiService {
    override suspend fun sendEmailCode(body: SendEmailCodeRequestDto): BaseResponse<Unit> = error("sendEmailCode 는 이 시나리오에서 호출되면 안 됨")

    override suspend fun sendFindCode(body: FindSendCodeRequestDto): BaseResponse<Unit> = error("sendFindCode 는 이 시나리오에서 호출되면 안 됨")

    override suspend fun findEmail(body: EmailFindRequestDto): BaseResponse<EmailFindDto> = error("findEmail 은 이 시나리오에서 호출되면 안 됨")

    override suspend fun verifyEmail(body: VerifyEmailRequestDto): BaseResponse<Unit> = onVerifyEmail()

    override suspend fun signUp(body: SignUpRequestDto): BaseResponse<SignUpDto> = error("signUp 은 이 시나리오에서 호출되면 안 됨")

    override suspend fun passwordChange(body: PasswordChangeRequestDto): BaseResponse<Unit> = error("passwordChange 는 이 시나리오에서 호출되면 안 됨")
}

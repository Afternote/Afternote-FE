package com.afternote.core.data.repoimpl

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.ReceiverRequestRejectedException
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.network.dto.DeletePushTokenRequestDto
import com.afternote.core.network.dto.PushTokenDto
import com.afternote.core.network.dto.ReceiverDetailDto
import com.afternote.core.network.dto.ReceiverListDto
import com.afternote.core.network.dto.RegisterPushTokenRequestDto
import com.afternote.core.network.dto.SocialAccountLinkRequestDto
import com.afternote.core.network.dto.UserConnectedAccountDto
import com.afternote.core.network.dto.UserCreateReceiverDto
import com.afternote.core.network.dto.UserCreateReceiverRequestDto
import com.afternote.core.network.dto.UserDto
import com.afternote.core.network.dto.UserMarketingConsentDto
import com.afternote.core.network.dto.UserPatchReceiverDto
import com.afternote.core.network.dto.UserPatchReceiverRequestDto
import com.afternote.core.network.dto.UserPushSettingDto
import com.afternote.core.network.dto.UserUpdateMarketingConsentRequestDto
import com.afternote.core.network.dto.UserUpdateProfileRequestDto
import com.afternote.core.network.dto.UserUpdatePushSettingRequestDto
import com.afternote.core.network.dto.UserUpdateReceiverMessageRequestDto
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionDto
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionUpdateRequestDto
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.service.UserApiService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `UserReceiverRepositoryImpl` 옆의 `mapReceiverRequestFailure` 는 `private` 이라 이 파일에서 직접 호출할 수
 * 없다 — 그 helper 를 공유하는 공개 계약([UserReceiverRepositoryImpl.createReceiver])을 통해 같은 회귀를 고정한다.
 */
class ReceiverRequestFailureTest {
    private fun repositoryThrowingOnCreate(apiError: ApiException) =
        UserReceiverRepositoryImpl(
            userApiService = CreateReceiverThrowingApiService(onCreateReceiver = { throw apiError }),
            authRepository = FakeAuthRepository(loggedIn = false),
            errorReporter = NoOpErrorReporter,
        )

    private suspend fun createReceiver(repository: UserReceiverRepositoryImpl) =
        repository.createReceiver(
            name = "친구",
            relation = "친구",
            phone = null,
            email = "friend@example.com",
            message = null,
        )

    @Test
    fun `400 서버 메시지는 사용자 노출 도메인 오류로 바뀐다`() =
        runBlocking {
            val apiError =
                ApiException(
                    status = 400,
                    code = 400,
                    serverMessage = "수신자 이메일은 필수입니다.",
                    fallbackMessage = "수신자 이메일은 필수입니다.",
                )
            val repository = repositoryThrowingOnCreate(apiError)

            val result = runCatching { createReceiver(repository) }.exceptionOrNull()

            assertTrue(result is ReceiverRequestRejectedException)
            val domainError = result as ReceiverRequestRejectedException
            assertEquals("수신자 이메일은 필수입니다.", domainError.userMessage)
            assertSame(apiError, domainError.cause)
        }

    @Test
    fun `409 서버 메시지는 사용자 노출 도메인 오류로 바뀐다`() =
        runBlocking {
            val apiError =
                ApiException(
                    status = 409,
                    code = 409,
                    serverMessage = "이미 등록된 수신자입니다.",
                    fallbackMessage = "이미 등록된 수신자입니다.",
                )
            val repository = repositoryThrowingOnCreate(apiError)

            val result = runCatching { createReceiver(repository) }.exceptionOrNull()

            assertTrue(result is ReceiverRequestRejectedException)
        }

    @Test
    fun `그 외 4xx 서버 메시지는 입력 오류로 변환하지 않는다`() =
        runBlocking {
            val apiError =
                ApiException(
                    status = 404,
                    code = 404,
                    serverMessage = "존재하지 않는 엔드포인트입니다.",
                    fallbackMessage = "존재하지 않는 엔드포인트입니다.",
                )
            val repository = repositoryThrowingOnCreate(apiError)

            val result = runCatching { createReceiver(repository) }.exceptionOrNull()

            assertSame(apiError, result)
        }

    @Test
    fun `5xx 서버 메시지는 사용자 노출 오류로 바꾸지 않는다`() =
        runBlocking {
            val apiError =
                ApiException(
                    status = 500,
                    code = 500,
                    serverMessage = "internal SQL details",
                    fallbackMessage = "internal SQL details",
                )
            val repository = repositoryThrowingOnCreate(apiError)

            val result = runCatching { createReceiver(repository) }.exceptionOrNull()

            assertSame(apiError, result)
        }
}

private object NoOpErrorReporter : ErrorReporter {
    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) = Unit
}

private class CreateReceiverThrowingApiService(
    private val onCreateReceiver: suspend (UserCreateReceiverRequestDto) -> BaseResponse<UserCreateReceiverDto>,
) : UserApiService {
    override suspend fun getReceivers(): BaseResponse<List<ReceiverListDto>> = TODO("이 테스트 미사용")

    override suspend fun createReceiver(request: UserCreateReceiverRequestDto): BaseResponse<UserCreateReceiverDto> =
        onCreateReceiver(request)

    override suspend fun getReceiverDetail(receiverId: Long): BaseResponse<ReceiverDetailDto> = TODO("이 테스트 미사용")

    override suspend fun updateReceiver(
        receiverId: Long,
        request: UserPatchReceiverRequestDto,
    ): BaseResponse<UserPatchReceiverDto> = TODO("이 테스트 미사용")

    override suspend fun updateReceiverMessage(
        receiverId: Long,
        request: UserUpdateReceiverMessageRequestDto,
    ): BaseResponse<Unit> = TODO("이 테스트 미사용")

    override suspend fun getMyProfile(): BaseResponse<UserDto> = TODO("이 테스트 미사용")

    override suspend fun updateMyProfile(request: UserUpdateProfileRequestDto): BaseResponse<UserDto> = TODO("이 테스트 미사용")

    override suspend fun deleteAccount(): BaseResponse<Unit> = TODO("이 테스트 미사용")

    override suspend fun registerPushToken(request: RegisterPushTokenRequestDto): BaseResponse<PushTokenDto> = TODO("이 테스트 미사용")

    override suspend fun deletePushToken(request: DeletePushTokenRequestDto): BaseResponse<Unit> = TODO("이 테스트 미사용")

    override suspend fun getMyPushSettings(): BaseResponse<UserPushSettingDto> = TODO("이 테스트 미사용")

    override suspend fun updateMyPushSettings(request: UserUpdatePushSettingRequestDto): BaseResponse<UserPushSettingDto> =
        TODO("이 테스트 미사용")

    override suspend fun getMyMarketingConsents(): BaseResponse<UserMarketingConsentDto> = TODO("이 테스트 미사용")

    override suspend fun updateMyMarketingConsents(request: UserUpdateMarketingConsentRequestDto): BaseResponse<UserMarketingConsentDto> =
        TODO("이 테스트 미사용")

    override suspend fun getConnectedAccounts(): BaseResponse<UserConnectedAccountDto> = TODO("이 테스트 미사용")

    override suspend fun linkConnectedAccount(
        provider: String,
        request: SocialAccountLinkRequestDto,
    ): BaseResponse<UserConnectedAccountDto> = TODO("이 테스트 미사용")

    override suspend fun unlinkConnectedAccount(provider: String): BaseResponse<UserConnectedAccountDto> = TODO("이 테스트 미사용")

    override suspend fun getReceiverDeliveryConditions(receiverId: Long): BaseResponse<ReceiverDeliveryConditionDto> = TODO("이 테스트 미사용")

    override suspend fun updateReceiverDeliveryConditions(
        receiverId: Long,
        request: ReceiverDeliveryConditionUpdateRequestDto,
    ): BaseResponse<ReceiverDeliveryConditionDto> = TODO("이 테스트 미사용")
}

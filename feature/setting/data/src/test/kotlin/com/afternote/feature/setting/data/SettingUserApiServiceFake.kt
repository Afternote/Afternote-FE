package com.afternote.feature.setting.data

import com.afternote.core.common.reporting.ErrorReporter
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
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.service.UserApiService

/**
 * 설정 저장소가 실제로 부르는 엔드포인트만 열어 둔 [UserApiService] fake.
 *
 * 나머지는 `TODO` 로 닫아 둔다 — 이 모듈 구현이 수신자·프로필 엔드포인트를 부르기 시작하면
 * 조용히 통과하지 않고 그 자리에서 터진다.
 */
internal class SettingUserApiServiceFake(
    private val onDeleteAccount: suspend () -> BaseResponse<Unit> = { TODO("이 테스트 미사용") },
    private val onGetConnectedAccounts: suspend () -> BaseResponse<UserConnectedAccountDto> = { TODO("이 테스트 미사용") },
    private val onLinkConnectedAccount: suspend (String, SocialAccountLinkRequestDto) -> BaseResponse<UserConnectedAccountDto> =
        { _, _ ->
            TODO("이 테스트 미사용")
        },
    private val onUnlinkConnectedAccount: suspend (String) -> BaseResponse<UserConnectedAccountDto> = { TODO("이 테스트 미사용") },
    private val onGetMyPushSettings: suspend () -> BaseResponse<UserPushSettingDto> = { TODO("이 테스트 미사용") },
    private val onUpdateMyPushSettings: suspend (UserUpdatePushSettingRequestDto) -> BaseResponse<UserPushSettingDto> = {
        TODO("이 테스트 미사용")
    },
    private val onGetMyMarketingConsents: suspend () -> BaseResponse<UserMarketingConsentDto> = { TODO("이 테스트 미사용") },
    private val onUpdateMyMarketingConsents: suspend (UserUpdateMarketingConsentRequestDto) -> BaseResponse<UserMarketingConsentDto> = {
        TODO("이 테스트 미사용")
    },
) : UserApiService {
    override suspend fun deleteAccount(): BaseResponse<Unit> = onDeleteAccount()

    override suspend fun getConnectedAccounts(): BaseResponse<UserConnectedAccountDto> = onGetConnectedAccounts()

    override suspend fun linkConnectedAccount(
        provider: String,
        request: SocialAccountLinkRequestDto,
    ): BaseResponse<UserConnectedAccountDto> = onLinkConnectedAccount(provider, request)

    override suspend fun unlinkConnectedAccount(provider: String): BaseResponse<UserConnectedAccountDto> =
        onUnlinkConnectedAccount(provider)

    override suspend fun getMyPushSettings(): BaseResponse<UserPushSettingDto> = onGetMyPushSettings()

    override suspend fun updateMyPushSettings(request: UserUpdatePushSettingRequestDto): BaseResponse<UserPushSettingDto> =
        onUpdateMyPushSettings(request)

    override suspend fun getMyMarketingConsents(): BaseResponse<UserMarketingConsentDto> = onGetMyMarketingConsents()

    override suspend fun updateMyMarketingConsents(request: UserUpdateMarketingConsentRequestDto): BaseResponse<UserMarketingConsentDto> =
        onUpdateMyMarketingConsents(request)

    override suspend fun getReceivers(): BaseResponse<List<ReceiverListDto>> = TODO("이 테스트 미사용")

    override suspend fun createReceiver(request: UserCreateReceiverRequestDto): BaseResponse<UserCreateReceiverDto> = TODO("이 테스트 미사용")

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

    override suspend fun registerPushToken(request: RegisterPushTokenRequestDto): BaseResponse<PushTokenDto> = TODO("이 테스트 미사용")

    override suspend fun deletePushToken(request: DeletePushTokenRequestDto): BaseResponse<Unit> = TODO("이 테스트 미사용")

    override suspend fun getReceiverDeliveryConditions(receiverId: Long): BaseResponse<ReceiverDeliveryConditionDto> = TODO("이 테스트 미사용")

    override suspend fun updateReceiverDeliveryConditions(
        receiverId: Long,
        request: ReceiverDeliveryConditionUpdateRequestDto,
    ): BaseResponse<ReceiverDeliveryConditionDto> = TODO("이 테스트 미사용")
}

internal class RecordingErrorReporter : ErrorReporter {
    val writtenFailures = mutableListOf<Pair<Throwable, Map<String, String>>>()

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        writtenFailures += throwable to attributes
    }
}

internal fun <T> dataResponse(data: T) = BaseResponse(status = 200, code = 200, data = data)

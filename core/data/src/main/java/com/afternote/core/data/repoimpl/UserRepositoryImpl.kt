package com.afternote.core.data.repoimpl

import android.util.Log
import com.afternote.core.data.mapper.delivery.toRequestDto
import com.afternote.core.data.mapper.user.toDomain
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserPushSetting
import com.afternote.core.network.dto.SocialAccountLinkRequestDto
import com.afternote.core.network.dto.UserCreateReceiverRequestDto
import com.afternote.core.network.dto.UserPatchReceiverRequestDto
import com.afternote.core.network.dto.UserUpdateProfileRequestDto
import com.afternote.core.network.dto.UserUpdatePushSettingRequestDto
import com.afternote.core.network.dto.UserUpdateReceiverMessageRequestDto
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionUpdateRequestDto
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.core.network.service.UserApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import com.afternote.core.data.mapper.delivery.toDomain as toDeliveryConditionsDomain

class UserRepositoryImpl
    @Inject
    constructor(
        private val userApiService: UserApiService,
        private val authRepository: AuthRepository,
    ) : UserRepository {
        private val receiverRefreshRevision = MutableStateFlow(0L)

        override val receiverListFlow: Flow<List<Receiver>> =
            receiverRefreshRevision.map { getReceivers() }

        override suspend fun getReceivers(): List<Receiver> =
            userApiService
                .getReceivers()
                .requireData()
                .map { it.toDomain() }

        override suspend fun createReceiver(
            name: String,
            relation: String,
            phone: String?,
            email: String?,
            message: String?,
        ): ReceiverCreated {
            val result =
                userApiService
                    .createReceiver(
                        UserCreateReceiverRequestDto(
                            name = name,
                            relation = relation,
                            phone = phone,
                            email = email,
                            message = message,
                        ),
                    ).requireData()
                    .toDomain()
            receiverRefreshRevision.update { it + 1 }
            return result
        }

        override suspend fun getReceiverDetail(receiverId: Long): ReceiverDetail =
            userApiService
                .getReceiverDetail(receiverId)
                .requireData()
                .toDomain()

        override suspend fun updateReceiver(
            receiverId: Long,
            name: String,
            phone: String,
            relation: String,
            email: String,
        ): Receiver =
            userApiService
                .updateReceiver(
                    receiverId = receiverId,
                    request =
                        UserPatchReceiverRequestDto(
                            name = name,
                            phone = phone,
                            relation = relation,
                            email = email,
                        ),
                ).requireData()
                .toDomain()

        override suspend fun updateReceiverMessage(
            receiverId: Long,
            message: String,
        ) {
            userApiService
                .updateReceiverMessage(
                    receiverId = receiverId,
                    request = UserUpdateReceiverMessageRequestDto(message = message),
                ).requireStatus()
        }

        override suspend fun getMyProfile(): User =
            userApiService
                .getMyProfile()
                .requireData()
                .toDomain()

        override suspend fun updateMyProfile(
            name: String?,
            phone: String?,
            profileImageUrl: String?,
        ): User =
            userApiService
                .updateMyProfile(
                    UserUpdateProfileRequestDto(
                        name = name,
                        phone = phone,
                        profileImageUrl = profileImageUrl,
                    ),
                ).requireData()
                .toDomain()

        /**
         * 탈퇴 성공 후 로컬 세션도 정리한다 (#586) — 서버가 계정을 지워도 토큰이 남으면 재시작 시
         * 죽은 토큰으로 홈이 뜨고 인증 요청이 연달아 401 로 실패한다(2026-07-28 에뮬 실측).
         *
         * 정리를 서버 호출 **뒤**에 두는 건 `AuthRepositoryImpl.logout()` 과 같은 이유 — DELETE 요청도
         * `AuthInterceptor` 를 지나므로 그 시점엔 토큰이 살아 있어야 한다.
         *
         * `clearSession()` 의 실패는 삼킨다. 서버 계정은 이미 지워졌으므로 여기서 예외를 올리면
         * 화면이 "탈퇴 실패" 로 표시돼 사용자가 재시도하고, 그 재시도는 없는 계정에 대해 실패한다.
         * 대신 흔적은 남긴다 — 조용히 넘기면 로컬 토큰이 남은 채로 이 버그가 재발해도 탐지되지 않는다.
         * 예외 인스턴스가 아니라 타입만 넘기는 건 release 에서 `Log.e` 가 제거되지 않아서다.
         */
        override suspend fun deleteAccount() {
            userApiService
                .deleteAccount()
                .requireStatus()
            authRepository.clearSession().onFailure {
                Log.e("UserRepository", "탈퇴 후 세션 정리 실패: ${it.javaClass.name}")
            }
        }

        override suspend fun logActivity() {
            userApiService
                .logActivity()
                .requireStatus()
        }

        override suspend fun getMyPushSettings(): UserPushSetting =
            userApiService
                .getMyPushSettings()
                .requireData()
                .toDomain()

        override suspend fun updateMyPushSettings(
            timeLetter: Boolean?,
            mindRecord: Boolean?,
            afterNote: Boolean?,
        ): UserPushSetting =
            userApiService
                .updateMyPushSettings(
                    UserUpdatePushSettingRequestDto(
                        timeLetter = timeLetter,
                        mindRecord = mindRecord,
                        afterNote = afterNote,
                    ),
                ).requireData()
                .toDomain()

        override suspend fun getConnectedAccounts(): UserConnectedAccount =
            userApiService
                .getConnectedAccounts()
                .requireData()
                .toDomain()

        override suspend fun linkConnectedAccount(
            provider: String,
            accessToken: String,
        ): UserConnectedAccount =
            userApiService
                .linkConnectedAccount(
                    provider = provider,
                    request = SocialAccountLinkRequestDto(accessToken = accessToken),
                ).requireData()
                .toDomain()

        override suspend fun unlinkConnectedAccount(provider: String): UserConnectedAccount =
            userApiService
                .unlinkConnectedAccount(provider)
                .requireData()
                .toDomain()

        override suspend fun getReceiverDeliveryConditions(receiverId: Long): ReceiverDeliveryConditions =
            userApiService
                .getReceiverDeliveryConditions(receiverId)
                .requireData()
                .toDeliveryConditionsDomain()

        override suspend fun updateReceiverDeliveryConditions(
            receiverId: Long,
            conditions: List<DeliveryConditionItem>,
        ): ReceiverDeliveryConditions =
            userApiService
                .updateReceiverDeliveryConditions(
                    receiverId = receiverId,
                    request = ReceiverDeliveryConditionUpdateRequestDto(conditions.map { it.toRequestDto() }),
                ).requireData()
                .toDeliveryConditionsDomain()
    }

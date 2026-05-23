package com.afternote.core.data.repoimpl

import com.afternote.core.data.mapper.user.toDomain
import com.afternote.core.data.mapper.user.toDto
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.user.DeliveryCondition
import com.afternote.core.model.user.DeliveryConditionType
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserPushSetting
import com.afternote.core.network.dto.DeliveryConditionRequest
import com.afternote.core.network.dto.SocialAccountLinkRequest
import com.afternote.core.network.dto.UserCreateReceiverRequest
import com.afternote.core.network.dto.UserPatchReceiverRequest
import com.afternote.core.network.dto.UserUpdateProfileRequest
import com.afternote.core.network.dto.UserUpdatePushSettingRequest
import com.afternote.core.network.dto.UserUpdateReceiverMessageRequest
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.core.network.service.UserApiService
import javax.inject.Inject

class UserRepositoryImpl
    @Inject
    constructor(
        private val userApiService: UserApiService,
    ) : UserRepository {
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
        ): ReceiverCreated =
            userApiService
                .createReceiver(
                    UserCreateReceiverRequest(
                        name = name,
                        relation = relation,
                        phone = phone,
                        email = email,
                        message = message,
                    ),
                ).requireData()
                .toDomain()

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
                        UserPatchReceiverRequest(
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
                    request = UserUpdateReceiverMessageRequest(message = message),
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
                    UserUpdateProfileRequest(
                        name = name,
                        phone = phone,
                        profileImageUrl = profileImageUrl,
                    ),
                ).requireData()
                .toDomain()

        override suspend fun deleteAccount() {
            userApiService
                .deleteAccount()
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
                    UserUpdatePushSettingRequest(
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
                    request = SocialAccountLinkRequest(accessToken = accessToken),
                ).requireData()
                .toDomain()

        override suspend fun unlinkConnectedAccount(provider: String): UserConnectedAccount =
            userApiService
                .unlinkConnectedAccount(provider)
                .requireData()
                .toDomain()

        override suspend fun getDeliveryCondition(): DeliveryCondition =
            userApiService
                .getDeliveryCondition()
                .requireData()
                .toDomain()

        override suspend fun updateDeliveryCondition(
            conditionType: DeliveryConditionType,
            inactivityPeriodDays: Int?,
            specificDate: String?,
        ): DeliveryCondition =
            userApiService
                .updateDeliveryCondition(
                    DeliveryConditionRequest(
                        conditionType = conditionType.toDto(),
                        inactivityPeriodDays = inactivityPeriodDays,
                        specificDate = specificDate,
                    ),
                ).requireData()
                .toDomain()
    }

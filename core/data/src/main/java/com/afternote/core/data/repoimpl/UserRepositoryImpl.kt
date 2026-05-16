package com.afternote.core.data.repoimpl

import com.afternote.core.data.mapper.user.UserMapper
import com.afternote.core.datastore.UserProfileDataSource
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.ReceiverDailyQuestionsResult
import com.afternote.core.model.ReceiverMindRecordsResult
import com.afternote.core.model.setting.DeliveryCondition
import com.afternote.core.model.setting.DeliveryConditionType
import com.afternote.core.model.setting.PushSettings
import com.afternote.core.model.setting.ReceiverDetail
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.model.user.UserProfileModel
import com.afternote.core.network.dto.RegisterReceiverRequestDto
import com.afternote.core.network.dto.UserUpdateProfileRequest
import com.afternote.core.network.dto.UserUpdatePushSettingRequest
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.core.network.service.UserApiService
import javax.inject.Inject

// TODO:리팩토링
class UserRepositoryImpl
    @Inject
    constructor(
        private val api: UserApiService,
        private val profileCache: UserProfileDataSource,
    ) : UserRepository {
        override suspend fun getMyProfile(): Result<UserProfileModel> =
            runCatching {
                val response = api.getMyProfile()
                val profile = UserMapper.toUserProfile(response.requireData())
                profileCache.saveUserName(profile.name)
                profile
            }

        override suspend fun getCachedUserName(): String? = profileCache.getCachedUserName()

        override suspend fun clearCachedProfile() {
            profileCache.clear()
        }

        override suspend fun updateMyProfile(
            name: String?,
            phone: String?,
            profileImageUrl: String?,
        ): Result<UserProfileModel> =
            runCatching {
                val response =
                    api.updateMyProfile(
                        body =
                            UserUpdateProfileRequest(
                                name = name,
                                phone = phone,
                                profileImageUrl = profileImageUrl,
                            ),
                    )
                val profile = UserMapper.toUserProfile(response.requireData())
                profileCache.saveUserName(profile.name)
                profile
            }

        override suspend fun withdrawAccount(): Result<Unit> =
            runCatching {
                val response = api.withdrawAccount()
                response.requireStatus()
                profileCache.clear()
            }

        override suspend fun getMyPushSettings(): Result<PushSettings> =
            runCatching {
                val response = api.getMyPushSettings()
                UserMapper.toPushSettings(response.requireData())
            }

        override suspend fun updateMyPushSettings(
            timeLetter: Boolean?,
            mindRecord: Boolean?,
            afterNote: Boolean?,
        ): Result<PushSettings> =
            runCatching {
                val response =
                    api.updateMyPushSettings(
                        body =
                            UserUpdatePushSettingRequest(
                                timeLetter = timeLetter,
                                mindRecord = mindRecord,
                                afterNote = afterNote,
                            ),
                    )
                UserMapper.toPushSettings(response.requireData())
            }

        override suspend fun getReceivers(): Result<List<ReceiverListItem>> =
            runCatching {
                val response = api.getReceivers()
                val list = response.requireData()
                list.map(UserMapper::toReceiverListItem)
            }

        override suspend fun registerReceiver(
            name: String,
            relation: String,
            phone: String?,
            email: String?,
        ): Result<Long> =
            runCatching {
                val response =
                    api.registerReceiver(
                        RegisterReceiverRequestDto(
                            name = name,
                            relation = relation,
                            phone = phone,
                            email = email,
                        ),
                    )
                if (response.status != 201) {
                    throw ApiException(
                        status = response.status,
                        code = response.code,
                        message = response.message ?: "Status 201 아님",
                    )
                }
                val data = response.requireData()
                data.receiverId
            }

        override suspend fun getReceiverDetail(receiverId: Long): Result<ReceiverDetail> =
            runCatching {
                val response = api.getReceiverDetail(receiverId = receiverId)
                UserMapper.toReceiverDetail(response.requireData())
            }

        override suspend fun updateReceiver(
            receiverId: Long,
            name: String,
            relation: String,
            phone: String?,
            email: String?,
        ): Result<Unit> =
            runCatching {
                val response =
                    api.updateReceiver(
                        receiverId = receiverId,
                        body =
                            RegisterReceiverRequestDto(
                                name = name,
                                relation = relation,
                                phone = phone,
                                email = email,
                            ),
                    )
                if (response.status !in 200..299) {
                    throw ApiException(
                        status = response.status,
                        code = response.code,
                        message = response.message ?: "Status 200 이상 299 이하가 아님",
                    )
                }
            }

        override suspend fun getReceiverDailyQuestions(
            receiverId: Long,
            page: Int,
            size: Int,
        ): Result<ReceiverDailyQuestionsResult> =
            runCatching {
                val response =
                    api.getReceiverDailyQuestions(
                        receiverId = receiverId,
                        page = page,
                        size = size,
                    )
                val body = response.requireData()
                val items = body.items.map(UserMapper::toDailyQuestionAnswerItem)
                UserMapper.toReceiverDailyQuestionsResult(items = items, hasNext = body.hasNext)
            }

        override suspend fun getReceiverMindRecords(
            receiverId: Long,
            page: Int,
            size: Int,
        ): Result<ReceiverMindRecordsResult> =
            runCatching {
                val response =
                    api.getReceiverMindRecords(
                        receiverId = receiverId,
                        page = page,
                        size = size,
                    )
                val body = response.requireData()
                val items = (body.items).map(UserMapper::toReceiverMindRecordItem)
                UserMapper.toReceiverMindRecordsResult(items = items, hasNext = body.hasNext)
            }

        override suspend fun getDeliveryCondition(): Result<DeliveryCondition> =
            runCatching {
                val response = api.getDeliveryCondition()
                UserMapper.toDeliveryCondition(response.requireData())
            }

        override suspend fun updateDeliveryCondition(
            conditionType: DeliveryConditionType,
            inactivityPeriodDays: Int?,
            specificDate: String?,
            leaveMessage: String?,
        ): Result<DeliveryCondition> =
            runCatching {
                val body =
                    UserMapper.toDeliveryConditionRequestDto(
                        conditionType = conditionType,
                        inactivityPeriodDays = inactivityPeriodDays,
                        specificDate = specificDate,
                        leaveMessage = leaveMessage,
                    )
                val response = api.updateDeliveryCondition(body)
                UserMapper.toDeliveryCondition(response.requireData())
            }
    }

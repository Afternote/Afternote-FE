package com.afternote.core.data.repositoryImpl

import android.util.Log
import com.afternote.core.data.mapper.user.UserMapper
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
    ) : UserRepository {
        override suspend fun getMyProfile(userId: Long): Result<UserProfileModel> =
            runCatching {
                Log.d(TAG, "getMyProfile: userId=$userId")
                val response = api.getMyProfile(userId = userId)
                val data = response.requireData()
                Log.d(
                    TAG,
                    "getMyProfile: API data name='${data.name}' email='${data.email}' phone='${data.phone}'",
                )
                val profile = UserMapper.toUserProfile(data)
                Log.d(
                    TAG,
                    "getMyProfile: mapped profile name='${profile.name}' email='${profile.email}'",
                )
                profile
            }

        override suspend fun updateMyProfile(
            userId: Long,
            name: String?,
            phone: String?,
            profileImageUrl: String?,
        ): Result<UserProfileModel> =
            runCatching {
                Log.d(TAG, "updateMyProfile: userId=$userId, name=$name, phone=$phone")
                val response =
                    api.updateMyProfile(
                        userId = userId,
                        body =
                            UserUpdateProfileRequest(
                                name = name,
                                phone = phone,
                                profileImageUrl = profileImageUrl,
                            ),
                    )
                Log.d(TAG, "updateMyProfile: response=$response")
                UserMapper.toUserProfile(response.requireData())
            }

        override suspend fun withdrawAccount(): Result<Unit> =
            runCatching {
                Log.d(TAG, "withdrawAccount: request")
                val response = api.withdrawAccount()
                response.requireStatus()
                Log.d(TAG, "withdrawAccount: success")
                Unit
            }

        override suspend fun getMyPushSettings(userId: Long): Result<PushSettings> =
            runCatching {
                Log.d(TAG, "getMyPushSettings: userId=$userId")
                val response = api.getMyPushSettings(userId = userId)
                Log.d(TAG, "getMyPushSettings: response=$response")
                UserMapper.toPushSettings(response.requireData())
            }

        override suspend fun updateMyPushSettings(
            userId: Long,
            timeLetter: Boolean?,
            mindRecord: Boolean?,
            afterNote: Boolean?,
        ): Result<PushSettings> =
            runCatching {
                Log.d(
                    TAG,
                    "updateMyPushSettings: userId=$userId, timeLetter=$timeLetter, mindRecord=$mindRecord, afterNote=$afterNote",
                )
                val response =
                    api.updateMyPushSettings(
                        userId = userId,
                        body =
                            UserUpdatePushSettingRequest(
                                timeLetter = timeLetter,
                                mindRecord = mindRecord,
                                afterNote = afterNote,
                            ),
                    )
                Log.d(TAG, "updateMyPushSettings: response=$response")
                UserMapper.toPushSettings(response.requireData())
            }

        override suspend fun getReceivers(userId: Long): Result<List<ReceiverListItem>> =
            runCatching {
                Log.d(TAG, "getReceivers: userId=$userId")
                val response = api.getReceivers(userId = userId)
                Log.d(TAG, "getReceivers: response=$response")
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
                Log.d(TAG, "registerReceiver: name=$name, relation=$relation")
                val response =
                    api.registerReceiver(
                        RegisterReceiverRequestDto(
                            name = name,
                            relation = relation,
                            phone = phone,
                            email = email,
                        ),
                    )
                Log.d(TAG, "registerReceiver: response=$response")
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
                Log.d(TAG, "getReceiverDetail: receiverId=$receiverId")
                val response = api.getReceiverDetail(receiverId = receiverId)
                Log.d(TAG, "getReceiverDetail: response=$response")
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
                Log.d(TAG, "updateReceiver: receiverId=$receiverId, name=$name")
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
                Log.d(TAG, "updateReceiver: response=$response")
                if (response.status !in 200..299) {
                    throw ApiException(
                        status = response.status,
                        code = response.code,
                        message = response.message ?: "Status 200 이상 299 이하가 아님",
                    )
                }
                Unit
            }

        override suspend fun getReceiverDailyQuestions(
            receiverId: Long,
            page: Int,
            size: Int,
        ): Result<ReceiverDailyQuestionsResult> =
            runCatching {
                Log.d(TAG, "getReceiverDailyQuestions: receiverId=$receiverId, page=$page, size=$size")
                val response =
                    api.getReceiverDailyQuestions(
                        receiverId = receiverId,
                        page = page,
                        size = size,
                    )
                Log.d(TAG, "getReceiverDailyQuestions: response=$response")
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
                Log.d(TAG, "getReceiverMindRecords: receiverId=$receiverId, page=$page, size=$size")
                val response =
                    api.getReceiverMindRecords(
                        receiverId = receiverId,
                        page = page,
                        size = size,
                    )
                Log.d(TAG, "getReceiverMindRecords: response=$response")
                val body = response.requireData()
                val items = (body.items).map(UserMapper::toReceiverMindRecordItem)
                UserMapper.toReceiverMindRecordsResult(items = items, hasNext = body.hasNext)
            }

        override suspend fun getDeliveryCondition(): Result<DeliveryCondition> =
            runCatching {
                Log.d(TAG, "getDeliveryCondition: request")
                val response = api.getDeliveryCondition()
                Log.d(TAG, "getDeliveryCondition: response=$response")
                UserMapper.toDeliveryCondition(response.requireData())
            }

        override suspend fun updateDeliveryCondition(
            conditionType: DeliveryConditionType,
            inactivityPeriodDays: Int?,
            specificDate: String?,
            leaveMessage: String?,
        ): Result<DeliveryCondition> =
            runCatching {
                Log.d(TAG, "updateDeliveryCondition: conditionType=$conditionType")
                val body =
                    UserMapper.toDeliveryConditionRequestDto(
                        conditionType = conditionType,
                        inactivityPeriodDays = inactivityPeriodDays,
                        specificDate = specificDate,
                        leaveMessage = leaveMessage,
                    )
                val response = api.updateDeliveryCondition(body)
                Log.d(TAG, "updateDeliveryCondition: response=$response")
                UserMapper.toDeliveryCondition(response.requireData())
            }

        companion object {
            private const val TAG = "UserRepositoryImpl"
        }
    }

package com.afternote.core.data.mapper.user

import com.afternote.core.model.setting.ConnectedAccounts
import com.afternote.core.model.setting.DeliveryCondition
import com.afternote.core.model.setting.DeliveryConditionType
import com.afternote.core.model.setting.PushSettings
import com.afternote.core.model.setting.ReceiverDetail
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.model.user.UserProfileModel
import com.afternote.core.network.dto.ConnectedAccountsResponseDto
import com.afternote.core.network.dto.DeliveryConditionRequestDto
import com.afternote.core.network.dto.DeliveryConditionResponseDto
import com.afternote.core.network.dto.DeliveryConditionTypeDto
import com.afternote.core.network.dto.ReceiverDetailResponseDto
import com.afternote.core.network.dto.ReceiverItemDto
import com.afternote.core.network.dto.UserPushSettingResponse
import com.afternote.core.network.dto.UserResponse

// TODO: 리팩토링

/**
 * User DTO를 Domain 모델로 변환. (스웨거 기준)
 */
object UserMapper {
    fun toUserProfile(dto: UserResponse): UserProfileModel =
        UserProfileModel(
            name = dto.name,
            email = dto.email,
            phone = dto.phone,
            profileImageUrl = dto.profileImageUrl,
        )

    fun toPushSettings(dto: UserPushSettingResponse): PushSettings =
        PushSettings(
            timeLetter = dto.timeLetter,
            mindRecord = dto.mindRecord,
            afterNote = dto.afterNote,
        )

    fun toReceiverListItem(dto: ReceiverItemDto): ReceiverListItem =
        ReceiverListItem(
            receiverId = dto.receiverId,
            name = dto.name,
            relation = dto.relation,
            mindRecordDeliveryEnabled = dto.mindRecordDeliveryEnabled,
        )

    fun toReceiverDetail(dto: ReceiverDetailResponseDto): ReceiverDetail =
        ReceiverDetail(
            receiverId = dto.receiverId,
            name = dto.name,
            relation = dto.relation,
            phone = dto.phone,
            email = dto.email,
            dailyQuestionCount = dto.dailyQuestionCount,
            timeLetterCount = dto.timeLetterCount,
            afterNoteCount = dto.afterNoteCount,
        )

    fun toDeliveryCondition(dto: DeliveryConditionResponseDto): DeliveryCondition =
        DeliveryCondition(
            conditionType = dto.conditionType.toDomain(),
            inactivityPeriodDays = dto.inactivityPeriodDays,
            specificDate = dto.specificDate,
            leaveMessage = dto.leaveMessage,
            conditionFulfilled = dto.conditionFulfilled,
            conditionMet = dto.conditionMet,
        )

    fun toDeliveryConditionRequestDto(
        conditionType: DeliveryConditionType,
        inactivityPeriodDays: Int?,
        specificDate: String?,
        leaveMessage: String? = null,
    ): DeliveryConditionRequestDto =
        DeliveryConditionRequestDto(
            conditionType = conditionType.toDto(),
            inactivityPeriodDays = inactivityPeriodDays,
            specificDate = specificDate,
            leaveMessage = leaveMessage,
        )

    private fun DeliveryConditionTypeDto.toDomain(): DeliveryConditionType =
        when (this) {
            DeliveryConditionTypeDto.NONE -> DeliveryConditionType.NONE
            DeliveryConditionTypeDto.DEATH_CERTIFICATE -> DeliveryConditionType.DEATH_CERTIFICATE
            DeliveryConditionTypeDto.INACTIVITY -> DeliveryConditionType.INACTIVITY
            DeliveryConditionTypeDto.SPECIFIC_DATE -> DeliveryConditionType.SPECIFIC_DATE
        }

    private fun DeliveryConditionType.toDto(): DeliveryConditionTypeDto =
        when (this) {
            DeliveryConditionType.NONE -> DeliveryConditionTypeDto.NONE
            DeliveryConditionType.DEATH_CERTIFICATE -> DeliveryConditionTypeDto.DEATH_CERTIFICATE
            DeliveryConditionType.INACTIVITY -> DeliveryConditionTypeDto.INACTIVITY
            DeliveryConditionType.SPECIFIC_DATE -> DeliveryConditionTypeDto.SPECIFIC_DATE
        }

    fun toConnectedAccounts(dto: ConnectedAccountsResponseDto): ConnectedAccounts =
        ConnectedAccounts(
            local = dto.local,
            google = dto.google,
            naver = dto.naver,
            kakao = dto.kakao,
            apple = dto.apple,
            localEmail = dto.localEmail,
            googleEmail = dto.googleEmail,
            naverEmail = dto.naverEmail,
            kakaoEmail = dto.kakaoEmail,
            appleEmail = dto.appleEmail,
        )
}

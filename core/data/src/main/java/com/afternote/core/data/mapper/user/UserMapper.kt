package com.afternote.core.data.mapper.user

import com.afternote.core.network.dto.DeliveryConditionResponseDto
import com.afternote.core.network.dto.DeliveryConditionTypeDto
import com.afternote.core.network.dto.ReceiverDetailResponseDto
import com.afternote.core.network.dto.ReceiverListResponseDto
import com.afternote.core.network.dto.UserConnectedAccountResponseDto
import com.afternote.core.network.dto.UserCreateReceiverResponseDto
import com.afternote.core.network.dto.UserPatchReceiverResponseDto
import com.afternote.core.network.dto.UserPushSettingResponseDto
import com.afternote.core.network.dto.UserResponseDto
import com.afternote.core.model.user.DeliveryCondition
import com.afternote.core.model.user.DeliveryConditionType
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserPushSetting

// ========================================
// Enum Mapper
// ========================================

fun DeliveryConditionTypeDto.toDomain(): DeliveryConditionType =
    when (this) {
        DeliveryConditionTypeDto.NONE -> DeliveryConditionType.NONE
        DeliveryConditionTypeDto.INACTIVITY -> DeliveryConditionType.INACTIVITY
        DeliveryConditionTypeDto.SPECIFIC_DATE -> DeliveryConditionType.SPECIFIC_DATE
    }

fun DeliveryConditionType.toDto(): DeliveryConditionTypeDto =
    when (this) {
        DeliveryConditionType.NONE -> DeliveryConditionTypeDto.NONE
        DeliveryConditionType.INACTIVITY -> DeliveryConditionTypeDto.INACTIVITY
        DeliveryConditionType.SPECIFIC_DATE -> DeliveryConditionTypeDto.SPECIFIC_DATE
    }

// ========================================
// Response Mapper (DTO → Domain)
// ========================================

fun UserResponseDto.toDomain(): User =
    User(
        name = name,
        email = email,
        phone = phone,
        profileImageUrl = profileImageUrl,
    )

fun ReceiverListResponseDto.toDomain(): Receiver =
    Receiver(
        receiverId = receiverId,
        name = name,
        relation = relation,
        authCode = authCode,
    )

fun ReceiverDetailResponseDto.toDomain(): ReceiverDetail =
    ReceiverDetail(
        receiverId = receiverId,
        name = name,
        relation = relation,
        phone = phone,
        email = email,
        dailyQuestionCount = dailyQuestionCount,
        timeLetterCount = timeLetterCount,
        afterNoteCount = afterNoteCount,
        message = message,
        authCode = authCode,
    )

fun UserCreateReceiverResponseDto.toDomain(): ReceiverCreated =
    ReceiverCreated(
        receiverId = receiverId,
        authCode = authCode,
    )

fun UserPatchReceiverResponseDto.toDomain(): Receiver =
    Receiver(
        receiverId = receiverId,
        name = name,
        relation = relation,
        authCode = "",
    )

fun UserPushSettingResponseDto.toDomain(): UserPushSetting =
    UserPushSetting(
        timeLetter = timeLetter,
        mindRecord = mindRecord,
        afterNote = afterNote,
    )

fun UserConnectedAccountResponseDto.toDomain(): UserConnectedAccount =
    UserConnectedAccount(
        local = local,
        google = google,
        naver = naver,
        kakao = kakao,
        apple = apple,
        localEmail = localEmail,
        googleEmail = googleEmail,
        naverEmail = naverEmail,
        kakaoEmail = kakaoEmail,
        appleEmail = appleEmail,
    )

fun DeliveryConditionResponseDto.toDomain(): DeliveryCondition =
    DeliveryCondition(
        conditionType = conditionType.toDomain(),
        inactivityPeriodDays = inactivityPeriodDays,
        specificDate = specificDate,
        conditionFulfilled = conditionFulfilled,
        conditionMet = conditionMet,
    )

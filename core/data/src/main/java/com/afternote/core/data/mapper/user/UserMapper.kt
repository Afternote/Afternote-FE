package com.afternote.core.data.mapper.user

import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.model.user.User
import com.afternote.core.network.dto.ReceiverDetailDto
import com.afternote.core.network.dto.ReceiverListDto
import com.afternote.core.network.dto.UserCreateReceiverDto
import com.afternote.core.network.dto.UserDto
import com.afternote.core.network.dto.UserPatchReceiverDto

// ========================================
// Enum Mapper
// ========================================

// ========================================
// Response Mapper (DTO → Domain)
// ========================================

fun UserDto.toDomain(): User =
    User(
        name = name,
        email = email,
        phone = phone,
        profileImageUrl = profileImageUrl,
    )

fun ReceiverListDto.toDomain(): Receiver =
    Receiver(
        receiverId = receiverId,
        name = name,
        relation = relation,
        authCode = authCode,
    )

fun ReceiverDetailDto.toDomain(): ReceiverDetail =
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

fun UserCreateReceiverDto.toDomain(): ReceiverCreated =
    ReceiverCreated(
        receiverId = receiverId,
        authCode = authCode,
    )

fun UserPatchReceiverDto.toDomain(): Receiver =
    Receiver(
        receiverId = receiverId,
        name = name,
        relation = relation,
        authCode = "",
    )

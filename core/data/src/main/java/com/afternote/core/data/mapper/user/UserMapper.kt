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

/** 목록 응답의 `relation` 은 DB 가 null 을 허용해 비어 올 수 있다 — 빈 문자열로 채워 [Receiver] 의 문자열 계약을 유지한다 (#2105). */
internal fun ReceiverListDto.toDomain(): Receiver =
    Receiver(
        receiverId = receiverId,
        name = name,
        relation = relation.orEmpty(),
    )

/**
 * 상세 응답의 nullable `relation` 을 빈 문자열로 옮긴다 (#2155).
 * 수정 화면은 빈 관계를 선택 안 함으로 그린다.
 */
fun ReceiverDetailDto.toDomain(): ReceiverDetail =
    ReceiverDetail(
        receiverId = receiverId,
        name = name,
        relation = relation.orEmpty(),
        phone = phone,
        email = email,
        dailyQuestionCount = dailyQuestionCount,
        timeLetterCount = timeLetterCount,
        afterNoteCount = afterNoteCount,
        message = message,
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
    )

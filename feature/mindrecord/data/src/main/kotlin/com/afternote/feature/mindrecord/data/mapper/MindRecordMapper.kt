package com.afternote.feature.mindrecord.data.mapper

import com.afternote.feature.mindrecord.data.dto.ReceiverDailyQuestionItemDto
import com.afternote.feature.mindrecord.data.dto.ReceiverDiaryItemDto
import com.afternote.feature.mindrecord.domain.model.MindRecordSummary
import com.afternote.feature.mindrecord.domain.model.MindRecordType

// 서버 createdAt 은 "yyyy.MM.dd 요일" — 기간 필터/정렬용 recordDate 는 ISO(yyyy-MM-dd)로 정규화.
private fun String.toIsoDate(): String = take(10).replace('.', '-')

fun ReceiverDailyQuestionItemDto.toDomain(): MindRecordSummary =
    MindRecordSummary(
        id = userDailyQuestionId,
        type = MindRecordType.DAILY_QUESTION,
        title = title,
        content = content,
        recordDate = createdAt.toIsoDate(),
        // 데일리질문 수신자 응답은 isDraft 미노출 — 서버가 draft 를 전달 대상에서 제외한다고 가정.
        isDraft = false,
        createdAt = createdAt,
        imageUrl = imageUrl,
    )

fun ReceiverDiaryItemDto.toDomain(): MindRecordSummary =
    MindRecordSummary(
        id = diaryId,
        type = MindRecordType.DIARY,
        title = title,
        content = content,
        recordDate = date.ifBlank { createdAt.toIsoDate() },
        isDraft = isDraft,
        createdAt = createdAt,
        imageUrl = imageUrl,
    )

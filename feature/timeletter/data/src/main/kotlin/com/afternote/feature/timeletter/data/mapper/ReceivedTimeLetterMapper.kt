package com.afternote.feature.timeletter.data.mapper

import com.afternote.feature.timeletter.data.dto.ReceivedTimeLetterDto
import com.afternote.feature.timeletter.data.dto.ReceivedTimeLetterListDto
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetter
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetterList

fun ReceivedTimeLetterDto.toDomain(): ReceivedTimeLetter =
    ReceivedTimeLetter(
        id = id,
        timeLetterReceiverId = timeLetterReceiverId,
        title = title,
        blocks = blocks.map { it.toDomain() },
        sendAt = sendAt,
        status = status.toDomain(),
        senderName = senderName,
        deliveredAt = deliveredAt,
        createdAt = createdAt,
        // 공개 전엔 서버가 isRead를 null로 보낼 수 있다 — 도메인은 non-null 계약이라 미확정 상태를
        // 안전한 쪽(false=안 읽음)으로 접어 화면이 잘못 "읽음"으로 표시되지 않게 한다.
        isRead = isRead ?: false,
    )

fun ReceivedTimeLetterListDto.toDomain(): ReceivedTimeLetterList =
    ReceivedTimeLetterList(
        timeLetters = timeLetters.map { it.toDomain() },
        totalCount = totalCount,
    )

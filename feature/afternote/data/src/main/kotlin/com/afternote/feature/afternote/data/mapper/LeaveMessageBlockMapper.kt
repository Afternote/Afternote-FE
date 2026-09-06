package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.LeaveMessageBlockDto
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock

/**
 * 응답 → 도메인. 본문이 없는 블록은 표시할 내용이 없어 버린다.
 * (서버 검증이 공백 본문을 막지만, 레거시 값을 감싼 블록까지 신뢰하지는 않는다.)
 */
fun List<LeaveMessageBlockDto>?.toLeaveMessageBlocks(): List<LeaveMessageBlock> =
    this
        ?.mapNotNull { dto ->
            val body = dto.body?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            LeaveMessageBlock(title = dto.title, body = body)
        }.orEmpty()

/** 도메인 → 생성 요청. 남길 말씀이 없으면 빈 배열 대신 필드 자체를 빼도록 null 로 접는다. */
fun List<LeaveMessageBlock>.toDto(): List<LeaveMessageBlockDto>? =
    map { LeaveMessageBlockDto(title = it.title, body = it.body) }
        .takeIf { it.isNotEmpty() }

/**
 * 도메인 → 수정 요청. **[toDto] 처럼 빈 배열을 null 로 접지 않는다** (#1617).
 *
 * 수정은 부분 갱신이라 `null`(안 건드림 → 키 생략)과 `[]`(전부 삭제)가 서로 다른 지시다.
 * 접어 버리면 마지막 말씀을 전부 지운 저장이 「유지」로 흡수돼 서버에 반영되지 않는다.
 */
fun List<LeaveMessageBlock>?.toPatchDto(): List<LeaveMessageBlockDto>? =
    this?.map { LeaveMessageBlockDto(title = it.title, body = it.body) }

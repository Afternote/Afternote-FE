package com.afternote.feature.receiver.data.mapper

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.data.mapper.afternoteTypeFromServerCategory
import com.afternote.feature.afternote.data.mapper.formatDateFromServer
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteDto
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteListDto
import com.afternote.feature.receiver.domain.model.AfterNoteListItem
import com.afternote.feature.receiver.domain.model.AfterNotesListResult

private const val RECEIVER_STAGE_KEY = "receiver_stage"
private const val RECEIVER_LIST_DECODING_STAGE = "receiver_list_decoding"
private const val RECEIVER_LIST_MAPPING_STAGE = "receiver_list_mapping"
private const val REJECTED_ITEM_COUNT_KEY = "rejected_item_count"

/** category 키가 누락됐거나 문자열이 아니어서 목록 디코딩 중 제외된 원소가 있음을 알리는 신호. */
internal class ReceiverListDecodingFailure : RuntimeException()

/**
 * 목록 응답에 도메인으로 변환할 수 없는 category 가 포함됐음을 알리는 non-fatal 신호.
 *
 * 서버 원문을 예외에 담지 않는다. 응답 내 식별자·제목·category 가 텔레메트리로
 * 흘러갈 수 있기 때문이다.
 */
internal class ReceiverListMappingFailure : RuntimeException()

/**
 * 서버 category 를 해석할 수 없으면 항목 하나만 목록에서 제외한다.
 *
 * 와이어 계약에 맞는 미래 값은 받아들이되 도메인에는 임의의 종류를 채우지 않는다. 그래야 필터·아이콘·
 * 상세 라우팅이 다른 종류로 조용히 왜곡되지 않는다. category 누락·null은 목록 serializer가 먼저 제외한다.
 */
fun ReceivedAfternoteDto.toDomainOrNull(): AfterNoteListItem? {
    val resolvedType = afternoteTypeFromServerCategory(category) ?: return null
    return AfterNoteListItem(
        id = id,
        serviceName = title,
        type = resolvedType,
        lastUpdatedAt = createdAt?.let { formatDateFromServer(it) },
    )
}

fun List<ReceivedAfternoteDto>.toReceiverDomainList(errorReporter: ErrorReporter): List<AfterNoteListItem> {
    var rejectedItemCount = 0
    val items =
        mapNotNull { dto ->
            dto.toDomainOrNull() ?: run {
                rejectedItemCount += 1
                null
            }
        }

    if (rejectedItemCount > 0) {
        errorReporter.recordFailure(
            throwable = ReceiverListMappingFailure(),
            attributes =
                mapOf(
                    RECEIVER_STAGE_KEY to RECEIVER_LIST_MAPPING_STAGE,
                    REJECTED_ITEM_COUNT_KEY to rejectedItemCount.toString(),
                ),
        )
    }

    return items
}

fun ReceivedAfternoteListDto.toReceiverDomainList(errorReporter: ErrorReporter): List<AfterNoteListItem> {
    if (decodingRejectedItemCount > 0) {
        errorReporter.recordFailure(
            throwable = ReceiverListDecodingFailure(),
            attributes =
                mapOf(
                    RECEIVER_STAGE_KEY to RECEIVER_LIST_DECODING_STAGE,
                    REJECTED_ITEM_COUNT_KEY to decodingRejectedItemCount.toString(),
                ),
        )
    }

    return afternotes.toReceiverDomainList(errorReporter)
}

fun ReceivedAfternoteListDto.toDomainResult(errorReporter: ErrorReporter): AfterNotesListResult =
    AfterNotesListResult(
        items = toReceiverDomainList(errorReporter),
        totalCount = totalCount,
    )

package com.afternote.feature.receiver.data.reporting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

internal const val RECEIVER_LIST_DECODING_STAGE = "receiver_list_decoding"
internal const val RECEIVER_LIST_MAPPING_STAGE = "receiver_list_mapping"

private const val RECEIVER_MAPPER_PACKAGE = "com.afternote.feature.receiver.data.mapper."
private const val RECEIVER_STAGE_KEY = "receiver_stage"
private const val REJECTED_ITEM_COUNT_KEY = "rejected_item_count"
private const val ERROR_TYPE_KEY = "error_type"

/**
 * 수신 목록 텔레메트리의 «분류 계약» 단언 (#1832).
 *
 * 디코딩·매핑 실패를 알리는 예외 타입은 mapper 파일 안에 갇힌 `private` 선언이라 테스트가 이름으로
 * 참조하지 않는다. 대신 [com.afternote.core.common.reporting.ErrorReporter] 가 기록에 붙이는 속성으로
 * 같은 계약을 검증한다 — 지켜야 할 것은 **두 단계가 각자 전용 타입으로 갈려 콘솔에서 구분된다**는
 * 사실이지 타입 이름이 아니다. `error_type` 은 원문을 버리는 redact 정책이 남기는 유일한 타입 정보다.
 *
 * @param expectedRejectedCounts 단계(`receiver_stage`) → 그 단계가 보고해야 할 제외 건수.
 *                               여기 없는 단계가 기록됐거나 빠졌으면 실패한다.
 */
internal fun RecordingErrorReporter.assertReceiverListFailureContract(expectedRejectedCounts: Map<String, String>) {
    val failuresByStage = failures.associateBy { it.attributes[RECEIVER_STAGE_KEY].orEmpty() }
    assertEquals(expectedRejectedCounts.keys, failuresByStage.keys)

    val typesByStage =
        expectedRejectedCounts.mapValues { (stage, expectedRejectedCount) ->
            val failure = requireNotNull(failuresByStage[stage]) { "$stage 실패가 기록되지 않았다" }
            assertEquals("$stage 제외 건수", expectedRejectedCount, failure.attributes[REJECTED_ITEM_COUNT_KEY])

            val errorType = requireNotNull(failure.attributes[ERROR_TYPE_KEY]) { "$stage 에 error_type 이 없다" }
            assertTrue(
                "$stage 은 수신 목록 전용 실패 타입으로 기록돼야 한다: $errorType",
                errorType.startsWith(RECEIVER_MAPPER_PACKAGE),
            )
            errorType
        }

    assertTrue(
        "단계마다 다른 실패 타입으로 갈려야 콘솔에서 원인을 구분한다: $typesByStage",
        typesByStage.values.toSet().size == typesByStage.size,
    )
}

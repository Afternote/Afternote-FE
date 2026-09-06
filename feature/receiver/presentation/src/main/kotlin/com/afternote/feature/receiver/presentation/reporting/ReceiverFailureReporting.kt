package com.afternote.feature.receiver.presentation.reporting

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.receiver.domain.error.ReceiverFailure

/**
 * 수신자 흐름에서 실패가 발생한 지점.
 *
 * [reportingName] 은 리포팅 콘솔의 필터 값이다. 바꾸면 그 시점 이후 기록이 기존 이슈 그룹과
 * 끊겨 추이를 잃으므로, 단계 자체가 사라지지 않는 한 유지한다.
 *
 * 흐름마다 전용 enum 과 `<흐름>_stage` 키를 따로 둔다 — 키를 합치면 콘솔에서 한 키에 서로 다른
 * 값 목록이 섞인다. 아래 단계들은 수신자 홈이 app 에 있던 시절 `home_stage` 키로 기록되던 것을
 * 화면 이전(#546)과 함께 이 키로 옮긴 것이다. 내려받기 두 단계는 afternote 쪽 요약 화면이
 * `afternote_stage` 키로도 기록하던 것을 #615 이전과 함께 여기로 수렴시켰다(이중 기록 해소).
 * reportingName 은 그대로라 단계별 식별은 유지되고, 키 축 추이만 새로 시작한다(#544 도입
 * 직후라 손실 구간은 며칠 분).
 */
enum class ReceiverFailureStage(
    val reportingName: String,
) {
    /** 수신자 홈 로드 — 네 요청이 모두 실패해 화면이 에러로 떨어진 경우. */
    RECEIVER_HOME_LOAD("receiver_home_load"),

    /**
     * 수신자 홈 로드 — 일부 요청만 실패한 경우.
     *
     * 화면은 성공한 섹션을 유지하고 실패한 기록 섹션은 조회 실패로 구분해 표시한다.
     */
    RECEIVER_HOME_PARTIAL_LOAD("receiver_home_partial_load"),

    /** 수신자 홈에서 시작한 모든 기록 내려받기 — 서버에서 묶음을 받아오는 단계. */
    RECEIVED_EXPORT_DOWNLOAD("received_export_download"),

    /** 수신자 홈에서 시작한 모든 기록 내려받기 — 받아온 묶음을 기기에 저장하는 단계. */
    RECEIVED_EXPORT_SAVE("received_export_save"),
}

/**
 * 수신자 흐름의 handled 실패를 공통 키 규격으로 기록한다.
 *
 * [ReceiverFailure.ExportNotSupported]는 장애가 아닌 기능 상태이므로 기록하지 않는다.
 * 화면 안내와 무관한 보고 정책은 이 창구에서 적용한다.
 *
 * @param failedSources 한 화면이 여러 요청을 모아 그리는 탓에 일부만 깨진 경우, 어떤 항목이
 *   비었는지 남긴다. 요청별로 따로 기록하면 한 번의 네트워크 단절이 보관 한도(최근 8건) 를
 *   혼자 채워 버리므로, 한 건에 목록으로 묶는다. 비어 있으면 이 키는 붙지 않는다.
 */
fun ErrorReporter.recordReceiverFailure(
    stage: ReceiverFailureStage,
    throwable: Throwable,
    failedSources: List<String> = emptyList(),
) {
    if (throwable is ReceiverFailure.ExportNotSupported) return
    recordFailure(
        throwable = throwable,
        attributes =
            buildMap {
                put(KEY_RECEIVER_STAGE, stage.reportingName)
                if (failedSources.isNotEmpty()) {
                    put(KEY_RECEIVER_FAILED_SOURCES, failedSources.joinToString(separator = ","))
                }
            },
    )
}

private const val KEY_RECEIVER_STAGE = "receiver_stage"

private const val KEY_RECEIVER_FAILED_SOURCES = "receiver_failed_sources"

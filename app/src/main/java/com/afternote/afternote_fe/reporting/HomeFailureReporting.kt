package com.afternote.afternote_fe.reporting

import com.afternote.core.common.reporting.ErrorReporter

/**
 * 홈(발신자·수신자) 화면에서 실패가 발생한 지점.
 *
 * [reportingName] 은 리포팅 콘솔의 필터 값이다. 바꾸면 그 시점 이후 기록이 기존 이슈 그룹과
 * 끊겨 추이를 잃으므로, 단계 자체가 사라지지 않는 한 유지한다.
 */
enum class HomeFailureStage(
    val reportingName: String,
) {
    /** 발신자 홈 요약(이름·수신인 지정 여부·기록 수) 로드. */
    AUTHOR_SUMMARY_LOAD("author_summary_load"),

    /** 수신자 홈 로드 — 네 요청이 모두 실패해 화면이 에러로 떨어진 경우. */
    RECEIVER_HOME_LOAD("receiver_home_load"),

    /**
     * 수신자 홈 로드 — 일부 요청만 실패한 경우.
     *
     * 화면은 실패한 항목을 0·빈 값으로 채우고 정상 진행하므로 사용자에게도, 콘솔에도 아무 신호가
     * 남지 않는 게 원래 동작이었다.
     */
    RECEIVER_HOME_PARTIAL_LOAD("receiver_home_partial_load"),

    /** 수신자 홈에서 시작한 모든 기록 내려받기 — 서버에서 묶음을 받아오는 단계. */
    RECEIVED_EXPORT_DOWNLOAD("received_export_download"),

    /** 수신자 홈에서 시작한 모든 기록 내려받기 — 받아온 묶음을 기기에 저장하는 단계. */
    RECEIVED_EXPORT_SAVE("received_export_save"),
}

/**
 * 홈 흐름의 handled 실패를 공통 키 규격으로 기록한다.
 *
 * @param failedSources 한 화면이 여러 요청을 모아 그리는 탓에 일부만 깨진 경우, 어떤 항목이
 *   비었는지 남긴다. 요청별로 따로 기록하면 한 번의 네트워크 단절이 보관 한도(최근 8건) 를
 *   혼자 채워 버리므로, 한 건에 목록으로 묶는다. 비어 있으면 이 키는 붙지 않는다.
 */
fun ErrorReporter.recordHomeFailure(
    stage: HomeFailureStage,
    throwable: Throwable,
    failedSources: List<String> = emptyList(),
) {
    recordFailure(
        throwable = throwable,
        attributes =
            buildMap {
                put(KEY_HOME_STAGE, stage.reportingName)
                if (failedSources.isNotEmpty()) {
                    put(KEY_HOME_FAILED_SOURCES, failedSources.joinToString(separator = ","))
                }
            },
    )
}

private const val KEY_HOME_STAGE = "home_stage"

private const val KEY_HOME_FAILED_SOURCES = "home_failed_sources"

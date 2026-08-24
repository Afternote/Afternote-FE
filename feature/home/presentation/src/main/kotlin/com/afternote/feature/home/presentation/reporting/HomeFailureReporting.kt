package com.afternote.feature.home.presentation.reporting

import com.afternote.core.common.reporting.ErrorReporter

/**
 * 발신자 홈 화면에서 실패가 발생한 지점.
 *
 * [reportingName] 은 리포팅 콘솔의 필터 값이다. 바꾸면 그 시점 이후 기록이 기존 이슈 그룹과
 * 끊겨 추이를 잃으므로, 단계 자체가 사라지지 않는 한 유지한다.
 *
 * 수신자 홈 단계들은 화면과 함께 `feature:receiver:presentation` 의 ReceiverFailureReporting
 * (`receiver_stage` 키)으로 분리됐다.
 */
enum class HomeFailureStage(
    val reportingName: String,
) {
    /** 발신자 홈 요약(이름·수신인 지정 여부·기록 수) 로드. */
    AUTHOR_SUMMARY_LOAD("author_summary_load"),
}

/** 발신자 홈 흐름의 handled 실패를 공통 키 규격으로 기록한다. */
fun ErrorReporter.recordHomeFailure(
    stage: HomeFailureStage,
    throwable: Throwable,
) {
    recordFailure(
        throwable = throwable,
        attributes = mapOf(KEY_HOME_STAGE to stage.reportingName),
    )
}

private const val KEY_HOME_STAGE = "home_stage"

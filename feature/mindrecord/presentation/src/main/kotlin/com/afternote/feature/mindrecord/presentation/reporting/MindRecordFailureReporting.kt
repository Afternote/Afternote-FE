package com.afternote.feature.mindrecord.presentation.reporting

import com.afternote.core.common.reporting.ErrorReporter

/**
 * 마음의 기록 흐름에서 실패가 발생한 지점 (#964).
 *
 * 이 모듈은 종전에 `Log.e` 도 `ErrorReporter` 도 0건이었다 — 실패가 UI 상태로만 흡수돼
 * 릴리즈에서 무슨 일이 있었는지 알 방법이 없었다.
 *
 * [reportingName] 은 리포팅 콘솔의 필터 값이다. 바꾸면 그 시점 이후 기록이 기존 이슈 그룹과
 * 끊겨 추이를 잃으므로, 단계 자체가 사라지지 않는 한 유지한다.
 *
 * 흐름마다 전용 enum 과 `<흐름>_stage` 키를 따로 둔다 — 키를 합치면 콘솔에서 한 키에 서로
 * 다른 값 목록이 섞인다(애프터노트·수신자·홈이 각각 자기 키를 쓰는 것과 같은 이유다).
 */
enum class MindRecordFailureStage(
    val reportingName: String,
) {
    /**
     * 임시저장 목록 조회. 사용자가 «쓰다 만 글» 을 찾으러 들어오는 화면이라, 실패하면
     * 작성물이 사라진 것처럼 보인다 — #519 가 실제로 그 형태의 결함이었다.
     */
    DRAFT_LIST_LOAD("draft_list_load"),

    /**
     * 임시저장 삭제. 되돌릴 수 없는 동작이고, 부분 실패(일부만 지워짐)가 목록과 서버 상태를
     * 어긋나게 둔다.
     */
    DRAFT_DELETE("draft_delete"),

    /**
     * 수신자가 받은 기록 열람. 유가족이 «지금 못 여는» 상황이라 읽기 실패지만 승격 가치가
     * 높다 — 재현할 계정도 조건도 우리 손에 없어 실기 QA 로 잡히지 않는다.
     */
    RECEIVER_RECORD_LOAD("receiver_record_load"),
}

/**
 * 마음의 기록 흐름의 handled 실패를 공통 키 규격으로 기록한다.
 *
 * 사용자 입력 오류·사용자 취소처럼 오류가 아닌 경로는 호출부에서 걸러 넘기지 않는다.
 * 코루틴 취소는 [ErrorReporter] 가 창구에서 거르므로 호출부가 다시 막지 않아도 된다 —
 * 이 모듈의 저장소는 `runCatching` 으로 취소까지 실패로 바꿔 돌려주는 자리가 있다.
 */
fun ErrorReporter.recordMindRecordFailure(
    stage: MindRecordFailureStage,
    throwable: Throwable,
) {
    recordFailure(
        throwable = throwable,
        attributes = mapOf(KEY_MIND_RECORD_STAGE to stage.reportingName),
    )
}

private const val KEY_MIND_RECORD_STAGE = "mind_record_stage"

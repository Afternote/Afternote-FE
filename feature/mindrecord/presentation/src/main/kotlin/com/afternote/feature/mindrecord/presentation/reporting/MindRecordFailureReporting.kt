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
     * 일기 저장(정식 등록·임시저장 공통). 사용자가 방금 쓴 글이 서버에 닿지 못한 자리라
     * 재현이 어렵고, 실패하면 작성물이 그대로 사라진다 — 실기 QA 로는 잡히지 않는다.
     */
    DIARY_SUBMIT("diary_submit"),

    /**
     * 데일리질문 답변 저장. 위와 같은 성질이고, upsert 라 실패가 기존 임시저장 상태와도
     * 얽힌다 (#1018).
     */
    DAILY_QUESTION_SUBMIT("daily_question_submit"),

    /**
     * 작성 화면의 미디어 업로드. 실패하면 본문에서 그 첨부만 빠진 채 저장이 이어질 수 있어,
     * 사용자는 «올린 줄 알았는데 없는» 기록을 남기게 된다 (#716·#731).
     */
    MEDIA_UPLOAD("media_upload"),

    /**
     * 기록 목록 조회(일기·데일리질문). **화면이 오류로 바뀌는 실패만** 여기로 온다 —
     * 재진입 갱신 실패는 보고 있던 목록을 그대로 두므로 승격하지 않는다.
     *
     * 목록 실패는 흔한 편이라 전부 올리면 non-fatal 한도(최근 8건)를 잡음으로 채워
     * 실제 장애를 밀어낸다. 「사용자가 오류 화면을 마주한 경우」로 줄여 그 선을 지킨다 (#964).
     */
    RECORD_LIST_LOAD("record_list_load"),

    /**
     * 기록 삭제. 되돌릴 수 없는 동작이라 실패하면 목록과 서버 상태가 어긋난 채 남는다 —
     * 임시저장 삭제([DRAFT_DELETE])와 같은 성질이다.
     */
    RECORD_DELETE("record_delete"),

    /**
     * 저장된 기록 상세 열람 (#759). 「내가 쓴 기록이 안 열린다」 는 자리라, 읽기지만
     * 사용자 데이터에 닿지 못한 실패다.
     */
    RECORD_DETAIL_LOAD("record_detail_load"),

    /**
     * 작성 화면이 여는 **오늘의 질문**(신규)·**기존 답변**(수정) 조회 (#964).
     *
     * 목록 조회와 달리 여기 실패하면 화면이 오류 문구만 남고 **쓸 수가 없다** — 사용자가
     * 「오늘 질문이 안 뜬다」로 마주하는 자리다. 그래서 읽기지만 승격 대상이다.
     */
    DAILY_QUESTION_LOAD("daily_question_load"),

    /**
     * 추억 공간 집계 조회. 네 출처(일기 최근 3개월 + 데일리질문)를 병렬로 모으는 자리라
     * **부분 실패는 삼키고**(카드가 한 장이라도 차면 그대로 보여 준다), 합친 결과가 비었고
     * 실패 출처가 하나라도 있을 때만 올라온다.
     */
    MEMORY_SPACE_LOAD("memory_space_load"),

    /**
     * 주간 리포트 조회. 실패하면 탭 전체가 오류 화면이 되고, 서버가 Gemini 를 다시 호출하는
     * 자리라(Afternote-BE#118) 재시도 비용도 크다 — 무엇이 실패했는지 남지 않으면 그 비용이
     * 어디서 새는지 알 수 없다 (#1882).
     */
    WEEKLY_REPORT_LOAD("weekly_report_load"),

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
 * 「취소는 에러가 아니다」는 **정책**이라 계측 지점이 늘어도 한 곳에서 지킨다. 호출부마다
 * 걸러 달라고 하면 새 지점이 생길 때마다 빠진다.
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

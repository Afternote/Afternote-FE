package com.afternote.feature.afternote.presentation.reporting

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.receiver.domain.error.ReceiverFailure

/**
 * 애프터노트 흐름에서 실패가 발생한 지점.
 *
 * [reportingName] 은 리포팅 콘솔의 필터 값이다. 바꾸면 그 시점 이후 기록이 기존 이슈 그룹과
 * 끊겨 추이를 잃으므로, 단계 자체가 사라지지 않는 한 유지한다.
 *
 * 흐름마다 전용 enum 과 `<흐름>_stage` 키를 따로 둔다 — 키를 합치면 콘솔에서 한 키에 서로 다른
 * 값 목록이 섞인다.
 */
enum class AfternoteFailureStage(
    val reportingName: String,
) {
    /** 애프터노트 등록·수정 제출 — 입력 검증 실패는 제외(사용자 입력 오류). */
    SAVE("save"),

    /** 수정 진입 시 기존 애프터노트 로드 — 여기서 깨지면 빈 폼으로 덮어쓸 위험이 있다. */
    PREFILL_LOAD("prefill_load"),

    /**
     * 신규 작성 진입 시 작성자가 등록한 수신자 목록 조회.
     *
     * 실패해도 화면에는 수신자 자리가 비어 보일 뿐이라 계측하지 않으면 흔적이 남지 않는다.
     * 수신자는 선택 항목이라 저장이 막히지는 않지만, 지정하려던 사용자에게는 등록해 둔 수신자가
     * 사라져 보이는 무음 결함이 된다.
     */
    AUTHOR_RECEIVER_LOAD("author_receiver_load"),

    /**
     * 수신자 선택 화면(#540)의 목록 조회 — [AUTHOR_RECEIVER_LOAD] 와 같은 `GET users/receivers` 지만
     * 실패한 화면이 달라 키를 나눈다. 이쪽은 화면이 실패를 보여 주고 재시도를 받는다.
     */
    RECEIVER_SELECT_LOAD("receiver_select_load"),

    MEMORIAL_THUMBNAIL_UPLOAD("memorial_thumbnail_upload"),

    /**
     * 선택한 영상에서 썸네일 프레임을 뽑는 로컬 디코딩 — 서버 호출 이전.
     *
     * 사용자에게는 썸네일 자리가 비어 보일 뿐 오류로 알려주지 않아, 계측하지 않으면 흔적이 남지 않는다.
     * 반대로 원격 썸네일의 Coil 로드 실패는 계측하지 않는다 — 일시적 네트워크 실패로도 나서
     * 보관 한도(최근 8건)를 잡음이 차지한다.
     */
    MEMORIAL_THUMBNAIL_EXTRACT("memorial_thumbnail_extract"),

    /**
     * 즉석 촬영 인텐트를 띄우지 못한 실패 — 결과 파일을 못 만들거나(저장공간) 받아 줄 앱이 없거나.
     *
     * 사용자에게는 "카메라를 사용할 수 없습니다" 한 줄만 나가 둘이 구분되지 않는다. 제보가 왔을 때
     * 어느 쪽인지 가르려면 예외 자체가 남아 있어야 한다.
     */
    MEMORIAL_CAPTURE_LAUNCH("memorial_capture_launch"),

    /**
     * 애프터노트 목록(Paging) 로드 — refresh·append 를 함께 싣는다.
     *
     * 목록은 실패해도 이미 그려 둔 페이지가 남아 «표시만 사라지는» 무음 결함이 된다. 한 번 실패한
     * 뒤 사용자가 재시도를 연타하면 같은 실패가 반복 기록되므로 호출부([com.afternote.feature.afternote.presentation.home.AfternoteHomeViewModel])
     * 가 중복을 억제한다 — 보관 한도(최근 8건)를 한 장애가 통째로 차지하지 않게 하기 위함이다.
     */
    LIST_LOAD("list_load"),

    DETAIL_LOAD("detail_load"),

    /**
     * 애프터노트 삭제 제출. `DETAIL` 은 삭제 종류가 아니라 화면 접두사다 — 삭제 진입점이 상세
     * 3종(갤러리·추억·계정)뿐이라 [DETAIL_LOAD] 와 같은 접두사를 쓴다. 목록 등에 삭제가 생기면 분화한다.
     */
    DETAIL_DELETE("detail_delete"),

    RECEIVED_DETAIL_LOAD("received_detail_load"),

    /** 수신자 본인 확인 인증번호 발송 — 수신자 이메일 미등록은 제외(사용자 입력 오류). */
    RECEIVER_EMAIL_CODE_SEND("receiver_email_code_send"),

    /** 수신자 본인 확인 인증번호 검증 — 만료·불일치는 제외(사용자 입력 오류). */
    RECEIVER_EMAIL_VERIFY("receiver_email_verify"),

    /** 마스터 키 검증 — 키 오타처럼 서버가 사유를 설명한 거절은 제외. */
    MASTER_KEY_VERIFY("master_key_verify"),

    DOCUMENT_UPLOAD("document_upload"),

    /** 열람 신청 제출 — 서버가 사유를 내려준 거절(이미 대기 중 등)은 제외. */
    DELIVERY_SUBMIT("delivery_submit"),

    /** 추억 플레이리스트 곡 검색. */
    MUSIC_SEARCH("music_search"),

    /**
     * 수신 추억 플레이리스트 전체보기 로드. 전용 endpoint 가 없어 [RECEIVED_DETAIL_LOAD] 와 같은 상세
     * 조회를 부르고 응답의 `playlist.songs` 만 쓴다 — 키를 나눈 건 실패한 화면이 달라서다.
     */
    RECEIVED_PLAYLIST_LOAD("received_playlist_load"),

    /** 발신자 상세의 열람 인증 상태 조회. */
    SENDER_STATUS_LOAD("sender_status_load"),
}

/**
 * 애프터노트 흐름의 handled 실패를 공통 키 규격으로 기록한다.
 *
 * 사용자 입력 오류·사용자 취소처럼 오류가 아닌 경로는 호출부에서 걸러 넘기지 않는다.
 */
fun ErrorReporter.recordAfternoteFailure(
    stage: AfternoteFailureStage,
    throwable: Throwable,
) {
    recordFailure(
        throwable = throwable,
        attributes = mapOf(KEY_AFTERNOTE_STAGE to stage.reportingName),
    )
}

/**
 * **수신자 흐름에서** 이 실패를 텔레메트리에 기록해야 하는지.
 *
 * `Receiver` 는 타입이 아니라 적용 맥락이다 — `Throwable` 확장이라 네트워크 타임아웃처럼 수신자와
 * 무관한 예외도 들어오고, 그런 실패는 기록 대상(true)이다. 타입으로 좁히는 건 아래 제외 판정뿐이다.
 *
 * 제외 대상은 "서버가 예상하고 처리한 거절" 하나다 — 앱이 정상 동작한 결과라 고칠 것이 없고, 무엇보다
 * Crashlytics 가 non-fatal 을 **최근 8건만 보관하고 초과분은 오래된 것부터 버려서**(코드 곳곳의
 * "보관 한도(최근 8건)" 서술이 이 수치를 가리킨다) 사용자 오류가 실제 장애를 밀어낸다.
 * https://firebase.google.com/docs/crashlytics/android/customize-crash-reports
 *
 * 사용자 거절 판정은 Data 계층이 [ReceiverFailure.UserRejection] 으로 번역했다 — FE 가 등재한 code 는
 * code 만으로, 미등재 code 는 `4xx + 비어 있지 않은 서버 문구`로. 5xx 와 문구 없는 미등재 4xx 는
 * [ReceiverFailure.UnexpectedServerFailure] 로 남아 기록된다. presentation 은 HTTP status·BE code·서버
 * message 를 되짚지 않고 도메인 결과만 소비한다.
 *
 * 다른 흐름에는 쓰지 않는다. 회원가입 이메일 인증은 사용자 오류가 code 1207 하나로만 와서 호출부가
 * 타입(`CoreAuthFailure.EmailVerification`)만 보고 거른다 — 문구 유무를 따질 필요가 없다.
 *
 * 화면 노출은 `UserRejection.reason != null` 로 더 좁다. 미등재 4xx 사용자 거절은 리포팅에서 제외되지만
 * 서버 원문을 노출하지 않고 화면 폴백으로 내려간다. 두 판정을 하나로 합치지 말 것.
 */
fun Throwable.shouldReportInReceiverFlow(): Boolean =
    when (this) {
        is ReceiverFailure -> !isExpectedUserRejection()
        else -> true
    }

/**
 * 루트로 좁혀 `when` 을 exhaustive 하게 만든다 — 수신자 실패 유형이 늘면 여기가 컴파일 에러로 잡힌다.
 * 판정을 빼먹은 새 유형은 조용히 기록 대상이 되므로(안전한 쪽), 놓쳐도 티가 나지 않는다.
 */
private fun ReceiverFailure.isExpectedUserRejection(): Boolean =
    when (this) {
        is ReceiverFailure.UserRejection -> {
            true
        }

        is ReceiverFailure.UnexpectedServerFailure -> {
            false
        }

        // 서버가 예상하고 거절한 것이 아니라 서버에 닿지도 못한 실패다. 이 타입이 생기기 전에도
        // IO 예외는 위 `else -> true` 로 기록됐으므로(#611) 기록 대상을 그대로 유지한다.
        is ReceiverFailure.NetworkUnavailable -> {
            false
        }

        // 서버가 사유를 확인해 거절한 4xx 다 — status·문구를 되짚을 것 없이 타입이 이미 그 사실이다.
        is ReceiverFailure.DeliveryConditionNotMet -> {
            true
        }
    }

private const val KEY_AFTERNOTE_STAGE = "afternote_stage"

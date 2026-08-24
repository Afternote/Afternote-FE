package com.afternote.feature.afternote.presentation.reporting

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.receiver.domain.error.ReceiverServerRejectionException

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

    MEMORIAL_THUMBNAIL_UPLOAD("memorial_thumbnail_upload"),

    /**
     * 선택한 영상에서 썸네일 프레임을 뽑는 로컬 디코딩 — 서버 호출 이전.
     *
     * 사용자에게는 썸네일 자리가 비어 보일 뿐 오류로 알려주지 않아, 계측하지 않으면 흔적이 남지 않는다.
     * 반대로 원격 썸네일의 Coil 로드 실패는 계측하지 않는다 — 일시적 네트워크 실패로도 나서
     * 보관 한도(최근 8건)를 잡음이 차지한다.
     */
    MEMORIAL_THUMBNAIL_EXTRACT("memorial_thumbnail_extract"),

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

    // RECEIVED_EXPORT_DOWNLOAD·RECEIVED_EXPORT_SAVE 는 수신자 흐름 이전(#615)과 함께
    // feature:receiver 의 ReceiverFailureStage(`receiver_stage` 키)로 이동 — home_stage 와의
    // 이중 기록(#546 참고)이 그 키로 수렴된다. 위 수신자 단계들(RECEIVED_*·RECEIVER_* 등)의
    // 이동은 열람 실패 처리 통일(#611·#614 계열)과 함께 별도 판단.
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
 * 유일한 제외 대상은 "서버가 예상하고 처리한 거절"(이메일 미등록·인증번호 만료·마스터 키 오타 등)이다 —
 * 앱이 정상 동작한 결과라 고칠 것이 없고, 무엇보다 보관 한도를 사용자 오류가 차지해 실제 장애를 밀어낸다.
 * Crashlytics 는 non-fatal 을 **최근 8건만 보관하고 초과분은 오래된 것부터 버린다** — 이 수치가 코드 곳곳의
 * "보관 한도(최근 8건)" 서술의 근거다.
 * https://firebase.google.com/docs/crashlytics/android/customize-crash-reports
 *
 * 그 판정은 **4xx + 서버 문구** 두 조건을 모두 만족할 때만 성립하고, 나머지는 전부 기록한다.
 *
 * | | 문구 있음 | 문구 없음 |
 * |---|---|---|
 * | 4xx | 제외 | 기록 |
 * | 5xx | 기록 | 기록 |
 *
 * 문구 유무만으로 가르지 않는 이유: 이 서버는 5xx 에도 `message` 를 싣는다 — 500 응답 body 에 내부 SQL
 * 문구가 그대로 실려 온 실측(#511)이 있다. 문구만 보면 정작 잡으려던 장애가 통째로 제외된다.
 *
 * 반대로 status 만으로도 가를 수 없다: 문구 없는 4xx 는 서버가 안내한 거절이 아니라 이쪽이 잘못된 요청을
 * 보내고 있다는 신호(파라미터 누락·잘못된 id·만료 토큰)라, 제외하면 FE 버그가 묻힌다. 이 서버는 4xx·5xx
 * 가리지 않고 문구를 싣는 것으로 관측돼(400·401·500 실측), 문구가 없다는 것 자체가 정상 경로가 아니다.
 *
 * 두 조건을 모두 요구하는 건 **확실할 때만 제외**하려는 것이다 — 판정이 빗나가면 기록을 더 하는 쪽으로
 * 빗나간다. 잡음은 콘솔에 보여서 나중에 좁힐 수 있지만, 제외한 건 보이지 않아 좁힐 기회조차 없다.
 *
 * 사유 code 로 좁히지 않은 이유: 서버의 code 체계가 사용자 오류와 장애를 아직 분리하지 않는다.
 *
 * 다른 흐름에는 쓰지 않는다. 회원가입 이메일 인증은 사용자 오류가 code 1207 하나로만 와서 호출부가
 * 타입(`EmailVerificationException`)만 보고 거른다 — 문구 유무를 따질 필요가 없다.
 *
 * 화면 노출 게이트(`toErrorPayload`, DocumentUploadUiState.kt)는 이 술어를 쓰지 않는다 — 노출은
 * 사유 code allowlist 로 더 좁게 가른다. 그쪽을 넓히더라도 이 판정을 따라 넓히지 말 것.
 */
fun Throwable.shouldReportInReceiverFlow(): Boolean {
    val isExpectedUserRejection =
        this is ReceiverServerRejectionException &&
            status in CLIENT_ERROR_STATUS_RANGE &&
            !serverMessage.isNullOrBlank()
    return !isExpectedUserRejection
}

/** 4xx = 요청을 보낸 쪽 문제. 이 대역 밖(5xx·그 외)은 서버 문구가 실려 와도 장애로 보고 기록한다. */
private val CLIENT_ERROR_STATUS_RANGE = 400..499

private const val KEY_AFTERNOTE_STAGE = "afternote_stage"

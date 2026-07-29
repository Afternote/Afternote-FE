package com.afternote.feature.afternote.presentation.reporting

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.error.ReceiverServerRejectionException

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

    /** 모든 기록 내려받기 — 서버에서 묶음을 받아오는 단계. */
    RECEIVED_EXPORT_DOWNLOAD("received_export_download"),

    /** 모든 기록 내려받기 — 받아온 묶음을 기기에 저장하는 단계. */
    RECEIVED_EXPORT_SAVE("received_export_save"),
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
 * **수신자 흐름에서** 서버가 사용자에게 보여줄 안내 문구까지 실어 내려준 거절인지.
 *
 * 문구가 있다는 것 자체가 "서버가 예상하고 처리한 거절"(이메일 미등록·인증번호 만료·마스터 키 오타 등)
 * 이라는 신호라 텔레메트리에서 제외한다 — 기록해 봐야 보관 한도(최근 8건)를 사용자 오류가 차지해 실제
 * 장애를 밀어낸다. 문구 없는 실패(5xx·인프라 예외)만 남는다.
 *
 * 다른 흐름에는 쓰지 않는다. 회원가입 이메일 인증은 사용자 오류가 code 1207 하나로만 와서 호출부가
 * 타입(`EmailVerificationException`)만 보고 거른다 — 문구 유무를 따질 필요가 없다.
 *
 * 사유 code 로 좁히지 않은 이유: 서버의 code 체계가 사용자 오류와 장애를 아직 분리하지 않는다.
 */
fun Throwable.isExplainedReceiverRejection(): Boolean = this is ReceiverServerRejectionException && !serverMessage.isNullOrBlank()

private const val KEY_AFTERNOTE_STAGE = "afternote_stage"

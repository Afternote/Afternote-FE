package com.afternote.feature.afternote.presentation.reporting

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.error.ReceiverServerRejectionException

/** 애프터노트 실패 지점. [reportingName]은 이미 수집되는 계약 값이므로 임의로 변경하지 않는다. */
enum class AfternoteFailureStage(
    val reportingName: String,
) {
    /** 애프터노트 등록·수정 제출 — 입력 검증 실패는 제외(사용자 입력 오류). */
    SAVE("save"),

    /** 수정 진입 시 기존 애프터노트 로드 — 여기서 깨지면 빈 폼으로 덮어쓸 위험이 있다. */
    PREFILL_LOAD("prefill_load"),

    MEMORIAL_THUMBNAIL_UPLOAD("memorial_thumbnail_upload"),

    /** 서버 호출 전 로컬 썸네일 추출 실패. 일시적인 원격 이미지 로드 실패는 포함하지 않는다. */
    MEMORIAL_THUMBNAIL_EXTRACT("memorial_thumbnail_extract"),

    DETAIL_LOAD("detail_load"),

    /** 상세 화면에서 수행하는 애프터노트 삭제. */
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

    MUSIC_SEARCH("music_search"),

    /** 수신 상세와 같은 API를 사용하지만 실패 화면이 달라 별도 단계로 기록한다. */
    RECEIVED_PLAYLIST_LOAD("received_playlist_load"),

    RECEIVED_RECORD_BOXES_LOAD("received_record_boxes_load"),

    /** 모든 기록 내려받기 — 서버에서 묶음을 받아오는 단계. */
    RECEIVED_EXPORT_DOWNLOAD("received_export_download"),

    /** 모든 기록 내려받기 — 받아온 묶음을 기기에 저장하는 단계. */
    RECEIVED_EXPORT_SAVE("received_export_save"),
}

/** 애프터노트 흐름의 handled 실패를 공통 키로 기록한다. 예상 가능한 사용자 경로는 호출부에서 제외한다. */
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
 * 수신자 흐름에서 텔레메트리에 기록할 실패인지 판단한다.
 *
 * 서버가 사유를 설명한 4xx만 예상된 사용자 거절로 제외한다. 메시지 없는 4xx는 잘못된 요청일 수 있고,
 * 5xx는 메시지 유무와 관계없이 장애일 수 있으므로 기록한다.
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

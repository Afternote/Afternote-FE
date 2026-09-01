package com.afternote.feature.afternote.domain.error

import com.afternote.feature.afternote.domain.repository.author.MediaKind

/**
 * 애프터노트 작성·저장 흐름의 도메인 실패 루트.
 * HTTP 상태·Retrofit·에러 바디 형식 같은 인프라 디테일은 Data 계층이 해석한 뒤 이 계열로 통일한다.
 *
 * 소비처는 이 루트로 좁힌 뒤 `when` 으로 가른다 — 실패 유형이 늘면 컴파일러가 소비처를 잡아준다.
 * `message` 는 Logcat·Crashlytics 단서용 진단 문구다. 표시 문구는 화면 리소스가 갖는다.
 */
sealed class AfternoteFailure(
    message: String?,
    cause: Throwable?,
) : Exception(message, cause) {
    /**
     * 추억 노트 미디어(장례식에 남길 영상·영정사진)를 저장 페이로드에 실을 URL 로 해석하지 못한 실패.
     *
     * 매체별로 타입을 나누지 않고 [media] 로 구분한다 — 화면은 둘을 같은 문구로 처리하고
     * (`AfternoteEditorViewModel.handleSaveFailure` 의 generic 폴백), 갈라 보던 코드가 없었다(#934 실측).
     *
     * `data class` 를 쓰지 않는다 — 생성된 equals/hashCode 가 선언 프로퍼티만 덮어 [cause] 를 무시한다.
     * 원본 예외를 cause 로 나르는 것이 이 타입의 계약이다.
     */
    class MediaSave(
        val media: MediaKind,
        cause: Throwable,
    ) : AfternoteFailure("memorial media save failed: ${media.name}", cause)

    /**
     * 서버 응답 없이 전송 계층에서 끝난 실패(DNS 해석 불가·타임아웃·연결 거부 등)의 도메인 표현.
     *
     * data 계층이 저장 API 호출의 IO 예외를 이 타입으로 치환한다 — presentation 은 core:network 에
     * 의존하지 않으므로, 이 타입 하나로 "네트워크 연결 안내" 분기를 할 수 있다
     * ([com.afternote.core.domain.error.CoreAuthFailure.NetworkUnavailable] 과 같은 규약).
     * 원인 예외는 [cause] 로 보존한다(로그 진단용).
     */
    class NetworkUnavailable(
        cause: Throwable,
    ) : AfternoteFailure("network unavailable", cause)
}

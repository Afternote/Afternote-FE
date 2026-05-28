package com.afternote.feature.afternote.domain.repository.author

/**
 * 추모 영상의 *서버 자원 상태* 를 표현하는 sealed 타입.
 *
 * Repository 가 입력 String 을 보고 *형식을 직접 판별* 한 뒤 그 결과를 sealed 분기로 도메인에 전달한다.
 * 도메인은 `"content://"`, `"X-Amz-"` 같은 인프라 디테일 문자열을 모르고 `when` 분기로만 처리.
 *
 * - [Empty] — 영상 없음
 * - [Existing] — 이미 서버가 알고 있는 자원 (입력 그대로 통과된 원격 URL). PATCH 시 페이로드에서 제거해
 *   *변경 없음* 신호.
 * - [FreshlyUploaded] — 방금 업로드해 새로 등록된 자원. POST/PATCH 페이로드에 그대로 보내야 서버가
 *   새 자원으로 등록.
 */
sealed interface VideoUploadOutcome {
    data object Empty : VideoUploadOutcome

    data class Existing(
        val url: String,
    ) : VideoUploadOutcome

    data class FreshlyUploaded(
        val url: String,
    ) : VideoUploadOutcome
}

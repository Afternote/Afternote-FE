package com.afternote.feature.afternote.domain.repository.author

/**
 * 영정 사진의 *서버 자원 상태* 를 표현하는 sealed 타입.
 *
 * 영상과 달리 사진은 *기존 영구 URL* 과 *새로 고른 로컬 URI* 가 별도 인자로 들어오며, Repository 가
 * 둘을 함께 보고 결과 분기를 결정한다.
 *
 * - [Empty] — 사진 없음
 * - [Existing] — 기존 영구 URL 그대로 (사용자가 새로 안 고름)
 * - [FreshlyUploaded] — 사용자가 새로 고른 로컬 URI 를 업로드한 결과
 */
sealed interface PhotoUploadOutcome {
    data object Empty : PhotoUploadOutcome

    data class Existing(
        val url: String,
    ) : PhotoUploadOutcome

    data class FreshlyUploaded(
        val url: String,
    ) : PhotoUploadOutcome
}

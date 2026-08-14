package com.afternote.feature.afternote.domain.repository.author

/**
 * 영정 사진의 *서버 자원 상태 해석* + 필요 시 업로드.
 *
 * 새로 고른 로컬 사진과 기존 원격 사진의 우선순위(픽 우선)는 *호출부* 가 [MediaInput] 을 구성할 때 정한다.
 * Repository 는 확정된 [MediaInput] 을 [when] 분기로 처리한다.
 *
 * - [MediaInput.Local] → 업로드 → [PhotoUploadOutcome.FreshlyUploaded]
 * - [MediaInput.Remote] → 입력 그대로 [PhotoUploadOutcome.Existing]
 * - [MediaInput.None] → [PhotoUploadOutcome.Empty]
 */
fun interface MemorialPhotoUploadRepository {
    suspend fun resolvePhoto(input: MediaInput): Result<PhotoUploadOutcome>
}

package com.afternote.feature.afternote.domain.repository.author

/**
 * 추모(플레이리스트) 영상의 *서버 자원 상태 해석* + 필요 시 업로드.
 *
 * 입력 [MediaInput] 으로 로컬/원격이 이미 확정돼 들어오므로, Repository 는 `content://` prefix 를
 * 판별하지 않고 [when] 분기로 [VideoUploadOutcome] 를 만든다.
 *
 * - [MediaInput.Local] → S3 presigned upload → [VideoUploadOutcome.FreshlyUploaded]
 * - [MediaInput.Remote] → 입력 그대로 [VideoUploadOutcome.Existing]
 * - [MediaInput.None] → [VideoUploadOutcome.Empty]
 */
fun interface MemorialVideoUploadRepository {
    suspend fun resolveVideo(input: MediaInput): Result<VideoUploadOutcome>
}

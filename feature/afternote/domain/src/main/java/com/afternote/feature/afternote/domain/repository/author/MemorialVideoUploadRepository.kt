package com.afternote.feature.afternote.domain.repository.author

/**
 * 추모(플레이리스트) 영상의 *서버 자원 상태 해석* + 필요 시 업로드.
 *
 * 입력 String 의 형식(로컬 `content://` 인지 원격 URL 인지)을 *Repository 가 직접 판별* 하고
 * [VideoUploadOutcome] sealed 분기로 도메인에 결과를 전달한다. 도메인은 인프라 형식 디테일을 모름.
 *
 * - 로컬 `content://` → S3 presigned upload → [VideoUploadOutcome.FreshlyUploaded]
 * - 원격 HTTPS URL (영구 또는 presigned GET) → 입력 그대로 [VideoUploadOutcome.Existing]
 * - null/blank → [VideoUploadOutcome.Empty]
 */
fun interface MemorialVideoUploadRepository {
    suspend fun resolveVideo(input: String?): Result<VideoUploadOutcome>
}

package com.afternote.feature.afternote.domain.repository

/**
 * 추모(플레이리스트) 영정 사진을 content URI에서 업로드하고 HTTPS URL을 반환합니다.
 */
fun interface MemorialPhotoUploadRepository {
    suspend fun upload(uriString: String): Result<String>
}

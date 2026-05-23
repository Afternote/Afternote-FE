package com.afternote.feature.afternote.domain.repository.author

/**
 * 영정 사진의 *서버 자원 상태 해석* + 필요 시 업로드.
 *
 * 영상과 달리 사진은 *기존 영구 URL* (`existingUrl`) 과 *새로 고른 로컬 URI* (`pickedUri`) 가
 * 별도 인자로 들어오며, Repository 가 둘을 함께 보고 분기를 결정한다.
 *
 * - `pickedUri` 가 로컬 `content://` → 업로드 → [PhotoUploadOutcome.FreshlyUploaded]
 * - 그 외에 `existingUrl` 이 있음 → [PhotoUploadOutcome.Existing]
 * - 둘 다 비어있음 → [PhotoUploadOutcome.Empty]
 */
fun interface MemorialPhotoUploadRepository {
    /**
     * @param existingUrl 직전 상태에 이미 저장돼 있던 *서버 영구 URL*. 수정 모드 진입 시 prefill 되는 기존 사진.
     *                    사용자가 이번 세션에 새 사진을 고르지 않았다면 이 값이 그대로 유지되어야 한다.
     * @param pickedUri 사용자가 *이번 편집 세션에 새로 선택* 한 로컬 `content://` URI.
     *                  null/blank 이면 새 선택이 없었다는 뜻이고, 그땐 `existingUrl` 로 폴백한다.
     */
    suspend fun resolvePhoto(
        existingUrl: String?,
        pickedUri: String?,
    ): Result<PhotoUploadOutcome>
}

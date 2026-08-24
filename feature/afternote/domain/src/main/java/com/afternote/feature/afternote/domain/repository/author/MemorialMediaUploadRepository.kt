package com.afternote.feature.afternote.domain.repository.author

/**
 * 추모 미디어(영정 사진·추모 영상)의 *상태 해석* + 필요 시 업로드.
 *
 * 로컬/원격 판별은 호출부가 [MediaInput] 을 구성할 때 끝내고, 여기서는 [when] 분기로만 처리한다.
 *
 * - [MediaInput.Local] → [kind] 에 맞는 업로드 후 그 결과 URL
 * - [MediaInput.Remote] → 입력 URL 그대로
 * - [MediaInput.None] → null
 */
fun interface MemorialMediaUploadRepository {
    suspend fun resolve(
        input: MediaInput,
        kind: MediaKind,
    ): Result<String?>
}

package com.afternote.feature.afternote.domain.repository.author

/**
 * 추모 음성 업로드 (#1118) — 로컬 `content://` URI → presigned 발급 → S3 PUT → 파일 URL.
 *
 * 사진·영상과 달리 core 의 공용 업로드 리포지터리를 쓰지 않는다. 확장자 결정 규칙이
 * *추억 노트 서버 계약*([MemorialAudioFormats]) 이라 공용 업로드가 가진 "못 읽으면 jpg 로 폴백" 규칙과
 * 양립하지 않는다 — 그 폴백을 타면 음성이 이미지 확장자로 발급돼 저장 단계에서 400 이 된다.
 */
fun interface MemorialAudioUploadRepository {
    /**
     * @param uriString 첨부한 음성의 로컬 content URI.
     * @return 성공 시 업로드된 파일 URL. 지원하지 않는 형식이면 실패.
     */
    suspend fun upload(uriString: String): Result<String>
}

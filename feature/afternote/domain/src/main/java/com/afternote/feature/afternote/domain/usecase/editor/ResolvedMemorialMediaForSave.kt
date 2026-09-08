package com.afternote.feature.afternote.domain.usecase.editor

/**
 * 저장 직전까지 해석·업로드된 추억 노트 미디어 URL 묶음. POST/PATCH 동일 규칙 — 백엔드가
 * `S3Service.resolvePublicUrl(key)` 로 영구 public URL 을 발급하므로 클라이언트가 받은 URL 을
 * 그대로 다시 보낼 수 있다 (presigned 변환 없음). `videoUrlForUpdate` / `memorialThumbnailUrlForUpdate`
 * 같은 PATCH 전용 필드는 불필요해 제거됨 (#258 BE 확정 결과).
 */
data class ResolvedMemorialMediaForSave(
    /** 업로드 반영 후 영상 URL (로컬 content 이면 업로드 결과). null 이면 영상 미첨부. */
    val resolvedVideoUrl: String?,
    /** 영정 사진 URL (로컬 content 이면 업로드 결과). null 이면 사진 미첨부. */
    val resolvedMemorialPhotoUrl: String?,
    /** 추모 음성 URL (로컬 content 이면 업로드 결과). null 이면 음성 미첨부 (#1118). */
    val resolvedMemorialAudioUrl: String?,
)

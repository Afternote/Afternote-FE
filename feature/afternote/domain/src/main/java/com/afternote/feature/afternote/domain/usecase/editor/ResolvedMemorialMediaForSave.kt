package com.afternote.feature.afternote.domain.usecase.editor

/**
 * 저장 직전까지 해석·업로드된 추모 미디어 URL 묶음.
 * 생성(Post)과 수정(Patch)에서 각각 다른 필드로 쓰인다.
 */
data class ResolvedMemorialMediaForSave(
    /** 생성·수정 공통: 업로드 반영 후 영상 URL (로컬 content면 업로드 결과). */
    val resolvedVideoUrl: String?,
    /** 생성·수정 공통: 영정 사진 URL. */
    val resolvedMemorialPhotoUrl: String?,
    /**
     * 수정(PATCH) 전용: 본문에 넣을 videoUrl. Presigned URL이면 null로 생략.
     * 생성 시에는 무시하고 [resolvedVideoUrl]을 쓴다.
     */
    val videoUrlForUpdate: String?,
    /** 수정(PATCH) 시 썸네일: [videoUrlForUpdate]가 null이면 null. */
    val funeralThumbnailUrlForUpdate: String?,
)

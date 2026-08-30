package com.afternote.feature.afternote.presentation.author.editor

/** 저장 시 추억 노트 미디어 필드 (로컬 URI / 기존 URL 혼재). */
data class SaveAfternoteMemorialMedia(
    val memorialVideoUrl: String? = null,
    val memorialThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
    val pickedMemorialPhotoUri: String? = null,
    /** 추모 음성 (#1118). 영상과 같이 로컬 URI 와 원격 URL 이 한 필드를 공유한다. */
    val memorialAudioUrl: String? = null,
)

/**
 * 이 폼 세션에서 새로 붙인 로컬 첨부(`content://` URI)인지 — 원격 prefill URL 과 가르는 단일 기준.
 *
 * 저장 분류(`AfternoteEditorViewModel.videoMediaInput`)·삭제 노출(`removableMemorialMediaTargets`)·
 * 썸네일 추출(`MemorialVideoUpload`)이 같은 판정을 써야 하므로 한 곳에 둔다 — 흩어 두면 한쪽만
 * 바뀌는 표류가 생긴다.
 */
internal fun String.isLocalContentUri(): Boolean = startsWith("content://")

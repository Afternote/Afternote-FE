package com.afternote.feature.afternote.presentation.author.editor

/** 저장 시 추억 노트 미디어 필드 (로컬 URI / 기존 URL 혼재). */
data class SaveAfternoteMemorialMedia(
    val memorialVideoUrl: String? = null,
    val memorialThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
    val pickedMemorialPhotoUri: String? = null,
)

/**
 * 이 폼 세션에서 새로 붙인 로컬 첨부(`content://` URI)인지 — 원격 prefill URL 과 가르는 단일 기준.
 *
 * 저장 분류(`AfternoteEditorViewModel.videoMediaInput`)와 썸네일 추출(`MemorialVideoUpload`)이 같은
 * 판정을 써야 하므로 한 곳에 둔다. 삭제 노출은 #1597부터 출처가 아니라 슬롯의 표시값 존재 여부를 본다.
 */
internal fun String.isLocalContentUri(): Boolean = startsWith("content://")

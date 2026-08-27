package com.afternote.feature.afternote.presentation.author.editor

/** 저장 시 추억 노트 미디어 필드 (로컬 URI / 기존 URL 혼재). */
data class SaveAfternoteMemorialMedia(
    val memorialVideoUrl: String? = null,
    val memorialThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
    val pickedMemorialPhotoUri: String? = null,
)

package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.core.ui.bottombar.BottomNavTab

/**
 * 콜백 그룹 (S107: 파라미터 7개 이하 유지).
 */
data class AfternoteEditorScreenCallbacks(
    val onBackClick: () -> Unit = {},
    /** 저장: [SaveAfternotePayloadBuilder]에서 페이로드를 만든 뒤 [AfternoteEditorViewModel.saveAfternote]로 넘기도록 상위에서 구성한다. */
    val onRegisterClick: () -> Unit = {},
    val onNavigateToAddSong: () -> Unit = {},
    val onNavigateToSelectReceiver: () -> Unit = {},
    val onBottomNavTabSelected: (BottomNavTab) -> Unit = {},
    val onThumbnailBytesReady: (ByteArray?) -> Unit = {},
)

/** 저장 시 추모 미디어 필드 (로컬 URI / 기존 URL 혼재). */
data class SaveAfternoteMemorialMedia(
    val funeralVideoUrl: String? = null,
    val funeralThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
    val pickedMemorialPhotoUri: String? = null,
)

package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.core.ui.bottombar.BottomNavTab

/**
 * 에디터 화면 콜백 묶음.
 *
 * S107(파라미터 ≤7)을 근거로 묶었지만 이 레포는 detekt·sonar 를 쓰지 않아 강제되지 않는다 — #439 가
 * 형제 봉투들을 flat 파라미터로 푼 근거가 그것이고, 이 봉투는 그 감사에서 누락됐다. 언번들은 #602.
 */
data class AfternoteEditorScreenCallbacks(
    val onBackClick: () -> Unit = {},
    /** 저장: [SaveAfternotePayloadBuilder]에서 페이로드를 만든 뒤 [AfternoteEditorViewModel.saveAfternote]로 넘기도록 상위에서 구성한다. */
    val onRegisterClick: () -> Unit = {},
    val onNavigateToAddSong: () -> Unit = {},
    val onNavigateToSelectReceiver: () -> Unit = {},
    val onBottomNavTabSelected: (BottomNavTab) -> Unit = {},
    val onThumbnailBytesReady: (ByteArray?) -> Unit = {},
    /** 로컬 영상에서 썸네일 프레임 추출이 실패했을 때 호출 → VM 이 개발자 텔레메트리로 기록. */
    val onThumbnailExtractionFailed: (Throwable) -> Unit = {},
    /** Snackbar 표출 직후 호출 → VM 의 `thumbnailUploadFailed` nullify. */
    val onThumbnailUploadErrorConsumed: () -> Unit = {},
)

/** 저장 시 추모 미디어 필드 (로컬 URI / 기존 URL 혼재). */
data class SaveAfternoteMemorialMedia(
    val funeralVideoUrl: String? = null,
    val funeralThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
    val pickedMemorialPhotoUri: String? = null,
)

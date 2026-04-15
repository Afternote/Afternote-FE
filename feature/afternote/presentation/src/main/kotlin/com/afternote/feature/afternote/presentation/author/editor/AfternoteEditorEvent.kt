package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.core.ui.bottombar.BottomNavTab

/**
 * 콜백 그룹 (S107: 파라미터 7개 이하 유지).
 */
data class AfternoteEditorScreenCallbacks(
    val onBackClick: () -> Unit = {},
    /** 등록: [RegisterAfternotePayloadBuilder] 등에서 페이로드를 만든 뒤 저장 이벤트로 넘기도록 상위에서 구성한다. */
    val onRegisterClick: () -> Unit = {},
    val onNavigateToAddSong: () -> Unit = {},
    val onNavigateToSelectReceiver: () -> Unit = {},
    val onBottomNavTabSelected: (BottomNavTab) -> Unit = {},
    val onThumbnailBytesReady: (ByteArray?) -> Unit = {},
)

/**
 * Message to show when save fails (validation or API error).
 * When non-null, the screen shows a Snackbar with this text.
 */
data class AfternoteEditorSaveError(
    val message: String,
)

/**
 * ViewModel → UI **단발성** 이벤트 (토스트·네비 등).
 * [kotlinx.coroutines.channels.Channel]로 한 번만 소비되며, UI → ViewModel 인텐트는
 * [AfternoteEditorViewModel]의 개별 public 메서드로 보낸다 (작성자 홈 화면과 동일 패턴).
 */
sealed interface AfternoteEditorEvent {
    data class SaveSuccess(
        val savedId: Long,
    ) : AfternoteEditorEvent

    data class ThumbnailUploaded(
        val url: String,
    ) : AfternoteEditorEvent
}

/** 저장 시 추모 미디어 필드 (로컬 URI / 기존 URL 혼재). */
data class SaveAfternoteMemorialMedia(
    val funeralVideoUrl: String? = null,
    val funeralThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
    val pickedMemorialPhotoUri: String? = null,
)

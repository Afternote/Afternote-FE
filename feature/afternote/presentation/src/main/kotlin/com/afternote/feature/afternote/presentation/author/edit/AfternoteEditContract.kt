package com.afternote.feature.afternote.presentation.author.edit

import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.edit.model.RegisterAfternotePayload

internal const val CATEGORY_GALLERY_AND_FILE = "갤러리 및 파일"
internal const val CATEGORY_MEMORIAL_GUIDELINE = "추모 가이드라인"

/**
 * 콜백 그룹 (S107: 파라미터 7개 이하 유지).
 */
data class AfternoteEditScreenCallbacks(
    val onBackClick: () -> Unit = {},
    val onRegisterClick: (RegisterAfternotePayload) -> Unit = {},
    val onNavigateToAddSong: () -> Unit = {},
    val onNavigateToSelectReceiver: () -> Unit = {},
    val onBottomNavTabSelected: (BottomNavTab) -> Unit = {},
    val onThumbnailBytesReady: (ByteArray?) -> Unit = {},
)

/**
 * Message to show when save fails (validation or API error).
 * When non-null, the screen shows a Snackbar with this text.
 */
data class AfternoteEditSaveError(
    val message: String,
)

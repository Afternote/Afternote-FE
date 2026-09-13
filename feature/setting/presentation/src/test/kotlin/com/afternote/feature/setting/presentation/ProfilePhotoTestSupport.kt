package com.afternote.feature.setting.presentation

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditUiState
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditViewModel

internal const val PROFILE_TEST_TIMEOUT_MILLIS = 5_000L

/** `ProfilePhotoWithAddBadge` 의 「추가」 배지 — 눌리는 상태에서는 clickable 이 자손 semantics 를 합친다. */
internal const val ADD_BADGE = "추가"

internal const val DEFAULT_PHOTO = "기본"
internal const val PROFILE_PHOTO = "프로필 사진"
internal const val GALLERY_ITEM = "갤러리에서 선택"
internal const val CAMERA_ITEM = "사진 촬영"
internal const val CAPTURE_UNAVAILABLE = "카메라를 사용할 수 없습니다."
internal const val SERVER_IMAGE_URL = "https://cdn.test/profiles/saved.jpg"

internal fun ProfileEditViewModel.success() = uiState.value as ProfileEditUiState.Success

/** 배지를 눌러 소스 시트를 띄우고 갈래가 그려질 때까지 기다린다. */
internal fun ComposeContentTestRule.openPhotoSourceSheet() {
    onNodeWithContentDescription(ADD_BADGE).performClick()
    waitUntil(timeoutMillis = PROFILE_TEST_TIMEOUT_MILLIS) {
        onAllNodesWithText(CAMERA_ITEM).fetchSemanticsNodes().isNotEmpty()
    }
}

internal fun ComposeContentTestRule.awaitPhotoSourceSheetClosed() {
    waitUntil(timeoutMillis = PROFILE_TEST_TIMEOUT_MILLIS) {
        onAllNodesWithText(CAMERA_ITEM).fetchSemanticsNodes().isEmpty()
    }
}

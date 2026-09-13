package com.afternote.feature.setting.presentation

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditUiState
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditViewModel

internal const val PROFILE_TEST_TIMEOUT_MILLIS = 5_000L

/**
 * core 의 `ProfileImagePicker` 가 그리는 편집 배지 — `core_ui_content_description_profile_edit`.
 *
 * 사진 선택을 받을 수 없는 상태에서는 배지가 아예 그려지지 않으므로, 잠김 판정은 클릭 동작이 아니라
 * 노드 부재로 한다.
 */
internal const val ADD_BADGE = "프로필 수정 버튼"

/**
 * 아바타 — `core_ui_content_description_profile_image`.
 *
 * 기본 아바타와 고른 사진이 같은 설명을 쓴다. 「사진이 실렸는가」는 이 노드로 갈리지 않으므로
 * `displayImageUri` 로 단언한다.
 */
internal const val PROFILE_IMAGE = "프로필 이미지"

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

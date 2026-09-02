package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MemorialVideoUploadInteractionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `빈 영상 카드 중앙을 누르면 단일 클릭 영역이 추가 콜백을 호출한다`() {
        var addClicks = 0
        composeRule.setContent {
            AfternoteTheme {
                MemorialVideoUpload(
                    onAddVideoClick = { addClicks += 1 },
                    onThumbnailBytesReady = {},
                    onThumbnailExtractionFailed = {},
                    thumbnailRetryToken = 0,
                )
            }
        }

        assertEquals(
            1,
            composeRule
                .onAllNodes(hasClickAction(), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .size,
        )

        // 카드 정중앙은 플러스 배지 자리다. 실제 포인터 입력으로 자식이 이벤트를 삼키지 않는지 본다.
        composeRule
            .onNodeWithTag(MEMORIAL_VIDEO_ADD_TEST_TAG, useUnmergedTree = true)
            .performTouchInput { click() }

        composeRule.runOnIdle { assertEquals(1, addClicks) }
    }
}

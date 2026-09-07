package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 수신자 상세의 영상 구역이 영상 유무로 갈리는 지점을 고정한다 (#1781).
 *
 * 영상이 없을 때 구역 자체가 사라지는 건 시안 결정이고(#274), 그 결정이 `ReceiverVideoSection` 의
 * 「영상 없음」 플레이스홀더를 현재 호출부에서 닿지 않게 만든다. 이 단언이 깨지는 순간 플레이스홀더가
 * 화면에 드러나므로, 치수를 썸네일과 같은 상수에서 끌어다 쓰는 근거가 여기 걸려 있다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceivedMemorialVideoSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `영상이 없으면 영상 구역을 통째로 숨긴다`() {
        composeRule.setContent {
            AfternoteTheme {
                MemorialReceivedDetailScreen(
                    senderName = "김발신",
                    onNavigateToFullList = {},
                    onNavigateToPlaylist = {},
                    onBackClick = {},
                    albumCovers = emptyList(),
                )
            }
        }

        composeRule.onNodeWithText(FUNERAL_VIDEO_LABEL).assertDoesNotExist()
    }

    @Test
    fun `영상이 있으면 영상 구역을 그린다`() {
        composeRule.setContent {
            AfternoteTheme {
                MemorialReceivedDetailScreen(
                    senderName = "김발신",
                    onNavigateToFullList = {},
                    onNavigateToPlaylist = {},
                    onBackClick = {},
                    albumCovers = emptyList(),
                    memorialVideoUrl = "https://cdn.example.com/memorial.mp4",
                )
            }
        }

        composeRule.onNodeWithText(FUNERAL_VIDEO_LABEL).performScrollTo().assertIsDisplayed()
    }

    private companion object {
        const val FUNERAL_VIDEO_LABEL = "장례식에 남길 영상"
    }
}

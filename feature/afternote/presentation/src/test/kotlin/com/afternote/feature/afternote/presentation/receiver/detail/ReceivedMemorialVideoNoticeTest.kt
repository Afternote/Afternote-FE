package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * 추모 영상 실행 안내가 Toast 가 아니라 스낵바로 나가고, 두 원인이 서로 다른 문구로 갈리는지 단언한다 (#1391).
 *
 * Toast 는 Compose semantics 에 안 잡혀 어떤 테스트로도 단언할 수 없었다 — 채널 전환이 곧 검증 가능성이다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceivedMemorialVideoNoticeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `막힌 URL 은 주소 문제로 안내한다`() {
        showDetail(videoUrl = "javascript:alert(1)")

        clickVideoCard()

        composeRule.onNodeWithText("영상 주소가 올바르지 않아 재생할 수 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("영상을 재생할 수 있는 앱이 없습니다.").assertDoesNotExist()
    }

    @Test
    fun `재생할 앱이 없으면 앱 부재로 안내한다`() {
        // 실행 가능한 액티비티가 없으면 ActivityNotFoundException 을 던지게 해 OS 거부를 재현한다.
        shadowOf(RuntimeEnvironment.getApplication()).checkActivities(true)
        showDetail(videoUrl = "https://cdn.example.com/memorial.mp4")

        clickVideoCard()

        composeRule.onNodeWithText("영상을 재생할 수 있는 앱이 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("영상 주소가 올바르지 않아 재생할 수 없습니다.").assertDoesNotExist()
    }

    private fun showDetail(videoUrl: String) {
        composeRule.setContent {
            AfternoteTheme {
                MemorialReceivedDetailScreen(
                    senderName = "김발신",
                    onNavigateToFullList = {},
                    onNavigateToPlaylist = {},
                    onBackClick = {},
                    albumCovers = emptyList(),
                    memorialVideoUrl = videoUrl,
                )
            }
        }
    }

    private fun clickVideoCard() {
        composeRule.onNodeWithContentDescription("영상 재생").performScrollTo().performClick()
        composeRule.waitForIdle()
    }
}

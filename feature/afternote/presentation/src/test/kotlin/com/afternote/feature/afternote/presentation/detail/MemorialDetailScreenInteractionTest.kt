package com.afternote.feature.afternote.presentation.detail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 영상 카드는 제목 라벨을 품은 단일 Button 으로 화면 semantics 에 실린다.
 * 카드를 집는 기준도 그 공개 semantics 다 — 테스트 전용 tag 를 두지 않는다 (#1673).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MemorialDetailScreenInteractionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `영상 URL이 있으면 카드 탭이 정확한 URL을 상위 콜백에 전달한다`() {
        val videoUrl = "https://cdn.example.com/memorial.mp4"
        var clickedUrl: String? = null

        composeRule.setContent {
            AfternoteTheme {
                MemorialDetailScreen(
                    onBackClick = {},
                    onEditClick = {},
                    onDeleteConfirm = {},
                    content = MemorialDetailContent(memorialVideoUrl = videoUrl),
                    userName = "서영",
                    onVideoClick = { clickedUrl = it },
                )
            }
        }

        composeRule
            .onNodeWithText(videoCardLabel())
            .performScrollTo()
            .assertHasClickAction()
            .performClick()

        composeRule.runOnIdle { assertEquals(videoUrl, clickedUrl) }
    }

    @Test
    fun `영상 URL이 없으면 영상 카드를 표시하지 않는다`() {
        composeRule.setContent {
            AfternoteTheme {
                MemorialDetailScreen(
                    onBackClick = {},
                    onEditClick = {},
                    onDeleteConfirm = {},
                    onVideoClick = {},
                    content = MemorialDetailContent(),
                    userName = "서영",
                )
            }
        }

        composeRule.onNodeWithText(videoCardLabel()).assertDoesNotExist()
    }

    @Test
    fun `타이틀 개행과 하단 고지 문구를 그대로 표시한다`() {
        composeRule.setContent {
            AfternoteTheme {
                MemorialDetailScreen(
                    onBackClick = {},
                    onEditClick = {},
                    onDeleteConfirm = {},
                    onVideoClick = {},
                    content = MemorialDetailContent(),
                    userName = "서영",
                )
            }
        }

        composeRule.onNodeWithText("추억 노트에 대한\n서영님의 기록").assertExists()
        composeRule
            .onNodeWithText("이 추억 노트는 지정된 신뢰할 연락처에게만 공유되며,\n안전하게 보관됩니다.")
            .performScrollTo()
            .assertExists()
    }

    private fun videoCardLabel(): String = composeRule.activity.getString(R.string.afternote_editor_funeral_video_label)
}

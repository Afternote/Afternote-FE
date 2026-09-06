package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.detail.MemorialDetailContent
import com.afternote.feature.afternote.presentation.detail.MemorialDetailScreen
import com.afternote.feature.afternote.presentation.receiver.detail.MemorialReceivedDetailScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 추모 영상 썸네일 오버레이(#463 3번)를 고정한다.
 *
 * 시안([node 4327:72864](https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/?node-id=4327-72864))은
 * 썸네일 좌하단에 「추모 영상」 라벨을 두는데 코드에는 그라디언트와 재생 아이콘만 있었다.
 *
 * 발신자·수신자 상세가 **같은 한 벌**을 그리는지도 함께 단언한다 — 두 벌로 갈라져 있던 탓에
 * 한쪽만 고치면 다른 쪽에 결손이 남는 구조였다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MemorialVideoThumbnailTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `썸네일 좌하단에 추모 영상 라벨을 그린다`() {
        composeRule.setContent {
            AfternoteTheme {
                MemorialVideoThumbnail(thumbnailUrl = null)
            }
        }

        composeRule
            .onNodeWithTag(MEMORIAL_VIDEO_OVERLAY_LABEL_TEST_TAG, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `장식 라벨을 카드 접근성 설명에 합치지 않는다`() {
        composeRule.setContent {
            AfternoteTheme {
                MemorialDetailScreen(
                    onBackClick = {},
                    onEditClick = {},
                    onDeleteConfirm = {},
                    onVideoClick = {},
                    content =
                        MemorialDetailContent(
                            memorialVideoUrl = "https://cdn.example.com/memorial.mp4",
                        ),
                    userName = "서영",
                )
            }
        }

        composeRule.onNodeWithText("추모 영상").assertDoesNotExist()
    }

    @Test
    fun `재생 컨트롤에 접근성 라벨을 붙인다`() {
        composeRule.setContent {
            AfternoteTheme {
                MemorialVideoThumbnail(thumbnailUrl = null)
            }
        }

        composeRule
            .onNodeWithContentDescription("영상 재생", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `영상 길이는 서버 계약에 없어 표시하지 않는다`() {
        composeRule.setContent {
            AfternoteTheme {
                MemorialVideoThumbnail(thumbnailUrl = null)
            }
        }

        // 시안 둘째 줄 「2분 34초」를 하드코딩으로 되살리는 회귀를 막는다.
        composeRule.onNodeWithText("2분 34초", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `발신자 상세의 영상 카드가 같은 오버레이를 그린다`() {
        composeRule.setContent {
            AfternoteTheme {
                MemorialDetailScreen(
                    onBackClick = {},
                    onEditClick = {},
                    onDeleteConfirm = {},
                    onVideoClick = {},
                    content =
                        MemorialDetailContent(
                            memorialVideoUrl = "https://cdn.example.com/memorial.mp4",
                        ),
                    userName = "서영",
                )
            }
        }

        composeRule
            .onNodeWithTag(MEMORIAL_VIDEO_OVERLAY_LABEL_TEST_TAG, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `수신자 상세의 영상 카드가 같은 오버레이를 그린다`() {
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

        composeRule
            .onNodeWithTag(MEMORIAL_VIDEO_OVERLAY_LABEL_TEST_TAG, useUnmergedTree = true)
            .assertExists()
    }
}

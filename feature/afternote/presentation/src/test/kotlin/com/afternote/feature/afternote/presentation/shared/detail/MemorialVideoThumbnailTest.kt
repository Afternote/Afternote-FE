package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.detail.MemorialDetailContent
import com.afternote.feature.afternote.presentation.detail.MemorialDetailScreen
import com.afternote.feature.afternote.presentation.receiver.detail.MemorialReceivedDetailScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 추모 영상 썸네일 오버레이(#463 3번)를 고정한다.
 *
 * 썸네일은 그라디언트와 중앙 재생 버튼만 그린다. 시안
 * ([node 4327:72864](https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/?node-id=4327-72864))의
 * 좌하단 라벨 「추모 영상」은 바로 위 8dp 간격의 섹션 헤더 「장례식에 남길 영상」과 정보가 겹쳐
 * 뺐다(#1779) — 되살아나지 않는지 **문자열 자원 부재**로 단언한다. 텍스트 조회로는 못 잡는다
 * (라벨이 `clearAndSetSemantics {}` 로 시맨틱을 비우고 있었다). 자세한 사유는 그 테스트의 KDoc.
 *
 * 발신자·수신자 상세가 **같은 한 벌**을 그리는지도 함께 단언한다 — 두 벌로 갈라져 있던 탓에
 * 한쪽만 고치면 다른 쪽에 결손이 남는 구조였다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MemorialVideoThumbnailTest {
    @get:Rule
    val composeRule = createComposeRule()

    /**
     * 뺀 라벨을 되살리는 회귀를 **문자열 자원 부재**로 막는다 (#1779).
     *
     * **`onNodeWithText` 로는 못 잡는다.** 라벨은 접근성 병합 노드가 장황해지지 않도록
     * `clearAndSetSemantics {}` 로 시맨틱을 비우고 있었다 — 그래서 텍스트 조회에 안 걸리고,
     * 그 단언은 라벨이 **있는** 코드에 대고 돌려도 초록이다(실측). 태그로만 조회할 수 있었는데
     * 이 변경이 그 태그 파일을 함께 지웠다.
     *
     * 남는 확실한 신호는 **문자열 자원**이다. 라벨을 되살리려면 `afternote_memorial_video_overlay_label`
     * 을 다시 넣어야 하므로, 그 이름이 해석되면 되살아난 것이다.
     */
    @Test
    fun `오버레이 라벨 문자열 자원이 되살아나지 않는다`() {
        val context = RuntimeEnvironment.getApplication()

        val identifier =
            context.resources.getIdentifier(
                "afternote_memorial_video_overlay_label",
                "string",
                context.packageName,
            )

        assertEquals("라벨 문자열이 돌아왔다 — 섹션 헤더 「장례식에 남길 영상」과 중복이라 뺀 것이다 (#1779)", 0, identifier)
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
    fun `발신자 상세의 영상 카드가 같은 썸네일을 그린다`() {
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
            .onNodeWithContentDescription("영상 재생", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `수신자 상세의 영상 카드가 같은 썸네일을 그린다`() {
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
            .onNodeWithContentDescription("영상 재생", useUnmergedTree = true)
            .assertExists()
    }
}

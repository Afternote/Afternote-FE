package com.afternote.feature.afternote.presentation.detail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.afternote.core.ui.R as CoreUiR

/**
 * 추억 노트 상세 헤더 블록을 정본 시안
 * [node 4327:72819](https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/?node-id=4327-72819) 에 고정한다 (#463 5번).
 *
 * 종전 코드는 타이틀 아래에 «회색 카드 + 중앙 정렬 프로필» 을 따로 쌓았는데, 그 카드는 시안에 없다.
 * 시안은 타이틀 오른쪽에 겹친 원 스택을 붙이고 「최종 작성일」을 타이틀 바로 아래로 올린다.
 * 되돌아가면 프로필이 다시 타이틀 «아래» 로 내려가므로, 여기서 «같은 행» 을 잠근다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w480dp-h2000dp")
class MemorialDetailHeaderTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `프로필 원 스택을 타이틀 오른쪽 같은 행에 놓는다`() {
        setContent(finalWriteDate = "2025.11.26")

        val title = composeRule.onNodeWithText(TITLE).getUnclippedBoundsInRoot()
        val profile = composeRule.onNodeWithContentDescription(profileDescription()).getUnclippedBoundsInRoot()

        assertTrue(
            "프로필은 타이틀 오른쪽에 있어야 한다 (타이틀 right ${title.right}, 프로필 left ${profile.left})",
            profile.left >= title.right,
        )
        assertTrue(
            "프로필은 타이틀과 같은 행이어야 한다 (타이틀 ${title.top}~${title.bottom}, 프로필 ${profile.top}~${profile.bottom})",
            profile.top < title.bottom,
        )
    }

    @Test
    fun `최종 작성일을 타이틀 바로 아래 헤더 안에 그린다`() {
        setContent(finalWriteDate = "2025.11.26")

        val title = composeRule.onNodeWithText(TITLE).getUnclippedBoundsInRoot()
        val writtenDate = composeRule.activity.getString(R.string.afternote_last_written_date, "2025.11.26")
        val date = composeRule.onNodeWithText(writtenDate).getUnclippedBoundsInRoot()

        assertTrue(
            "최종 작성일은 타이틀과 같은 열에서 시작해야 한다 — 카드 안으로 다시 들어가면 왼쪽이 어긋난다 " +
                "(타이틀 left ${title.left}, 날짜 left ${date.left})",
            date.left == title.left,
        )
        // 시안 실측 간격 4. 종전 카드 배치에서는 24(Spacer) + 16(카드 패딩) 이 끼어 있었다.
        assertEquals(4.dp, date.top - title.bottom)
    }

    @Test
    fun `최종 작성일 값이 없으면 그 줄을 그리지 않는다`() {
        setContent(finalWriteDate = "")

        val prefixOnly = composeRule.activity.getString(R.string.afternote_last_written_date, "")

        composeRule.onNodeWithText(prefixOnly).assertDoesNotExist()
        composeRule.onNodeWithText(TITLE).assertExists()
    }

    @Test
    fun `겹친 장식 원은 접근성 트리에 노출하지 않는다`() {
        setContent(finalWriteDate = "2025.11.26")

        // 시안의 원은 셋이지만 실데이터가 붙는 것은 맨 앞 하나뿐이다. 뒤 둘은 장식이라
        // contentDescription 이 없어야 한다 — 있으면 스크린리더가 "프로필 이미지" 를 세 번 읽는다.
        composeRule.onAllNodesWithContentDescription(profileDescription()).assertCountEquals(1)
    }

    private fun setContent(finalWriteDate: String) {
        composeRule.setContent {
            AfternoteTheme {
                MemorialDetailScreen(
                    onBackClick = {},
                    onEditClick = {},
                    onDeleteConfirm = {},
                    onVideoClick = {},
                    content = MemorialDetailContent(finalWriteDate = finalWriteDate),
                    userName = "서영",
                )
            }
        }
    }

    private fun profileDescription(): String = composeRule.activity.getString(CoreUiR.string.core_ui_content_description_profile_image)

    private companion object {
        const val TITLE = "추억 노트에 대한\n서영님의 기록"
    }
}

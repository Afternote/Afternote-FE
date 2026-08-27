package com.afternote.feature.receiver.presentation.afternotelist

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.PROFILE_ICON_TEST_TAG
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeScreen
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.afternote.core.ui.R as CoreUiR
import com.afternote.feature.afternote.presentation.R as AfternoteR
import com.afternote.feature.receiver.presentation.R as ReceiverR

/**
 * 수신자 애프터노트 목록이 발신자용 크롬을 물려받지 않는지 (#620).
 *
 * 이 목록은 발신자와 같은 [AfternoteHomeScreen] 을 공유하는데, 종전에는 회원 상단바(프로필·설정)와
 * 발신자 문구("소중한 사람에게 남길 기록을 미리 정리해 보세요")가 그대로 따라왔다. 수신자는 로그인
 * 사용자가 아니고 기록을 남기는 주체도 아니라, 설정은 눌러도 아무 일이 없었다.
 *
 * 수신자 홈에 같은 규칙을 고정한 [com.afternote.feature.receiver.presentation.home.ReceiverHomeHeaderTest]
 * (#613) 의 목록 화면 판이다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceiverAfternoteListChromeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** [ReceiverAfternoteHomeEntry] 가 화면에 넘기는 것과 같은 구성 — Entry 는 hiltViewModel 을 잡아 직접 못 띄운다. */
    @Composable
    private fun ReceiverList() {
        val items =
            flowOf(
                PagingData.from(
                    listOf(
                        ListItemUiModel(
                            id = 1L,
                            serviceName = "인스타그램",
                            date = "2026.07.29",
                            iconResId = AfternoteR.drawable.feature_afternote_img_insta_pattern,
                            type = AfternoteType.SOCIAL_NETWORK,
                        ),
                    ),
                ),
            ).collectAsLazyPagingItems()

        AfternoteHomeScreen(
            items = items,
            selectedType = null,
            onTypeSelected = {},
            onListItemClick = { _, _ -> },
            headerDescription = stringResource(ReceiverR.string.receiver_afternote_list_header_description),
            nextStep = null,
        )
    }

    private fun renderList() {
        composeRule.setContent {
            AfternoteTheme { ReceiverList() }
        }
    }

    @Test
    fun `수신자 목록에는 회원 상단바 액션이 없다`() {
        renderList()

        composeRule.onNodeWithTag(PROFILE_ICON_TEST_TAG).assertDoesNotExist()
        val setting = composeRule.activity.getString(CoreUiR.string.core_ui_home_top_bar_setting)
        composeRule.onNodeWithContentDescription(setting).assertDoesNotExist()
    }

    @Test
    fun `수신자 목록 헤더는 발신자 작성 유도 문구를 쓰지 않는다`() {
        renderList()

        val authorDescription = composeRule.activity.getString(AfternoteR.string.afternote_home_header_description)
        composeRule.onNodeWithText(authorDescription).assertDoesNotExist()
    }

    @Test
    fun `수신자 목록 헤더는 수신자 관점 문구를 보여준다`() {
        renderList()

        val receiverDescription =
            composeRule.activity.getString(ReceiverR.string.receiver_afternote_list_header_description)
        composeRule.onNodeWithText(receiverDescription).assertIsDisplayed()
    }
}

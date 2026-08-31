package com.afternote.feature.afternote.presentation.receiver.afternotelist

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
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeScreen
import com.afternote.feature.afternote.presentation.shared.body.infinite.InfiniteListBody
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.afternote.core.ui.R as CoreUiR

/**
 * 수신자 애프터노트 목록이 발신자용 크롬을 물려받지 않는지 (#620).
 *
 * 이 목록은 발신자와 같은 [AfternoteHomeScreen] 을 공유하는데, 종전에는 회원 상단바(프로필·설정)와
 * 발신자 문구("소중한 사람에게 남길 기록을 미리 정리해 보세요")가 그대로 따라왔다. 수신자는 로그인
 * 사용자가 아니고 기록을 남기는 주체도 아니라, 설정은 눌러도 아무 일이 없었다.
 *
 * 수신자 홈에 같은 규칙을 고정한 `ReceiverHomeHeaderTest`(#613, :feature:receiver:presentation) 의
 * 목록 화면 판이다.
 *
 * 두 관심사를 서로 다른 깊이에서 띄우는 이유:
 * - 상단바는 [AfternoteHomeScreen] 의 `Scaffold` 가 본문 분기와 무관하게 항상 그리므로 화면째 띄운다.
 * - 헤더 문구는 본문 분기 안에 있어 화면째로는 못 본다. 화면은 `loadState.refresh` 가 Loading 인 동안
 *   초기 로딩(LoadingBody)을 그리는데, `collectAsLazyPagingItems` 의 첫 상태가 바로 그 Loading 이고
 *   이를 걷어내는 수집은 컴포지션 이펙트에서 돈다. Robolectric 테스트 클래스가 여럿 누적된 뒤 실행되면
 *   그 이펙트가 첫 단언까지 진행되지 않아 로딩 화면인 채로 실패한다 (#1370 실측: :feature:afternote:presentation
 *   에서 같은 화면이 단독 실행이면 통과, 29개 클래스 뒤면 실패 — waitUntil 은 타임아웃). 그래서 헤더는
 *   loadState 를 보지 않는 [InfiniteListBody] 를 직접 띄워 순번과 무관하게 판정한다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceiverAfternoteListChromeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `수신자 목록에는 회원 상단바 액션이 없다`() {
        composeRule.setContent { AfternoteTheme { ReceiverListScreen() } }

        composeRule.onNodeWithTag(PROFILE_ICON_TEST_TAG).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(string(CoreUiR.string.core_ui_home_top_bar_setting)).assertDoesNotExist()
    }

    @Test
    fun `수신자 목록 헤더는 수신자 관점 문구를 보여준다`() {
        composeRule.setContent { AfternoteTheme { ReceiverListBody() } }

        composeRule
            .onNodeWithText(string(R.string.afternote_receiver_afternote_list_header_description))
            .assertIsDisplayed()
    }

    @Test
    fun `수신자 목록 헤더는 발신자 작성 유도 문구를 쓰지 않는다`() {
        composeRule.setContent { AfternoteTheme { ReceiverListBody() } }

        composeRule
            .onNodeWithText(string(R.string.afternote_home_header_description))
            .assertDoesNotExist()
    }

    private fun string(resId: Int): String = composeRule.activity.getString(resId)

    /** [ReceiverAfternoteHomeEntry] 가 화면에 넘기는 것과 같은 구성 — Entry 는 hiltViewModel 을 잡아 직접 못 띄운다. */
    @Composable
    private fun ReceiverListScreen() {
        AfternoteHomeScreen(
            items = receiverItems(),
            selectedType = null,
            onTypeSelected = {},
            onListItemClick = { _, _ -> },
            headerDescription = stringResource(R.string.afternote_receiver_afternote_list_header_description),
            nextStep = null,
        )
    }

    /** 화면이 목록 상태에서 그리는 본문. 헤더는 loadState 가 아니라 넘겨받은 문구만 보므로 수집을 기다리지 않는다. */
    @Composable
    private fun ReceiverListBody() {
        InfiniteListBody(
            items = receiverItems(),
            selectedType = null,
            onTypeSelected = {},
            onListItemClick = { _, _ -> },
            headerDescription = stringResource(R.string.afternote_receiver_afternote_list_header_description),
            nextStep = null,
        )
    }

    @Composable
    private fun receiverItems() =
        flowOf(
            PagingData.from(
                listOf(
                    ListItemUiModel(
                        id = 1L,
                        serviceName = "인스타그램",
                        date = "2026.07.29",
                        iconResId = R.drawable.afternote_img_insta_pattern,
                        type = AfternoteType.SOCIAL_NETWORK,
                    ),
                ),
            ),
        ).collectAsLazyPagingItems()
}

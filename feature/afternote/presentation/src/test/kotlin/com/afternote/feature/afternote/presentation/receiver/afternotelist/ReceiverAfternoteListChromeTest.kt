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
import com.afternote.feature.afternote.presentation.home.AfternoteHomeScreen
import com.afternote.feature.afternote.presentation.shared.component.EmptyListBody
import com.afternote.feature.afternote.presentation.shared.component.InfiniteListBody
import com.afternote.feature.afternote.presentation.shared.component.ListItemUiModel
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

    /**
     * 0건 헤더 승격(#1175)이 수신자에게 새지 않는지.
     *
     * [AfternoteHomeScreen] 은 `showsHeaderOnEmptyList` 를 디폴트 없이 받으므로 수신자 호출부가 `false` 를
     * 명시하지 않으면 컴파일이 막힌다. 여기서는 그 `false` 가 실제로 그리는 본문에 발신자 조각(화면 제목·
     * 발신자 문구·NEXT STEP)이 하나도 없다는 것을 고정한다 — 헤더가 [EmptyListBody] 자체로 내려가는 순간
     * 수신자에게도 그대로 새기 때문이다(#620 과 같은 통로).
     */
    @Test
    fun `수신자 0건 목록 본문은 발신자 헤더를 물려받지 않는다`() {
        composeRule.setContent { AfternoteTheme { ReceiverEmptyBody() } }

        composeRule.onNodeWithText(string(R.string.afternote_home_title)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.afternote_home_header_description)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.afternote_home_next_step_section_title)).assertDoesNotExist()
    }

    /**
     * 0건 본문 문구도 수신자 관점인지.
     *
     * 종전에는 [EmptyListBody] 가 `afternote_empty_list_body` 를 직접 박아 두어, 수신자가 전달받은
     * 애프터노트 0건으로 이 본문에 닿으면 «아래 연필 버튼을 눌러 애프터노트를 등록해 보세요» 를 읽었다.
     * 그 연필 버튼은 수신자 화면에 없다 — `onFabClick` 을 넘기지 않으므로 [AfternoteHomeScreen] 이
     * FAB 자체를 그리지 않는다(아래 «작성 FAB 이 없다» 가 그 전제를 따로 고정한다). 못 누르는 버튼을
     * 누르라고 시키던 것이라 #620 과 같은 부류의 누수다.
     */
    @Test
    fun `수신자 0건 본문은 수신자 관점 문구를 보여준다`() {
        composeRule.setContent { AfternoteTheme { ReceiverEmptyBody() } }

        composeRule
            .onNodeWithText(string(R.string.afternote_receiver_list_empty_body))
            .assertIsDisplayed()
    }

    @Test
    fun `수신자 0건 본문은 발신자 작성 유도 문구를 쓰지 않는다`() {
        composeRule.setContent { AfternoteTheme { ReceiverEmptyBody() } }

        composeRule
            .onNodeWithText(string(R.string.afternote_empty_list_body))
            .assertDoesNotExist()
    }

    /**
     * 0건 문구를 수신자용으로 가른 이유의 전제 — 수신자 화면에는 누를 FAB 이 없다.
     *
     * FAB 은 상단바와 마찬가지로 `Scaffold` 가 본문 분기와 무관하게 그리므로 화면째 띄워 본다. 없음 단언은
     * 로딩 순번(클래스 KDoc 참조)에 걸리지 않는다 — 어느 본문이 그려지든 결과가 같다.
     */
    @Test
    fun `수신자 목록에는 작성 FAB 이 없다`() {
        composeRule.setContent { AfternoteTheme { ReceiverListScreen() } }

        composeRule
            .onNodeWithContentDescription(string(CoreUiR.string.core_ui_fab_content_description_add))
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
            // 수신자 0건 시안이 아직 없어 헤더를 올리지 않는다 — 근거는 ReceiverAfternoteHomeEntry 주석 (#1175).
            showsHeaderOnEmptyList = false,
            emptyListDescription = stringResource(R.string.afternote_receiver_list_empty_body),
        )
    }

    /** 화면이 0건(카테고리 필터 없음) 상태에서 그리는 본문. `showsHeaderOnEmptyList = false` 쪽 가지다. */
    @Composable
    private fun ReceiverEmptyBody() {
        EmptyListBody(description = stringResource(R.string.afternote_receiver_list_empty_body))
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

package com.afternote.feature.afternote.presentation.home

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.component.AfternoteListContent
import com.afternote.feature.afternote.presentation.shared.component.EmptyListBody
import com.afternote.feature.afternote.presentation.shared.component.ListItemUiModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #567 — 빈 목록 두 상태가 각자의 확정 문구를 보여주는지 검증한다.
 *
 * 전체 목록 0건은 [EmptyHomeBody](작성자)·[EmptyListBody](수신자), 카테고리 필터 결과 0건은
 * [AfternoteListContent] 내부 빈 상태 경로다. 두 문구가 서로의 상태에 새어 나오지 않아야 한다.
 *
 * 작성자 전체 0건을 [EmptyListBody] 가 아니라 [EmptyHomeBody] 로 판정하는 이유: 실제로 그려지는 것이
 * 그쪽이고, 이 상태를 목록 경로([AfternoteListContent])로 합치려는 «정리» 가 들어오면 전체 0건 문구가
 * 조용히 필터 0건 문구로 바뀐다. 그 리팩터링을 여기서 빨갛게 만든다 (#1175 후속).
 *
 * 두 상태를 가르는 [AfternoteHomeScreen] 전체가 아니라 각 빈 상태 컴포저블을 직접 띄우는 이유:
 * 화면은 `loadState.refresh` 가 Loading 인 동안 초기 로딩(LoadingBody)을 그리는데,
 * `collectAsLazyPagingItems` 의 첫 상태가 바로 그 Loading 이고 이를 걷어내는 수집은 컴포지션 이펙트에서 돈다.
 * 이 모듈처럼 Robolectric 테스트가 여러 클래스 누적된 뒤 실행되면 그 이펙트가 첫 단언까지 진행되지 않아
 * (실측: 이 클래스가 첫 번째면 통과, 32개 클래스 뒤면 로딩 화면인 채로 실패 — waitUntil 은 타임아웃)
 * 화면 단위로는 순서에 따라 결과가 갈린다. 문구 회귀는 두 Body 가 각자 무엇을 그리는지로 닫는다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteHomeEmptyCopyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `전체 목록 0건이면 애프터노트 등록 안내 문구를 보여준다`() {
        composeRule.setContent { AfternoteTheme { AuthorEmptyBody() } }

        composeRule.onNodeWithText(string(R.string.afternote_empty_list_body)).assertExists()
        composeRule.onNodeWithText(string(R.string.afternote_home_filtered_empty)).assertDoesNotExist()
    }

    /**
     * 수신자 경로(`showsHeaderOnEmptyList = false`)의 0건 본문. 문구 자체는 관점마다 다르지만(#1630),
     * **카테고리 필터 0건 문구가 섞이지 않는다**는 것은 같다. 발신자 문구가 수신자에게 새지 않는 것까지 함께 본다.
     */
    @Test
    fun `헤더 없는 0건 본문에도 카테고리 필터 문구가 새지 않는다`() {
        composeRule.setContent {
            AfternoteTheme { EmptyListBody(description = stringRes(R.string.afternote_receiver_list_empty_body)) }
        }

        composeRule.onNodeWithText(string(R.string.afternote_receiver_list_empty_body)).assertExists()
        composeRule.onNodeWithText(string(R.string.afternote_home_filtered_empty)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.afternote_empty_list_body)).assertDoesNotExist()
    }

    @Test
    fun `카테고리 필터 결과 0건이면 카테고리 전용 안내 문구를 보여준다`() {
        composeRule.setContent { AfternoteTheme { FilteredEmptyContent() } }

        composeRule.onNodeWithText(string(R.string.afternote_home_filtered_empty)).assertExists()
        composeRule.onNodeWithText(string(R.string.afternote_empty_list_body)).assertDoesNotExist()
    }

    private fun string(resId: Int): String = composeRule.activity.getString(resId)

    @Composable
    private fun stringRes(resId: Int): String = stringResource(resId)

    /** 작성자 전체 0건 본문. [AfternoteHomeScreen] 의 `showsHeaderOnEmptyList = true` 경로가 그리는 것. */
    @Composable
    private fun AuthorEmptyBody() {
        EmptyHomeBody(
            headerDescription = stringRes(R.string.afternote_home_header_description),
            nextStep = null,
            emptyListDescription = stringRes(R.string.afternote_empty_list_body),
            onTypeSelected = {},
        )
    }

    /** 카테고리를 고른 채 결과가 0건인 목록. [AfternoteListContent] 는 itemCount 만 보므로 수집을 기다리지 않는다. */
    @Composable
    private fun FilteredEmptyContent() {
        val items = flowOf(PagingData.empty<ListItemUiModel>()).collectAsLazyPagingItems()
        AfternoteListContent(
            items = items,
            selectedType = AfternoteType.SOCIAL_NETWORK,
            onTypeSelected = {},
            onListItemClick = { _, _ -> },
        )
    }
}

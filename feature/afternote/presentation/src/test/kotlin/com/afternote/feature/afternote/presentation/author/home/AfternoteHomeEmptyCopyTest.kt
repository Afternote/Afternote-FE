package com.afternote.feature.afternote.presentation.author.home

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
import com.afternote.feature.afternote.presentation.shared.body.EmptyListBody
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.AfternoteListContent
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #567 — 빈 목록 두 상태가 각자의 확정 문구를 보여주는지 검증한다.
 *
 * 전체 목록 0건은 [EmptyListBody], 카테고리 필터 결과 0건은 [AfternoteListContent] 내부 빈 상태 경로다.
 * 두 문구가 서로의 상태에 새어 나오지 않아야 한다.
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
        composeRule.setContent {
            AfternoteTheme { EmptyListBody(description = stringRes(R.string.feature_afternote_empty_list_body)) }
        }

        composeRule.onNodeWithText(string(R.string.feature_afternote_empty_list_body)).assertExists()
        composeRule.onNodeWithText(string(R.string.afternote_home_filtered_empty)).assertDoesNotExist()
    }

    @Test
    fun `카테고리 필터 결과 0건이면 카테고리 전용 안내 문구를 보여준다`() {
        composeRule.setContent { AfternoteTheme { FilteredEmptyContent() } }

        composeRule.onNodeWithText(string(R.string.afternote_home_filtered_empty)).assertExists()
        composeRule.onNodeWithText(string(R.string.feature_afternote_empty_list_body)).assertDoesNotExist()
    }

    private fun string(resId: Int): String = composeRule.activity.getString(resId)

    @Composable
    private fun stringRes(resId: Int): String = stringResource(resId)

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

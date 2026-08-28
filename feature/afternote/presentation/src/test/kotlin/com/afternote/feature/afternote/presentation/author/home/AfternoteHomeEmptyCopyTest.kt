package com.afternote.feature.afternote.presentation.author.home

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
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
 * 전체 목록 0건은 [com.afternote.feature.afternote.presentation.shared.body.EmptyListBody] 경로,
 * 카테고리 필터 결과 0건은 [com.afternote.feature.afternote.presentation.shared.body.infinite.InfiniteListBody]
 * 내부 빈 상태 경로다. 두 문구가 서로의 상태에 새어 나오지 않아야 한다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteHomeEmptyCopyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `전체 목록 0건이면 애프터노트 등록 안내 문구를 보여준다`() {
        composeRule.setContent { EmptyHomeScreen(selectedType = null) }

        composeRule.onNodeWithText(string(R.string.feature_afternote_empty_list_body)).assertExists()
        composeRule.onNodeWithText(string(R.string.afternote_home_filtered_empty)).assertDoesNotExist()
    }

    @Test
    fun `카테고리 필터 결과 0건이면 카테고리 전용 안내 문구를 보여준다`() {
        composeRule.setContent { EmptyHomeScreen(selectedType = AfternoteType.SOCIAL_NETWORK) }

        composeRule.onNodeWithText(string(R.string.afternote_home_filtered_empty)).assertExists()
        composeRule.onNodeWithText(string(R.string.feature_afternote_empty_list_body)).assertDoesNotExist()
    }

    private fun string(resId: Int): String = composeRule.activity.getString(resId)

    /** refresh 를 NotLoading 으로 명시해 초기 로딩 분기(LoadingBody)를 확정적으로 지나친다. */
    @Composable
    private fun EmptyHomeScreen(selectedType: AfternoteType?) {
        val items =
            flowOf(
                PagingData.empty<ListItemUiModel>(
                    LoadStates(
                        refresh = LoadState.NotLoading(endOfPaginationReached = true),
                        prepend = LoadState.NotLoading(endOfPaginationReached = true),
                        append = LoadState.NotLoading(endOfPaginationReached = true),
                    ),
                ),
            ).collectAsLazyPagingItems()
        AfternoteTheme {
            AfternoteHomeScreen(
                items = items,
                selectedType = selectedType,
                onTypeSelected = {},
                onListItemClick = { _, _ -> },
                headerDescription = "소중한 사람에게 남길 기록을 미리 정리해 보세요.",
                nextStep = null,
                onFabClick = {},
            )
        }
    }
}

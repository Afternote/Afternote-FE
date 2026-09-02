package com.afternote.feature.afternote.presentation.home

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.component.ListItemUiModel
import com.afternote.feature.afternote.presentation.shared.component.ListRefreshErrorBanner
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * 목록이 남아 있는 상태의 새로고침 실패가 무음으로 끝나지 않는지에 대한 회귀 가드 (#705).
 *
 * 종전 [AfternoteHomeScreen] 은 `itemCount > 0` 이면 `LoadState.Error` 를 어디에도 싣지 않아,
 * 당겨 새로고침이 실패해도 화면은 이전 목록 그대로였다.
 *
 * 표시 여부를 정하는 판정은 화면 파일의 private 함수다 — 테스트가 부르려고 공개 범위를 넓히지
 * 않는다(#1678). 세 갈래를 화면을 그려서 확인한다. 로드 상태를 [PagingData.from] 에 직접 실어
 * 수집이 끝난 뒤의 상태를 고정하므로, 첫 상태가 Loading 인 흐름에 기대지 않는다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteHomeRefreshErrorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `목록이 남아 있는 새로고침 실패는 배너로 알린다`() {
        composeRule.setContent { AfternoteTheme { HomeScreen(itemCount = 1, refresh = errorState) } }

        composeRule.onNodeWithText(string(R.string.afternote_home_refresh_error)).assertIsDisplayed()
    }

    @Test
    fun `보여 줄 것이 없는 실패는 전면 오류가 맡으므로 배너를 그리지 않는다`() {
        composeRule.setContent { AfternoteTheme { HomeScreen(itemCount = 0, refresh = errorState) } }

        composeRule.onNodeWithText(string(R.string.afternote_home_refresh_error)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.afternote_home_load_error)).assertIsDisplayed()
    }

    @Test
    fun `실패가 아니면 배너를 그리지 않는다`() {
        composeRule.setContent {
            AfternoteTheme { HomeScreen(itemCount = 1, refresh = LoadState.NotLoading(endOfPaginationReached = true)) }
        }

        composeRule.onNodeWithText(string(R.string.afternote_home_refresh_error)).assertDoesNotExist()
    }

    @Test
    fun `배너는 실패 안내와 다시 시도를 함께 내놓는다`() {
        var retries = 0
        composeRule.setContent {
            AfternoteTheme { ListRefreshErrorBanner(onRetry = { retries += 1 }) }
        }

        composeRule.onNodeWithText(string(R.string.afternote_home_refresh_error)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.afternote_home_retry)).performClick()

        composeRule.runOnIdle { assertEquals(1, retries) }
    }

    @Composable
    private fun HomeScreen(
        itemCount: Int,
        refresh: LoadState,
    ) {
        val data =
            PagingData.from(
                data = List(itemCount) { index -> listItem(index.toLong()) },
                sourceLoadStates =
                    LoadStates(
                        refresh = refresh,
                        prepend = LoadState.NotLoading(endOfPaginationReached = true),
                        append = LoadState.NotLoading(endOfPaginationReached = true),
                    ),
            )
        AfternoteHomeScreen(
            items = flowOf(data).collectAsLazyPagingItems(),
            selectedType = null,
            onTypeSelected = {},
            onListItemClick = { _, _ -> },
            headerDescription = "",
            nextStep = null,
            showsHeaderOnEmptyList = false,
        )
    }

    private fun listItem(id: Long) =
        ListItemUiModel(
            id = id,
            serviceName = "인스타그램",
            date = "2026.07.29",
            iconResId = R.drawable.afternote_img_insta_pattern,
            type = AfternoteType.SOCIAL_NETWORK,
        )

    private val errorState get() = LoadState.Error(IOException("목록 새로고침 실패"))

    private fun string(resId: Int): String = composeRule.activity.getString(resId)
}

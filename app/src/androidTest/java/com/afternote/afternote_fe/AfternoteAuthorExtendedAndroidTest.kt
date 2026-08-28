package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeEntry
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class AfternoteAuthorExtendedAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun home_loadingErrorRetrySuccess_filterAndRoutesStayConnectedToRepository() {
        val firstLoadStarted = CompletableDeferred<Unit>()
        val releaseFirstLoad = CompletableDeferred<Unit>()
        val pagingSource =
            RetryListPagingSource(
                firstLoadStarted = firstLoadStarted,
                releaseFirstLoad = releaseFirstLoad,
                successItems = authorListItems(),
            )
        val listFlows = mutableMapOf<AfternoteType?, Flow<PagingData<ListItem>>>()
        val repository =
            FakeAfternoteRepository.strict().apply {
                onGetPagedAfternotes = { type -> listFlows[type] ?: flowOf(PagingData.empty()) }
            }
        listFlows[null] =
            Pager(PagingConfig(pageSize = 20)) { pagingSource }.flow
        listFlows[AfternoteType.SOCIAL_NETWORK] = flowOf(PagingData.empty())
        val viewModel = AfternoteHomeViewModel(repository)
        val detailRoutes = mutableListOf<Long>()
        val addRoutes = mutableListOf<AfternoteType?>()

        composeRule.setContent {
            AfternoteTheme {
                AfternoteHomeEntry(
                    navigateToDetail = detailRoutes::add,
                    navigateToAdd = addRoutes::add,
                    onSettingClick = {},
                    viewModel = viewModel,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { firstLoadStarted.isCompleted }
        composeRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()

        releaseFirstLoad.complete(Unit)
        composeRule.onNodeWithText("애프터노트 목록을 불러오지 못했습니다.").assertIsDisplayed()
        assertEquals(1, pagingSource.loadCalls.get())

        composeRule.onNodeWithText("다시 시도").performClick()
        composeRule.onNodeWithText("Instagram").assertIsDisplayed()
        assertEquals(2, pagingSource.loadCalls.get())

        composeRule.onNodeWithText("Instagram").performClick()
        composeRule.onNodeWithText("Google Drive").performScrollTo().performClick()
        composeRule
            .onNodeWithContentDescription("추억 노트")
            .performScrollTo()
            .performClick()
        assertEquals(listOf(101L, 102L, 103L), detailRoutes)

        composeRule.onNodeWithText("소셜네트워크").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.requestedTypes.lastOrNull() == AfternoteType.SOCIAL_NETWORK
        }
        composeRule.onNodeWithText("소셜네트워크").assertIsSelected()
        composeRule
            .onNodeWithText("해당 카테고리에 등록된 애프터노트가 없어요.")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("추가").performClick()

        assertEquals(listOf(AfternoteType.SOCIAL_NETWORK), addRoutes)
        listFlows[null] = flowOf(PagingData.empty())
        composeRule.onNodeWithText("전체").performClick()
        composeRule
            .onNodeWithText("아직 등록된 답변이 없어요.\n답변을 등록해 자신을 알아 보아요.")
            .assertIsDisplayed()
        assertEquals(
            listOf(null, AfternoteType.SOCIAL_NETWORK, null),
            repository.requestedTypes,
        )
    }
}

private class RetryListPagingSource(
    private val firstLoadStarted: CompletableDeferred<Unit>,
    private val releaseFirstLoad: CompletableDeferred<Unit>,
    private val successItems: List<ListItem>,
) : PagingSource<Int, ListItem>() {
    val loadCalls = AtomicInteger()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListItem> {
        val attempt = loadCalls.incrementAndGet()
        return if (attempt == 1) {
            firstLoadStarted.complete(Unit)
            releaseFirstLoad.await()
            LoadResult.Error(IllegalStateException("offline"))
        } else {
            LoadResult.Page(
                data = successItems,
                prevKey = null,
                nextKey = null,
            )
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ListItem>): Int? = null
}

private fun authorListItems(): List<ListItem> =
    listOf(
        ListItem(
            id = 101L,
            serviceName = "Instagram",
            date = "2026.08.22",
            type = AfternoteType.SOCIAL_NETWORK,
        ),
        ListItem(
            id = 102L,
            serviceName = "Google Drive",
            date = "2026.08.22",
            type = AfternoteType.GALLERY_AND_FILES,
        ),
        ListItem(
            id = 103L,
            serviceName = "추억 노트",
            date = "2026.08.22",
            type = AfternoteType.MEMORIAL,
        ),
    )

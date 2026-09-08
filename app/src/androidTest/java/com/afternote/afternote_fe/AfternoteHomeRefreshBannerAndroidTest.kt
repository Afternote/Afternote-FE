package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeErrorReporter
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import com.afternote.feature.afternote.presentation.home.AfternoteHomeEntry
import com.afternote.feature.afternote.presentation.home.AfternoteHomeViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import com.afternote.feature.afternote.presentation.R as AfternoteR

/**
 * 새로고침 실패의 표시 갈림 세 가지를 실기에서 잠근다 (#1790 · 구현은 #705).
 *
 * | 상태 | 그려야 하는 것 |
 * |---|---|
 * | 실패 + 목록 0건 | 전면 오류 본문 |
 * | 실패 아님 | 둘 다 아님 |
 * | 실패 + 목록 있음 | 새로고침 실패 배너 (목록은 그대로) |
 *
 * **왜 계측인가.** 이 갈림은 `AfternoteHomeScreen` 본문의 `when` 이 `LazyPagingItems.loadState` 와
 * `itemCount` 를 함께 보고 정한다. JVM(Robolectric)에서 화면을 띄우면 `collectAsLazyPagingItems` 의
 * 첫 로드 상태가 `Loading` 인 채로 남아 단언 시점까지 이펙트가 진행되지 않는다 — 클래스가 누적된
 * 모듈 전량 실행에서 재현되고 `waitUntil` 로도 풀리지 않는다(#1443 이 실측, PR #1608 에서 두 번 무너짐).
 * 실기에서는 같은 수집이 정상으로 돌아 세 갈림이 결정적으로 그려진다.
 *
 * 판정 함수(`shouldShowRefreshErrorBanner`)의 공개 범위는 넓히지 않는다(#1678). 화면을 그대로 띄워
 * **그려진 결과**로 확인하므로 프로덕션 seam 도 새로 만들지 않는다 — 이 파일은 #1804(MVI Screen/Content
 * 2단 전환)이 다시 쓸 화면 파일을 건드리지 않는다.
 *
 * 상태 전이는 페이징 소스가 만든다. 실패/성공은 [failing] 이 정하고, 「당겨 새로고침」은
 * 현재 세대의 소스를 무효화해 만든다 — `LazyPagingItems.refresh()` 가 내부에서 하는 것과 같은 일이라
 * 제스처 임계값에 결과가 좌우되지 않는다.
 */
@RunWith(AndroidJUnit4::class)
class AfternoteHomeRefreshBannerAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    private val failing = AtomicBoolean(true)
    private val currentSource = AtomicReference<PagingSource<Int, ListItem>?>(null)

    @Test
    fun homeRefreshFailure_showsBannerOverKeptList_andFullErrorOnlyWhenListIsEmpty() {
        val repository =
            FakeAfternoteRepository.strict().apply {
                onGetPagedAfternotes = {
                    Pager(PagingConfig(pageSize = 20)) {
                        ScriptedListPagingSource(failing, authorListItems())
                            .also(currentSource::set)
                    }.flow
                }
            }
        val viewModel = AfternoteHomeViewModel(repository, FakeErrorReporter())

        composeRule.setContent {
            AfternoteTheme {
                AfternoteHomeEntry(
                    navigateToDetail = {},
                    navigateToAdd = {},
                    onSettingClick = {},
                    viewModel = viewModel,
                )
            }
        }

        // 실패 + 목록 0건 → 전면 오류만.
        awaitText(fullErrorCopy)
        composeRule.onNodeWithText(fullErrorCopy).assertIsDisplayed()
        composeRule.onNodeWithText(bannerCopy).assertDoesNotExist()

        // 실패 아님 → 둘 다 아님. 목록이 실제로 그려졌음을 함께 단언해 로딩 화면의 false green 을 막는다.
        failing.set(false)
        composeRule.onNodeWithText(retryCopy).performClick()
        awaitText(FIRST_ITEM_NAME)
        composeRule.onNodeWithText(FIRST_ITEM_NAME).assertIsDisplayed()
        composeRule.onNodeWithText(bannerCopy).assertDoesNotExist()
        composeRule.onNodeWithText(fullErrorCopy).assertDoesNotExist()

        // 실패 + 목록 있음 → 배너. 전면 오류가 목록을 밀어내지 않는다.
        failing.set(true)
        refreshList()
        awaitText(bannerCopy)
        composeRule.onNodeWithText(bannerCopy).assertIsDisplayed()
        composeRule.onNodeWithText(fullErrorCopy).assertDoesNotExist()
        composeRule.onNodeWithText(FIRST_ITEM_NAME).assertIsDisplayed()

        // 배너의 «다시 시도» 가 같은 목록을 복구하고 배너를 걷는다.
        failing.set(false)
        composeRule.onNodeWithText(retryCopy).performClick()
        awaitTextGone(bannerCopy)
        composeRule.onNodeWithText(FIRST_ITEM_NAME).assertIsDisplayed()
        composeRule.onNodeWithText(fullErrorCopy).assertDoesNotExist()
    }

    /** 「당겨 새로고침」과 같은 경로 — 현재 세대의 소스를 무효화해 refresh 로드를 다시 태운다. */
    private fun refreshList() {
        val source = requireNotNull(currentSource.get()) { "페이징 소스가 아직 만들어지지 않았다" }
        source.invalidate()
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitTextGone(text: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }

    /** 문구는 리소스가 정본이다 — 문구가 바뀌어도 단언이 따라간다 (#567). */
    private fun copy(resId: Int): String =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getString(resId)

    private val fullErrorCopy: String get() = copy(AfternoteR.string.afternote_home_load_error)
    private val bannerCopy: String get() = copy(AfternoteR.string.afternote_home_refresh_error)
    private val retryCopy: String get() = copy(AfternoteR.string.afternote_home_retry)

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L

        /** 목록이 살아 있는지 보는 기준 항목. 첫 카드라 스크롤 없이 그려진다. */
        const val FIRST_ITEM_NAME = "Instagram"
    }
}

/** 실패/성공을 [failing] 으로 갈아끼우는 목록 소스. 세대마다 새 인스턴스가 필요해 팩토리가 매번 만든다. */
private class ScriptedListPagingSource(
    private val failing: AtomicBoolean,
    private val items: List<ListItem>,
) : PagingSource<Int, ListItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListItem> =
        if (failing.get()) {
            LoadResult.Error(IllegalStateException("offline"))
        } else {
            LoadResult.Page(data = items, prevKey = null, nextKey = null)
        }

    override fun getRefreshKey(state: PagingState<Int, ListItem>): Int? = null
}

private fun authorListItems(): List<ListItem> =
    listOf(
        ListItem(
            id = 201L,
            serviceName = "Instagram",
            date = "2026.09.03",
            type = AfternoteType.SOCIAL_NETWORK,
        ),
        ListItem(
            id = 202L,
            serviceName = "Google Drive",
            date = "2026.09.03",
            type = AfternoteType.GALLERY_AND_FILES,
        ),
    )

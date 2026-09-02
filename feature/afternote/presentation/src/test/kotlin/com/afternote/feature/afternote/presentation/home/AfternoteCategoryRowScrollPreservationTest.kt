package com.afternote.feature.afternote.presentation.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.component.AfternoteListContent
import com.afternote.feature.afternote.presentation.shared.component.ListItemUiModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #1635 ② — 카테고리 행의 가로 스크롤 위치가 본문 분기를 건너 살아남는지.
 *
 * 탭 5개(전체·소셜네트워크·비즈니스·갤러리 및 파일·추모)의 폭 합이 기본 화면 폭을 넘어 이 행은 실제로
 * 스크롤된다. 종전에는 [AfternoteTypeFilterRow] 가 `rememberScrollState()` 를 **자기 안에서** 만들어,
 * 본문이 바뀌면(0건 → 로딩 → 목록) 그 서브트리가 폐기되면서 위치가 0 으로 돌아갔다. 게다가 호출부가
 * 둘이라(목록 경로·0건 경로) `remember` 슬롯 자체가 서로 달랐다. **행을 오른쪽으로 밀어 끝 탭을 고르면
 * 전환 후 방금 고른 탭이 화면 밖에 있었다.**
 *
 * 여기서 재현하는 것은 실제 전환 순서 그대로다 — 무필터 0건([EmptyHomeBody]) → 전환 중
 * ([ReloadingBody]) → 필터 결과([AfternoteListContent]). 화면이 `when` 위에서 만들어 셋에 꿰는 그
 * [ScrollState] 를 테스트가 대신 들고 있다.
 *
 * [ScrollState.maxValue] 를 매 단계 함께 단언하는 이유: 넘긴 상태를 본문이 행까지 전달하지 않고 행이
 * 다시 자기 것을 만들면, 넘긴 쪽 상태는 **레이아웃에 붙지 않아** `maxValue` 가 초깃값
 * ([Int.MAX_VALUE])에 머문다. `value` 만 보면 그 회귀가 통과해 버린다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteCategoryRowScrollPreservationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private enum class Stage { EMPTY, RELOADING, FILTERED }

    @Test
    fun `본문이 바뀌어도 카테고리 행의 가로 스크롤 위치가 남는다`() {
        val scrollState = ScrollState(initial = 0)
        var stage by mutableStateOf(Stage.EMPTY)
        composeRule.setContent { AfternoteTheme { Bodies(stage, scrollState) } }
        composeRule.waitForIdle()

        val maxValue = scrollState.maxValue
        assertTrue(
            "카테고리 행이 스크롤되지 않아 이 회귀를 잴 수 없다 (maxValue=$maxValue)",
            maxValue in 1..<Int.MAX_VALUE,
        )
        composeRule.runOnIdle { scrollState.dispatchRawDelta(SCROLL_DELTA) }
        composeRule.waitForIdle()
        val scrolled = composeRule.runOnIdle { scrollState.value }
        assertTrue("행을 오른쪽으로 밀지 못했다 (value=$scrolled)", scrolled > 0)

        stage = Stage.RELOADING
        composeRule.waitForIdle()
        assertEquals("전환 중 로딩 본문에서 가로 스크롤이 초기화됐다", scrolled, scrollState.value)
        assertEquals("전환 중 로딩 본문이 넘겨받은 스크롤 상태를 행까지 전달하지 않았다", maxValue, scrollState.maxValue)

        stage = Stage.FILTERED
        composeRule.waitForIdle()
        assertEquals("전환이 끝나자 가로 스크롤이 초기화됐다", scrolled, scrollState.value)
        assertEquals("목록 본문이 넘겨받은 스크롤 상태를 행까지 전달하지 않았다", maxValue, scrollState.maxValue)
    }

    @Composable
    private fun Bodies(
        stage: Stage,
        scrollState: ScrollState,
    ) {
        // 탭 폭 합이 확실히 넘치도록 폭을 고정한다 — Robolectric 폰트 폭에 기대지 않는다.
        Box(
            modifier =
                Modifier
                    .width(ROW_WIDTH)
                    .fillMaxHeight(),
        ) {
            when (stage) {
                Stage.EMPTY -> {
                    EmptyHomeBody(
                        headerDescription = stringResource(R.string.afternote_home_header_description),
                        nextStep = null,
                        emptyListDescription = stringResource(R.string.afternote_empty_list_body),
                        onTypeSelected = {},
                        filterRowScrollState = scrollState,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Stage.RELOADING -> {
                    ReloadingBody(
                        headerDescription = stringResource(R.string.afternote_home_header_description),
                        nextStep = null,
                        selectedType = AfternoteType.MEMORIAL,
                        onTypeSelected = {},
                        filterRowScrollState = scrollState,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Stage.FILTERED -> {
                    AfternoteListContent(
                        items = flowOf(PagingData.empty<ListItemUiModel>()).collectAsLazyPagingItems(),
                        selectedType = AfternoteType.MEMORIAL,
                        onTypeSelected = {},
                        onListItemClick = { _, _ -> },
                        filterRowScrollState = scrollState,
                    )
                }
            }
        }
    }

    private companion object {
        /**
         * 탭 5개가 들어가지 않는 폭. 이 행이 실제로 스크롤되는 조건을 확정한다 — Robolectric 폰트 폭에
         * 기대지 않는다(`AfternoteCategoryRowInteractionTest` 와 같은 수법).
         *
         * **넉넉히 좁혀야 한다.** 200dp 로 재 보면 넘침이 9px 뿐이라, 끝까지 밀어 더보기 화살표가 사라지는
         * 순간 그 폭(16dp + 여백)이 탭 쪽으로 돌아가 행이 다 들어가고 `maxValue` 가 0 이 된다 —
         * [ScrollState.maxValue] 세터가 위치를 0 으로 깎아, 회귀가 없어도 이 테스트가 빨개진다.
         */
        val ROW_WIDTH = 100.dp

        /** 오른쪽으로 미는 양. 끝 탭을 고르려면 사용자가 하는 그 동작이다. */
        const val SCROLL_DELTA = 40f
    }
}

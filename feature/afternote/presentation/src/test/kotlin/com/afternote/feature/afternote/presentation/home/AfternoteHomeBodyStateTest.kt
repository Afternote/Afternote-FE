package com.afternote.feature.afternote.presentation.home

import androidx.paging.LoadState
import com.afternote.feature.afternote.domain.AfternoteType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * #1634 — 목록 화면의 본문 분기 «순서» 회귀 가드.
 *
 * 종전에는 `refreshState is LoadState.Error && itemCount == 0` 이 목록 경로보다 앞서, 카테고리 필터를
 * 건 채 조회가 실패하면 전면 에러가 **카테고리 행까지 걷어 갔다.** 화면에 남는 조작은 «다시 시도»
 * 하나뿐이라 다른 카테고리로도 「전체」로도 나갈 수 없었다 — 바로 아래 줄 주석이 「막다른 상태 방지」라고
 * 적어 둔 그 상태를 윗 분기가 뚫고 있었다.
 *
 * 화면을 띄우지 않고 [afternoteHomeBodyState] 로 판정하는 이유는 그 KDoc 참조 — 이 모듈에서 화면 단위
 * Paging 판정은 Robolectric 실행 순번에 걸린다. 각 본문이 실제로 무엇을 그리는지는
 * `AfternoteHomeFilteredErrorTest`·`AfternoteHomeEmptyHeaderTest` 가 본문 컴포저블을 직접 띄워 본다.
 */
class AfternoteHomeBodyStateTest {
    @Test
    fun `필터를 건 채 조회가 실패하면 카테고리 행이 남는 본문을 고른다`() {
        val state =
            afternoteHomeBodyState(
                refreshState = loadError(),
                itemCount = 0,
                selectedType = AfternoteType.BUSINESS,
            )

        assertEquals(AfternoteHomeBodyState.FilteredError(AfternoteType.BUSINESS), state)
    }

    /** 완료 조건 — 필터 없는 전면 실패의 종전 렌더는 그대로 둔다. 「전체」가 기본 상태라 돌아갈 곳이 없다. */
    @Test
    fun `필터 없는 전면 실패는 종전대로 전면 에러 본문이다`() {
        val state =
            afternoteHomeBodyState(
                refreshState = loadError(),
                itemCount = 0,
                selectedType = null,
            )

        assertEquals(AfternoteHomeBodyState.Error, state)
    }

    @Test
    fun `이미 그린 목록이 있으면 새로고침 실패에도 목록을 그대로 그린다`() {
        val state =
            afternoteHomeBodyState(
                refreshState = loadError(),
                itemCount = 3,
                selectedType = null,
            )

        assertEquals(AfternoteHomeBodyState.List, state)
    }

    @Test
    fun `보여 줄 것이 없는 첫 로드는 로딩 본문이다`() {
        val state =
            afternoteHomeBodyState(
                refreshState = LoadState.Loading,
                itemCount = 0,
                selectedType = null,
            )

        assertEquals(AfternoteHomeBodyState.InitialLoading, state)
    }

    @Test
    fun `이미 그린 목록의 새로고침은 목록을 유지한다`() {
        val state =
            afternoteHomeBodyState(
                refreshState = LoadState.Loading,
                itemCount = 3,
                selectedType = null,
            )

        assertEquals(AfternoteHomeBodyState.List, state)
    }

    /** 실패가 아닌 «성공했는데 0건» 은 종전대로 목록 경로에 남는다 — 카테고리 행은 그쪽이 그린다 (#567·#1633). */
    @Test
    fun `필터 결과가 0건이면 목록 경로에 남긴다`() {
        val state =
            afternoteHomeBodyState(
                refreshState = LoadState.NotLoading(endOfPaginationReached = true),
                itemCount = 0,
                selectedType = AfternoteType.BUSINESS,
            )

        assertEquals(AfternoteHomeBodyState.List, state)
    }

    @Test
    fun `0건이고 필터도 없으면 첫 진입 빈 본문이다`() {
        val state =
            afternoteHomeBodyState(
                refreshState = LoadState.NotLoading(endOfPaginationReached = true),
                itemCount = 0,
                selectedType = null,
            )

        assertEquals(AfternoteHomeBodyState.Empty, state)
    }

    private fun loadError(): LoadState.Error = LoadState.Error(IllegalStateException("조회 실패"))
}

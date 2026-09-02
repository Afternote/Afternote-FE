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
 *
 * #1635 로 «상단을 이미 보고 있었는가» 축이 하나 붙었다 — 카테고리 전환·재시도가 만드는
 * `refresh = Loading` + `itemCount == 0` 을 첫 진입과 같이 판정하면 헤더·필터 행이 통째로 사라진다.
 * 그 축의 되먹임([drawsTopChrome])까지 여기서 함께 고정한다.
 */
class AfternoteHomeBodyStateTest {
    @Test
    fun `필터를 건 채 조회가 실패하면 카테고리 행이 남는 본문을 고른다`() {
        val state =
            afternoteHomeBodyState(
                refreshState = loadError(),
                itemCount = 0,
                selectedType = AfternoteType.BUSINESS,
                chromeAlreadyVisible = true,
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
                chromeAlreadyVisible = false,
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
                chromeAlreadyVisible = true,
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
                chromeAlreadyVisible = false,
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
                chromeAlreadyVisible = true,
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
                chromeAlreadyVisible = true,
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
                chromeAlreadyVisible = true,
            )

        assertEquals(AfternoteHomeBodyState.Empty, state)
    }

    /**
     * #1635 ① — 카테고리 전환 중 상단이 사라지지 않는다.
     *
     * 전환은 새 Paging 세대를 만들고 그 첫 상태가 `Loading` 이다. 0건 상태에서 전환하면 `itemCount` 도
     * 0 이라 종전에는 첫 진입과 똑같이 판정돼 헤더·필터 행이 통째로 로딩 화면에 덮였다.
     */
    @Test
    fun `상단을 보고 있던 중의 로드는 상단을 남기는 본문이다`() {
        val state =
            afternoteHomeBodyState(
                refreshState = LoadState.Loading,
                itemCount = 0,
                selectedType = AfternoteType.BUSINESS,
                chromeAlreadyVisible = true,
            )

        assertEquals(AfternoteHomeBodyState.Reloading(AfternoteType.BUSINESS), state)
    }

    /** 「전체」로 돌아오는 전환도 같은 자리를 지난다 — 이쪽 선택 탭은 정의상 `null` 이다. */
    @Test
    fun `전체로 돌아오는 전환도 상단을 남기고 전체 탭을 들고 간다`() {
        val state =
            afternoteHomeBodyState(
                refreshState = LoadState.Loading,
                itemCount = 0,
                selectedType = null,
                chromeAlreadyVisible = true,
            )

        assertEquals(AfternoteHomeBodyState.Reloading(null), state)
    }

    /** 완료 조건 — 첫 진입의 종전 렌더(전체 로딩)는 그대로 둔다. 아직 보여 준 상단이 없다. */
    @Test
    fun `상단을 보여 준 적이 없으면 종전대로 전체 로딩이다`() {
        val state =
            afternoteHomeBodyState(
                refreshState = LoadState.Loading,
                itemCount = 0,
                selectedType = AfternoteType.BUSINESS,
                chromeAlreadyVisible = false,
            )

        assertEquals(AfternoteHomeBodyState.InitialLoading, state)
    }

    /**
     * #1634 가 남긴 구멍 — 필터 실패에서 «다시 시도» 를 누르면 그 순간 상단이 사라졌다.
     * 재시도도 `refresh = Loading` + `itemCount == 0` 을 지난다.
     */
    @Test
    fun `필터 실패에서 재시도를 눌러도 카테고리 행이 남는다`() {
        val settled =
            afternoteHomeBodyState(
                refreshState = loadError(),
                itemCount = 0,
                selectedType = AfternoteType.BUSINESS,
                chromeAlreadyVisible = false,
            )
        val retrying =
            afternoteHomeBodyState(
                refreshState = LoadState.Loading,
                itemCount = 0,
                selectedType = AfternoteType.BUSINESS,
                chromeAlreadyVisible = settled.drawsTopChrome(showsHeaderOnEmptyList = true),
            )

        assertEquals(AfternoteHomeBodyState.FilteredError(AfternoteType.BUSINESS), settled)
        assertEquals(AfternoteHomeBodyState.Reloading(AfternoteType.BUSINESS), retrying)
    }

    /**
     * 되먹임이 발산하지 않는다 — 로딩 갈래가 내는 두 상태의 상단 유무가 각자 자기 입력과 같다.
     * (`true` → [AfternoteHomeBodyState.Reloading] → `true`, `false` → InitialLoading → `false`)
     */
    @Test
    fun `로딩 판정은 상단 유무 되먹임에서 고정점이다`() {
        listOf(true, false).forEach { chrome ->
            val state =
                afternoteHomeBodyState(
                    refreshState = LoadState.Loading,
                    itemCount = 0,
                    selectedType = null,
                    chromeAlreadyVisible = chrome,
                )

            assertEquals(chrome, state.drawsTopChrome(showsHeaderOnEmptyList = true))
        }
    }

    /**
     * #1633 이 세운 수신자 안전장치를 로딩 축에서도 다시 고정한다.
     *
     * 수신자는 0건에 상단을 그리지 않는다(`showsHeaderOnEmptyList = false`). 그 상태에서 카테고리를
     * 고르면 되먹임이 `false` 라 종전대로 전체 로딩을 지난다 — 발신자 헤더·필터 행이 로딩 본문을 통해
     * 수신자 화면으로 새지 않는다.
     */
    @Test
    fun `수신자 0건에서 카테고리를 골라도 로딩 본문으로 상단이 새지 않는다`() {
        val receiverEmpty =
            afternoteHomeBodyState(
                refreshState = LoadState.NotLoading(endOfPaginationReached = true),
                itemCount = 0,
                selectedType = null,
                chromeAlreadyVisible = false,
            )
        val receiverChrome = receiverEmpty.drawsTopChrome(showsHeaderOnEmptyList = false)
        val authorChrome = receiverEmpty.drawsTopChrome(showsHeaderOnEmptyList = true)

        assertEquals(AfternoteHomeBodyState.Empty, receiverEmpty)
        assertEquals(false, receiverChrome)
        assertEquals(true, authorChrome)
        assertEquals(
            AfternoteHomeBodyState.InitialLoading,
            afternoteHomeBodyState(
                refreshState = LoadState.Loading,
                itemCount = 0,
                selectedType = AfternoteType.BUSINESS,
                chromeAlreadyVisible = receiverChrome,
            ),
        )
    }

    /** 상단이 없던 전면 실패에서 재시도해도 상단이 깜빡 나타났다 사라지지 않는다. */
    @Test
    fun `필터 없는 전면 실패의 재시도는 종전대로 전체 로딩이다`() {
        val settled =
            afternoteHomeBodyState(
                refreshState = loadError(),
                itemCount = 0,
                selectedType = null,
                chromeAlreadyVisible = false,
            )

        assertEquals(false, settled.drawsTopChrome(showsHeaderOnEmptyList = true))
        assertEquals(
            AfternoteHomeBodyState.InitialLoading,
            afternoteHomeBodyState(
                refreshState = LoadState.Loading,
                itemCount = 0,
                selectedType = null,
                chromeAlreadyVisible = settled.drawsTopChrome(showsHeaderOnEmptyList = true),
            ),
        )
    }

    private fun loadError(): LoadState.Error = LoadState.Error(IllegalStateException("조회 실패"))
}

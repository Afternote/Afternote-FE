package com.afternote.feature.afternote.presentation.home

import androidx.paging.LoadState
import com.afternote.feature.afternote.domain.AfternoteType

/**
 * [AfternoteHomeScreen] 이 그릴 본문. 화면의 `when` 을 값으로 끄집어낸 것이다.
 *
 * 분기를 컴포저블 밖으로 뺀 이유는 **순서를 테스트로 고정하기 위해서**다. 이 화면을 통째로 띄워
 * 판정하려면 `collectAsLazyPagingItems` 의 첫 상태(Loading)를 걷어내는 컴포지션 이펙트가 먼저 돌아야
 * 하는데, 이 모듈은 Robolectric 테스트 클래스가 여럿 누적되면 그 이펙트가 첫 단언까지 진행되지 않는다
 * (`AfternoteHomeEmptyCopyTest` KDoc 의 실측: 단독 실행이면 통과, 32개 클래스 뒤면 로딩 화면인 채로 실패 —
 * `waitUntil` 은 타임아웃). 그래서 «어떤 본문을 그리는가» 는 순수 함수로 내려 실행 순번과 무관하게
 * 고정하고, 각 본문이 «무엇을 그리는가» 는 본문 컴포저블을 직접 띄워 본다 (#1634).
 */
internal sealed interface AfternoteHomeBodyState {
    /** 보여 줄 것이 전무한 채 첫 로드 중. */
    data object InitialLoading : AfternoteHomeBodyState

    /**
     * 카테고리 필터 없는 전면 실패. 재시도 하나만 주는 [com.afternote.feature.afternote.presentation.shared.component.ErrorListBody]
     * 가 맞다 — 「전체」가 이미 기본 상태라 돌아갈 곳이 따로 없다.
     */
    data object Error : AfternoteHomeBodyState

    /**
     * 카테고리 필터가 걸린 채로 실패. [Error] 와 갈라 두는 이유는 **빠져나갈 곳이 있기 때문**이다 —
     * 전면 에러로 덮으면 카테고리 행이 사라져 다른 카테고리로도 「전체」로도 이동할 수 없는 막다른
     * 상태가 된다 (#1634).
     *
     * 고른 카테고리를 상태가 들고 있는 이유: 이 갈래는 정의상 `selectedType != null` 이라, 값을 여기 담아야
     * 본문 컴포저블이 `AfternoteType?` 이 아닌 **non-null** 로 받는다. 「필터 실패인데 선택 탭이 「전체」」
     * 같은 불가능한 상태를 계약에서 지운다 (#1633 의 `EmptyHomeBody` 가 `selectedTab = null` 을 못 박은 것과 같은 규칙).
     */
    data class FilteredError(
        val selectedType: AfternoteType,
    ) : AfternoteHomeBodyState

    /**
     * 목록 경로. 항목이 있거나(새로고침이 실패해도 Paging 이 기존 페이지를 유지한다) 카테고리 필터가
     * 걸린 상태다 — 필터 결과 0건도 여기 남겨 카테고리 행을 유지한다.
     */
    data object List : AfternoteHomeBodyState

    /** 0건이고 카테고리 필터도 없는 «첫 진입». */
    data object Empty : AfternoteHomeBodyState
}

/**
 * 로드 상태·항목 수·선택 필터를 그릴 본문으로 옮긴다.
 *
 * 순서가 곧 우선순위다. 특히 **실패 판정이 목록 경로보다 앞서지만, 필터가 걸린 실패는
 * [AfternoteHomeBodyState.FilteredError] 로 갈라진다** — 종전에는 이 자리에서 전면 에러가 필터 행까지
 * 걷어 가 「전체」로 돌아올 수단이 사라졌다 (#1634).
 */
internal fun afternoteHomeBodyState(
    refreshState: LoadState,
    itemCount: Int,
    selectedType: AfternoteType?,
): AfternoteHomeBodyState =
    when {
        refreshState is LoadState.Loading && itemCount == 0 -> {
            AfternoteHomeBodyState.InitialLoading
        }

        // 전면 에러는 보여 줄 데이터가 전무할 때만. 목록이 있는 상태의 refresh 실패는 Paging 이 기존
        // 페이지를 유지하므로(itemCount > 0) 아래 List 가 목록을 그대로 보여준다.
        refreshState is LoadState.Error && itemCount == 0 -> {
            if (selectedType == null) {
                AfternoteHomeBodyState.Error
            } else {
                AfternoteHomeBodyState.FilteredError(selectedType)
            }
        }

        itemCount > 0 || selectedType != null -> {
            AfternoteHomeBodyState.List
        }

        else -> {
            AfternoteHomeBodyState.Empty
        }
    }

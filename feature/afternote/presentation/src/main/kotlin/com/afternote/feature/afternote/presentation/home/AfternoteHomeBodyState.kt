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
    /** 보여 줄 것도, 이미 그려 둔 상단도 없는 «첫 진입» 로드 중. 화면 전체가 로딩이다. */
    data object InitialLoading : AfternoteHomeBodyState

    /**
     * 상단(헤더·카테고리 필터 행)을 이미 보고 있던 사용자가 맞는 로드 중 (#1635).
     *
     * 카테고리를 바꾸면 [AfternoteHomeViewModel] 의 `flatMapLatest` 가 **새 Paging 세대**를 만들고,
     * 그 세대의 첫 상태는 `refresh = Loading` 이다. 항목이 이미 있으면 Paging 이 직전 세대의 페이지를
     * 그대로 들고 있어 `itemCount` 가 유지되지만(실측), **0건 상태에서 전환하면** `itemCount == 0` 이
     * 되어 [InitialLoading] 과 구분이 없어진다. 그때 화면 전체를 로딩으로 덮으면 **방금 탭한 카테고리
     * 행까지 통째로 사라졌다가 다시 나타난다.** 재시도(`retry()`)도 같은 자리를 지난다.
     *
     * 그래서 «첫 진입» 과 «이미 상단을 보고 있던 로드» 를 갈라, 후자는 상단을 그대로 두고 본문만
     * 로딩으로 바꾼다. 어느 쪽인지는 화면이 [drawsTopChrome] 로 직전 렌더를 기억해 알려 준다.
     *
     * 고른 카테고리를 담는 이유는 [FilteredError] 와 다르다 — 이쪽은 「전체」로 돌아오는 전환도 지나므로
     * `null` 이 정상값이다. 방금 탭한 탭이 로딩 중에도 선택 상태로 보여야 한다.
     */
    data class Reloading(
        val selectedType: AfternoteType?,
    ) : AfternoteHomeBodyState

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
 *
 * @param chromeAlreadyVisible 직전에 그린 본문이 상단(헤더·카테고리 필터 행)을 그리고 있었는지.
 *   로드 상태만으로는 «첫 진입» 과 «카테고리 전환·재시도» 를 가를 수 없어 화면이 기억해 넘긴다 —
 *   자세한 이유는 [AfternoteHomeBodyState.Reloading] KDoc (#1635). 기본값을 두지 않는다: `false` 를
 *   묵시적으로 물려받으면 전환 중 상단이 사라지는 종전 동작으로 조용히 되돌아간다.
 */
internal fun afternoteHomeBodyState(
    refreshState: LoadState,
    itemCount: Int,
    selectedType: AfternoteType?,
    chromeAlreadyVisible: Boolean,
): AfternoteHomeBodyState =
    when {
        refreshState is LoadState.Loading && itemCount == 0 -> {
            // 이미 상단을 보고 있었다면 그것을 빼앗지 않는다 — 카테고리 전환·재시도가 여기를 지난다 (#1635).
            if (chromeAlreadyVisible) {
                AfternoteHomeBodyState.Reloading(selectedType)
            } else {
                AfternoteHomeBodyState.InitialLoading
            }
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

/**
 * 이 본문이 상단(헤더·카테고리 필터 행)을 그리는지.
 *
 * [afternoteHomeBodyState] 의 `chromeAlreadyVisible` 을 만드는 짝이다. 화면은 이번 프레임의 판정을
 * 다음 프레임의 입력으로 되먹여 «직전 렌더에 상단이 있었는가» 를 유지한다 (#1635).
 *
 * [AfternoteHomeBodyState.Empty] 만 호출부 설정을 본다 — 0건이고 필터도 없는 그 상태에서만
 * `showsHeaderOnEmptyList` 가 상단의 유무를 가르기 때문이다(그 KDoc 참조). 이 되먹임이 있어야
 * **상단이 없던 상태에서 시작한 로드는 종전대로 [AfternoteHomeBodyState.InitialLoading] 로 남는다** —
 * 수신자 0건(`showsHeaderOnEmptyList = false`)이나 필터 없는 전면 실패에서 재시도를 눌렀을 때
 * 발신자 헤더·필터 행이 로딩 화면을 통해 새어 나오지 않는다.
 */
internal fun AfternoteHomeBodyState.drawsTopChrome(showsHeaderOnEmptyList: Boolean): Boolean =
    when (this) {
        is AfternoteHomeBodyState.Reloading,
        is AfternoteHomeBodyState.FilteredError,
        AfternoteHomeBodyState.List,
        -> true

        AfternoteHomeBodyState.Empty -> showsHeaderOnEmptyList

        AfternoteHomeBodyState.InitialLoading,
        AfternoteHomeBodyState.Error,
        -> false
    }

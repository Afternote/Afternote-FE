package com.afternote.feature.afternote.presentation.home

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.feature.afternote.domain.AfternoteType

/**
 * 상단(헤더·카테고리 필터 행)을 이미 보고 있던 사용자가 맞는 로딩 본문 (#1635).
 *
 * **[com.afternote.core.ui.loading.LoadingBody] 로 화면을 통째로 덮지 않는다.** 카테고리를 바꾸면
 * [AfternoteHomeViewModel] 의 `flatMapLatest` 가 새 Paging 세대를 만들고, 0건 상태에서 전환하면
 * `refresh = Loading` + `itemCount == 0` 이라 종전에는 첫 진입과 똑같이 판정돼 **방금 탭한 카테고리
 * 행까지 사라졌다가 다시 나타났다.** 사라진 동안 그 화면에는 조작할 것이 하나도 없고, 응답이 느리면
 * 그 상태가 그대로 길어진다. 어느 로딩이 이 본문을 타는지는 [afternoteHomeBodyState] 가 가른다.
 *
 * 상단 배치는 목록·필터 0건·필터 실패와 같다 — [HomeBodyTopSpacing]·[HomeBodySectionSpacing] 를 공유해
 * 로딩이 끼어도 헤더와 행이 **같은 자리에** 머문다. 위쪽이 뛰지 않는 것이 이 본문의 목적이다.
 *
 * 이 본문도 `showsHeaderOnEmptyList` 를 보지 않는다. 볼 필요가 없다 — [drawsTopChrome] 되먹임 때문에
 * 상단이 없던 상태에서 시작한 로드는 애초에 이 본문에 오지 않는다. 수신자 0건에서 카테고리를 고르면
 * 종전대로 전체 로딩을 지나 목록 상태(그쪽도 헤더를 그린다)로 간다.
 */
@Composable
internal fun ReloadingBody(
    headerDescription: String,
    nextStep: NextStep?,
    selectedType: AfternoteType?,
    onTypeSelected: (AfternoteType?) -> Unit,
    filterRowScrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(HomeBodySectionSpacing),
    ) {
        Spacer(Modifier.height(HomeBodyTopSpacing))
        HomeHeaderSection(
            description = headerDescription,
            nextStep = nextStep,
        )
        // 필터 행과 로딩 사이에는 간격을 두지 않는다 — 목록 상태의 AfternoteListContent 와 같은 배치다.
        Column(modifier = Modifier.weight(1f)) {
            // 방금 탭한 카테고리를 로딩 중에도 선택 상태로 둔다. 「전체」로 돌아오는 전환이면 null 이다.
            AfternoteTypeFilterRow(
                onTabSelected = onTypeSelected,
                selectedTab = selectedType,
                scrollState = filterRowScrollState,
            )
            LoadingBody(modifier = Modifier.weight(1f))
        }
    }
}

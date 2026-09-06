package com.afternote.feature.afternote.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.shared.component.ErrorListBody

/**
 * 카테고리 필터를 건 채 조회가 실패했을 때의 본문 (#1634).
 *
 * **전면 에러([ErrorListBody])로 덮지 않는다.** 그 본문에 남는 조작은 «다시 시도» 하나뿐이라 카테고리
 * 행이 사라지고, 서버가 계속 실패하는 동안 사용자는 자기가 고른 카테고리에 갇힌다 — 다른 카테고리로도
 * 「전체」로도 나갈 수 없는 막다른 상태다. 카테고리를 고르면 [AfternoteHomeViewModel.selectTab] 이
 * 새 조회를 걸므로, 행만 남겨 두면 그 자체가 복구 수단이 된다.
 *
 * **실패를 0건 문구로 덮지도 않는다.** 목록 경로의 필터 0건 문구(`afternote_home_filtered_empty`)는
 * «이 카테고리에 등록된 애프터노트가 없어요» 라고 단정하는데, 실제로는 서버 응답을 못 받은 것이고
 * 재시도 수단까지 사라진다 — 무음 실패(#705)와 같은 부류다. 그래서 카테고리 행만 목록 상태에서
 * 가져오고 그 아래는 실패 문구와 재시도를 그대로 둔다.
 *
 * 헤더를 함께 그리는 이유: 같은 필터의 성공 상태(목록·필터 0건)는
 * [com.afternote.feature.afternote.presentation.shared.component.InfiniteListBody] 로 헤더를 그린다.
 * 실패에서만 상단이 통째로 사라지면 카테고리를 옮길 때마다 화면 위쪽이 뛴다. 여백을
 * [HomeBodyTopSpacing]·[HomeBodySectionSpacing] 로 공유해 세 상태가 헤더·필터 행을 같은 자리에 둔다.
 * 이 본문은 `showsHeaderOnEmptyList` 를 보지 않는다 — 그 값은 «0건이고 필터도 없을 때» 를 가르는 것이라
 * (KDoc 참조) 필터가 걸린 이 상태와 무관하고, 수신자도 필터가 걸린 성공 상태에서는 이미 헤더를 본다.
 */
@Composable
internal fun FilteredErrorBody(
    headerDescription: String,
    nextStep: NextStep?,
    selectedType: AfternoteType,
    onTypeSelected: (AfternoteType?) -> Unit,
    onRetry: () -> Unit,
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
        // 필터 행과 실패 본문 사이에는 간격을 두지 않는다 — 목록 상태의 AfternoteListContent 와 같은 배치다.
        // 남은 높이를 weight 로 받아 실패 문구가 «필터 행 아래 영역» 의 가운데에 온다.
        Column(modifier = Modifier.weight(1f)) {
            AfternoteTypeFilterRow(
                onTabSelected = onTypeSelected,
                selectedTab = selectedType,
            )
            ErrorListBody(
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

package com.afternote.feature.afternote.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.shared.component.EmptyListBody

/**
 * 애프터노트가 0건이고 카테고리 필터도 없는 «첫 진입» 본문.
 *
 * 종전에는 헤더가 [com.afternote.feature.afternote.presentation.shared.component.InfiniteListBody]
 * 안에만 있어, 이 경로로 빠지는 사용자에게 화면 제목(`afternote_home_title`)과 설명이 **한 번도 그려지지
 * 않았다.** 서버 데이터와 무관한 구조 결함이라 목록 상태와 같은 자리에 헤더를 올려 닫는다 (#1175).
 *
 * 여백을 [HomeBodyTopSpacing]·[HomeBodySectionSpacing] 로 공유해 목록 상태와 헤더 위치가 어긋나지 않게 한다.
 *
 * [nextStep] 은 여전히 호출부가 정한다 — 문구를 만드는 원천이 서버에도 ViewModel 에도 없어 현재는 `null`
 * 이고(Afternote-BE#270), 이 함수는 값이 생겼을 때 카드가 헤더 아래 제자리에 붙는 것만 보장한다.
 *
 * 카테고리 필터 행도 여기서 그린다 — 시안 `애프터노트_목록X` 는 0건에서도 헤더 아래 필터 행을 두고,
 * 그게 없으면 첫 진입 사용자는 카테고리라는 축이 있다는 것 자체를 볼 수 없다. **이 경로를 목록 상태의
 * [com.afternote.feature.afternote.presentation.shared.component.InfiniteListBody] 로 합치지 마라** —
 * 그쪽 [com.afternote.feature.afternote.presentation.shared.component.AfternoteListContent] 의
 * 0건 문구는 `afternote_home_filtered_empty`(카테고리 필터 결과 0건)라, 합치는 순간 전체 0건 문구
 * `afternote_empty_list_body` 가 그 문구로 뒤바뀐다. 두 문구는 #567 에서 일부러 갈라 놓은 것이다.
 */
@Composable
internal fun EmptyHomeBody(
    headerDescription: String,
    nextStep: NextStep?,
    emptyListDescription: String,
    onTypeSelected: (AfternoteType?) -> Unit,
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
        // 필터 행과 빈 본문 사이에는 간격을 두지 않는다 — 목록 상태의 AfternoteListContent 와 같은 배치다.
        Column {
            // 이 본문은 «0건이고 필터도 없음» 분기에서만 그려지므로 선택 탭은 정의상 «전체»(null)다.
            // selectedType 을 넘겨받지 않는 이유이자, `무필터 0건 본문은 전체 탭을 선택 상태로 그린다`
            // 테스트가 이 불변을 고정한다.
            AfternoteTypeFilterRow(
                onTabSelected = onTypeSelected,
                selectedTab = null,
            )
            EmptyListBody(description = emptyListDescription)
        }
    }
}

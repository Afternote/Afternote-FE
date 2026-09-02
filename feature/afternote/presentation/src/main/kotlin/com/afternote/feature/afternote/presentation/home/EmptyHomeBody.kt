package com.afternote.feature.afternote.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
 */
@Composable
internal fun EmptyHomeBody(
    headerDescription: String,
    nextStep: NextStep?,
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
        EmptyListBody()
    }
}

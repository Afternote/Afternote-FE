package com.afternote.feature.afternote.presentation.shared.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 상세 화면에서 사용하는 회색 배경 + 둥근 모서리 컨테이너.
 *
 * Material3 [androidx.compose.material3.Card] 는 elevation·ripple·surface tint 등
 * 이 화면에서 쓰지 않는 기본값이 얹혀 있어 커스텀 디자인 시스템과 충돌한다.
 * 단순 Column 위에 `clip + background + padding` 을 체이닝해 렌더링 비용을 줄이고
 * [DetailCard] 와 일관된 스타일링 방식을 유지한다.
 */
@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(AfternoteDesign.colors.gray2)
                .padding(16.dp),
        content = content,
    )
}

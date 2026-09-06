package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.afternote.core.ui.theme.AfternoteDesign

/** 플레이리스트 목록의 헤더·액션은 유지한 채 본문 중앙에 표시하는 공용 빈 상태. */
@Composable
fun PlaylistEmptyContent(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AfternoteDesign.typography.bodySmallB,
            color = AfternoteDesign.colors.gray4,
            textAlign = TextAlign.Center,
        )
    }
}

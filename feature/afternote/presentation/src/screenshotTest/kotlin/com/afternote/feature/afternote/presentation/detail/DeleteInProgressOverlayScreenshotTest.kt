package com.afternote.feature.afternote.presentation.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [DeleteInProgressOverlay] 는 상세 화면 위에 겹쳐 그리는 반투명 스크림이라
 * 하위 콘텐츠를 어둡게 덮는 alpha 효과가 핵심이다. 뒤에 더미 콘텐츠를 깔고
 * 그 위에 오버레이를 얹어 스크림 대비와 중앙 인디케이터를 함께 회귀 검증한다.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun deleteInProgressOverlayScreenshot() {
    AfternoteTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "상세 콘텐츠")
            DeleteInProgressOverlay()
        }
    }
}

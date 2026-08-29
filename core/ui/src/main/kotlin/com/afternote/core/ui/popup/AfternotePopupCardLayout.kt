package com.afternote.core.ui.popup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.modifierextention.dropShadow
import com.afternote.core.ui.theme.AfternoteDesign

private val PopupCardShape = RoundedCornerShape(16.dp)

@Composable
fun AfternotePopupCardLayout(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .dropShadow(
                    shape = PopupCardShape,
                    color = Color.Black.copy(alpha = 0.15f),
                    blur = 10.dp,
                    offsetX = 0.dp,
                    offsetY = 2.dp,
                    spread = 0.dp,
                ),
        shape = PopupCardShape,
        color = AfternoteDesign.colors.white,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 32.dp,
                    ),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
        ) {
            content()
        }
    }
}

/**
 * 피그마 기준 공통 알림 카드: 흰 배경·16dp 라운드·본문 메시지·하단 액션 영역.
 *
 * [Popup] / [PopupContent]의 [PopupType.Default](단일 버튼), [PopupType.Variant2](이중 버튼)에서 공유합니다.
 */
@Composable
fun AfternotePopupCardLayout(
    message: String,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit,
) {
    AfternotePopupCardLayout(modifier = modifier) {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth(),
            style =
                AfternoteDesign.typography.bodyBase
                    .copy(
                        textAlign = TextAlign.Center,
                        color = AfternoteDesign.colors.gray9,
                    ),
        )

        Spacer(modifier = Modifier.height(20.dp))

        actions()
    }
}

package com.afternote.core.ui.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.dropShadow
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

private val PopupCardShape = RoundedCornerShape(16.dp)

/**
 * 피그마 기준 공통 팝업 카드 껍데기: 흰 배경·16dp 라운드·그림자·내부 패딩.
 *
 * 메시지+버튼 패턴은 [AfternotePopupCardLayout] (메시지 오버로드), 폼 다이얼로그는 이 슬롯 버전을 씁니다.
 */
@Composable
fun AfternotePopupCardLayout(
    modifier: Modifier = Modifier,
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
                        horizontal = 20.dp,
                        vertical = 32.dp,
                    ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

/**
 * 피그마 기준 공통 알림 카드: 흰 배경·16dp 라운드·본문 메시지·하단 액션 영역.
 *
 * [InfoPopupContent](단일 버튼), [ConfirmationPopupContent](이중 버튼)에서 공유합니다.
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
            style =
                AfternoteDesign.typography.textField.copy(
                    fontWeight = FontWeight.Medium,
                    color = AfternoteDesign.colors.gray9,
                    textAlign = TextAlign.Center,
                ),
        )

        Spacer(modifier = Modifier.height(20.dp))

        actions()
    }
}

/** 개발용: 단일/이중 버튼 카드 스타일을 한 화면에서 확인 (피그마 시안 대응). */
@Preview(showBackground = true, name = "Popup cards — single & double")
@Composable
private fun AfternotePopupCardLayoutsPreview() {
    AfternoteTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(AfternoteDesign.colors.gray8)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InfoPopupContent(
                message = "아이디/비밀번호 찾기의 경우,\n고객센터로 문의 바랍니다.",
                onConfirm = {},
            )
            ConfirmationPopupContent(
                message = "아이디/비밀번호 찾기의 경우,\n고객센터로 문의 바랍니다.",
                onDismiss = {},
                onConfirm = {},
            )
        }
    }
}

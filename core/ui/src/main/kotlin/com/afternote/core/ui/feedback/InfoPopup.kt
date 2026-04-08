package com.afternote.core.ui.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.window.Dialog
import com.afternote.core.ui.expand.dropShadow
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun InfoPopup(
    message: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "확인",
) {
    Dialog(onDismissRequest = onConfirm) {
        InfoPopupContent(
            message = message,
            onConfirm = onConfirm,
            modifier = modifier,
            confirmText = confirmText,
        )
    }
}

@Composable
fun InfoPopupContent(
    message: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "확인",
) {
    val buttonShape = RoundedCornerShape(8.dp)

    AfternotePopupCardLayout(
        message = message,
        modifier = modifier,
    ) {
        Surface(
            onClick = onConfirm,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .dropShadow(
                        shape = buttonShape,
                        color = Color.Black.copy(alpha = 0.05f),
                        blur = 5.dp,
                        offsetX = 0.dp,
                        offsetY = 2.dp,
                        spread = 0.dp,
                    ),
            shape = buttonShape,
            color = AfternoteDesign.colors.gray9,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 24.dp,
                            vertical = 16.dp,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = confirmText,
                    style =
                        AfternoteDesign.typography.textField.copy(
                            fontWeight = FontWeight.Medium,
                            color = AfternoteDesign.colors.white,
                            textAlign = TextAlign.Center,
                        ),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoPopupPreview() {
    AfternoteTheme {
        InfoPopupContent(
            message = "아이디/비밀번호 찾기의 경우,\n고객센터로 문의 바랍니다.",
            onConfirm = {},
        )
    }
}

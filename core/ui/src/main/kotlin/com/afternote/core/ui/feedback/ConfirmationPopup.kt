package com.afternote.core.ui.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.afternote.core.ui.expand.dropShadow
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun ConfirmationPopup(
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String = "아니요",
    confirmText: String = "예",
    isLoading: Boolean = false,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = !isLoading,
                dismissOnClickOutside = !isLoading,
            ),
    ) {
        ConfirmationPopupContent(
            message = message,
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            modifier = modifier,
            dismissText = dismissText,
            confirmText = confirmText,
            isLoading = isLoading,
        )
    }
}

@Composable
fun ConfirmationPopupContent(
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String = "아니요",
    confirmText: String = "예",
    isLoading: Boolean = false,
) {
    val buttonShape = RoundedCornerShape(8.dp)
    val secondaryButtonTextStyle =
        AfternoteDesign.typography.textField.copy(
            fontWeight = FontWeight.Medium,
            color = AfternoteDesign.colors.gray9,
            textAlign = TextAlign.Center,
        )
    val primaryButtonTextStyle =
        secondaryButtonTextStyle.copy(color = AfternoteDesign.colors.white)

    AfternotePopupCardLayout(
        message = message,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .dropShadow(
                            shape = buttonShape,
                            color = Color.Black.copy(alpha = 0.05f),
                            blur = 5.dp,
                            offsetX = 0.dp,
                            offsetY = 2.dp,
                            spread = 0.dp,
                        ).clip(buttonShape)
                        .background(AfternoteDesign.colors.gray3)
                        .clickable(enabled = !isLoading, onClick = onDismiss)
                        .padding(
                            horizontal = 24.dp,
                            vertical = 16.dp,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = dismissText,
                    style = secondaryButtonTextStyle,
                )
            }

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .dropShadow(
                            shape = buttonShape,
                            color = Color.Black.copy(alpha = 0.05f),
                            blur = 5.dp,
                            offsetX = 0.dp,
                            offsetY = 2.dp,
                            spread = 0.dp,
                        ).clip(buttonShape)
                        .background(AfternoteDesign.colors.gray9)
                        .clickable(enabled = !isLoading, onClick = onConfirm)
                        .padding(
                            horizontal = 24.dp,
                            vertical = 16.dp,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                    )
                } else {
                    Text(
                        text = confirmText,
                        style = primaryButtonTextStyle,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmationPopupPreview() {
    AfternoteTheme {
        ConfirmationPopupContent(
            message = "인스타그램에 대한 기록을 삭제하시겠습니까?\n삭제 시, 되돌릴 수 없습니다.",
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmationPopupCustomButtonsPreview() {
    AfternoteTheme {
        ConfirmationPopupContent(
            message = "사망 프로토콜이 아직 실행되지 않았습니다.\n프로토콜을 실행하시겠습니까?",
            onDismiss = {},
            onConfirm = {},
            dismissText = "취소",
            confirmText = "실행",
        )
    }
}

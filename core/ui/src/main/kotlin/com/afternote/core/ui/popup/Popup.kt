package com.afternote.core.ui.popup

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.afternote.core.ui.dropShadow
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 앱 내 모든 "메시지 + 버튼" 팝업의 단일 진입점.
 *
 * - [PopupType.Default]  : 메시지 + 단일 확인 버튼
 * - [PopupType.Variant2] : 메시지 + 좌측 취소 / 우측 확인 두 버튼
 *
 * 새로운 팝업 디자인이 필요해지면 새 [PopupType] 값을 추가하는 방식으로만 확장합니다.
 */
enum class PopupType {
    Default,
    Variant2,
}

@Composable
fun Popup(
    type: PopupType,
    message: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String =
        when (type) {
            PopupType.Default -> "확인"
            PopupType.Variant2 -> "예"
        },
    onDismiss: () -> Unit = onConfirm,
    dismissText: String = "아니요",
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
        PopupContent(
            type = type,
            message = message,
            onConfirm = onConfirm,
            modifier = modifier,
            confirmText = confirmText,
            onDismiss = onDismiss,
            dismissText = dismissText,
            isLoading = isLoading,
        )
    }
}

/**
 * Dialog 래퍼 없이 카드 본체만 렌더링합니다. 프리뷰 및 테스트 전용.
 */
@Composable
internal fun PopupContent(
    type: PopupType,
    message: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String =
        when (type) {
            PopupType.Default -> "확인"
            PopupType.Variant2 -> "예"
        },
    onDismiss: () -> Unit = onConfirm,
    dismissText: String = "아니요",
    isLoading: Boolean = false,
) {
    AfternotePopupCardLayout(
        message = message,
        modifier = modifier,
    ) {
        when (type) {
            PopupType.Default ->
                DefaultActionButton(
                    text = confirmText,
                    onClick = onConfirm,
                )

            PopupType.Variant2 ->
                Variant2ActionButtons(
                    dismissText = dismissText,
                    confirmText = confirmText,
                    onDismiss = onDismiss,
                    onConfirm = onConfirm,
                    isLoading = isLoading,
                )
        }
    }
}

private val ButtonShape = RoundedCornerShape(8.dp)

@Composable
private fun DefaultActionButton(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .dropShadow(
                    shape = ButtonShape,
                    color = Color.Black.copy(alpha = 0.05f),
                    blur = 5.dp,
                    offsetX = 0.dp,
                    offsetY = 2.dp,
                    spread = 0.dp,
                ),
        shape = ButtonShape,
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
                text = text,
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

@Composable
private fun Variant2ActionButtons(
    dismissText: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean,
) {
    val secondaryButtonTextStyle =
        AfternoteDesign.typography.textField.copy(
            fontWeight = FontWeight.Medium,
            color = AfternoteDesign.colors.gray9,
            textAlign = TextAlign.Center,
        )
    val primaryButtonTextStyle =
        secondaryButtonTextStyle.copy(color = AfternoteDesign.colors.white)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .dropShadow(
                        shape = ButtonShape,
                        color = Color.Black.copy(alpha = 0.05f),
                        blur = 5.dp,
                        offsetX = 0.dp,
                        offsetY = 2.dp,
                        spread = 0.dp,
                    ).clip(ButtonShape)
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
                        shape = ButtonShape,
                        color = Color.Black.copy(alpha = 0.05f),
                        blur = 5.dp,
                        offsetX = 0.dp,
                        offsetY = 2.dp,
                        spread = 0.dp,
                    ).clip(ButtonShape)
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

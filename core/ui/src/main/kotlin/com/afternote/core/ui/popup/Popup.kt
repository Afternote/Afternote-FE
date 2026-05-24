package com.afternote.core.ui.popup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType

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
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String =
        when (type) {
            PopupType.Default -> "확인"
            PopupType.Variant2 -> "예"
        },
    dismissText: String = "아니요",
    isLoading: Boolean = false,
) {
    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
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
            onDismiss = onDismiss,
            modifier = modifier,
            confirmText = confirmText,
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
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String =
        when (type) {
            PopupType.Default -> "확인"
            PopupType.Variant2 -> "예"
        },
    dismissText: String = "아니요",
    isLoading: Boolean = false,
) {
    AfternotePopupCardLayout(
        message = message,
        modifier = modifier,
    ) {
        when (type) {
            PopupType.Default -> {
                AfternoteButton(
                    text = confirmText,
                    onClick = { if (!isLoading) onConfirm() },
                    type = AfternoteButtonType.Default,
                )
            }

            PopupType.Variant2 -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    AfternoteButton(
                        text = dismissText,
                        onClick = { if (!isLoading) onDismiss() },
                        type = AfternoteButtonType.Default,
                        modifier = Modifier.weight(1f),
                    )
                    AfternoteButton(
                        text = confirmText,
                        onClick = { if (!isLoading) onConfirm() },
                        type = AfternoteButtonType.Default,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

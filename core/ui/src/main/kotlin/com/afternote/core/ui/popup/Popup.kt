package com.afternote.core.ui.popup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.afternote.core.ui.R
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
        stringResource(
            when (type) {
                PopupType.Default -> R.string.core_ui_popup_confirm
                PopupType.Variant2 -> R.string.core_ui_popup_yes
            },
        ),
    dismissText: String = stringResource(R.string.core_ui_popup_no),
    isLoading: Boolean = false,
    confirmButtonColor: Color? = null,
    dismissButtonColor: Color? = null,
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
            confirmButtonColor = confirmButtonColor,
            dismissButtonColor = dismissButtonColor,
        )
    }
}

/**
 * Dialog 래퍼 없이 카드 본체만 렌더링하는 [Popup] 내부 구현.
 *
 * 파일 밖으로 열지 않는다 — 팝업의 공개 계약은 [Popup] 하나이고, 문구·버튼 동작 검증도
 * [Popup] 을 그려서 한다 (#1672).
 */
@Composable
private fun PopupContent(
    type: PopupType,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String =
        stringResource(
            when (type) {
                PopupType.Default -> R.string.core_ui_popup_confirm
                PopupType.Variant2 -> R.string.core_ui_popup_yes
            },
        ),
    dismissText: String = stringResource(R.string.core_ui_popup_no),
    isLoading: Boolean = false,
    confirmButtonColor: Color? = null,
    dismissButtonColor: Color? = null,
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
                    containerColor = confirmButtonColor,
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
                        containerColor = dismissButtonColor,
                    )
                    AfternoteButton(
                        text = confirmText,
                        onClick = { if (!isLoading) onConfirm() },
                        type = AfternoteButtonType.Default,
                        modifier = Modifier.weight(1f),
                        containerColor = confirmButtonColor,
                    )
                }
            }
        }
    }
}

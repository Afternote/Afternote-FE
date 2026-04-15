package com.afternote.feature.afternote.presentation.author.editor.processing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.modifierextention.addFocusCleaner
import com.afternote.core.ui.popup.AfternotePopupCardLayout
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R

/**
 * 직접 입력하기 다이얼로그 콜백
 */
@Immutable
data class CustomServiceDialogCallbacks(
    val onDismiss: () -> Unit = {},
    val onAddClick: () -> Unit = {},
)

/**
 * 직접 입력하기 다이얼로그 파라미터
 */
@Immutable
data class CustomServiceDialogParams(
    val serviceNameState: TextFieldState,
    val callbacks: CustomServiceDialogCallbacks = CustomServiceDialogCallbacks(),
)

/**
 * 직접 입력하기 다이얼로그 컴포넌트
 *
 * 피그마 디자인 기반:
 * - 타이틀: "직접 입력하기" (18sp, Bold, AfternoteDesign.colors.gray9, 중앙 정렬)
 * - 추가 서비스명 입력 필드 (AfternoteDesign.colors.gray1 배경, 8dp radius)
 * - 추가하기 버튼 (AfternoteDesign.colors.gray9 배경, 전체 너비)
 * - 다이얼로그: 흰색 배경, 16dp radius, drop shadow
 */
@Composable
fun CustomServiceDialog(
    modifier: Modifier = Modifier,
    params: CustomServiceDialogParams,
) {
    val focusManager = LocalFocusManager.current
    Dialog(
        onDismissRequest = params.callbacks.onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
    ) {
        AfternotePopupCardLayout(
            modifier =
                modifier
                    .fillMaxWidth()
                    .addFocusCleaner(focusManager),
        ) {
            // 타이틀
            Text(
                text = stringResource(R.string.afternote_editor_custom_service_dialog_title),
                style = AfternoteDesign.typography.bodyLargeB,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 추가 서비스명 입력 필드
            Column {
                Text(
                    text = stringResource(R.string.afternote_editor_custom_service_name_label),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray9,
                )
                Spacer(modifier = Modifier.height(6.dp))
                AfternoteTextField(
                    state = params.serviceNameState,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 추가하기 버튼
            AfternoteButton(
                text = stringResource(R.string.add_button),
                onClick = {
                    focusManager.clearFocus()
                    params.callbacks.onAddClick()
                },
                type = AfternoteButtonType.Default,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomServiceDialogPreview() {
    AfternoteTheme {
        CustomServiceDialog(
            params =
                CustomServiceDialogParams(
                    serviceNameState = rememberTextFieldState(),
                ),
        )
    }
}

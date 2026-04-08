package com.afternote.core.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 빈 공간(배경)을 탭했을 때만 포커스를 해제합니다.
 *
 * [androidx.compose.foundation.clickable] 등이 터치를 소비하는 위에서는 이 제스처가 호출되지 않아,
 * 텍스트 필드·버튼 터치 시 키보드가 내려갔다 올라가는 깜빡임이 생기지 않습니다.
 *
 * 버튼 클릭으로 키보드를 내리려면 해당 `onClick` 안에서 [FocusManager.clearFocus]를 호출하세요.
 *
 * @param focusManager [androidx.compose.ui.platform.LocalFocusManager.current]
 */
fun Modifier.addFocusCleaner(focusManager: FocusManager): Modifier =
    this.pointerInput(focusManager) {
        detectTapGestures(
            onTap = {
                focusManager.clearFocus()
            },
        )
    }

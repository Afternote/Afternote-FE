package com.afternote.core.ui.modifierextention

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

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

/**
 * 리플 없이 클릭 가능하게 만드는 Modifier 확장 함수.
 *
 * `composed` + [androidx.compose.foundation.clickable] 대신 [pointerInput]으로 탭을 처리하고,
 * 모바일·TalkBack에서 버튼으로 인식되도록 [semantics]를 둡니다.
 * (하드웨어 키보드/D-pad의 포커스·엔터 활성화는 지원하지 않습니다.)
 */
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    this
        .semantics {
            role = Role.Button
            onClick {
                onClick()
                true
            }
        }.pointerInput(onClick) {
            detectTapGestures(onTap = { onClick() })
        }

package com.afternote.feature.afternote.presentation.navigation

import androidx.compose.runtime.Composable
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * afternote feature 전용 라이트 모드 테마 래퍼.
 *
 * Nav2 시절엔 `afternoteComposable` 이 `composable<T>` 등록과 테마를 함께 감쌌지만, Nav3 의
 * `entry<T>` 는 reified 확장으로 이미 제공되므로 테마만 남긴다.
 */
@Composable
fun AfternoteLightTheme(content: @Composable () -> Unit) {
    AfternoteTheme(content = content)
}

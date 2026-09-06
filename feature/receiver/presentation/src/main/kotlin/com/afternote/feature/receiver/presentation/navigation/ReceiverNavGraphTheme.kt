package com.afternote.feature.receiver.presentation.navigation

import androidx.compose.runtime.Composable
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 수신자 화면용 테마 래퍼. 작성자 측 `AfternoteLightTheme` 와 동일하게 [AfternoteTheme]
 * 라이트 모드를 강제 적용해 다크 모드 변형을 차단한다.
 *
 * Nav2 시절엔 `receiverComposable` 이 `composable<T>` 등록과 테마를 함께 감쌌지만, Nav3 의
 * `entry<T>` 는 reified 확장으로 이미 제공되므로 테마만 남긴다.
 */
@Composable
internal fun ReceiverTheme(content: @Composable () -> Unit) {
    AfternoteTheme(content = content)
}

package com.afternote.feature.afternote.presentation.receiver.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 수신자 그래프용 [composable] 래퍼. 작성자 측 `afternoteComposable` 와 동일하게
 * [AfternoteTheme] 라이트 모드를 강제 적용해 다크 모드 변형을 차단한다.
 */
internal inline fun <reified T : Any> NavGraphBuilder.receiverComposable(noinline content: @Composable (NavBackStackEntry) -> Unit) {
    composable<T> { backStackEntry ->
        AfternoteTheme {
            content(backStackEntry)
        }
    }
}

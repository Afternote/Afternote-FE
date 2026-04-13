package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Lifecycle-aware 한 단발성 이벤트 수집기.
 *
 * [LaunchedEffect] + [repeatOnLifecycle]을 결합하여 UI가 STARTED 상태일 때만
 * 이벤트를 수신합니다. [rememberUpdatedState]로 리컴포지션 시 오래된 람다(Stale Lambda)
 * 참조를 방지합니다.
 */
@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    onEvent: (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)

    LaunchedEffect(flow, lifecycleOwner.lifecycle) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { event -> currentOnEvent(event) }
        }
    }
}

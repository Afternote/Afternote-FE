package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * 화면이 이미 떠 있는 상태에서 다시 포그라운드로 돌아올 때만 [onReload] 를 부른다.
 *
 * [LifecycleEventEffect] 의 `ON_RESUME` 은 최초 진입 시에도 한 번 호출되는데, 그 시점은
 * ViewModel `init` 이 이미 최초 로드를 마친 뒤라 그대로 연결하면 진입할 때마다 같은 데이터를
 * 두 번 불러온다. 첫 번째 `ON_RESUME` 은 건너뛰고 그 다음부터만 [onReload] 를 호출해
 * "재진입 시 갱신"이라는 의도만 남긴다.
 */
@Composable
fun ReloadOnReentryEffect(onReload: () -> Unit) {
    var hasEnteredOnce by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (hasEnteredOnce) {
            onReload()
        }
        hasEnteredOnce = true
    }
}

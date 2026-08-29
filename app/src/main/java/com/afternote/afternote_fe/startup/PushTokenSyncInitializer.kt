package com.afternote.afternote_fe.startup

import android.content.Context
import androidx.startup.Initializer
import com.afternote.afternote_fe.messaging.PushTokenSynchronizer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 앱 기동 시 FCM 토큰 등록 관찰을 시작한다 (#1493).
 *
 * Hilt 는 [Initializer] 에 직접 주입하지 못하므로 [EntryPointAccessors] 로 꺼낸다.
 * 관찰은 앱 프로세스 수명 동안 유지돼야 해서 여기서 만든 스코프를 취소하지 않는다.
 */
class PushTokenSyncInitializer : Initializer<Unit> {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface PushTokenSyncEntryPoint {
        fun pushTokenSynchronizer(): PushTokenSynchronizer
    }

    override fun create(context: Context) {
        val synchronizer =
            EntryPointAccessors
                .fromApplication(context.applicationContext, PushTokenSyncEntryPoint::class.java)
                .pushTokenSynchronizer()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            synchronizer.observeLogin()
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

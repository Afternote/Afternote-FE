package com.afternote.afternote_fe

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.afternote.afternote_fe.messaging.FcmNotificationChannel
import com.afternote.afternote_fe.messaging.PushTargetSynchronizer
import com.afternote.afternote_fe.update.ForceUpdateGate
import com.afternote.core.network.di.CoilImageLoaderEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GlobalApplication :
    Application(),
    SingletonImageLoader.Factory {
    /**
     * 푸시 대상 식별자 등록 관찰의 주체 (#1493).
     *
     * 이 필드는 `super.onCreate()` 안에서 채워지므로 [onCreate] 의 `super` 호출 뒤부터 쓸 수 있다.
     */
    @Inject
    lateinit var pushTargetSynchronizer: PushTargetSynchronizer

    /**
     * 강제 업데이트 관문 (#1539).
     *
     * [pushTargetSynchronizer] 와 같은 이유로 여기 있다 — 아래 [startForceUpdateCheck] 주석 참고.
     */
    @Inject
    lateinit var forceUpdateGate: ForceUpdateGate

    /** Hilt 가 만든 [ImageLoader] 를 Coil 앱 전역 싱글톤으로 등록 — 모든 AsyncImage 가 명시적 imageLoader 없이 이걸 사용. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        EntryPointAccessors
            .fromApplication(context, CoilImageLoaderEntryPoint::class.java)
            .imageLoader()

    override fun onCreate() {
        super.onCreate()
        FcmNotificationChannel.create(this)
        startPushTargetSync()
        startForceUpdateCheck()
        // 그 밖의 기동 초기화는 startup 패키지의 Initializer 에 둔다.
    }

    /**
     * 푸시 대상 식별자 등록 관찰을 시작한다 (#1493).
     *
     * `androidx.startup` Initializer 가 아니라 여기인 이유는 **시점**이다. `InitializationProvider`
     * 는 `Application.onCreate()` **이전**에 도는 ContentProvider 라, 그 시점에 Hilt 컴포넌트를
     * 꺼내면 계기 테스트에서 «The component was not created» 로 프로세스가 죽는다 — 테스트는
     * `HiltTestApplication` 을 쓰고 컴포넌트는 `HiltAndroidRule` 이 만들기 때문이다. 앱 모듈
     * androidTest 가 전멸한다. 이 자리는 테스트에서 아예 실행되지 않아(=`HiltTestApplication` 이
     * 이 클래스를 대체) 그 충돌이 없다.
     *
     * 관찰은 앱 프로세스 수명 동안 유지돼야 해서 이 스코프는 취소하지 않는다.
     */
    private fun startPushTargetSync() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            pushTargetSynchronizer.observeLogin()
        }
    }

    /**
     * 서버에 «이 버전을 아직 써도 되는가» 를 프로세스 기동마다 한 번 물어본다 (#1539).
     *
     * `androidx.startup` Initializer 가 아닌 이유는 [startPushTargetSync] 와 같다 —
     * `InitializationProvider` 는 `Application.onCreate()` 이전에 도는 ContentProvider 라
     * 그 시점의 Hilt 접근이 계기 테스트를 전멸시킨다.
     *
     * 화면 진입을 기다리게 하지 않는다. 결과가 오면 [ForceUpdateGate.prompt] 가 바뀌고
     * [MainActivity] 가 그 위에 팝업을 얹을 뿐이라, 응답이 늦거나 영영 오지 않아도
     * 사용자는 평소대로 앱을 쓴다.
     */
    private fun startForceUpdateCheck() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            forceUpdateGate.refresh()
        }
    }
}

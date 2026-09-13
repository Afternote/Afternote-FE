package com.afternote.feature.setting.presentation

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat

/**
 * 테스트가 정한 결과를 즉시 또는 지연 전달하는 레지스트리. 실제 갤러리·카메라 Activity 를 띄우지 않는다.
 *
 * 온보딩의 `OnboardingProfilePickerResultTest` 가 세운 관용구를 갈래 둘로 늘린 것이다 — 프로필 사진은
 * 갤러리와 촬영 두 경로가 같은 화면에 걸려 있어, 어느 쪽이 몇 번 떴는지까지 봐야 「눌리지 않아야 할 때
 * 눌리지 않았는가」를 판정할 수 있다.
 */
internal class FakeMediaResultRegistry : ActivityResultRegistry() {
    /** 갤러리 피커가 돌려줄 결과. `null` 이면 사용자가 취소한 것이다. */
    var galleryResult: Uri? = null

    /** 촬영 결과. `false` 면 사용자가 취소했거나 카메라 앱이 저장에 실패한 것이다. */
    var captureSucceeds: Boolean = true

    /** `false` 면 촬영을 받아 줄 앱이 없는 기기를 흉내 낸다 — `launch` 자체가 터진다. */
    var captureAvailable: Boolean = true

    var deferCaptureResult: Boolean = false
    private var pendingCaptureRequestCode: Int? = null

    /** 시도 횟수다 — 실행 실패로 터진 호출도 센다. */
    var galleryLaunches: Int = 0
        private set

    var captureLaunches: Int = 0
        private set

    /** 촬영 인텐트에 실려 나간 결과 파일 URI. 카메라 앱이 여기에 써 넣는다. */
    var lastCaptureTarget: Uri? = null
        private set

    fun deliverCaptureResult() {
        val requestCode = checkNotNull(pendingCaptureRequestCode)
        pendingCaptureRequestCode = null
        dispatchResult(requestCode, captureSucceeds)
    }

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        @Suppress("UNCHECKED_CAST")
        when (contract) {
            is ActivityResultContracts.TakePicture -> {
                captureLaunches++
                if (!captureAvailable) throw ActivityNotFoundException("no camera app")
                lastCaptureTarget = input as Uri
                if (deferCaptureResult) {
                    pendingCaptureRequestCode = requestCode
                } else {
                    dispatchResult(requestCode, captureSucceeds as O)
                }
            }

            else -> {
                galleryLaunches++
                dispatchResult(requestCode, galleryResult as O)
            }
        }
    }
}

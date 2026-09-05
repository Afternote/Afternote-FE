package com.afternote.afternote_fe.messaging

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.push.DevicePushTargetProvider
import com.google.android.gms.tasks.Task
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * 이 기기를 FCM 에 등록하고 발송 대상 식별자를 돌려준다 (#1493).
 *
 * 이 앱은 매니페스트에 `firebase_messaging_installation_id_enabled=true` 를 켜 둬서 (#779)
 * **FID(Firebase Installation ID) 기반 등록 모델**을 쓴다. 그 모드에서 `getToken()` 은
 * `IllegalStateException: API disabled` 로 죽는다 — 발송 식별자가 registration token 이 아니라
 * FID 이기 때문이다(0829 에뮬레이터 실측으로 확인).
 *
 * 그래서 [currentTargetId] 의 순서가 둘이다.
 * 1. [FirebaseMessaging.register] 로 등록 시퀀스를 강제한다. 이걸 부르지 않으면 자동 초기화가
 *    돌 때까지 FID 가 FCM 에 붙지 않는다.
 * 2. [FirebaseInstallations.getId] 로 그 FID 를 읽는다. 콜백(`onRegistered`)만 기다리면 회전이
 *    없는 한 값을 못 받으므로, 지금 값이 필요한 등록 경로에서는 이렇게 직접 읽는다.
 *
 * 실패(Google Play 서비스 부재·네트워크·Firebase 미초기화)는 예외로 올리지 않고 null 로 접는다 —
 * 로그인·로그아웃 흐름이 이것 때문에 막힐 이유가 없다.
 */
@Singleton
class FirebaseDevicePushTargetProvider
    @Inject
    constructor(
        private val errorReporter: ErrorReporter,
    ) : DevicePushTargetProvider {
        override suspend fun currentTargetId(): String? =
            foldFailure(stage = STAGE_REGISTER) {
                FirebaseMessaging.getInstance().register().awaitOrNull(stage = STAGE_REGISTER)
                installationId()
            }

        override suspend fun existingTargetId(): String? = foldFailure(stage = STAGE_INSTALLATION_ID) { installationId() }

        private suspend fun installationId(): String? = FirebaseInstallations.getInstance().id.awaitOrNull(stage = STAGE_INSTALLATION_ID)

        /**
         * **동기** 실패까지 접는다. [awaitOrNull] 이 받는 것은 Task 콜백 실패뿐인데, 이 SDK 는
         * 그 자리에서 바로 던지기도 한다 — `FirebaseMessaging.getInstance()` 는 `FirebaseApp`
         * 초기화 전이면 `IllegalStateException` 을 던지고, FID 모델 전환 전 `getToken()` 이 내던
         * `API disabled` 도 Task 실패가 아니라 호출 즉시 던져진 예외였다. 그 갈래가 새면 이 클래스를
         * 부르는 등록 관찰이 통째로 끝나므로, 계약대로 여기서 null 로 접고 기록만 남긴다.
         */
        private suspend fun foldFailure(
            stage: String,
            block: suspend () -> String?,
        ): String? =
            runCatchingCancellable { block() }
                .onFailure { error -> errorReporter.recordFailure(error, mapOf("stage" to stage)) }
                .getOrNull()

        /**
         * 실패는 삼키되 [ErrorReporter] 에 남긴다. 조용히 null 이 되면 «푸시가 안 온다» 의 원인이
         * 실기 로그를 뒤지기 전에는 드러나지 않는다 — FID 모델 전환 전 `getToken()` 이 던지던
         * `API disabled` 가 정확히 그렇게 묻혀 있었다.
         */
        private suspend fun <T> Task<T>.awaitOrNull(stage: String): T? =
            suspendCancellableCoroutine { continuation ->
                addOnSuccessListener { value -> continuation.resume(value) }
                    .addOnFailureListener { error ->
                        errorReporter.recordFailure(error, mapOf("stage" to stage))
                        continuation.resume(null)
                    }.addOnCanceledListener { continuation.resume(null) }
            }
    }

private const val STAGE_REGISTER = "fcm_register"
private const val STAGE_INSTALLATION_ID = "fcm_installation_id"

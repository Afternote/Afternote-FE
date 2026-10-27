package com.afternote.afternote_fe.messaging

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.push.DevicePushTargetProvider
import com.afternote.core.domain.push.PushPermission
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.repository.push.PushTargetRepository
import com.afternote.core.domain.result.runCatchingCancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 이 기기의 FCM 식별자(FID)를 서버에 등록해 둔다 (#1493).
 *
 * 등록 시점은 두 곳이다.
 * 1. **로그인이 확정될 때마다** — 최초 로그인뿐 아니라 이미 로그인된 채 앱을 켠 경우도 포함한다.
 *    재설치·앱 데이터 삭제로 식별자가 새로 발급됐거나 서버 쪽 기록이 사라졌을 수 있어, 서버가
 *    upsert 로 받아 주는 것을 믿고 실행마다 한 번 다시 알린다.
 * 2. **`onRegistered` 로 FID 가 회전할 때** — 로그인 상태가 아니면 보류한다. 다음 로그인 확정이
 *    1번 경로로 새 식별자를 싣는다.
 *
 * 두 경로는 거의 동시에 발화한다 — 1번이 부르는 `register()` 가 성공하면 그 자리에서 2번 콜백이
 * 뜨기 때문이다. 같은 값을 두 번 보내면 서버가 동시 upsert 를 처리하다 500 을 돌려주므로
 * (0829 실측), 뮤텍스로 직렬화하고 직전에 성공한 값은 다시 보내지 않는다.
 *
 * 해제(로그아웃)는 여기가 아니라 `AuthRepositoryImpl.logout()` 이 맡는다 — 세션이 아직 살아 있는
 * 그 시점에만 서버 호출이 통하기 때문이다. 여기서는 로그아웃을 보면 캐시만 비워, 다음 로그인이
 * 다시 등록하도록 한다.
 *
 * ### 알림 권한 (#2321)
 *
 * 서버는 OS 권한을 읽지 못해 앱이 보고한 값으로 권한 거부 기기의 푸시를 건너뛴다. 그래서 로그인
 * 확정과 [onForeground] 때 권한을 읽어, 직전에 보고한 값과 다르면 `push-installations` 로 보고하고
 * 알고 있는 식별자를 새 권한으로 다시 등록한다. 사용자가 시스템 설정에서 알림을 끄고 돌아오는 경로가
 * 이 [onForeground] 다. 등록 중복 판정도 (식별자, 권한) 쌍으로 한다 — 권한만 바뀐 재등록은 보내야 한다.
 */
@Singleton
class PushTargetSynchronizer
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val devicePushTargetProvider: DevicePushTargetProvider,
        private val pushTargetRepository: PushTargetRepository,
        private val installationIdStore: PushInstallationIdStore,
        private val permissionReader: PushPermissionReader,
        private val errorReporter: ErrorReporter,
    ) {
        private val mutex = Mutex()

        /** 서버가 받아 준 마지막 등록. 같은 쌍은 다시 보내지 않는다. */
        private var lastRegistered: Registration? = null

        /** 등록을 시도한 마지막 식별자. 등록이 실패했어도 남겨 [onForeground] 가 다시 시도한다. */
        private var knownTargetId: String? = null

        private var lastReportedPermission: PushPermission? = null

        /**
         * 로그인 상태를 계속 지켜본다. 앱 프로세스가 사는 동안 돈다.
         *
         * 방출 하나를 처리하다 난 예외는 여기서 삼킨다. 이 `collect` 가 끝나면 **그 프로세스에서는
         * 이후 어떤 로그인도 다시 등록하지 못해**, 이 클래스가 고치려던 «푸시가 기기에 도달하지
         * 않는다» 로 그대로 되돌아가기 때문이다. 한 번의 실패는 그 로그인 한 건만 잃게 둔다.
         */
        suspend fun observeLogin() {
            authRepository.isLoggedIn
                .distinctUntilChanged()
                .collect { isLoggedIn ->
                    runCatchingCancellable {
                        if (isLoggedIn) {
                            syncPermission()
                            registerCurrentTargetId()
                        } else {
                            mutex.withLock {
                                lastRegistered = null
                                knownTargetId = null
                                lastReportedPermission = null
                            }
                        }
                    }.onFailure { error ->
                        errorReporter.recordFailure(error, mapOf("stage" to STAGE_OBSERVE_LOGIN))
                    }
                }
        }

        /**
         * FID 회전 통보를 받았을 때. 로그인 상태가 아니면 아무것도 하지 않는다.
         *
         * 부르는 쪽이 `FirebaseMessagingService.onRegistered` 의 `runBlocking` 이라 예외가 새면
         * SDK 스레드로 나간다. 해제 경로(`AuthRepositoryImpl.unregisterDevicePushTarget`)와 같이
         * 여기서 접는다.
         */
        suspend fun onTargetIdRotated(targetId: String) {
            runCatchingCancellable {
                if (isLoggedIn()) registerOnce(targetId)
            }.onFailure { error ->
                errorReporter.recordFailure(error, mapOf("stage" to STAGE_TARGET_ROTATED))
            }
        }

        /**
         * 앱이 앞으로 나올 때. 그사이 시스템 설정에서 알림 권한이 바뀌었으면 보고하고 다시 등록한다.
         * 로그인 상태가 아니면 아무것도 하지 않는다. 권한이 그대로면 서버 호출도 없다.
         */
        suspend fun onForeground() {
            runCatchingCancellable {
                if (isLoggedIn()) syncPermission()
            }.onFailure { error ->
                errorReporter.recordFailure(error, mapOf("stage" to STAGE_FOREGROUND))
            }
        }

        private suspend fun syncPermission() {
            mutex.withLock {
                val permission = permissionReader.current()
                val installationId = installationIdStore.installationId()
                if (lastReportedPermission != permission) {
                    pushTargetRepository
                        .reportPermission(installationId, permission)
                        .onSuccess { lastReportedPermission = permission }
                        .onFailure { error ->
                            errorReporter.recordFailure(error, mapOf("stage" to STAGE_REPORT_PERMISSION))
                        }
                }
                knownTargetId?.let { targetId -> registerLocked(targetId, installationId, permission) }
            }
        }

        /**
         * 기기 식별자 조회는 Firebase SDK 를 그대로 타므로 **동기 예외**로 실패할 수 있다
         * (`FirebaseApp` 미초기화 등). 그 갈래를 여기서 닫아 두면 위 `collect` 는 관찰만 이어 간다.
         */
        private suspend fun registerCurrentTargetId() {
            val targetId =
                runCatchingCancellable { devicePushTargetProvider.currentTargetId() }
                    .onFailure { error ->
                        errorReporter.recordFailure(error, mapOf("stage" to STAGE_DEVICE_ID))
                    }.getOrNull() ?: return
            registerOnce(targetId)
        }

        private suspend fun registerOnce(targetId: String) {
            mutex.withLock {
                registerLocked(
                    targetId = targetId,
                    installationId = installationIdStore.installationId(),
                    permission = permissionReader.current(),
                )
            }
        }

        /** 같은 값을 연달아 보내지 않는다 — 서버 동시 upsert 가 500 을 내기 때문이다. [mutex] 안에서만 부른다. */
        private suspend fun registerLocked(
            targetId: String,
            installationId: String,
            permission: PushPermission,
        ) {
            knownTargetId = targetId
            val registration = Registration(targetId, permission)
            if (lastRegistered == registration) return
            pushTargetRepository
                .register(targetId, installationId, permission)
                .onSuccess { lastRegistered = registration }
                .onFailure { error ->
                    errorReporter.recordFailure(error, mapOf("stage" to STAGE_REGISTER))
                }
        }

        private data class Registration(
            val targetId: String,
            val permission: PushPermission,
        )

        private suspend fun isLoggedIn(): Boolean =
            runCatchingCancellable {
                authRepository.getAccessToken().getOrNull() != null
            }.getOrNull() ?: false
    }

private const val STAGE_REGISTER = "push_target_register"
private const val STAGE_DEVICE_ID = "push_target_device_id"
private const val STAGE_OBSERVE_LOGIN = "push_target_observe_login"
private const val STAGE_TARGET_ROTATED = "push_target_rotated"
private const val STAGE_REPORT_PERMISSION = "push_permission_report"
private const val STAGE_FOREGROUND = "push_target_foreground"

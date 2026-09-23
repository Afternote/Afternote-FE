package com.afternote.afternote_fe.notification

import android.content.Context
import com.afternote.core.common.notification.NotificationScheduler
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.auth.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 데일리 알림(매일 09:00 로컬 알림) 예약을 로그인 상태에 맞춘다 (#2146).
 *
 * 알림 문구가 계정의 기록을 재촉하는 말이라, 기록할 계정이 없는 기기에는 예약을 두지 않는다.
 * 로그인이 확정되면 예약하고 로그아웃되면 지운다. 로그아웃, 탈퇴, 세션 강제 만료는 모두 토큰
 * 저장소를 비워 [AuthRepository.isLoggedIn] 을 false 로 바꾸므로 이 관찰 하나가 세 경로를 다 받는다.
 * 로그아웃 상태로 앱을 켜면 첫 방출이 false 라, 이전 빌드가 세션 없이 넣어 둔 예약도 여기서 지워진다.
 *
 * 예약은 `ExistingPeriodicWorkPolicy.KEEP` 이다. 로그인한 채 앱을 켤 때마다 다시 불러도 다음 발화
 * 시각은 밀리지 않는다.
 *
 * ### 왜 App Startup Initializer 가 아닌가
 *
 * 전에는 `DailyNotificationInitializer` 가 세션을 보지 않고 예약했다. 세션은 Hilt 로 만든 저장소에
 * 있고 Initializer 는 Hilt 에 닿으면 안 되므로(`StartupInitializerHiltKonsistTest`) 판정 자리를
 * `GlobalApplication.onCreate` 로 옮겼다.
 *
 * 옛 Initializer 는 Application.onCreate 에서 부르면 WorkManager 가 먼저 준비된다는 보장이 없다며
 * `WorkManagerInitializer` 를 선행 의존으로 걸었다. 그 보장은 이 자리에서도 선다. work-runtime 라이브러리
 * 매니페스트가 `WorkManagerInitializer` 를 앱의 `InitializationProvider` 에 병합해 두고, ContentProvider 는
 * Application.onCreate 보다 먼저 초기화된다.
 */
@Singleton
class DailyNotificationScheduleSynchronizer
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val authRepository: AuthRepository,
        private val errorReporter: ErrorReporter,
    ) {
        /**
         * 로그인 상태를 계속 지켜본다. 앱 프로세스가 사는 동안 돈다.
         *
         * 방출 하나를 처리하다 난 예외는 기록만 하고 관찰은 이어 간다. 여기서 `collect` 가 끝나면
         * 그 프로세스에서는 이후 로그아웃이 예약을 지우지 못한다.
         */
        suspend fun observeLogin() {
            authRepository.isLoggedIn
                .distinctUntilChanged()
                .collect { isLoggedIn ->
                    runCatchingCancellable {
                        if (isLoggedIn) {
                            NotificationScheduler.scheduleDailyNotification(
                                context,
                                hour = DAILY_HOUR,
                                minute = DAILY_MINUTE,
                            )
                        } else {
                            NotificationScheduler.cancelDailyNotification(context)
                        }
                    }.onFailure { error ->
                        errorReporter.recordFailure(error, mapOf("stage" to STAGE_SYNC))
                    }
                }
        }
    }

// TODO: 설정(DataStore 등)에서 시·분 읽기. 알림 시각은 PM 알림 문서가 정한다(0920 회의 기준 미정).
private const val DAILY_HOUR = 9
private const val DAILY_MINUTE = 0

private const val STAGE_SYNC = "daily_notification_sync"

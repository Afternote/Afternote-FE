package com.afternote.afternote_fe.update

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.appversion.AppVersionRepository
import com.afternote.core.model.appversion.AppVersionCheck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 관문이 사용자에게 보여 줄 것 — 보낼 스토어 주소 하나다. */
data class ForceUpdatePrompt(
    val storeUrl: String,
)

/**
 * 서버가 «이 버전은 더 못 쓴다» 고 말할 때 사용자를 스토어로 보내는 관문 (#1539).
 *
 * `GET /app/version` 은 #423 이 추가한 뒤로 소비처가 0건이었다. 서버는 판정을 들고 있는데
 * 앱이 물어보지 않아, 구버전 사용자가 계속 쓰다 다른 곳에서 깨졌다.
 *
 * ### fail-open — 못 물어봤으면 통과시킨다
 *
 * 조회 실패·타임아웃·파싱 실패는 전부 «업데이트 불필요» 와 같게 다룬다. 서버가 죽었다는
 * 이유로 앱 전체가 잠기면 강제 업데이트가 막으려던 것보다 큰 피해가 난다.
 * BE `@Operation` 설명은 반대로 «API 호출 실패 시에도 스플래시 진입을 막는다» 고 적고 있지만,
 * 이 저장소는 #1539 완료 조건대로 fail-open 을 택했다.
 *
 * 지연도 같은 원칙으로 닫혀 있다 — 이 관문은 시작을 **지연시키지 않는다**. 앱은 평소대로
 * 뜨고, 판정이 돌아왔을 때만 그 위에 팝업이 얹힌다. 그래서 응답이 영영 오지 않아도
 * 사용자는 아무것도 잃지 않는다.
 *
 * ### 보낼 곳이 없으면 걸지 않는다
 *
 * `updateRequired=true` 라도 **실제로 그 설치본을 갱신할 수 있는 경로가 있을 때만** 막는다.
 * 두 조건을 각각 확인한다.
 *
 * 1. 서버가 준 `storeUrl` 이 실제 Play Store 주소인가 ([playStoreTargetOrNull]).
 *    dev 서버는 지금 `APP_ANDROID_STORE_URL` 미설정으로 플레이스홀더를 들고 있다.
 *    거기로 보내면 사용자는 갈 곳 없이 팝업만 마주한다.
 * 2. 이 빌드가 스토어로 갱신될 수 있는 산출물인가 ([InstalledBuild.storeDistributed]).
 *    로컬·Firebase 빌드는 기본 versionCode 를 달고 있어 서버 `latestVersionCode` 와 같은 축에
 *    있지 않고(`1 < 10001` 이 언제나 참), 서명도 달라 스토어가 그 위에 덮어쓰지 못한다.
 *
 * 두 조건이 다 갖춰지는 시점은 Play 내부 테스트 트랙 배포(#852)가 서고 서버가 실 스토어 URL 을
 * 들고 나서다. 그때 이 관문은 **코드 변경 없이 스스로 켜진다**.
 *
 * ### 계측에서는 돌지 않는다
 *
 * 유일한 기동 지점이 `GlobalApplication.onCreate` 인데, androidTest 는 그 클래스를
 * `HiltTestApplication` 으로 대체하므로 [refresh] 가 호출되지 않는다 —
 * `PushTargetSynchronizer`(#1493) 와 같은 구조다. 그래도 필요하면
 * [AppVersionRepository] 가 도메인 인터페이스라 페이크로 대체할 수 있다.
 */
@Singleton
class ForceUpdateGate
    @Inject
    constructor(
        private val appVersionRepository: AppVersionRepository,
        private val installedBuild: InstalledBuild,
        private val errorReporter: ErrorReporter,
    ) {
        private val promptState = MutableStateFlow<ForceUpdatePrompt?>(null)

        /** null 이면 막을 이유가 없다 — 초기값이자 fail-open 의 결과값이다. */
        val prompt: StateFlow<ForceUpdatePrompt?> = promptState.asStateFlow()

        /**
         * 서버 판정을 한 번 받아 온다. 프로세스 기동마다 한 번 부른다.
         *
         * 실패해도 [prompt] 를 건드리지 않는다 — 직전 판정이 있었다면 그대로 두고,
         * 없었다면 null(통과) 로 남는다.
         */
        suspend fun refresh() {
            val check =
                appVersionRepository
                    .checkAndroidVersion(installedBuild.versionCode)
                    .onFailure { error ->
                        errorReporter.recordFailure(error, mapOf(KEY_STAGE to STAGE_CHECK))
                    }.getOrNull() ?: return

            promptState.value = decide(check)
        }

        private fun decide(check: AppVersionCheck): ForceUpdatePrompt? {
            if (!check.updateRequired) return null

            val storeUrl = playStoreTargetOrNull(check.storeUrl)
            if (storeUrl == null) {
                // 서버가 막으라고 했는데 보낼 곳을 주지 않았다. 서버 설정 결함이므로 텔레메트리로 남기고 통과시킨다.
                errorReporter.recordFailure(
                    UnroutableStoreUrlException(),
                    mapOf(KEY_STAGE to STAGE_STORE_URL),
                )
                return null
            }

            // 스토어가 이 설치본을 갱신할 수 없다면 막아 봐야 빠져나갈 길이 없다.
            if (!installedBuild.storeDistributed) return null

            return ForceUpdatePrompt(storeUrl = storeUrl)
        }

        private companion object {
            const val KEY_STAGE = "stage"
            const val STAGE_CHECK = "force_update_check"
            const val STAGE_STORE_URL = "force_update_store_url"
        }
    }

/**
 * 서버가 `updateRequired=true` 와 함께 준 주소가 Play Store 로 가지 않을 때 남기는 실패 타입.
 *
 * [ErrorReporter] 가 문구를 지우므로 콘솔에서 이 갈래를 가르는 것은 `error_type` 과
 * `stage=force_update_store_url` 이다. 그래서 이 타입의 **이름 자체가 관측 계약**이고,
 * 타입을 내보낼 이유는 그것뿐이다 — 프로덕션 사용처가 이 파일뿐이라 `private` 로 닫고
 * 테스트는 `error_type` 문자열로 계약을 고정한다 (#1678 가드).
 */
private class UnroutableStoreUrlException : RuntimeException("force update store url is not a Play Store target")

/**
 * Play Store 앱 상세로 실제로 가는 주소면 그 값을, 아니면 null 을 돌려준다.
 *
 * 화이트리스트로 판정하는 이유 — 「http 로 시작하니 열리겠지」 식 판정은 dev 시드의
 * `http://your-playstore-domain` 같은 플레이스홀더를 그대로 통과시킨다. 관문이 사용자를
 * 가두는 대가로 얻는 게 «갈 수 있는 곳» 이므로, 갈 수 있다고 확신할 때만 막는다.
 */
internal fun playStoreTargetOrNull(storeUrl: String?): String? {
    val url = storeUrl?.trim().orEmpty()
    if (!url.startsWith(MARKET_DETAILS_PREFIX) && !url.startsWith(PLAY_DETAILS_PREFIX)) return null
    return url
}

private const val MARKET_DETAILS_PREFIX = "market://details?id="
private const val PLAY_DETAILS_PREFIX = "https://play.google.com/store/apps/details?"

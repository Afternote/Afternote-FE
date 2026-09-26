package com.afternote.core.common.deeplink

import java.net.URI

/**
 * 링크 문자열 하나를 [NavigationTarget] 으로 옮기는 단일 관문 (#924).
 *
 * 입력을 `String` 으로 받아 이 모듈이 Android 플랫폼 타입(`android.net.Uri`)에 묶이지 않는다. 그래서
 * 이 파서의 테스트는 Robolectric 없이 도는 순수 JVM 테스트다. 지금 이 함수를 부르는 곳은 브라우저
 * App Link(`Intent.getData()`) 하나다. FCM 이 목적지를 URL 로 실을지 키로 실을지는
 * Afternote-BE#261 이 정하고, URL 로 정해질 때만 이 함수를 함께 지난다.
 *
 * ## 계약
 *
 * | 항목 | 값 |
 * |---|---|
 * | scheme | `https` 만 |
 * | host | `afternote.kro.kr` 만 (대소문자 무시) |
 * | userinfo | 없음 — 붙으면 거절 |
 * | port | 생략 또는 443 |
 * | path | 아래 표의 정규 경로만. 소문자·끝 슬래시 없음·퍼센트 인코딩 없음 |
 * | query | 지원하는 키 없음 — 붙으면 거절 |
 * | fragment | 지원 안 함 — 붙으면 거절 |
 *
 * | 경로 | 목적지 | 관문 |
 * |---|---|---|
 * | `/` | [NavigationTarget.Home] | 로그인 |
 * | `/afternote` | [NavigationTarget.AfternoteHome] | 로그인 + 지문 |
 * | `/mindrecord/daily-question` | [NavigationTarget.DailyQuestionCompose] | 로그인 |
 * | `/settings/notification` | [NavigationTarget.NotificationSettings] | 로그인 |
 *
 * 애프터노트 상세·타임레터 상세가 표에 없는 이유는 [NavigationTarget] KDoc 에 있다. 수신자 경로는
 * 서버 식별자·접근 관문이 PM 결정을 기다리고 있어 넣지 않는다(#1951).
 *
 * canonical URL 은 `https://afternote.kro.kr` + 경로다([canonicalUrl]).
 *
 * ## 왜 이렇게까지 좁히나
 *
 * 링크는 **앱 밖에서 오는 입력**이다. 관대하게 받으면 그만큼이 공격면이다 — 커스텀 scheme 은
 * Digital Asset Links 검증을 못 받아 아무 앱이나 선점할 수 있다. 그래서 계약에 **명시된 것만**
 * 통과시키고 나머지는 전부 [AppLinkResolution.Rejected] 다. 모르는 입력을 추측해서 여는 경로는 없다.
 *
 * userinfo 를 거절하는 이유는 위장 방어가 아니라 정규형이다. `https://evil@afternote.kro.kr/` 의 host 는
 * 실제로 우리 도메인이라 막을 위장이 없고, 위장이 되는 반대 모양(`https://afternote.kro.kr@evil.com/`)은
 * host 가 `evil.com` 으로 읽혀 host 검사에서 이미 떨어진다. userinfo 는 계약에서 뜻이 없는 칸이라 받으면
 * 목적지 하나를 가리키는 URL 이 끝없이 생긴다. query·fragment·끝 슬래시를 거절하는 것과 같은 이유다.
 */
object AfternoteAppLinkParser {
    /** 검증된 App Link 의 유일한 scheme. */
    const val CANONICAL_SCHEME: String = "https"

    /** 검증된 App Link 의 유일한 host. `assetlinks.json` 이 이 도메인에 올라가 있다(Afternote-BE#228). */
    const val CANONICAL_HOST: String = "afternote.kro.kr"

    /** [target] 을 가리키는 정규 URL. `parse(canonicalUrl(t)) == Resolved(t)` 가 목적지 전량에서 성립한다. */
    fun canonicalUrl(target: NavigationTarget): String = "$CANONICAL_SCHEME://$CANONICAL_HOST${target.canonicalPath}"

    /**
     * [rawLink] 를 목적지로 옮긴다. 계약 밖이면 사유와 안전한 기본 진입을 담은
     * [AppLinkResolution.Rejected] 다 — 예외를 던지지 않으며 `null` 도 없다.
     */
    fun parse(rawLink: String?): AppLinkResolution {
        val trimmed = rawLink?.trim().orEmpty()
        if (trimmed.isEmpty()) return reject(AppLinkRejectionReason.MALFORMED_URI)

        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return reject(AppLinkRejectionReason.MALFORMED_URI)

        if (!CANONICAL_SCHEME.equals(uri.scheme, ignoreCase = true)) {
            return reject(AppLinkRejectionReason.UNSUPPORTED_SCHEME)
        }
        if (!CANONICAL_HOST.equals(uri.host, ignoreCase = true)) {
            return reject(AppLinkRejectionReason.UNSUPPORTED_HOST)
        }
        if (uri.rawUserInfo != null) return reject(AppLinkRejectionReason.UNSUPPORTED_USERINFO)
        if (uri.port !in ACCEPTED_PORTS) return reject(AppLinkRejectionReason.UNSUPPORTED_PORT)
        if (uri.rawQuery != null) return reject(AppLinkRejectionReason.UNSUPPORTED_QUERY)
        if (uri.rawFragment != null) return reject(AppLinkRejectionReason.UNSUPPORTED_FRAGMENT)

        val segments =
            uri.rawPath.orEmpty().toCanonicalSegments()
                ?: return reject(AppLinkRejectionReason.UNKNOWN_PATH)

        return resolvePath(segments)
    }

    private fun resolvePath(segments: List<String>): AppLinkResolution =
        when {
            segments.isEmpty() -> {
                AppLinkResolution.Resolved(NavigationTarget.Home)
            }

            segments == listOf(SEGMENT_AFTERNOTE) -> {
                AppLinkResolution.Resolved(NavigationTarget.AfternoteHome)
            }

            segments == listOf(SEGMENT_MINDRECORD, SEGMENT_DAILY_QUESTION) -> {
                AppLinkResolution.Resolved(NavigationTarget.DailyQuestionCompose)
            }

            segments == listOf(SEGMENT_SETTINGS, SEGMENT_NOTIFICATION) -> {
                AppLinkResolution.Resolved(NavigationTarget.NotificationSettings)
            }

            else -> {
                reject(AppLinkRejectionReason.UNKNOWN_PATH)
            }
        }

    /**
     * 정규 경로 세그먼트 목록. 계약 밖 문자가 하나라도 있으면 `null` 이고, 호출부가 그것을
     * [AppLinkRejectionReason.UNKNOWN_PATH] 거절로 바꾼다. 빈 목록은 거절이 아니라 루트(`/`)다.
     *
     * 빈 세그먼트(`""`)를 버리지 않고 **거절**하는 것이 요점이다 — 버리면 `/settings//notification` 이나
     * `/settings/notification/` 이 정규형과 같은 목적지가 되어 canonical URL 이 하나가 아니게 된다.
     */
    private fun String.toCanonicalSegments(): List<String>? {
        if (isEmpty() || this == "/") return emptyList()
        if (!startsWith('/')) return null

        val segments = drop(1).split('/')
        return segments.takeIf { parsed -> parsed.all(SEGMENT_FORMAT::matches) }
    }

    private fun reject(reason: AppLinkRejectionReason): AppLinkResolution = AppLinkResolution.Rejected(reason)

    /** 생략(`-1`)이거나 https 기본 포트만 허용한다. 다른 포트는 우리 서비스가 서지 않는다. */
    private val ACCEPTED_PORTS = setOf(-1, 443)

    /** 정규 경로 세그먼트는 소문자·숫자·하이픈뿐이다. 대문자·퍼센트 인코딩·점은 정규형이 아니다. */
    private val SEGMENT_FORMAT = Regex("[a-z0-9-]+")

    private const val SEGMENT_AFTERNOTE = "afternote"
    private const val SEGMENT_MINDRECORD = "mindrecord"
    private const val SEGMENT_DAILY_QUESTION = "daily-question"
    private const val SEGMENT_SETTINGS = "settings"
    private const val SEGMENT_NOTIFICATION = "notification"
}

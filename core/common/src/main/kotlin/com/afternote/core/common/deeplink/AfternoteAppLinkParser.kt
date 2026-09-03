package com.afternote.core.common.deeplink

import java.net.URI

/**
 * 링크 문자열 하나를 [NavigationTarget] 으로 옮기는 단일 관문 (#924).
 *
 * 브라우저 App Link(`Intent.getData()`)와 FCM `data.link` 가 **같은 함수**를 지난다. 입력을
 * `String` 으로 받는 이유가 그것이다 — 한쪽은 `android.net.Uri`, 다른 쪽은 payload 의 문자열이라
 * 공통분모가 문자열뿐이고, 문자열로 받아야 이 모듈이 Android 플랫폼 타입에도 묶이지 않는다
 * (그래서 이 파서의 테스트는 Robolectric 없이 도는 순수 JVM 테스트다).
 *
 * ## 계약
 *
 * | 항목 | 값 |
 * |---|---|
 * | scheme | `https` 만 |
 * | host | `afternote.kro.kr` 만 (대소문자 무시, userinfo 금지, 포트는 생략 또는 443) |
 * | path | 아래 표의 정규 경로만. 소문자·끝 슬래시 없음·퍼센트 인코딩 없음 |
 * | query | 지원하는 키 없음 — 붙으면 거절 |
 * | fragment | 지원 안 함 — 붙으면 거절 |
 *
 * | 경로 | 목적지 | ID 형식 | 관문 |
 * |---|---|---|---|
 * | `/` | [NavigationTarget.Home] | — | 로그인 |
 * | `/afternote` | [NavigationTarget.AfternoteHome] | — | 로그인 + 지문 |
 * | `/afternote/{id}` | [NavigationTarget.AfternoteDetail] | 양의 10진 정수 | 로그인 + 지문 |
 * | `/received` | [NavigationTarget.ReceivedRecordBox] | — | 로그인 |
 * | `/received/senders/{senderId}` | [NavigationTarget.ReceivedSenderDetail] | 소문자 UUID | 로그인 |
 * | `/received/afternote/{id}` | [NavigationTarget.ReceivedAfternoteDetail] | 양의 10진 정수 | 로그인 + 수신자 본인인증 |
 * | `/timeletter/{id}` | [NavigationTarget.TimeLetterDetail] | 양의 10진 정수 | 로그인 |
 * | `/mindrecord/daily-question` | [NavigationTarget.DailyQuestionCompose] | — | 로그인 |
 * | `/settings/notification` | [NavigationTarget.NotificationSettings] | — | 로그인 |
 *
 * canonical URL 은 `https://afternote.kro.kr` + 경로다([canonicalUrl]).
 *
 * ## 왜 이렇게까지 좁히나
 *
 * 링크는 **앱 밖에서 오는 입력**이다. 관대하게 받으면 그만큼이 공격면이다 — 커스텀 scheme 은
 * Digital Asset Links 검증을 못 받아 아무 앱이나 선점할 수 있고, `https://evil@afternote.kro.kr/`
 * 같은 userinfo 는 눈으로 우리 도메인처럼 보인다. 그래서 계약에 **명시된 것만** 통과시키고 나머지는
 * 전부 [AppLinkResolution.Rejected] 다. 모르는 입력을 추측해서 여는 경로는 없다.
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
        if (!CANONICAL_HOST.equals(uri.host, ignoreCase = true) ||
            uri.rawUserInfo != null ||
            uri.port !in ACCEPTED_PORTS
        ) {
            return reject(AppLinkRejectionReason.UNSUPPORTED_HOST)
        }
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

            segments.size == 2 && segments[0] == SEGMENT_AFTERNOTE -> {
                segments[1].resolveWithId(NavigationTarget::AfternoteDetail)
            }

            segments == listOf(SEGMENT_RECEIVED) -> {
                AppLinkResolution.Resolved(NavigationTarget.ReceivedRecordBox)
            }

            segments.size == 3 && segments[0] == SEGMENT_RECEIVED && segments[1] == SEGMENT_SENDERS -> {
                segments[2]
                    .takeIf(SENDER_ID_FORMAT::matches)
                    ?.let { AppLinkResolution.Resolved(NavigationTarget.ReceivedSenderDetail(it)) }
                    ?: reject(AppLinkRejectionReason.MALFORMED_ID)
            }

            segments.size == 3 && segments[0] == SEGMENT_RECEIVED && segments[1] == SEGMENT_AFTERNOTE -> {
                segments[2].resolveWithId(NavigationTarget::ReceivedAfternoteDetail)
            }

            segments.size == 2 && segments[0] == SEGMENT_TIMELETTER -> {
                segments[1].resolveWithId(NavigationTarget::TimeLetterDetail)
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

    private fun String.resolveWithId(target: (Long) -> NavigationTarget): AppLinkResolution =
        toResourceIdOrNull()
            ?.let { AppLinkResolution.Resolved(target(it)) }
            ?: reject(AppLinkRejectionReason.MALFORMED_ID)

    /**
     * 정규 경로 세그먼트 목록. 계약 밖 문자가 하나라도 있으면 `null` 이다.
     *
     * 빈 세그먼트를 버리지 않고 **거절**하는 것이 요점이다 — 버리면 `/afternote//1` 이나
     * `/afternote/1/` 이 정규형과 같은 목적지가 되어 canonical URL 이 하나가 아니게 된다.
     */
    private fun String.toCanonicalSegments(): List<String>? {
        if (isEmpty() || this == "/") return emptyList()
        if (!startsWith('/')) return null

        val segments = drop(1).split('/')
        return segments.takeIf { parsed -> parsed.all(SEGMENT_FORMAT::matches) }
    }

    /**
     * 양의 10진 정수 ID. 앞자리 `0`·부호·공백·비-ASCII 숫자는 전부 거절이다.
     *
     * 자릿수 판정에 정규식 `\d` 를 쓰지 않는다 — Android 의 `\d` 는 유니코드 숫자(예: 아라비아-인도
     * 숫자)까지 물어 서버 JVM 과 다르게 판정한다. `'0'..'9'` 로 직접 좁힌다.
     */
    private fun String.toResourceIdOrNull(): Long? {
        if (isEmpty() || length > MAX_ID_LENGTH) return null
        if (any { character -> character !in '0'..'9' }) return null
        if (length > 1 && first() == '0') return null
        return toLongOrNull()?.takeIf { id -> id > 0L }
    }

    private fun reject(reason: AppLinkRejectionReason): AppLinkResolution = AppLinkResolution.Rejected(reason)

    /** 생략(`-1`)이거나 https 기본 포트만 허용한다. 다른 포트는 우리 서비스가 서지 않는다. */
    private val ACCEPTED_PORTS = setOf(-1, 443)

    /** 정규 경로 세그먼트는 소문자·숫자·하이픈뿐이다. 대문자·퍼센트 인코딩·점은 정규형이 아니다. */
    private val SEGMENT_FORMAT = Regex("[a-z0-9-]+")

    /** `SenderRegistry` 가 만드는 소문자 canonical UUID. */
    private val SENDER_ID_FORMAT =
        Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

    /** `Long.MAX_VALUE` 의 자릿수. 넘으면 `toLongOrNull` 이전에 잘라 낸다. */
    private const val MAX_ID_LENGTH = 19

    private const val SEGMENT_AFTERNOTE = "afternote"
    private const val SEGMENT_RECEIVED = "received"
    private const val SEGMENT_SENDERS = "senders"
    private const val SEGMENT_TIMELETTER = "timeletter"
    private const val SEGMENT_MINDRECORD = "mindrecord"
    private const val SEGMENT_DAILY_QUESTION = "daily-question"
    private const val SEGMENT_SETTINGS = "settings"
    private const val SEGMENT_NOTIFICATION = "notification"
}

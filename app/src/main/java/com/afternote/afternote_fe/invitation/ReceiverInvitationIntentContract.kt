package com.afternote.afternote_fe.invitation

import android.content.Intent

/**
 * 카카오톡 초대 메시지 버튼이 여는 `kakao{앱키}://kakaolink?inviteToken=…` Intent 를 해석한다 (#944).
 *
 * `NotificationIntentContract` 처럼 순수 객체다 — 판정 규칙은 [resolve] 하나에 모여 있고,
 * `MainActivity` 는 [fromIntent] 만 부른다.
 *
 * 토큰은 서버가 발급한 43자 base64url 이다. 형식 밖 값은 우리가 만든 링크가 아니므로 저장하지 않는다 —
 * 스킴은 누구나 던질 수 있어 검증 없이 보관하면 임의 문자열이 서버 경로 세그먼트까지 흘러간다.
 */
internal object ReceiverInvitationIntentContract {
    fun fromIntent(intent: Intent): String? {
        val data = runCatching { intent.data }.getOrNull() ?: return null
        return resolve(
            scheme = data.scheme,
            host = data.host,
            rawToken = runCatching { data.getQueryParameter(QUERY_INVITE_TOKEN) }.getOrNull(),
        )
    }

    private fun resolve(
        scheme: String?,
        host: String?,
        rawToken: String?,
    ): String? {
        if (scheme == null || !scheme.startsWith(SCHEME_PREFIX)) return null
        if (host != HOST_KAKAOLINK) return null
        val token = rawToken?.takeIf { TOKEN_FORMAT.matches(it) } ?: return null
        return token
    }

    /** 카카오 네이티브 앱 키를 붙인 스킴 — 키 값 자체는 매니페스트 플레이스홀더가 맞춘다. */
    private const val SCHEME_PREFIX = "kakao"
    private const val HOST_KAKAOLINK = "kakaolink"
    private const val QUERY_INVITE_TOKEN = "inviteToken"

    /** base64url 문자만, 길이 상한 128 — 서버 발급 길이(43)의 여유분이지 계약값은 아니다. */
    private val TOKEN_FORMAT = Regex("^[A-Za-z0-9_-]{1,128}$")
}

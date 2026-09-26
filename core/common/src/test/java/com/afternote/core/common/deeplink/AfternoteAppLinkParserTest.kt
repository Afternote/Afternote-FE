package com.afternote.core.common.deeplink

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * URI 계약 표의 모든 행과 fail-closed 거절 전량을 잠근다 (#924).
 *
 * 표의 «있는 행»만 세면 계약이 넓어지는 것을 못 잡는다. 그래서 거절 쪽도 사유별로 단언한다 —
 * 알 수 없는 path·미지원 query·fragment 는 각각 다른 이유로 거절돼야 한다.
 */
class AfternoteAppLinkParserTest {
    @Test
    fun `계약 표의 모든 행이 목적지로 해석된다`() {
        val rows =
            mapOf(
                "https://afternote.kro.kr/" to NavigationTarget.Home,
                "https://afternote.kro.kr/afternote" to NavigationTarget.AfternoteHome,
                "https://afternote.kro.kr/mindrecord/daily-question" to
                    NavigationTarget.DailyQuestionCompose,
                "https://afternote.kro.kr/settings/notification" to
                    NavigationTarget.NotificationSettings,
            )

        rows.forEach { (link, expected) ->
            assertEquals(link, AppLinkResolution.Resolved(expected), AfternoteAppLinkParser.parse(link))
        }
    }

    @Test
    fun `호스트 없는 사이트 루트도 홈이다`() {
        assertEquals(
            AppLinkResolution.Resolved(NavigationTarget.Home),
            AfternoteAppLinkParser.parse("https://afternote.kro.kr"),
        )
    }

    @Test
    fun `scheme 과 host 는 대소문자를 무시한다`() {
        assertEquals(
            AppLinkResolution.Resolved(NavigationTarget.AfternoteHome),
            AfternoteAppLinkParser.parse("HTTPS://AFTERNOTE.KRO.KR/afternote"),
        )
    }

    @Test
    fun `기본 포트 443 은 생략과 같다`() {
        assertEquals(
            AppLinkResolution.Resolved(NavigationTarget.AfternoteHome),
            AfternoteAppLinkParser.parse("https://afternote.kro.kr:443/afternote"),
        )
    }

    @Test
    fun `앞뒤 공백은 다듬어 해석한다`() {
        assertEquals(
            AppLinkResolution.Resolved(NavigationTarget.AfternoteHome),
            AfternoteAppLinkParser.parse("  https://afternote.kro.kr/afternote\n"),
        )
    }

    @Test
    fun `알 수 없는 path 는 거절이다`() {
        listOf(
            "https://afternote.kro.kr/unknown",
            "https://afternote.kro.kr/settings/notification/extra",
            "https://afternote.kro.kr/received/senders",
            "https://afternote.kro.kr/mindrecord",
            "https://afternote.kro.kr/settings",
            "https://afternote.kro.kr/Afternote",
            "https://afternote.kro.kr/afternote/",
            "https://afternote.kro.kr/settings//notification",
            "https://afternote.kro.kr/%2Fafternote",
            "https://afternote.kro.kr/ａfternote",
            "https://afternote.kro.kr/afternote/../settings/notification",
        ).forEach { link ->
            assertEquals(link, rejectedBecause(AppLinkRejectionReason.UNKNOWN_PATH), AfternoteAppLinkParser.parse(link))
        }
    }

    @Test
    fun `지원하지 않는 query 는 거절이다`() {
        listOf(
            "https://afternote.kro.kr/afternote?utm_source=mail",
            "https://afternote.kro.kr/settings/notification?ref=push",
            "https://afternote.kro.kr/afternote?",
        ).forEach { link ->
            assertEquals(
                link,
                rejectedBecause(AppLinkRejectionReason.UNSUPPORTED_QUERY),
                AfternoteAppLinkParser.parse(link),
            )
        }
    }

    @Test
    fun `fragment 는 거절이다`() {
        assertEquals(
            rejectedBecause(AppLinkRejectionReason.UNSUPPORTED_FRAGMENT),
            AfternoteAppLinkParser.parse("https://afternote.kro.kr/afternote#top"),
        )
    }

    @Test
    fun `https 가 아닌 scheme 은 거절이다`() {
        listOf(
            "http://afternote.kro.kr/afternote",
            "afternote://afternote.kro.kr/afternote",
            "/afternote",
            "mailto:hello@afternote.kro.kr",
        ).forEach { link ->
            assertEquals(
                link,
                rejectedBecause(AppLinkRejectionReason.UNSUPPORTED_SCHEME),
                AfternoteAppLinkParser.parse(link),
            )
        }
    }

    @Test
    fun `다른 host 는 거절이다`() {
        listOf(
            "https://evil.example.com/afternote",
            "https://afternote.kro.kr.evil.example.com/afternote",
            "https://afternote.kro.kr@evil.example.com/afternote",
            "https://sub.afternote.kro.kr/afternote",
            "https:///afternote",
        ).forEach { link ->
            assertEquals(
                link,
                rejectedBecause(AppLinkRejectionReason.UNSUPPORTED_HOST),
                AfternoteAppLinkParser.parse(link),
            )
        }
    }

    @Test
    fun `우리 host 에 userinfo 가 붙으면 거절이다`() {
        listOf(
            "https://evil@afternote.kro.kr/afternote",
            "https://user:pass@afternote.kro.kr/afternote",
            "https://@afternote.kro.kr/afternote",
        ).forEach { link ->
            assertEquals(
                link,
                rejectedBecause(AppLinkRejectionReason.UNSUPPORTED_USERINFO),
                AfternoteAppLinkParser.parse(link),
            )
        }
    }

    @Test
    fun `생략이나 443 이 아닌 포트는 거절이다`() {
        listOf(
            "https://afternote.kro.kr:8443/afternote",
            "https://afternote.kro.kr:80/afternote",
        ).forEach { link ->
            assertEquals(
                link,
                rejectedBecause(AppLinkRejectionReason.UNSUPPORTED_PORT),
                AfternoteAppLinkParser.parse(link),
            )
        }
    }

    @Test
    fun `비어 있거나 파싱되지 않는 입력은 거절이다`() {
        listOf(null, "", "   ", "https://afternote.kro.kr/after note", "::::").forEach { link ->
            assertEquals(
                "$link",
                rejectedBecause(AppLinkRejectionReason.MALFORMED_URI),
                AfternoteAppLinkParser.parse(link),
            )
        }
    }

    @Test
    fun `거절의 기본 진입은 홈이고 로그인 관문을 지난다`() {
        val rejected = AfternoteAppLinkParser.parse("https://afternote.kro.kr/unknown")

        assertEquals(NavigationTarget.Home, rejected.target)
        assertEquals(listOf(AuthGate.LOGIN), rejected.target.requiredGates)
    }

    /**
     * ID 를 싣는 상세 경로는 계약에서 뺐다([NavigationTarget] KDoc) — 애프터노트 상세는 도착할
     * 자리가 없고, 타임레터 상세는 ID 가 무엇인지 정할 발급처가 없다. 다시 넣을 때 이 테스트가
     * 먼저 빨개져서, 경로만 몰래 살아나는 일이 없게 한다.
     */
    @Test
    fun `ID 를 싣는 상세 경로는 아직 목적지가 아니다`() {
        listOf(
            "/afternote/42",
            "/afternote/1",
            "/timeletter/9",
            "/timeletter/1",
        ).forEach { path ->
            assertEquals(
                path,
                rejectedBecause(AppLinkRejectionReason.UNKNOWN_PATH),
                AfternoteAppLinkParser.parse("https://afternote.kro.kr$path"),
            )
        }
    }

    @Test
    fun `미확정 수신자 경로는 목적지로 선언하지 않는다`() {
        listOf(
            "/received",
            "/received/senders/3f2504e0-4f89-41d3-9a0c-0305e82c3301",
            "/received/afternote/7",
        ).forEach { path ->
            assertEquals(
                path,
                rejectedBecause(AppLinkRejectionReason.UNKNOWN_PATH),
                AfternoteAppLinkParser.parse("https://afternote.kro.kr$path"),
            )
        }
    }

    private fun rejectedBecause(reason: AppLinkRejectionReason): AppLinkResolution =
        AppLinkResolution.Rejected(reason, NavigationTarget.Home)
}

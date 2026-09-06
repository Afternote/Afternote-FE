package com.afternote.core.common.deeplink

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * URI 계약 표의 모든 행과 fail-closed 거절 전량을 잠근다 (#924).
 *
 * 표의 «있는 행»만 세면 계약이 넓어지는 것을 못 잡는다. 그래서 거절 쪽도 사유별로 단언한다 —
 * 알 수 없는 path·잘못된 ID·미지원 query 는 각각 다른 이유로 거절돼야 한다.
 */
class AfternoteAppLinkParserTest {
    @Test
    fun `계약 표의 모든 행이 목적지로 해석된다`() {
        val rows =
            mapOf(
                "https://afternote.kro.kr/" to NavigationTarget.Home,
                "https://afternote.kro.kr/afternote" to NavigationTarget.AfternoteHome,
                "https://afternote.kro.kr/afternote/42" to NavigationTarget.AfternoteDetail(42L),
                "https://afternote.kro.kr/received" to NavigationTarget.ReceivedRecordBox,
                "https://afternote.kro.kr/received/senders/$SENDER_ID" to
                    NavigationTarget.ReceivedSenderDetail(SENDER_ID),
                "https://afternote.kro.kr/received/afternote/7" to
                    NavigationTarget.ReceivedAfternoteDetail(7L),
                "https://afternote.kro.kr/timeletter/9" to NavigationTarget.TimeLetterDetail(9L),
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
            AppLinkResolution.Resolved(NavigationTarget.ReceivedRecordBox),
            AfternoteAppLinkParser.parse("  https://afternote.kro.kr/received\n"),
        )
    }

    @Test
    fun `알 수 없는 path 는 거절이다`() {
        listOf(
            "https://afternote.kro.kr/unknown",
            "https://afternote.kro.kr/afternote/42/extra",
            "https://afternote.kro.kr/received/senders",
            "https://afternote.kro.kr/mindrecord",
            "https://afternote.kro.kr/settings",
            "https://afternote.kro.kr/Afternote",
            "https://afternote.kro.kr/afternote/",
            "https://afternote.kro.kr/afternote//42",
            "https://afternote.kro.kr/%2Fafternote",
            "https://afternote.kro.kr/afternote/../settings/notification",
            // 대문자 UUID 는 정규 세그먼트가 아니라 ID 판정에 닿기 전에 걸린다.
            "https://afternote.kro.kr/received/senders/${SENDER_ID.uppercase()}",
        ).forEach { link ->
            assertEquals(link, rejectedBecause(AppLinkRejectionReason.UNKNOWN_PATH), AfternoteAppLinkParser.parse(link))
        }
    }

    @Test
    fun `잘못된 ID 는 거절이다`() {
        listOf(
            "https://afternote.kro.kr/afternote/abc",
            "https://afternote.kro.kr/afternote/0",
            "https://afternote.kro.kr/afternote/007",
            "https://afternote.kro.kr/afternote/99999999999999999999",
            "https://afternote.kro.kr/timeletter/-1",
            "https://afternote.kro.kr/received/afternote/1e3",
            "https://afternote.kro.kr/received/senders/not-a-uuid",
            "https://afternote.kro.kr/received/senders/${SENDER_ID.dropLast(1)}",
            "https://afternote.kro.kr/received/senders/${SENDER_ID.replace('-', '0')}",
        ).forEach { link ->
            assertEquals(link, rejectedBecause(AppLinkRejectionReason.MALFORMED_ID), AfternoteAppLinkParser.parse(link))
        }
    }

    /**
     * 유니코드 숫자는 ID 가 아니다. Android 정규식의 `\d` 는 이것들을 숫자로 물어 서버 JVM 과
     * 다르게 판정하므로, 파서가 `'0'..'9'` 로 직접 좁히는지 여기서 잠근다.
     */
    @Test
    fun `아라비아-인도 숫자는 ID 가 아니다`() {
        assertEquals(
            rejectedBecause(AppLinkRejectionReason.UNKNOWN_PATH),
            AfternoteAppLinkParser.parse("https://afternote.kro.kr/afternote/١٢"),
        )
    }

    @Test
    fun `지원하지 않는 query 는 거절이다`() {
        listOf(
            "https://afternote.kro.kr/afternote?utm_source=mail",
            "https://afternote.kro.kr/afternote/42?ref=push",
            "https://afternote.kro.kr/received?",
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
    fun `다른 host 와 userinfo 위장은 거절이다`() {
        listOf(
            "https://evil.example.com/afternote",
            "https://afternote.kro.kr.evil.example.com/afternote",
            "https://sub.afternote.kro.kr/afternote",
            "https://evil@afternote.kro.kr/afternote",
            "https://afternote.kro.kr:8443/afternote",
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

    private fun rejectedBecause(reason: AppLinkRejectionReason): AppLinkResolution =
        AppLinkResolution.Rejected(reason, NavigationTarget.Home)

    private companion object {
        const val SENDER_ID = "3f2504e0-4f89-41d3-9a0c-0305e82c3301"
    }
}

package com.afternote.core.common.deeplink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 목적지 계약의 불변식을 전량 훑는다 (#924).
 *
 * 목적지를 새로 더할 때 여기서 걸리게 하는 것이 목적이다. [expectedGates] 의 `when` 이 exhaustive라
 * 새 목적지를 만들면 **컴파일이 먼저 막는다** — 관문을 정하지 않은 목적지가 계약에 실릴 수 없고,
 * 그 `when` 을 고치러 오면 바로 옆 [ALL_TARGETS] 도 같이 눈에 든다.
 */
class NavigationTargetContractTest {
    @Test
    fun `표에 적힌 관문 조합 그대로다`() {
        ALL_TARGETS.forEach { target ->
            assertEquals("$target", expectedGates(target), target.requiredGates)
        }
    }

    @Test
    fun `관문은 로그인부터 시작해 오름차순이고 중복이 없다`() {
        ALL_TARGETS.forEach { target ->
            val gates = target.requiredGates

            assertEquals("$target 의 첫 관문", AuthGate.LOGIN, gates.firstOrNull())
            assertEquals("$target 의 관문 중복", gates.size, gates.toSet().size)
            assertEquals("$target 의 관문 순서", gates.sortedBy(AuthGate::ordinal), gates)
        }
    }

    @Test
    fun `정규 경로는 슬래시로 시작하고 끝나지 않는다`() {
        ALL_TARGETS.forEach { target ->
            val path = target.canonicalPath

            assertTrue("$target 의 경로 시작", path.startsWith('/'))
            assertTrue("$target 의 경로 끝", path == "/" || !path.endsWith('/'))
        }
    }

    @Test
    fun `정규 경로는 목적지마다 서로 다르다`() {
        val paths = ALL_TARGETS.map(NavigationTarget::canonicalPath)

        assertEquals(paths.size, paths.toSet().size)
    }

    /** 계약의 왕복 — 표를 통해 나간 URL 은 반드시 같은 목적지로 돌아온다. */
    @Test
    fun `정규 URL 은 같은 목적지로 되돌아온다`() {
        ALL_TARGETS.forEach { target ->
            val url = AfternoteAppLinkParser.canonicalUrl(target)

            assertEquals(url, AppLinkResolution.Resolved(target), AfternoteAppLinkParser.parse(url))
        }
    }

    @Test
    fun `정규 URL 은 확정된 scheme 과 host 를 쓴다`() {
        assertEquals(
            "https://afternote.kro.kr/settings/notification",
            AfternoteAppLinkParser.canonicalUrl(NavigationTarget.NotificationSettings),
        )
    }

    @Test
    fun `리포팅 값은 서로 다르고 비어 있지 않다`() {
        val reportValues = AppLinkRejectionReason.entries.map(AppLinkRejectionReason::reportValue)

        assertEquals(reportValues.size, reportValues.toSet().size)
        assertEquals(emptyList<String>(), reportValues.filter(String::isBlank))
    }

    /** 이슈 #924 경로 표의 «필요한 게이트» 열 원문. 목적지가 늘면 컴파일로 막힌다. */
    private fun expectedGates(target: NavigationTarget): List<AuthGate> =
        when (target) {
            NavigationTarget.Home -> listOf(AuthGate.LOGIN)
            NavigationTarget.AfternoteHome -> listOf(AuthGate.LOGIN, AuthGate.BIOMETRIC)
            is NavigationTarget.AfternoteDetail -> listOf(AuthGate.LOGIN, AuthGate.BIOMETRIC)
            NavigationTarget.ReceivedRecordBox -> listOf(AuthGate.LOGIN)
            is NavigationTarget.ReceivedSenderDetail -> listOf(AuthGate.LOGIN)
            is NavigationTarget.ReceivedAfternoteDetail -> listOf(AuthGate.LOGIN, AuthGate.RECEIVER_IDENTITY)
            is NavigationTarget.TimeLetterDetail -> listOf(AuthGate.LOGIN)
            NavigationTarget.DailyQuestionCompose -> listOf(AuthGate.LOGIN)
            NavigationTarget.NotificationSettings -> listOf(AuthGate.LOGIN)
        }

    private companion object {
        const val SENDER_ID = "3f2504e0-4f89-41d3-9a0c-0305e82c3301"

        /** 목적지 전량. 인자를 갖는 목적지는 대표값 하나로 센다. */
        val ALL_TARGETS: List<NavigationTarget> =
            listOf(
                NavigationTarget.Home,
                NavigationTarget.AfternoteHome,
                NavigationTarget.AfternoteDetail(42L),
                NavigationTarget.ReceivedRecordBox,
                NavigationTarget.ReceivedSenderDetail(SENDER_ID),
                NavigationTarget.ReceivedAfternoteDetail(7L),
                NavigationTarget.TimeLetterDetail(9L),
                NavigationTarget.DailyQuestionCompose,
                NavigationTarget.NotificationSettings,
            )
    }
}

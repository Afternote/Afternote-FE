package com.afternote.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 시스템 다크모드를 따라가지 않는다는 계약 (#1719).
 *
 * 종전 기본값은 `isSystemInDarkTheme()` 이었는데, 다크 팔레트를 물려도 **화면이 따라오지
 * 않았다** — 색을 토큰이 아니라 코드에 박은 자리가 반전되지 않아, 「마음의 기록」 헤더가
 * 검정 배경에 검정 글자로 남고 주간 요약 카드는 배경만 밝은 채로 라벨이 흐려졌다.
 * 「배선은 됐고 화면은 안 맞은」 중간 상태가 라이트 고정보다도, 완전한 다크 지원보다도
 * 나빴다. 그래서 팔레트가 확정될 때까지 라이트로 잠근다.
 *
 * 이 테스트가 지키는 것은 **잠금이 조용히 풀리지 않는 것**이다. 기본값을
 * `isSystemInDarkTheme()` 으로 되돌리면 첫 번째 테스트가 night 한정자에서 실패한다.
 * 되돌릴 때는 하드코딩 색 29곳을 토큰으로 옮긴 뒤 이 테스트도 같이 고쳐야 한다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ThemeDarkModeLockTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun resolveIsLightMode(theme: @Composable (@Composable () -> Unit) -> Unit): Boolean {
        var isLightMode: Boolean? = null
        composeRule.setContent {
            theme { isLightMode = AfternoteDesign.colors.isLightMode }
        }
        composeRule.waitForIdle()
        return requireNotNull(isLightMode) { "테마가 content 를 부르지 않았다" }
    }

    @Test
    @Config(qualifiers = "night")
    fun `시스템이 다크여도 기본값은 라이트 팔레트다`() {
        assertEquals(true, resolveIsLightMode { content -> AfternoteTheme(content = content) })
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `시스템이 라이트일 때도 당연히 라이트 팔레트다`() {
        assertEquals(true, resolveIsLightMode { content -> AfternoteTheme(content = content) })
    }

    /**
     * 잠금은 **기본값** 에만 걸린다. 다크 팔레트와 파라미터는 팔레트 확정 뒤를 위해 살아 있어야
     * 하고, 그때 이 값을 다시 시스템에 묶는 것으로 되돌린다.
     */
    @Test
    @Config(qualifiers = "notnight")
    fun `명시로 넘기면 다크 팔레트를 여전히 그릴 수 있다`() {
        assertEquals(
            false,
            resolveIsLightMode { content -> AfternoteTheme(isDarkTheme = true, content = content) },
        )
    }
}

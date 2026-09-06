package com.afternote.konsist

import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

/**
 * 시스템바가 라이트로 잠긴 채 남는지 (#1719).
 *
 * `AfternoteTheme` 이 라이트로 잠긴 동안 Compose 는 다크 기기에서도 `gray1`(#FAFAFA) 을
 * 상태바 밑까지 그린다. 무인자 `enableEdgeToEdge()` 는 `SystemBarStyle.auto` 라 그 기기에서
 * 흰 아이콘을 고르므로, 밝은 배경 위에 흰 시계·배터리가 얹혀 사라진다.
 *
 * 그래서 `app` 의 호출부는 [com.afternote.afternote_fe.enableLightEdgeToEdge] 하나로 모았다.
 * 이 가드는 **무인자 호출이 다시 스며드는 것**을 막는다 — 새 Activity 를 만들 때 손이 먼저
 * 기억하는 형태가 그쪽이라서다.
 *
 * 다크 팔레트가 확정돼 잠금을 풀 때는 이 가드도 함께 지운다. 되돌릴 자리는 셋이다 —
 * `AfternoteTheme` 의 `isDarkTheme` 기본값 · `enableLightEdgeToEdge()` · `themes.xml` 의
 * `windowLightStatusBar`.
 */
class LightSystemBarLockKonsistTest {
    @Test
    fun `app 은 무인자 enableEdgeToEdge 를 쓰지 않는다`() {
        AfternoteKonsistScope
            .files
            .filter { file -> file.path.contains("/app/src/") }
            .filterNot { file -> file.path.endsWith("/$HELPER_FILE") }
            .assertFalse { file -> BARE_CALL.containsMatchIn(file.text) }
    }

    private companion object {
        /** 잠금을 실제로 거는 자리. 여기만 `enableEdgeToEdge` 를 인자와 함께 부른다. */
        const val HELPER_FILE = "LightEdgeToEdge.kt"

        /** `enableEdgeToEdge()` — 인자 없는 호출. 공백만 허용한다. */
        val BARE_CALL = Regex("""\benableEdgeToEdge\s*\(\s*\)""")
    }
}

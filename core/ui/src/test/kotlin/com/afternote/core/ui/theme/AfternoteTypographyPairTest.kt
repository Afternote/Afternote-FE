package com.afternote.core.ui.theme

import androidx.compose.ui.text.TextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `(B)`/`(R)` 로 갈린 타이포 토큰 쌍의 현재 관계를 잠근다.
 *
 * `#1815` 로 여섯 스타일의 웨이트가 시안대로 Regular 이 되면서 두 쌍이 **완전히 같아졌다.**
 * 값이 같은 채로 이름만 둘인 상태는 #1862 에서 디자이너 확인 뒤 정리하는데, 그때까지
 * **한쪽만 조용히 고쳐 두 자리가 갈라지는 것**을 막는 것이 이 테스트의 목적이다.
 *
 * 그러니 이 테스트가 깨졌다면 되돌리기 전에 어느 쪽인지부터 가른다.
 * - 시안이 두 스타일을 실제로 갈랐다면 → #1862 에서 판정하고 이 테스트를 갱신한다.
 * - 의도치 않게 한쪽만 건드렸다면 → 그 변경을 되돌린다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteTypographyPairTest {
    private val typography = AfternoteTypography()

    /** 리뷰가 「같다」를 판정한 여섯 필드. 나머지 필드는 두 쌍 모두 기본값이다. */
    private fun TextStyle.comparedFields() =
        listOf(
            "fontFamily" to fontFamily,
            "fontWeight" to fontWeight,
            "fontSize" to fontSize,
            "lineHeight" to lineHeight,
            "lineBreak" to lineBreak,
            "localeList" to localeList,
        )

    private fun assertSameStyle(
        name: String,
        b: TextStyle,
        r: TextStyle,
    ) {
        b.comparedFields().zip(r.comparedFields()).forEach { (bField, rField) ->
            assertEquals("$name 의 ${bField.first} 가 갈렸다 — #1862 참조", rField.second, bField.second)
        }
    }

    @Test
    fun `bodySmall 은 B 와 R 이 완전히 같다 - 갈리면 1862 에서 판정한다`() {
        assertSameStyle("bodySmall", typography.bodySmallB, typography.bodySmallR)
    }

    @Test
    fun `captionLarge 는 B 와 R 이 완전히 같다 - 갈리면 1862 에서 판정한다`() {
        assertSameStyle("captionLarge", typography.captionLargeB, typography.captionLargeR)
    }

    @Test
    fun `bodyLarge 는 B 와 R 의 줄높이가 다르다 - 이 쌍은 합치면 안 된다`() {
        assertNotEquals(
            "bodyLargeB 와 bodyLargeR 의 lineHeight 가 같아졌다 — 통합 대상이 아닌 쌍이다",
            typography.bodyLargeR.lineHeight,
            typography.bodyLargeB.lineHeight,
        )
    }

    @Test
    fun `여섯 스타일 어디에도 Bold 가 남지 않았다`() {
        val bolded =
            listOf(
                "h2" to typography.h2,
                "h3" to typography.h3,
                "bodyLargeB" to typography.bodyLargeB,
                "bodySmallB" to typography.bodySmallB,
                "primaryButton" to typography.primaryButton,
                "captionLargeB" to typography.captionLargeB,
            ).filter { (_, style) -> style.fontWeight?.weight?.let { it > 400 } == true }
                .map { (name, style) -> "$name(${style.fontWeight?.weight})" }

        assertEquals("나눔바른고딕 Bold 는 본문 크기대에서 획이 불균일하다 — #1815", emptyList<String>(), bolded)
    }
}

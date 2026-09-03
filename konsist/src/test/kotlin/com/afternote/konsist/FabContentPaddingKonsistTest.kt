package com.afternote.konsist

import org.junit.Test

/**
 * FAB 이 가리는 자리를 목록이 스스로 비우는지 (#1713).
 *
 * `Scaffold` 는 FAB 을 콘텐츠 **위에** 띄우고 자리를 예약해 주지 않는다. 목록이
 * `contentPadding` 을 두지 않으면 마지막 항목이 FAB 뒤로 들어가고, 스크롤이 없을 만큼
 * 항목이 적으면 사용자가 밀어낼 방법조차 없다.
 *
 * 상수값만 단언하는 가드는 **적용 지점을 지워도 통과한다** — 값은 그대로이기 때문이다
 * (#1713 리뷰). 그래서 FAB 을 띄우는 목록 화면이 그 상수를 실제로 쓰고 있는지를 자리마다
 * 센다. 레이아웃으로 재는 편이 이상적이지만 Robolectric 에서 LazyColumn 끝까지 스크롤하는
 * 것이 불안정해, 값이 아니라 **자리**를 고정하는 쪽을 택했다.
 *
 * 화면이 늘거나 목록이 하나 더 생기면 [EXPECTED] 를 함께 고친다.
 */
class FabContentPaddingKonsistTest {
    @Test
    fun `FAB 을 띄우는 목록 화면은 하단 여백을 예약한다`() {
        val actual =
            EXPECTED.keys.associateWith { fileName ->
                val file =
                    AfternoteKonsistScope.files.singleOrNull { it.path.endsWith("/$fileName") }
                        ?: error("$fileName 을 찾지 못했다 — 파일을 옮겼으면 이 가드도 함께 고칠 것 (#1713).")
                CONTENT_PADDING.findAll(file.text).count()
            }

        check(actual == EXPECTED) {
            buildString {
                appendLine("FAB 하단 여백을 두는 자리가 달라졌다 (#1713).")
                appendLine("  기대: $EXPECTED")
                appendLine("  실제: $actual")
                appendLine()
                appendLine("줄었다면 그 목록의 마지막 항목이 FAB 뒤로 들어간다.")
                appendLine("의도한 변경이면 EXPECTED 를 함께 고칠 것.")
            }
        }
    }

    private companion object {
        /** `contentPadding = PaddingValues(bottom = AfternoteFabContentBottomPadding)` 한 줄. */
        val CONTENT_PADDING =
            Regex("""contentPadding\s*=\s*PaddingValues\(\s*bottom\s*=\s*AfternoteFabContentBottomPadding\s*,?\s*\)""")

        /** 파일별 적용 지점 수. 일기 화면은 목록형·캘린더형 둘이라 2 다. */
        val EXPECTED =
            mapOf(
                "DiaryScreen.kt" to 2,
                "DailyQuestionAnswerListScreen.kt" to 1,
            )
    }
}

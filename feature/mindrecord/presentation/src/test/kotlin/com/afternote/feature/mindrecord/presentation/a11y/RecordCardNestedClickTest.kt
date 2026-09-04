package com.afternote.feature.mindrecord.presentation.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.testing.assertAccessibleClickTargets
import com.afternote.core.ui.testing.scanEnabledClickTargets
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionListCard
import com.afternote.feature.mindrecord.presentation.component.DiaryCard
import com.afternote.feature.mindrecord.presentation.component.DiaryComponent
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * 기록 카드 3종의 «더보기 메뉴» 중첩 클릭 계약 (#1669).
 *
 * ### 판정: 카드를 재구성하지 않고 스캐너 예외를 넓혔다
 *
 * #1179 스캔이 세 카드에서 같은 형태를 잡았다 — 카드 전체가 상세로 가는 `clickable`(#759)
 * 안에, 20dp «더보기 메뉴» 가 또 `clickable` 이다.
 *
 * 이 형태는 결함이 아니다. 「눌러서 이동하는 목록 행 + 그 끝의 오버플로 메뉴」는 Android
 * 목록의 정본이고(Material3 `ListItem` 의 `trailingContent`), 안쪽 `clickable` 이
 * `mergeDescendants` 로 제 병합 경계를 세우기 때문에 TalkBack 이 카드와 버튼을 각각 별개로
 * 짚는다. 메뉴를 카드의 custom accessibility action 으로 옮기는 재구성(안 b)은 **눈으로 보는
 * 사용자에게서 아이콘을 뺏는** 대가를 치른다.
 *
 * 그래서 `core:ui` 스캐너에 예외를 뒀다. 다만 「끝단 버튼」 같은 넓은 이름이면 다음 사람이
 * 아무 데나 붙이므로, 예외가 통과시키는 형태를 두 축으로 좁혔다 (`isTrailingAccessoryOf`) —
 * **끝단**(중심이 뒷절반) · **보조 크기**(너비 1/4 · 높이 1/2 이하). 여기에 호출부가 이름과
 * `Role.Button` 을 더한다. 컨테이너 **모양도 세로 위치도 보지 않는다** — 둘 다 같은 형태를
 * 형상 때문에 갈랐다. 반씩 나눠 가진 영역·세로로 긴 띠·Role 없는 아이콘은 그대로 위반으로
 * 남는다. 그 경계는 `TouchTargetAssertionsTest` 가 지킨다.
 *
 * 이 파일은 **세 카드가 그 예외 안에 실제로 들어오는지**를 지킨다. 카드가 세로로 길어지거나
 * 메뉴가 커지거나 위치가 앞쪽으로 옮겨지면 예외를 벗어나 다시 위반이 된다.
 */
private const val QUESTION_TITLE = "오늘 가장 고마웠던 사람은"
private const val DIARY_TITLE = "9월의 첫 기록"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
class RecordCardNestedClickTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var slot: MutableState<RecordCard>? = null

    private enum class RecordCard(
        val label: String,
        /** 카드 본문에서 «메뉴가 아닌 자리» 를 짚기 위한 텍스트. */
        val bodyText: String,
    ) {
        DailyQuestionListCard("데일리질문 카드", QUESTION_TITLE),
        DiaryCard("일기 카드", DIARY_TITLE),
        DiaryComponent("일기 컴포넌트", DIARY_TITLE),

        /** 프로덕션 형상 — 2열 staggered grid 의 열 폭에 이미지가 붙어 세로가 폭을 넘는다. */
        DiaryCardInGrid("일기 카드(그리드 폭 + 이미지)", DIARY_TITLE),
    }

    private var cardClicks = 0

    @Test
    fun `기록 카드 3종은 클릭 타깃 계약을 전부 지킨다`() {
        RecordCard.entries.forEach { card ->
            render(card)
            runCatching { composeRule.assertAccessibleClickTargets() }
                .onFailure { failure ->
                    throw AssertionError("${card.label}: ${failure.message}", failure)
                }
        }
    }

    /**
     * 카드가 두 타깃을 **각각** 내놓는지 — 카드 본체와 메뉴다.
     *
     * 위 테스트만으로는 메뉴가 통째로 사라져도 통과한다. 중첩을 «없애서» 계약을 지키는 것과
     * «예외로 인정받아» 지키는 것은 다르므로, 둘이 모두 살아 있음을 함께 못박는다.
     */
    @Test
    fun `카드 본체와 더보기 메뉴가 각각 짚인다`() {
        RecordCard.entries.forEach { card ->
            render(card)
            val targets = composeRule.scanEnabledClickTargets()

            val menus = targets.filter { it.name == MORE_MENU_NAME }
            assertEquals("${card.label}: 더보기 메뉴가 하나가 아니다: ${targets.map { it.name }}", 1, menus.size)
            assertTrue(
                "${card.label}: 카드 본체 타깃이 없다: ${targets.map { it.name }}",
                targets.any { it.name != MORE_MENU_NAME },
            )
            // 메뉴는 카드 안에 있으므로 조상이 실재한다 — 예외로 통과한 것이지 중첩이 없는 게 아니다.
            assertTrue(
                "${card.label}: 더보기 메뉴가 카드보다 크다",
                menus.single().layoutWidth < targets.first { it.name != MORE_MENU_NAME }.layoutWidth,
            )
        }
    }

    /**
     * 예외의 근거는 «안쪽이 탭을 소비해 이중 발화가 없다» 는 런타임 사실인데, 스캐너가 이
     * 중첩에 영구히 눈을 감으므로 그 사실을 지키는 트립와이어가 따로 없었다 (#1669 리뷰).
     *
     * 세 카드를 모아 한 번에 보고한다 — `forEach` 안에서 바로 throw 하면 두 번째·세 번째
     * 카드의 결과가 가려진다.
     */
    @Test
    fun `더보기 메뉴를 눌러도 카드 본문 이동은 일어나지 않는다`() {
        val violations = mutableListOf<String>()
        RecordCard.entries.forEach { card ->
            cardClicks = 0
            render(card)

            composeRule.onNodeWithContentDescription(MORE_MENU_NAME).performClick()
            composeRule.waitForIdle()
            if (cardClicks != 0) violations += "${card.label}: 메뉴 탭이 카드 onClick 을 $cardClicks 회 불렀다"

            composeRule.onNodeWithText(card.bodyText).performClick()
            composeRule.waitForIdle()
            if (cardClicks != 1) violations += "${card.label}: 본문 탭이 카드 onClick 을 $cardClicks 회 불렀다"
        }

        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    private fun render(card: RecordCard) {
        val existing = slot
        if (existing == null) {
            val created = mutableStateOf(card)
            slot = created
            composeRule.setContent {
                AfternoteTheme {
                    when (created.value) {
                        RecordCard.DailyQuestionListCard -> {
                            // 메뉴 항목 콜백은 이 테스트의 관심사가 아니다 — 다만 «더보기 메뉴»
                            // 자체가 그려져야 중첩 클릭을 볼 수 있으므로 no-op 이라도 넘긴다.
                            DailyQuestionListCard(
                                answer = SAMPLE_QUESTION,
                                onClick = { cardClicks++ },
                                onEdit = {},
                                onDelete = {},
                            )
                        }

                        RecordCard.DiaryCard -> {
                            DiaryCard(
                                diary = SAMPLE_DIARY,
                                onClick = { cardClicks++ },
                                onEdit = {},
                                onDelete = {},
                            )
                        }

                        RecordCard.DiaryComponent -> {
                            DiaryComponent(
                                diary = SAMPLE_DIARY,
                                onClick = { cardClicks++ },
                                onEdit = {},
                                onDelete = {},
                            )
                        }

                        RecordCard.DiaryCardInGrid -> {
                            DiaryCard(
                                diary = SAMPLE_DIARY_WITH_IMAGE,
                                modifier = Modifier.width(GRID_COLUMN_WIDTH),
                                onClick = { cardClicks++ },
                                onEdit = {},
                                onDelete = {},
                            )
                        }
                    }
                }
            }
        } else {
            composeRule.runOnIdle { existing.value = card }
        }
        composeRule.waitForIdle()
    }

    private companion object {
        /** `mindrecord_more_menu_cd` 의 값. 세 카드가 같은 문자열을 쓴다. */
        const val MORE_MENU_NAME = "더보기 메뉴"

        val SAMPLE_DATE: LocalDate = LocalDate.of(2026, 9, 2)

        val SAMPLE_QUESTION =
            DailyQuestion(
                id = 1L,
                title = QUESTION_TITLE,
                date = SAMPLE_DATE,
                content = "먼저 연락해 준 친구에게 고마웠다.",
            )

        val SAMPLE_DIARY =
            DailyDiary(
                id = 2L,
                title = DIARY_TITLE,
                date = SAMPLE_DATE,
                content = "오늘은 오래 걸었다.",
            )

        /** `DiaryScreen` 의 `LazyVerticalStaggeredGrid(Fixed(2))` 열 폭 상한. */
        val GRID_COLUMN_WIDTH = 176.dp

        val SAMPLE_DIARY_WITH_IMAGE = SAMPLE_DIARY.copy(imageUrl = "https://cdn.example.net/a.png")
    }
}

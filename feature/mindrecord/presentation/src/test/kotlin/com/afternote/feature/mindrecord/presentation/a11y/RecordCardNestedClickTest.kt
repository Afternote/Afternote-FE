package com.afternote.feature.mindrecord.presentation.a11y

import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
 * 목록의 정본이고(Material3 `ListItem` 의 `trailingContent`), TalkBack 은 unmerged tree 에서
 * 행과 버튼을 각각 별개 노드로 짚어 준다. 메뉴를 카드의 custom accessibility action 으로
 * 옮기는 재구성(안 b)은 **눈으로 보는 사용자에게서 아이콘을 뺏는** 대가를 치른다.
 *
 * 그래서 `core:ui` 스캐너에 예외를 뒀다. 다만 「끝단 버튼」 같은 넓은 이름이면 다음 사람이
 * 아무 데나 붙이므로, 예외가 통과시키는 형태를 세 축으로 좁혔다 (`isTrailingAccessoryOf`) —
 * **행 모양**(가로 > 세로) · **보조 크기**(행 너비의 1/4 이하) · **끝단 위치**(중심이 뒷절반).
 * 여기에 호출부가 이름과 Role 을 더한다. 반씩 나눠 가진 두 클릭 영역이나 정사각 상자 안의
 * 정사각 버튼은 그대로 위반으로 남는다 — 그 경계는 `TouchTargetAssertionsTest` 가 지킨다.
 *
 * 이 파일은 **세 카드가 그 예외 안에 실제로 들어오는지**를 지킨다. 카드가 세로로 길어지거나
 * 메뉴가 커지거나 위치가 앞쪽으로 옮겨지면 예외를 벗어나 다시 위반이 된다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
class RecordCardNestedClickTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var slot: MutableState<RecordCard>? = null

    private enum class RecordCard(
        val label: String,
    ) {
        DailyQuestionListCard("데일리질문 카드"),
        DiaryCard("일기 카드"),
        DiaryComponent("일기 컴포넌트"),
    }

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

    private fun render(card: RecordCard) {
        val existing = slot
        if (existing == null) {
            val created = mutableStateOf(card)
            slot = created
            composeRule.setContent {
                AfternoteTheme {
                    when (created.value) {
                        RecordCard.DailyQuestionListCard -> DailyQuestionListCard(answer = SAMPLE_QUESTION)
                        RecordCard.DiaryCard -> DiaryCard(diary = SAMPLE_DIARY)
                        RecordCard.DiaryComponent -> DiaryComponent(diary = SAMPLE_DIARY)
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
                title = "오늘 가장 고마웠던 사람은",
                date = SAMPLE_DATE,
                content = "먼저 연락해 준 친구에게 고마웠다.",
            )

        val SAMPLE_DIARY =
            DailyDiary(
                id = 2L,
                title = "9월의 첫 기록",
                date = SAMPLE_DATE,
                content = "오늘은 오래 걸었다.",
            )
    }
}

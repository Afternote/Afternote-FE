package com.afternote.feature.mindrecord.presentation.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.testing.EnabledClickTarget
import com.afternote.core.ui.testing.MinimumTouchTargetSize
import com.afternote.core.ui.testing.scanEnabledClickTargets
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.BottomToolbar
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionListCard
import com.afternote.feature.mindrecord.presentation.component.DiaryCard
import com.afternote.feature.mindrecord.presentation.component.DiaryComponent
import com.afternote.feature.mindrecord.presentation.component.MemoriesCard
import com.afternote.feature.mindrecord.presentation.component.ReceiverMindRecordTopBar
import com.afternote.feature.mindrecord.presentation.component.TextStyleToolbar
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import com.afternote.feature.mindrecord.presentation.model.TextStyleState
import com.afternote.feature.mindrecord.presentation.screen.receiver.ReceiverMindRecordFilterSheet
import com.afternote.feature.mindrecord.presentation.screen.sender.DiaryWriteScreenContent
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryWriteUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.ReceiverMindRecordFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * 작성 툴바의 클릭 타깃 접근성 계약 (#1179).
 *
 * ### 이슈의 전제가 실측과 달랐다
 *
 * #1179 는 「48dp 미만 11곳」을 후보로 들고 스캐너 실측 뒤 착수하라고 적었다. 재 보니
 * **크기 미달은 0건**이다 — `layout` 은 20~24dp 라도 시스템 최소 터치 타깃 확장이 붙어
 * `touch` 는 전부 48dp 이상이다. 그래서 `minimumInteractiveComponentSize` 를 덧붙이지 않았다.
 * 붙였다면 레이아웃 footprint 만 48dp 로 밀려 시안과 어긋났을 것이다.
 *
 * 대신 같은 스캐너가 **다른 계약 위반**을 드러냈고 그쪽을 고쳤다.
 *
 * | 위반 | 건수 | 처방 |
 * |---|---|---|
 * | 접근 가능한 이름 누락 | 3 | 정렬 아이콘에 `contentDescription`(이미 있던 문자열) |
 * | Role 누락 | 16 | 버튼은 `Role.Button`, 정렬·서식 스타일은 선택 semantics |
 * | 중첩 클릭 | 3 | **이 PR 범위 밖** — 아래 참조 |
 *
 * ### 정렬과 서식은 semantics 가 다르다
 *
 * 종전에는 둘 다 맨 `clickable` 이라 역할도 상태도 안 실렸다. 정렬은 «셋 중 하나»(`selectable`
 * + `Role.RadioButton` + `selectableGroup`), 서식 B·I·U·S 는 «각각 켜고 끄기»(`toggleable` +
 * `Role.Checkbox`) 다. 스크린리더가 두 묶음을 다르게 읽어야 한다.
 *
 * ### 중첩 클릭 3건을 여기서 안 본 이유
 *
 * 카드 전체가 상세로 가는 `clickable` 인데(#759) 그 안의 «더보기 메뉴» 도 `clickable` 이라
 * 스캐너가 중첩으로 잡는다. 「행 + 끝단 액션」은 흔한 형태이고 스캐너도 텍스트필드 끝단
 * 버튼에는 예외를 두고 있어, **공용 스캐너의 예외를 넓힐지 카드를 재구성할지는 이 PR 이
 * 혼자 정할 문제가 아니다.** 별도 이슈로 남겼다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
class WriteToolbarAccessibilityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var alignSlot: MutableState<AlignedToolbar>? = null
    private var candidateSlot: MutableState<OtherCandidate>? = null

    @Test
    fun `작성 툴바의 모든 클릭 타깃은 48dp 이상이고 이름과 역할을 갖는다`() {
        renderToolbars()

        val targets = composeRule.scanEnabledClickTargets()
        assertTrue("클릭 타깃을 하나도 못 찾았다", targets.isNotEmpty())

        val tooSmall = targets.filter { it.width < MinimumTouchTargetSize || it.height < MinimumTouchTargetSize }
        val unnamed = targets.filter { it.name.isBlank() }
        val roleless = targets.filter { it.role == null }

        assertTrue("48dp 미달: ${tooSmall.describe()}", tooSmall.isEmpty())
        assertTrue("이름 없음: ${unnamed.describe()}", unnamed.isEmpty())
        assertTrue("Role 없음: ${roleless.describe()}", roleless.isEmpty())
    }

    /**
     * 정렬 그룹은 **두 곳**에 있다 — 늘 보이는 하단 바와, 키보드 위 스타일 패널이다. 종전에는
     * 하단 바 쪽만 맨 `IconButton` 이라 선택 상태가 안 실렸는데, 작성 화면에서 실제로 늘 보이는
     * 건 그쪽이다. 그래서 한 툴바만 보지 않고 둘을 각각 렌더해 같은 계약을 건다 (#1179 리뷰).
     */
    @Test
    fun `정렬은 두 툴바 모두에서 단일 선택이다`() {
        AlignedToolbar.entries.forEach { toolbar ->
            renderAligned(toolbar, TextAlign.Center)

            val aligns = composeRule.scanEnabledClickTargets().filter { it.name in ALIGN_NAMES }
            assertTrue("${toolbar.label}: 정렬 3개를 못 찾았다: ${aligns.describe()}", aligns.size == 3)
            assertTrue("${toolbar.label}: 단일 선택이 아니다: ${aligns.describe()}", aligns.all { it.role == Role.RadioButton })

            val selected = aligns.filter { it.selected == true }
            assertTrue("${toolbar.label}: 선택된 정렬이 정확히 하나가 아니다: ${aligns.describe()}", selected.size == 1)
            // 「하나가 켜져 있다」로는 부족하다 — 넘긴 값과 켜진 항목이 어긋나도 통과하기 때문이다.
            assertEquals("${toolbar.label}: 넘긴 정렬과 켜진 항목이 다르다", "가운데 정렬", selected.single().name)
        }
    }

    /**
     * 작성 화면 **첫 상태**. richeditor 의 기본 문단은 `textAlign` 이 `Unspecified` 라, 정규화가
     * 없으면 정렬 3종의 `selected` 가 모두 false 가 된다 — 스크린리더가 「선택 안 됨」을 셋 다
     * 읽어 «셋 중 하나» 계약이 처음부터 깨진다. 위 테스트는 `TextAlign.Center` 를 명시해서
     * 이 자리를 못 잡는다 (#1179 리뷰).
     */
    @Test
    fun `정렬을 고른 적 없어도 왼쪽 하나가 켜져 있다`() {
        AlignedToolbar.entries.forEach { toolbar ->
            renderAligned(toolbar, TextAlign.Unspecified)

            val aligns = composeRule.scanEnabledClickTargets().filter { it.name in ALIGN_NAMES }
            val selected = aligns.filter { it.selected == true }
            assertEquals("${toolbar.label}: 첫 상태에서 켜진 정렬이 정확히 하나가 아니다: ${aligns.describe()}", 1, selected.size)
            assertEquals("${toolbar.label}: 첫 상태의 기본 정렬이 왼쪽이 아니다", "왼쪽 정렬", selected.single().name)
        }
    }

    @Test
    fun `서식은 개별 토글이다`() {
        renderStyleToolbarOnly()
        val targets = composeRule.scanEnabledClickTargets()

        // 서식 넷 — 서로 독립인 토글이라 «선택» 이 아니라 «켜짐/꺼짐» 이다.
        val styles = targets.filter { it.name in setOf("굵게", "기울임", "밑줄", "취소선") }
        assertTrue("서식 4개를 못 찾았다: ${targets.map { it.name }}", styles.size == 4)
        assertTrue("서식이 토글이 아니다: ${styles.describe()}", styles.all { it.role == Role.Checkbox })
        assertTrue("서식에 토글 상태가 없다: ${styles.describe()}", styles.all { it.toggleableState != null })
    }

    /**
     * 기호 하나(「T」)나 숫자 하나(「1」)가 그대로 이름이 되던 타깃들.
     *
     * 「비어 있지 않다」만 보면 이런 이름도 통과한다 — 낭독하면 「T, 버튼」·「1, 버튼」 이라
     * 무엇을 하는 버튼인지 알 수 없다. 그래서 **정확한 이름**을 건다 (#1179 리뷰).
     */
    @Test
    fun `기호로만 읽히던 타깃이 목적을 말한다`() {
        renderBottomToolbarOnly(draftCount = 3)
        val names = composeRule.scanEnabledClickTargets().map { it.name }

        assertTrue("텍스트 설정 진입 버튼이 「T」로만 읽힌다: $names", "텍스트 설정" in names)
        assertTrue("임시저장 수량이 숫자로만 읽힌다: $names", "임시저장 목록 3개" in names)
        assertTrue("이름이 기호 하나로 남은 타깃이 있다: $names", names.none { it == "T" || it == "3" })
    }

    /** 수량을 모를 때(조회 중·실패)도 「–, 버튼」 대신 그 사실을 말해야 한다. */
    @Test
    fun `임시저장 수량을 모를 때도 목적을 말한다`() {
        renderBottomToolbarOnly(draftCount = null)
        val names = composeRule.scanEnabledClickTargets().map { it.name }

        assertTrue("수량 미확인이 「–」로만 읽힌다: $names", "임시저장 목록, 수량 미확인" in names)
    }

    /**
     * #1179 가 든 후보 중 **툴바 밖** 것들. 두 툴바만 보면 나머지의 48dp 결론이 CI 에서
     * 재현되지 않는다 (#1179 리뷰).
     *
     * 여기서 실제로 위반이 여럿 나왔고 함께 고쳤다 — 수신자 상단바의 필터 타깃은 자식이 없어
     * **이름이 통째로 비어** 있었고, 필터 시트의 정렬 칩은 색만 바뀔 뿐 선택 상태가 semantics 에
     * 없었다. `DiaryWriteScreenContent` 까지 넓힌 뒤에는 본문 편집기의 이름 누락·수신자 행의
     * Role 누락·기분 칩 3종의 역할과 선택 상태 누락이 더 나왔다. 스캐너를 안 돌렸으면 전부
     * 안 보였다.
     */
    @Test
    fun `툴바 밖 후보도 48dp 이상이고 이름과 역할을 갖는다`() {
        OtherCandidate.entries.forEach { candidate ->
            renderCandidate(candidate)
            val targets = composeRule.scanEnabledClickTargets().filterNot { it.name in FRAMEWORK_OWNED_NAMES }
            assertTrue("${candidate.label}: 클릭 타깃을 하나도 못 찾았다", targets.isNotEmpty())

            val tooSmall = targets.filter { it.width < MinimumTouchTargetSize || it.height < MinimumTouchTargetSize }
            val unnamed = targets.filter { it.name.isBlank() }
            // 텍스트 입력은 역할 대신 편집 semantics 로 읽히므로 공용 스캐너와 같은 기준으로 뺀다.
            val roleless = targets.filter { it.role == null && !it.isEditableText && it.name !in SLOT_OWNED_NAMES }

            assertTrue("${candidate.label} 48dp 미달: ${tooSmall.describe()}", tooSmall.isEmpty())
            assertTrue("${candidate.label} 이름 없음: ${unnamed.describe()}", unnamed.isEmpty())
            assertTrue("${candidate.label} Role 없음: ${roleless.describe()}", roleless.isEmpty())
        }
    }

    private enum class AlignedToolbar(
        val label: String,
    ) {
        Bottom("하단 바"),
        StylePanel("스타일 패널"),
    }

    /** #1179 후보 중 툴바 밖에 있어 이 PR 이 새로 렌더하는 것들. */
    private enum class OtherCandidate(
        val label: String,
    ) {
        DailyQuestionListCard("데일리질문 카드"),
        DiaryCard("일기 카드"),
        DiaryComponent("일기 컴포넌트"),
        ReceiverMindRecordTopBar("수신자 상단바"),
        MemoriesCard("MEMORIES 카드"),
        ReceiverMindRecordFilterSheet("수신자 필터 시트"),
        DiaryWriteScreen("일기 작성 화면"),
    }

    /**
     * `setContent` 는 rule 당 한 번만 부를 수 있다. 후보를 여럿 도는 테스트는 **상태를 바꿔**
     * 같은 트리에서 갈아 끼운다 — 후보마다 rule 을 새로 만들면 한 테스트에 담을 수 없다.
     */
    private fun renderAligned(
        toolbar: AlignedToolbar,
        textAlign: TextAlign,
    ) {
        val slot = alignSlot
        if (slot == null) {
            val created = mutableStateOf(toolbar)
            alignSlot = created
            composeRule.setContent {
                AfternoteTheme {
                    when (created.value) {
                        AlignedToolbar.Bottom -> {
                            BottomToolbar(
                                onTextStyleClick = {},
                                onAlignChange = {},
                                onDraftCountClick = {},
                                onSaveDraftClick = {},
                                onLinkClick = {},
                                textAlign = textAlign,
                            )
                        }

                        AlignedToolbar.StylePanel -> {
                            TextStyleToolbar(
                                onClose = {},
                                onTypeClick = {},
                                onLinkClick = {},
                                onBoldClick = {},
                                onItalicClick = {},
                                onUnderlineClick = {},
                                onStrikethroughClick = {},
                                onAlignChange = {},
                                onTextStyleChange = {},
                                styleState = TextStyleState(textAlign = textAlign),
                            )
                        }
                    }
                }
            }
        } else {
            composeRule.runOnIdle { slot.value = toolbar }
        }
        composeRule.waitForIdle()
    }

    private fun renderBottomToolbarOnly(draftCount: Int?) {
        composeRule.setContent {
            AfternoteTheme {
                BottomToolbar(
                    onTextStyleClick = {},
                    onAlignChange = {},
                    onDraftCountClick = {},
                    onSaveDraftClick = {},
                    onLinkClick = {},
                    draftCount = draftCount,
                )
            }
        }
    }

    private fun renderCandidate(candidate: OtherCandidate) {
        val slot = candidateSlot
        if (slot == null) {
            val created = mutableStateOf(candidate)
            candidateSlot = created
            composeRule.setContent {
                AfternoteTheme {
                    when (created.value) {
                        OtherCandidate.DailyQuestionListCard -> {
                            DailyQuestionListCard(
                                answer = SAMPLE_QUESTION,
                                onClick = {},
                                onEdit = {},
                                onDelete = {},
                            )
                        }

                        OtherCandidate.DiaryCard -> {
                            DiaryCard(diary = SAMPLE_DIARY, onClick = {}, onEdit = {}, onDelete = {})
                        }

                        OtherCandidate.DiaryComponent -> {
                            DiaryComponent(diary = SAMPLE_DIARY, onClick = {}, onEdit = {}, onDelete = {})
                        }

                        OtherCandidate.ReceiverMindRecordTopBar -> {
                            ReceiverMindRecordTopBar(filter = ReceiverMindRecordFilter(), onFilterClick = {})
                        }

                        OtherCandidate.MemoriesCard -> {
                            // 다시 읽기 CTA 가 있는 판을 그린다 — null 이면 그 타깃 자체가 없어
                            // 스캔이 카드 본체만 보게 된다 (#793 이 그 분기를 세웠다).
                            MemoriesCard(question = "질문", answer = "답변", onReadAgainClick = {})
                        }

                        OtherCandidate.ReceiverMindRecordFilterSheet -> {
                            ReceiverMindRecordFilterSheet(
                                current = ReceiverMindRecordFilter(),
                                onDismiss = {},
                                onApply = {},
                                onReset = {},
                            )
                        }

                        // 화면 자체는 `hiltViewModel()` 을 물지만 `DiaryWriteScreenContent` 가
                        // 상태만 받는 seam 으로 열려 있어 그쪽을 렌더한다. 상단바 완료 버튼과
                        // 기분 선택처럼 툴바 밖에 있는 타깃이 여기에서만 드러난다.
                        OtherCandidate.DiaryWriteScreen -> {
                            DiaryWriteScreenContent(
                                uiState = DiaryWriteUiState(),
                                onBackClick = {},
                                onSubmit = {},
                                onSaveDraft = {},
                                onDraftListClick = {},
                                onTitleChanged = {},
                                onContentChanged = {},
                                onMoodSelected = {},
                                onReceiverRowClick = {},
                                onDateRowClick = {},
                            )
                        }
                    }
                }
            }
        } else {
            composeRule.runOnIdle { slot.value = candidate }
        }
        composeRule.waitForIdle()
    }

    private fun renderStyleToolbarOnly() {
        composeRule.setContent {
            AfternoteTheme {
                TextStyleToolbar(
                    onClose = {},
                    onBoldClick = {},
                    onItalicClick = {},
                    onUnderlineClick = {},
                    onStrikethroughClick = {},
                    onAlignChange = {},
                    onTextStyleChange = {},
                    styleState = TextStyleState(),
                    onLinkClick = {},
                    onTypeClick = {},
                )
            }
        }
    }

    private fun renderToolbars() {
        composeRule.setContent {
            AfternoteTheme {
                Column {
                    BottomToolbar(
                        onTextStyleClick = {},
                        onAlignChange = {},
                        onLinkClick = {},
                        onSaveDraftClick = {},
                        onDraftCountClick = {},
                    )
                    TextStyleToolbar(
                        onClose = {},
                        onBoldClick = {},
                        onItalicClick = {},
                        onUnderlineClick = {},
                        onStrikethroughClick = {},
                        onAlignChange = {},
                        onTextStyleChange = {},
                        styleState = TextStyleState(),
                        onLinkClick = {},
                        onTypeClick = {},
                    )
                }
            }
        }
    }

    private fun List<EnabledClickTarget>.describe(): String =
        joinToString { "${it.name.ifBlank { "(이름없음)" }} ${it.width}x${it.height} role=${it.role}" }

    private companion object {
        val ALIGN_NAMES = setOf("왼쪽 정렬", "가운데 정렬", "오른쪽 정렬")

        /**
         * material3 가 스스로 만들어 넣는 타깃. 시트 바깥을 덮는 scrim 이고 이름·역할 모두
         * 라이브러리 소관이라 이 저장소에서 고칠 수 없다 — 판정에서 뺀다.
         */
        val FRAMEWORK_OWNED_NAMES = setOf("Close sheet")

        /**
         * 우리가 내용을 채우지만 **클릭 액션은 material3 가 얹는** 슬롯. 시트 접기·펴기 손잡이라
         * 버튼도 선택도 아니어서 Role 을 주면 오히려 틀리게 읽힌다. 이름은 우리가 채웠다.
         */
        val SLOT_OWNED_NAMES = setOf("필터 시트 크기 조절")

        val SAMPLE_QUESTION =
            DailyQuestion(title = "질문", date = LocalDate.of(2026, 1, 1), content = "답변")
        val SAMPLE_DIARY =
            DailyDiary(title = "일기", date = LocalDate.of(2026, 1, 1), content = "본문")
    }
}

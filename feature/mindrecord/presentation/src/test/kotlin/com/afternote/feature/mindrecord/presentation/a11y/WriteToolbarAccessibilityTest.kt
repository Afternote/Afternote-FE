package com.afternote.feature.mindrecord.presentation.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.testing.EnabledClickTarget
import com.afternote.core.ui.testing.MinimumTouchTargetSize
import com.afternote.core.ui.testing.scanEnabledClickTargets
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.BottomToolbar
import com.afternote.feature.mindrecord.presentation.component.TextStyleToolbar
import com.afternote.feature.mindrecord.presentation.model.TextStyleState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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

    @Test
    fun `정렬은 단일 선택이고 서식은 개별 토글이다`() {
        // 정렬 pill 은 두 툴바가 모두 그리므로 스타일 툴바 하나만 띄워 중복을 없앤다.
        renderStyleToolbarOnly()
        val targets = composeRule.scanEnabledClickTargets()

        // 정렬 셋 — 하나만 선택된 상태로 읽혀야 한다.
        val aligns = targets.filter { it.name in setOf("왼쪽 정렬", "가운데 정렬", "오른쪽 정렬") }
        assertTrue("정렬 3개를 못 찾았다: ${targets.map { it.name }}", aligns.size == 3)
        assertTrue("정렬이 단일 선택이 아니다: ${aligns.describe()}", aligns.all { it.role == Role.RadioButton })
        assertTrue("정렬에 선택 상태가 없다: ${aligns.describe()}", aligns.count { it.selected == true } == 1)

        // 서식 넷 — 서로 독립인 토글이라 «선택» 이 아니라 «켜짐/꺼짐» 이다.
        val styles = targets.filter { it.name in setOf("굵게", "기울임", "밑줄", "취소선") }
        assertTrue("서식 4개를 못 찾았다: ${targets.map { it.name }}", styles.size == 4)
        assertTrue("서식이 토글이 아니다: ${styles.describe()}", styles.all { it.role == Role.Checkbox })
        assertTrue("서식에 토글 상태가 없다: ${styles.describe()}", styles.all { it.toggleableState != null })
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
}

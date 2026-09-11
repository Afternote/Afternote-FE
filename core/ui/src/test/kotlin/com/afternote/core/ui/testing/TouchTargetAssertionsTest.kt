package com.afternote.core.ui.testing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TouchTargetAssertionsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `scanner uses expanded touch bounds and preserves layout bounds`() {
        composeRule.setContent {
            Column(Modifier.padding(64.dp)) {
                Box(
                    Modifier
                        .testTag("small")
                        .size(24.dp)
                        .clickable(role = Role.Button, onClick = {})
                        .semantics { contentDescription = "Small" },
                )
                Box(
                    Modifier
                        .testTag("disabled")
                        .size(12.dp)
                        .clickable(enabled = false, role = Role.Button, onClick = {})
                        .semantics { contentDescription = "Disabled" },
                )
                Box(
                    Modifier
                        .testTag("toggle")
                        .size(24.dp)
                        .toggleable(value = true, role = Role.Checkbox, onValueChange = {})
                        .semantics { contentDescription = "Toggle" },
                )
            }
        }

        val targets = composeRule.scanEnabledClickTargets()
        val small = targets.single { it.name == "Small" }
        val toggle = targets.single { it.name == "Toggle" }

        assertEquals(48f, small.width.value, 0.1f)
        assertEquals(48f, small.height.value, 0.1f)
        assertEquals(24f, small.layoutWidth.value, 0.1f)
        assertEquals(24f, small.layoutHeight.value, 0.1f)
        assertFalse(targets.any { it.testTag == "disabled" })
        assertEquals(Role.Checkbox, toggle.role)
        assertEquals("On", toggle.toggleableState.toString())
    }

    @Test
    fun `named button nested in editable click is an automatic nested exception`() {
        composeRule.setContent {
            Column(Modifier.padding(64.dp)) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clickable(onClick = {})
                        .semantics {
                            contentDescription = "Field"
                            editableText = AnnotatedString("")
                            requestFocus { true }
                        },
                ) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clickable(role = Role.Button, onClick = {})
                            .semantics { contentDescription = "Clear" },
                    )
                }
                Box(
                    Modifier
                        .size(72.dp)
                        .clickable(role = Role.Button, onClick = {})
                        .semantics { contentDescription = "Outer" },
                ) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clickable(role = Role.Button, onClick = {})
                            .semantics { contentDescription = "Inner" },
                    )
                }
            }
        }

        val targets = composeRule.scanEnabledClickTargets()

        assertFalse(targets.single { it.name == "Clear" }.hasClickAncestor)
        assertTrue(targets.single { it.name == "Inner" }.hasClickAncestor)
    }

    /**
     * #1669 — 「눌러서 이동하는 목록 행 + 끝단 오버플로 메뉴」는 중첩이어도 결함이 아니다.
     *
     * 같은 행 안이라도 끝단에 있지 않으면(«Leading») 예외에 들지 않는다. 위치가 판정에
     * 실제로 쓰이는지를 함께 못박는다 — 크기만 보면 아무 데나 붙은 작은 버튼이 통과한다.
     */
    @Test
    fun `named trailing accessory in a clickable row is a nested exception`() {
        composeRule.setContent {
            Column(Modifier.padding(16.dp)) {
                Box(
                    Modifier
                        .size(width = 320.dp, height = 72.dp)
                        .clickable(role = Role.Button, onClick = {})
                        .semantics { contentDescription = "Row" },
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .size(20.dp)
                            .clickable(role = Role.Button, onClick = {})
                            .semantics { contentDescription = "Overflow" },
                    )
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .size(20.dp)
                            .clickable(role = Role.Button, onClick = {})
                            .semantics { contentDescription = "Leading" },
                    )
                }
            }
        }

        val targets = composeRule.scanEnabledClickTargets()

        assertFalse(targets.single { it.name == "Overflow" }.hasClickAncestor)
        assertTrue(targets.single { it.name == "Leading" }.hasClickAncestor)
    }

    /**
     * #1669 — 컨테이너가 **세로로 긴** 카드여도 같은 예외를 받는다.
     *
     * `DiaryCard` 는 2열 staggered grid(열 폭 ≤ 176dp)에 놓이고 이미지가 붙으면 세로가 폭을
     * 넘는다. 「가로가 세로보다 길다」를 축으로 두면 같은 «항목 + 머리 끝단 메뉴» 형태인데도
     * 형상 때문에 예외에서 빠져 세 카드의 처방이 갈렸다.
     */
    @Test
    fun `named trailing accessory in a tall card is also a nested exception`() {
        composeRule.setContent {
            Column(Modifier.padding(16.dp)) {
                Box(
                    Modifier
                        .size(width = 176.dp, height = 300.dp)
                        .clickable(role = Role.Button, onClick = {})
                        .semantics { contentDescription = "Card" },
                ) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                            .clickable(role = Role.Button, onClick = {})
                            .semantics { contentDescription = "CardOverflow" },
                    )
                }
            }
        }

        assertFalse(composeRule.scanEnabledClickTargets().single { it.name == "CardOverflow" }.hasClickAncestor)
    }

    /** 폭만 좁고 세로로 긴 띠는 «작은 보조» 가 아니다 — 높이 상한이 그것을 가른다. */
    @Test
    fun `a tall trailing strip is not an accessory`() {
        composeRule.setContent {
            Column(Modifier.padding(16.dp)) {
                Box(
                    Modifier
                        .size(width = 176.dp, height = 300.dp)
                        .clickable(role = Role.Button, onClick = {})
                        .semantics { contentDescription = "Card" },
                ) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(width = 32.dp, height = 300.dp)
                            .clickable(role = Role.Button, onClick = {})
                            .semantics { contentDescription = "Strip" },
                    )
                }
            }
        }

        assertTrue(composeRule.scanEnabledClickTargets().single { it.name == "Strip" }.hasClickAncestor)
    }

    /** Role 이 Button 이 아니면 예외가 아니다 — `Icon` 이 심는 `Role.Image` 로는 통과하지 못한다. */
    @Test
    fun `a trailing accessory without the button role stays a nested violation`() {
        composeRule.setContent {
            Column(Modifier.padding(16.dp)) {
                Box(
                    Modifier
                        .size(width = 320.dp, height = 72.dp)
                        .clickable(role = Role.Button, onClick = {})
                        .semantics { contentDescription = "Row" },
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .size(20.dp)
                            .clickable(role = Role.Image, onClick = {})
                            .semantics { contentDescription = "Imaged" },
                    )
                }
            }
        }

        assertTrue(composeRule.scanEnabledClickTargets().single { it.name == "Imaged" }.hasClickAncestor)
    }

    /**
     * 예외가 «작은 끝단 액션» 을 정확히 가리키는지 — 행을 반으로 나눠 갖는 두 번째 영역은
     * 어느 쪽을 눌렀는지 모호하므로 그대로 위반이어야 한다.
     */
    @Test
    fun `a trailing half of a row is not an accessory`() {
        composeRule.setContent {
            Column(Modifier.padding(16.dp)) {
                Box(
                    Modifier
                        .size(width = 320.dp, height = 72.dp)
                        .clickable(role = Role.Button, onClick = {})
                        .semantics { contentDescription = "Row" },
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .size(width = 160.dp, height = 72.dp)
                            .clickable(role = Role.Button, onClick = {})
                            .semantics { contentDescription = "Half" },
                    )
                }
            }
        }

        assertTrue(composeRule.scanEnabledClickTargets().single { it.name == "Half" }.hasClickAncestor)
    }

    /** 이름이나 Role 이 없으면 위치·크기가 맞아도 예외가 아니다 — 그 둘이 별개로 읽히는 근거다. */
    @Test
    fun `an unnamed trailing accessory stays a nested violation`() {
        composeRule.setContent {
            Column(Modifier.padding(16.dp)) {
                Box(
                    Modifier
                        .size(width = 320.dp, height = 72.dp)
                        .clickable(role = Role.Button, onClick = {})
                        .semantics { contentDescription = "Row" },
                ) {
                    Box(
                        Modifier
                            .testTag("nameless")
                            .align(Alignment.CenterEnd)
                            .size(20.dp)
                            .clickable(role = Role.Button, onClick = {}),
                    )
                }
            }
        }

        assertTrue(composeRule.scanEnabledClickTargets().single { it.testTag == "nameless" }.hasClickAncestor)
    }

    @Test
    fun `assertion fails missing name role and state`() {
        composeRule.setContent {
            Column(Modifier.padding(64.dp)) {
                Box(Modifier.size(48.dp).clickable(role = Role.Button, onClick = {}))
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable(onClick = {})
                        .semantics { contentDescription = "No role" },
                )
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable(role = Role.Checkbox, onClick = {})
                        .semantics { contentDescription = "No toggle state" },
                )
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable(role = Role.RadioButton, onClick = {})
                        .semantics { contentDescription = "No selected state" },
                )
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable(role = Role.Switch, onClick = {})
                        .semantics { contentDescription = "No switch state" },
                )
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable(role = Role.Tab, onClick = {})
                        .semantics { contentDescription = "No tab state" },
                )
            }
        }

        val targets = composeRule.scanEnabledClickTargets()
        val error = runCatching { composeRule.assertAccessibleClickTargets() }.exceptionOrNull()
        assertTrue(error is AssertionError)
        assertTrue(error?.message.orEmpty().contains("접근 가능한 이름 누락"))
        assertTrue(error?.message.orEmpty().contains("Role 누락"))
        assertTrue(error?.message.orEmpty().contains("선택 상태 누락"))
        assertTrue(targets.single { it.name == "No switch state" }.lacksRequiredState())
        assertTrue(targets.single { it.name == "No tab state" }.lacksRequiredState())
    }

    @Test
    fun `assertion fails when there are zero enabled click targets`() {
        composeRule.setContent { Box(Modifier.fillMaxSize()) }

        val zeroError = runCatching { composeRule.assertAccessibleClickTargets() }.exceptionOrNull()
        assertTrue(zeroError is AssertionError)
        assertTrue(zeroError?.message.orEmpty().contains("하나도 없습니다"))
    }

    @Test
    fun `foundation expands an 18dp visual to a 48dp touch target without layout growth`() {
        composeRule.setContent {
            Box(Modifier.padding(64.dp)) {
                Box(
                    Modifier
                        .size(18.dp)
                        .clickable(role = Role.Button, onClick = {})
                        .semantics { contentDescription = "Expanded" },
                ) {
                    Box(Modifier.fillMaxSize().testTag("visual"))
                }
            }
        }

        composeRule.assertAccessibleClickTargets()
        val expanded = composeRule.scanEnabledClickTargets().single { it.name == "Expanded" }
        assertEquals(48f, expanded.width.value, 0.1f)
        assertEquals(48f, expanded.height.value, 0.1f)
        assertEquals(18f, expanded.layoutWidth.value, 0.1f)
        assertEquals(18f, expanded.layoutHeight.value, 0.1f)
    }
}

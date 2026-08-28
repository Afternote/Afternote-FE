package com.afternote.core.ui.testing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
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
    fun `named button nested in editable click is the only automatic nested exception`() {
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

package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.shared.detail.PlaylistSongItem
import com.afternote.feature.afternote.presentation.shared.detail.SelectableSongListBody
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SelectableSongListBodyInteractionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `두 곡을 선택하고 한 곡을 해제한 뒤 선택 삭제와 전체 삭제를 실행한다`() {
        val deleteAllCalls = mutableListOf<Set<String>>()
        val deleteSelectedCalls = mutableListOf<Set<String>>()
        renderSelectableList(
            onDeleteAll = deleteAllCalls::add,
            onDeleteSelected = deleteSelectedCalls::add,
        )

        composeRule.onAllNodes(checkboxMatcher).assertCountEquals(2)
        composeRule.onAllNodes(checkboxMatcher, useUnmergedTree = true).assertCountEquals(2)
        composeRule.onAllNodes(toggleableStateMatcher, useUnmergedTree = true).assertCountEquals(2)
        composeRule.onNode(hasText(FIRST_TITLE) and checkboxMatcher).assertIsOff()
        composeRule.onNode(hasText(SECOND_TITLE) and checkboxMatcher).assertIsOff()

        composeRule.onNode(hasText(FIRST_TITLE) and checkboxMatcher).performClick()
        composeRule.onNode(hasText(SECOND_TITLE) and checkboxMatcher).performClick()
        composeRule.onNode(hasText(FIRST_TITLE) and checkboxMatcher).assertIsOn()
        composeRule.onNode(hasText(SECOND_TITLE) and checkboxMatcher).assertIsOn()

        composeRule.onNode(hasText(FIRST_TITLE) and checkboxMatcher).performClick()
        composeRule.onNode(hasText(FIRST_TITLE) and checkboxMatcher).assertIsOff()
        composeRule.onNode(hasText(SECOND_TITLE) and checkboxMatcher).assertIsOn()
        composeRule.onNodeWithText(DELETE_SELECTED).performClick()

        composeRule.runOnIdle {
            assertEquals(emptyList<Set<String>>(), deleteAllCalls)
            assertEquals(listOf(setOf(SECOND_KEY)), deleteSelectedCalls)
        }
        composeRule.onNodeWithText(DELETE_SELECTED).assertDoesNotExist()
        composeRule.onAllNodes(checkboxMatcher).apply {
            get(0).assertIsOff()
            get(1).assertIsOff()
        }

        composeRule.onNode(hasText(FIRST_TITLE) and checkboxMatcher).performClick()
        composeRule.onNode(hasText(SECOND_TITLE) and checkboxMatcher).performClick()
        composeRule.onNodeWithText(DELETE_ALL).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(setOf(FIRST_KEY, SECOND_KEY)), deleteAllCalls)
            assertEquals(listOf(setOf(SECOND_KEY)), deleteSelectedCalls)
        }
        composeRule.onNodeWithText(DELETE_ALL).assertDoesNotExist()
    }

    @Test
    fun `읽기 전용 곡은 클릭과 선택 semantics를 노출하지 않는다`() {
        composeRule.setContent {
            AfternoteTheme {
                PlaylistSongItem(song = songs.first())
            }
        }

        composeRule.onNodeWithText(FIRST_TITLE).assertHasNoClickAction()
        composeRule.onAllNodes(checkboxMatcher).assertCountEquals(0)
        composeRule.onAllNodes(checkboxMatcher, useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodes(toggleableStateMatcher).assertCountEquals(0)
        composeRule.onAllNodes(toggleableStateMatcher, useUnmergedTree = true).assertCountEquals(0)
    }

    private fun renderSelectableList(
        onDeleteAll: (Set<String>) -> Unit,
        onDeleteSelected: (Set<String>) -> Unit,
    ) {
        composeRule.setContent {
            AfternoteTheme {
                SelectableSongListBody(
                    songs = songs,
                    header = {},
                    initialSelectedSongKeys = emptySet(),
                    actionLabel = DELETE_ALL,
                    onAction = onDeleteAll,
                    secondaryActionLabel = DELETE_SELECTED,
                    onSecondaryAction = onDeleteSelected,
                )
            }
        }
    }

    private companion object {
        const val FIRST_KEY = "song:1"
        const val SECOND_KEY = "song:2"
        const val FIRST_TITLE = "첫 번째 곡"
        const val SECOND_TITLE = "두 번째 곡"
        const val DELETE_ALL = "전체 삭제"
        const val DELETE_SELECTED = "선택 삭제"

        val songs =
            listOf(
                PlaylistSongDisplay(selectionKey = FIRST_KEY, title = FIRST_TITLE, artist = "가수 A"),
                PlaylistSongDisplay(selectionKey = SECOND_KEY, title = SECOND_TITLE, artist = "가수 B"),
            )
        val checkboxMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        val toggleableStateMatcher =
            SemanticsMatcher("ToggleableState가 정의됨") { node ->
                node.config.getOrNull(SemanticsProperties.ToggleableState) != null
            }
    }
}

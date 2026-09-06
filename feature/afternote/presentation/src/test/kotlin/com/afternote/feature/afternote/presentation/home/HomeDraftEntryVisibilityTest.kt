package com.afternote.feature.afternote.presentation.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 임시저장 진입점은 **작성자에게만** 있다 (#808).
 *
 * 목록 헤더는 작성자와 수신자가 같은 것을 쓴다 — 발신자 문구가 수신자에게 샜던 #620 과 같은 자리라,
 * 진입점도 콜백이 없으면 아예 안 그리는지 가드한다. 수신자에게는 임시저장이라는 개념 자체가 없다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HomeDraftEntryVisibilityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `작성자 헤더는 임시저장 진입점을 그린다`() {
        composeRule.setContent {
            AfternoteTheme {
                HomeHeaderSection(description = "설명", nextStep = null, onDraftListClick = {})
            }
        }

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.afternote_home_draft_entry))
            .assertHasClickAction()
    }

    @Test
    fun `수신자 헤더에는 임시저장 진입점이 없다`() {
        composeRule.setContent {
            AfternoteTheme {
                HomeHeaderSection(description = "설명", nextStep = null)
            }
        }

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.afternote_home_draft_entry))
            .assertDoesNotExist()
    }
}

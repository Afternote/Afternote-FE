package com.afternote.feature.afternote.presentation.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** prefill 실패 본문이 사유와 복구 수단을 모두 내놓는지 (#705). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EditorPrefillErrorBodyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `실패 사유와 다시 불러오기를 함께 보여주고 클릭을 전달한다`() {
        var retries = 0
        composeRule.setContent {
            AfternoteTheme { EditorPrefillErrorBody(onRetry = { retries += 1 }) }
        }

        composeRule.onNodeWithText(string(R.string.afternote_editor_prefill_load_failed)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.afternote_editor_prefill_retry)).performClick()

        composeRule.runOnIdle { assertEquals(1, retries) }
    }

    private fun string(resId: Int): String = composeRule.activity.getString(resId)
}

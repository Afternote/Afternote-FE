package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MemorialMediaSourceBottomSheetInteractionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `지울 첨부가 없으면 삭제 항목이 뜨지 않는다`() {
        composeRule.setContent {
            AfternoteTheme {
                MemorialMediaSourceBottomSheet(
                    target = MemorialMediaTarget.VIDEO,
                    onPickFromGallery = {},
                    onCapture = {},
                    onRemove = null,
                )
            }
        }

        composeRule.onNodeWithText("영상 삭제").assertDoesNotExist()
    }

    @Test
    fun `첨부가 있으면 삭제 항목이 뜨고 탭이 콜백을 부른다`() {
        var removeClicks = 0
        composeRule.setContent {
            AfternoteTheme {
                MemorialMediaSourceBottomSheet(
                    target = MemorialMediaTarget.PHOTO,
                    onPickFromGallery = {},
                    onCapture = {},
                    onRemove = { removeClicks += 1 },
                )
            }
        }

        composeRule.onNodeWithText("사진 삭제").performClick()

        composeRule.runOnIdle { assertEquals(1, removeClicks) }
    }

    @Test
    fun `삭제 문구는 슬롯 종류를 따라간다`() {
        composeRule.setContent {
            AfternoteTheme {
                MemorialMediaSourceBottomSheet(
                    target = MemorialMediaTarget.VIDEO,
                    onPickFromGallery = {},
                    onCapture = {},
                    onRemove = {},
                )
            }
        }

        composeRule.onNodeWithText("영상 삭제").assertExists()
        composeRule.onNodeWithText("사진 삭제").assertDoesNotExist()
    }
}

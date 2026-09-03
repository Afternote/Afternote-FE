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

    @Test
    fun `음성 슬롯은 갤러리-촬영이 아니라 파일 선택-녹음으로 갈린다`() {
        // 음성은 사진 선택기 대상이 아니라 문서 선택기로 고르고, «촬영» 대신 녹음 인텐트를 쏜다 (#1118).
        composeRule.setContent {
            AfternoteTheme {
                MemorialMediaSourceBottomSheet(
                    target = MemorialMediaTarget.AUDIO,
                    onPickFromGallery = {},
                    onCapture = {},
                    onRemove = {},
                )
            }
        }

        composeRule.onNodeWithText("파일에서 선택").assertExists()
        composeRule.onNodeWithText("음성 녹음").assertExists()
        composeRule.onNodeWithText("음성 삭제").assertExists()
        composeRule.onNodeWithText("갤러리에서 선택").assertDoesNotExist()
        composeRule.onNodeWithText("영상 촬영").assertDoesNotExist()
    }
}

package com.afternote.core.ui.sheet

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
class MediaSelectSheetContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `주입한 항목이 순서대로 렌더되고 클릭이 해당 항목으로 전달된다`() {
        val clicked = mutableListOf<String>()

        composeRule.setContent {
            AfternoteTheme {
                MediaSelectSheetContent(
                    items =
                        listOf(
                            mediaItem(R.drawable.core_ui_ic_image, "이미지 추가하기") { clicked += "image" },
                            mediaItem(R.drawable.core_ui_ic_mic, "음성 추가하기") { clicked += "voice" },
                            mediaItem(R.drawable.core_ui_ic_file, "파일 추가하기") { clicked += "file" },
                            mediaItem(R.drawable.core_ui_ic_link, "링크 추가하기") { clicked += "link" },
                        ),
                )
            }
        }

        composeRule.onNodeWithText("이미지 추가하기").assertHasClickAction()
        composeRule.onNodeWithText("음성 추가하기").assertHasClickAction()
        composeRule.onNodeWithText("파일 추가하기").assertHasClickAction()
        composeRule.onNodeWithText("링크 추가하기").assertHasClickAction()

        composeRule.onNodeWithText("파일 추가하기").performClick()
        composeRule.onNodeWithText("이미지 추가하기").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("file", "image"), clicked)
        }
    }

    /**
     * 항목 수를 계약에 박지 않았다는 회귀 가드 — 서류 업로드(2갈래)처럼 갈래가 적은 화면도,
     * 음성 첨부(#1118)처럼 갈래가 느는 화면도 같은 컴포넌트를 쓴다.
     */
    @Test
    fun `항목이 두 개뿐이어도 그 두 개만 그린다`() {
        composeRule.setContent {
            AfternoteTheme {
                MediaSelectSheetContent(
                    items =
                        listOf(
                            mediaItem(R.drawable.core_ui_ic_image, "이미지 추가하기") {},
                            mediaItem(R.drawable.core_ui_ic_file, "파일 추가하기") {},
                        ),
                )
            }
        }

        composeRule.onNodeWithText("이미지 추가하기").assertExists()
        composeRule.onNodeWithText("파일 추가하기").assertExists()
        composeRule.onNodeWithText("음성 추가하기").assertDoesNotExist()
        composeRule.onNodeWithText("링크 추가하기").assertDoesNotExist()
    }

    @Test
    fun `헤더 문구는 core 리소스 기본값을 쓰고 호출부가 덮을 수 있다`() {
        val defaultTitle = RuntimeEnvironment.getApplication().getString(R.string.core_ui_media_sheet_title)

        composeRule.setContent {
            AfternoteTheme {
                MediaSelectSheetContent(
                    items = listOf(mediaItem(R.drawable.core_ui_ic_image, "이미지 추가하기") {}),
                )
            }
        }

        composeRule.onNodeWithText(defaultTitle).assertExists()
    }

    private fun mediaItem(
        iconRes: Int,
        label: String,
        onClick: () -> Unit,
    ) = MediaSheetItem(iconRes = iconRes, label = label, onClick = onClick)
}

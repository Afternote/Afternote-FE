package com.afternote.feature.afternote.presentation.editor.account

import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodSection
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiverSection
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 계정 기반 에디터(소셜네트워크·비즈니스)의 섹션 순서와 필수 마크를 시안 정본에 고정한다 (#494).
 *
 * 이 순서는 주석으로만 남아 있었고 가드가 없었다. 07-18 시안 개편 delta 를 추적하던 #494 가
 * 두 항목(수신자 추가 섹션 신설 · 정보 처리 방법 라디오 3택)을 들고 있었는데, 정본 프레임
 * `4327:70468` 의 TEXT 노드 전수 실측 결과 수신자 섹션은 살아 있고 라디오는 사라졌다.
 * 라디오가 되돌아오면 이 화면의 섹션 사이를 비집고 들어오므로, 순서를 여기서 잠근다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w480dp-h2000dp")
class AccountEditorSectionOrderTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `계정 정보 다음에 수신자 추가, 그다음 처리 방법 리스트와 남기실 말씀을 그린다`() {
        setAccountEditorContent()

        val accountInfoTop = labelTop(R.string.afternote_editor_label_account_info)
        val receiverAddTop = labelTop(R.string.afternote_editor_label_receiver_add)
        val processMethodTop = labelTop(R.string.afternote_editor_label_process_method_list)
        val messagesTop = labelTop(R.string.afternote_editor_label_messages)

        assertTrue(
            "수신자 추가는 계정 정보 아래에 있어야 한다 (계정 $accountInfoTop, 수신자 $receiverAddTop)",
            accountInfoTop < receiverAddTop,
        )
        assertTrue(
            "처리 방법 리스트는 수신자 추가 아래에 있어야 한다 (수신자 $receiverAddTop, 처리 방법 $processMethodTop)",
            receiverAddTop < processMethodTop,
        )
        assertTrue(
            "남기실 말씀은 처리 방법 리스트 아래에 있어야 한다 (처리 방법 $processMethodTop, 말씀 $messagesTop)",
            processMethodTop < messagesTop,
        )
    }

    @Test
    fun `수신자 추가 라벨에는 필수 표시를 붙이지 않는다`() {
        setAccountEditorContent()

        val receiverAdd = composeRule.activity.getString(R.string.afternote_editor_label_receiver_add)
        val accountInfo = composeRule.activity.getString(R.string.afternote_editor_label_account_info)

        // 필수 라벨은 EditorSectionLabel 이 contentDescription 을 붙인다. 계정 정보에는 붙고
        // 수신자 추가에는 붙지 않아야 한다 — 정본 시안에 수신자 섹션 필수 마크가 없다.
        composeRule.onNodeWithContentDescription(requiredDescriptionOf(receiverAdd)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(requiredDescriptionOf(accountInfo)).assertExists()
    }

    private fun setAccountEditorContent() {
        composeRule.setContent {
            AfternoteTheme {
                AccountEditorContent(
                    editorMessages = emptyList(),
                    accountSection =
                        AccountSection(
                            idState = rememberTextFieldState(),
                            passwordState = rememberTextFieldState(),
                        ),
                    recipientSection =
                        AfternoteEditorReceiverSection(
                            afternoteEditReceivers =
                                listOf(
                                    AfternoteEditorReceiver(id = 1L, name = "홍길동", label = "가족"),
                                ),
                            onAddClick = {},
                            onItemDeleteClick = {},
                        ),
                    processingMethodSection =
                        ProcessingMethodSection(
                            items = emptyList(),
                            onItemDeleteClick = {},
                            onItemAdded = {},
                            onItemEdited = { _, _ -> },
                        ),
                    onMessageRegisterClick = {},
                    onMessageDeleteClick = {},
                    onMessageAddClick = {},
                )
            }
        }
    }

    private fun labelTop(resId: Int): Dp =
        composeRule
            .onNodeWithText(composeRule.activity.getString(resId))
            .getUnclippedBoundsInRoot()
            .top

    private fun requiredDescriptionOf(label: String): String =
        composeRule.activity.getString(
            R.string.afternote_editor_semantics_required_field_description,
            label,
        )
}

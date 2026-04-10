package com.afternote.feature.afternote.presentation.author.editor.gallery
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.Label
import com.afternote.core.ui.LabelStyle
import com.afternote.core.ui.SelectableRadioCard
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessage
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageSection
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiverSection
import com.afternote.feature.afternote.presentation.author.editor.model.InfoMethodSection
import com.afternote.feature.afternote.presentation.author.editor.model.InformationProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.processing.OptionRadioCardContent
import com.afternote.feature.afternote.presentation.author.editor.processing.ProcessingMethodList
import com.afternote.feature.afternote.presentation.author.editor.processing.ProcessingMethodListParams
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodSection
import com.afternote.feature.afternote.presentation.author.editor.receiver.RecipientDesignationSection

/**
 * 갤러리 및 파일 선택 시 표시되는 콘텐츠
 */
@Composable
fun GalleryAndFileEditorContent(
    modifier: Modifier = Modifier,
    bottomPadding: PaddingValues,
    params: GalleryAndFileEditorContentParams,
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    // Scaffold가 제공하는 bottomPadding을 사용 (네비게이션 바 높이 + 시스템 바 높이 자동 계산)
    val bottomPaddingDp = bottomPadding.calculateBottomPadding()
    // Viewport 높이 = 창 높이 - bottomPadding (네비게이션 바 상단까지의 높이)
    // 하단 여백은 네비게이션 바 상단까지의 Viewport 높이의 10%로 계산
    val viewportHeight =
        with(density) {
            windowInfo.containerSize.height.toDp() - bottomPaddingDp
        }
    val spacerHeight = viewportHeight * 0.1f

    GalleryAndFileEditorContentBody(
        modifier = modifier,
        params = params,
        spacerHeight = spacerHeight,
    )
}

@Composable
private fun GalleryAndFileEditorContentBody(
    modifier: Modifier = Modifier,
    params: GalleryAndFileEditorContentParams,
    spacerHeight: Dp,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 계정 처리 방법 섹션
        Label(
            text = "계정 처리 방법",
            isRequired = true,
            style = LabelStyle(requiredDotOffsetY = 2.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        InformationProcessingMethod.entries.forEachIndexed { index, method ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            SelectableRadioCard(
                selected = params.infoMethodSection.selectedMethod == method,
                onClick = { params.infoMethodSection.onMethodSelected(method) },
                modifier = Modifier.fillMaxWidth(),
                content = {
                    OptionRadioCardContent(
                        option = method,
                        selected = params.infoMethodSection.selectedMethod == method,
                    )
                },
            )
        }

        if (params.infoMethodSection.selectedMethod ==
            InformationProcessingMethod.TRANSFER_TO_ADDITIONAL_AFTERNOTE_EDIT_RECEIVER
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            RecipientDesignationSection(section = params.recipientSection)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 처리 방법 리스트 섹션
        Label(
            text = "처리 방법 리스트",
            isRequired = true,
            style = LabelStyle(requiredDotOffsetY = 2.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        ProcessingMethodList(
            params =
                ProcessingMethodListParams(
                    items = params.processingMethodSection.items,
                    onItemDeleteClick = params.processingMethodSection.callbacks.onItemDeleteClick,
                    onItemAdded = params.processingMethodSection.callbacks.onItemAdded,
                    onTextFieldVisibilityChanged = params.processingMethodSection.callbacks.onTextFieldVisibilityChanged,
                    onItemEdited = params.processingMethodSection.callbacks.onItemEdited,
                ),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 남기실 말씀
        EditorMessageSection(
            messages = params.editorMessages,
            onRegisterClick = params.onMessageRegisterClick,
            onDeleteClick = params.onMessageDeleteClick,
            onAddClick = params.onMessageAddClick,
        )

        // 갤러리 및 파일 탭 하단 여백 (Viewport 높이의 10%, 800dp 기준 약 80dp)
        // LocalWindowInfo를 사용하여 창 높이를 기준으로 계산
        Spacer(modifier = Modifier.height(spacerHeight))
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryAndFileEditorContentPreview() {
    AfternoteTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
        ) {
            // 첫 번째 옵션 선택됨 (파란 테두리), 두 번째는 선택 안 됨 (테두리 없음) 상태를 한 화면에 표시
            GalleryAndFileEditorContent(
                bottomPadding = PaddingValues(bottom = 88.dp),
                params =
                    GalleryAndFileEditorContentParams(
                        editorMessages = listOf(EditorMessage()),
                        infoMethodSection =
                            InfoMethodSection(
                                selectedMethod = InformationProcessingMethod.TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER,
                                onMethodSelected = {},
                            ),
                        recipientSection = AfternoteEditorReceiverSection(),
                        processingMethodSection = ProcessingMethodSection(),
                    ),
            )
        }
    }
}

@Preview(showBackground = true, name = "추가 수신자 선택됨")
@Composable
private fun GalleryAndFileEditorContentWithAfternoteEditorReceiversPreview() {
    AfternoteTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
        ) {
            GalleryAndFileEditorContent(
                bottomPadding = PaddingValues(bottom = 88.dp),
                params =
                    GalleryAndFileEditorContentParams(
                        editorMessages = listOf(EditorMessage()),
                        infoMethodSection =
                            InfoMethodSection(
                                selectedMethod = InformationProcessingMethod.TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER,
                                onMethodSelected = {},
                            ),
                        recipientSection =
                            AfternoteEditorReceiverSection(
                                afternoteEditReceivers = emptyList(),
                            ),
                    ),
            )
        }
    }
}

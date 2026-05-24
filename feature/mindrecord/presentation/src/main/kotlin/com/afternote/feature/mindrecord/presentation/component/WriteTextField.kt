package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.TextStyleState
import com.afternote.feature.mindrecord.presentation.model.TextStyleType
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor

/**
 * HTML 직렬화 가능한 리치 텍스트 입력 영역.
 *
 * - 외부에서 받은 [value] 는 초기 시드 HTML 로만 사용된다 (이후 외부 업데이트는 무시).
 * - 사용자 입력이 발생하면 [onValueChange] 로 직렬화된 HTML 문자열을 emit 한다.
 *   서버 페이로드의 `content` 필드에 그대로 실어 보내면 된다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WriteTextField(
    modifier: Modifier = Modifier,
    value: String? = null,
    onValueChange: ((String) -> Unit)? = null,
) {
    val state = rememberRichTextState()

    LaunchedEffect(Unit) {
        if (!value.isNullOrEmpty()) state.setHtml(value)
    }

    LaunchedEffect(state.annotatedString) {
        onValueChange?.invoke(state.toHtml())
    }

    val styleState =
        TextStyleState(
            isBold = state.currentSpanStyle.fontWeight == FontWeight.Bold,
            isItalic = state.currentSpanStyle.fontStyle == FontStyle.Italic,
            isUnderline = state.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true,
            isStrikethrough = state.currentSpanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true,
            textAlign = state.currentParagraphStyle.textAlign ?: TextAlign.Start,
            textStyle = currentTextStyleType(state.currentSpanStyle.fontSize.value),
        )

    var showTextStyleToolbar by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.isImeVisible
    val editorFocusRequester = remember { FocusRequester() }

    LaunchedEffect(imeVisible) {
        if (!imeVisible) showTextStyleToolbar = false
    }

    fun keepEditorFocus(action: () -> Unit) {
        action()
        runCatching { editorFocusRequester.requestFocus() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(color = AfternoteDesign.colors.white),
        ) {
            BasicRichTextEditor(
                state = state,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .focusRequester(editorFocusRequester)
                        .padding(16.dp),
            )
            if (state.annotatedString.text.isEmpty()) {
                Text(
                    text = stringResource(R.string.mindrecord_write_field_placeholder),
                    color = AfternoteDesign.colors.gray4,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Text(
                text = stringResource(R.string.mindrecord_write_field_character_count, state.annotatedString.text.length),
                color = AfternoteDesign.colors.gray4,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
            )
        }

        if (showTextStyleToolbar && imeVisible) {
            TextStyleToolbar(
                styleState = styleState,
                onClose = { showTextStyleToolbar = false },
                onBoldClick = {
                    keepEditorFocus { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
                },
                onItalicClick = {
                    keepEditorFocus { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
                },
                onUnderlineClick = {
                    keepEditorFocus { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }
                },
                onStrikethroughClick = {
                    keepEditorFocus { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) }
                },
                onAlignChange = { align ->
                    keepEditorFocus { state.addParagraphStyle(ParagraphStyle(textAlign = align)) }
                },
                onTextStyleChange = { type ->
                    keepEditorFocus { state.addSpanStyle(type.toSpanStyle()) }
                },
            )
        }

        BottomToolbar(
            modifier = Modifier.imePadding(),
            onTextStyleClick = { showTextStyleToolbar = !showTextStyleToolbar },
            onAlignChange = { align ->
                keepEditorFocus { state.addParagraphStyle(ParagraphStyle(textAlign = align)) }
            },
        )
    }
}

private fun TextStyleType.toSpanStyle(): SpanStyle =
    when (this) {
        TextStyleType.TITLE -> SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
        TextStyleType.HEADER -> SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
        TextStyleType.SUBHEADER -> SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
        TextStyleType.BODY -> SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
    }

private fun currentTextStyleType(currentFontSize: Float): TextStyleType =
    when (currentFontSize.toInt()) {
        20 -> TextStyleType.TITLE
        18 -> TextStyleType.HEADER
        14 -> TextStyleType.SUBHEADER
        else -> TextStyleType.BODY
    }

@Preview(showBackground = true)
@Composable
private fun WriteTextFieldPreview() {
    AfternoteTheme {
        WriteTextField()
    }
}

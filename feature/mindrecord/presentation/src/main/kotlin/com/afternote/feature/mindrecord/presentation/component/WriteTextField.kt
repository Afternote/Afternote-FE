package com.afternote.feature.mindrecord.presentation.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.afternote.feature.mindrecord.presentation.util.escapeHtml
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    onSaveDraftClick: () -> Unit = {},
    onDraftCountClick: () -> Unit = {},
    draftCount: Int = 0,
    /**
     * 갤러리에서 고른 이미지를 서버에 업로드하고 영구 URL 을 반환하는 업로더
     * (`POST /files/presigned-url` → S3 PUT). null 이면 업로드 없이 로컬 URI 를 그대로 삽입한다.
     * 업로드 실패(반환값 null) 시 이미지는 본문에 삽입되지 않는다.
     */
    onImagePicked: (suspend (uriString: String) -> String?)? = null,
) {
    val state = rememberRichTextState()
    val scope = rememberCoroutineScope()

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
    var sheet: KeyboardSheet by remember { mutableStateOf(KeyboardSheet.None) }
    val imeVisible = WindowInsets.isImeVisible
    // 지연 판정 시점에도 최신 값을 읽어야 한다 — 람다가 진입 시점 값에 고정되면 안 된다.
    val keyboardShown by rememberUpdatedState(imeVisible)
    val editorFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 키보드가 내려가면 패널도 닫는다. 다만 T 로 막 켜서 키보드를 올리는 중인 프레임에는
    // 아직 imeVisible=false 라 그때 끄면 토글이 헛돈다 — 켜는 순간은 건너뛴다 (#722).
    LaunchedEffect(imeVisible) {
        if (imeVisible || !showTextStyleToolbar) return@LaunchedEffect
        // 키보드가 올라올 시간을 준 뒤에도 여전히 없으면 그때 닫는다.
        delay(KEYBOARD_SETTLE_MS)
        if (!keyboardShown) showTextStyleToolbar = false
    }

    fun keepEditorFocus(action: () -> Unit) {
        action()
        runCatching { editorFocusRequester.requestFocus() }
    }

    // 선택된 미디어 URI 를 HTML 태그로 감싸 에디터에 append. compose-richeditor 의 setHtml 이
    // <img>·<a href> 같은 표준 태그를 파싱해 rich span 으로 변환한다.
    // 음성/파일은 업로드 미지원 (raw content:// URI 를 그대로 href 로 사용).
    fun appendMediaToEditor(
        uri: Uri?,
        asImage: Boolean,
    ) {
        if (uri == null) return
        val html =
            if (asImage) "<img src=\"$uri\" />" else "<a href=\"$uri\">$uri</a>"
        keepEditorFocus { state.setHtml(state.toHtml() + html) }
    }

    val imageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            val uploader = onImagePicked
            if (uploader == null) {
                appendMediaToEditor(uri, asImage = true)
            } else {
                // 업로드 완료 후 영구 URL 로 삽입 — 로컬 URI 는 다른 기기/수신자에게 렌더되지 않는다.
                scope.launch {
                    val uploadedUrl = uploader(uri.toString())
                    if (uploadedUrl != null) {
                        keepEditorFocus { state.setHtml(state.toHtml() + "<img src=\"$uploadedUrl\" />") }
                    }
                }
            }
        }
    val voiceLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            appendMediaToEditor(uri, asImage = false)
        }
    val fileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            appendMediaToEditor(uri, asImage = false)
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
            onTextStyleClick = {
                // 키보드가 없으면 패널이 렌더되지 않으므로 포커스와 키보드를 먼저 되살린다.
                // 종전에는 T 를 눌러도 토글만 뒤집혔다가 imeVisible=false 라 즉시 초기화돼
                // 아무 일도 일어나지 않았다 (#722).
                if (showTextStyleToolbar) {
                    showTextStyleToolbar = false
                } else {
                    showTextStyleToolbar = true
                    runCatching { editorFocusRequester.requestFocus() }
                    keyboardController?.show()
                }
            },
            onAlignChange = { align ->
                keepEditorFocus { state.addParagraphStyle(ParagraphStyle(textAlign = align)) }
            },
            onLinkClick = { sheet = KeyboardSheet.MediaSelect },
            onSaveDraftClick = onSaveDraftClick,
            onDraftCountClick = onDraftCountClick,
            draftCount = draftCount,
        )
    }

    when (sheet) {
        KeyboardSheet.None -> {
            Unit
        }

        KeyboardSheet.MediaSelect -> {
            MediaSelectBottomSheet(
                onDismiss = { sheet = KeyboardSheet.None },
                onImageClick = {
                    sheet = KeyboardSheet.None
                    imageLauncher.launch("image/*")
                },
                onVoiceClick = {
                    sheet = KeyboardSheet.None
                    voiceLauncher.launch("audio/*")
                },
                onFileClick = {
                    sheet = KeyboardSheet.None
                    fileLauncher.launch("*/*")
                },
                onLinkClick = { sheet = KeyboardSheet.LinkAdd },
            )
        }

        KeyboardSheet.LinkAdd -> {
            LinkBottomSheet(
                onDismiss = { sheet = KeyboardSheet.None },
                onConfirm = { url ->
                    // 검증을 통과한 값이라도 속성에 넣기 전에 이스케이프한다 — 따옴표나
                    // 꺾쇠가 섞이면 a 태그를 깨고 나올 수 있다 (#722).
                    val safeUrl = url.trim().escapeHtml()
                    keepEditorFocus {
                        state.setHtml(state.toHtml() + "<a href=\"$safeUrl\">$safeUrl</a>")
                    }
                    sheet = KeyboardSheet.None
                },
            )
        }
    }
}

/** [WriteTextField] 의 키보드 영역에서 토글되는 바텀시트 상태. 한 번에 하나만 표시한다. */
private sealed interface KeyboardSheet {
    data object None : KeyboardSheet

    data object MediaSelect : KeyboardSheet

    data object LinkAdd : KeyboardSheet
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

/** T 로 키보드를 올릴 때 IME 가 올라오기를 기다리는 시간 (#722). */
private const val KEYBOARD_SETTLE_MS = 300L

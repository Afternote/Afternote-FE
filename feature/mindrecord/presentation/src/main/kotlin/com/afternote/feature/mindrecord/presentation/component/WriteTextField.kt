package com.afternote.feature.mindrecord.presentation.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
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
import com.afternote.feature.mindrecord.presentation.util.mediaDisplayName
import com.afternote.feature.mindrecord.presentation.util.mediaImageSize
import com.afternote.feature.mindrecord.presentation.util.toUploadedFileKey
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
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
    draftCount: Int? = null,
    /**
     * 갤러리에서 고른 이미지를 서버에 업로드하고 URL 을 반환하는 업로더
     * (`POST /files/presigned-url` → S3 PUT). 업로드 실패(반환값 null) 시 본문에 삽입하지 않는다.
     *
     * **null 을 넘기지 않는다.** 업로더가 없으면 로컬 `content://` URI 가 본문과 저장 데이터에
     * 그대로 들어가는데, 그 주소는 다른 기기·수신자에게 아무 의미가 없다 (#731).
     */
    onImagePicked: (suspend (uriString: String) -> String?)? = null,
    /**
     * 음성·파일 첨부용 업로더. [onImagePicked] 와 같은 경로를 쓰지만 삽입 형태가 다르다 —
     * 이미지는 `<img>`, 나머지는 파일명을 텍스트로 하는 `<a href>` 다 (#731).
     */
    onMediaPicked: (suspend (uriString: String) -> String?)? = null,
) {
    val context = LocalContext.current
    // 업로드 실패를 조용히 삼키지 않는다 — 종전에는 실패하면 아무 일도 일어나지 않았다 (#731).
    // 문자열이 아니라 리소스 ID 로 든다. LocalContext.current.getString 은 Compose 가
    // 리소스 변경(로케일·설정)에 재구성으로 반응하지 못하는 경로라 lint 가 막는다 (#731 리뷰).
    var mediaError by remember { mutableStateOf<MediaErrorMessage?>(null) }
    // 이번 작성에서 붙인 첨부. 에디터가 `<img>` 를 그리지 못해 사진이 대체 문자로만 보이므로,
    // 무엇을 붙였는지 여기서 이름으로 확인한다 (#731).
    val attachments = remember { mutableStateListOf<String>() }
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
    val editorFocusRequester = remember { FocusRequester() }

    LaunchedEffect(imeVisible) {
        if (!imeVisible) showTextStyleToolbar = false
    }

    fun keepEditorFocus(action: () -> Unit) {
        action()
        runCatching { editorFocusRequester.requestFocus() }
    }

    /**
     * 고른 미디어를 업로드한 뒤에만 본문에 넣는다.
     *
     * 업로더가 없거나 업로드가 실패하면 **아무것도 넣지 않고** 사유를 남긴다. 종전에는
     * 로컬 `content://` URI 를 그대로 `href` 와 링크 텍스트로 써서, 다른 기기에서 해석할 수
     * 없는 주소가 사용자 본문과 저장 데이터에 남았다 (#731).
     *
     * 이미지는 `<img>`, 음성·파일은 **파일명을 텍스트로 하는** `<a href>` 로 넣는다. 종전에는
     * 링크 텍스트까지 `content://…` 라 무엇을 첨부했는지 알아볼 수 없었다.
     */
    fun attachMedia(
        uri: Uri?,
        asImage: Boolean,
    ) {
        if (uri == null) return
        val uploader = if (asImage) onImagePicked else onMediaPicked
        if (uploader == null) {
            mediaError = MediaErrorMessage(R.string.mindrecord_write_media_upload_unavailable)
            return
        }
        val displayName = context.mediaDisplayName(uri)
        scope.launch {
            mediaError = null
            val uploadedUrl = uploader(uri.toString())
            if (uploadedUrl == null) {
                mediaError = MediaErrorMessage(R.string.mindrecord_write_media_upload_failed, displayName)
                return@launch
            }
            // 본문에 넣는 값은 업로드 URL 이 아니라 **fileKey** 다. 서버가 본문의 미디어
            // 참조를 훑어 staging → permanent 로 옮기고 전체 URL 로 재작성하는데, 전체 URL 을
            // 넣으면 그 앞에 호스트를 한 번 더 붙여 접근 불가한 주소가 저장된다 (#549·#731).
            // `img src` 와 `a href` 에 같은 규칙이 적용되는 것을 실서버로 확인했다.
            val fileKey = uploadedUrl.toUploadedFileKey()
            val html =
                if (asImage) {
                    // 크기를 비워 두면 직렬화 때 width="0" height="0" 이 붙어 어디서도 보이지
                    // 않는다. 다만 고정값을 박으면 세로 사진이 본문에 4:3 으로 박제되므로
                    // 원본 비율로 높이를 계산한다 (#731 리뷰).
                    val (imageWidth, imageHeight) = context.mediaImageSize(uri, MEDIA_IMAGE_WIDTH_PX)
                    "<img src=\"$fileKey\" alt=\"$displayName\" width=\"$imageWidth\" " +
                        "height=\"$imageHeight\" />"
                } else {
                    "<a href=\"$fileKey\">$displayName</a>"
                }
            keepEditorFocus { state.setHtml(state.toHtml() + html) }
            attachments += displayName
        }
    }

    val imageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            attachMedia(uri, asImage = true)
        }
    val voiceLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            attachMedia(uri, asImage = false)
        }
    val fileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            attachMedia(uri, asImage = false)
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
            // 첨부 목록과 오류 문구는 같은 자리를 두고 다투면 안 된다 — 종전에는 정렬·패딩이
            // 같아 둘 다 표시되는 순간 정확히 겹쳐 그려졌다. 한 번이라도 첨부에 성공한 뒤의
            // 실패는 전부 그 상태였다 (#731 리뷰).
            if (attachments.isNotEmpty() || mediaError != null) {
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (attachments.isNotEmpty()) {
                        AttachmentSummary(names = attachments)
                    }
                    mediaError?.let { message ->
                        Text(
                            text = message.resolve(),
                            color = AfternoteDesign.colors.gray6,
                        )
                    }
                }
            }
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
                    keepEditorFocus {
                        state.setHtml(state.toHtml() + "<a href=\"$url\">$url</a>")
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

/** 에디터 본문 이미지의 가로 기준 크기(px). 높이는 원본 비율로 계산한다 (#731). */
private const val MEDIA_IMAGE_WIDTH_PX = 320

/**
 * 이번 작성에서 붙인 첨부 목록.
 *
 * compose-richeditor 1.0.0 의 HTML 파서는 `<img>` 를 다루지 않아, 본문에 사진을 넣어도
 * 에디터에는 자리 문자만 보인다 (AAR 어느 클래스에도 `img` 상수가 없다). 저장되는
 * 본문에는 정상적으로 들어가지만 작성 중에는 확인할 방법이 없어, 무엇을 붙였는지
 * 이름으로라도 보여 준다 (#731).
 */
@Composable
private fun AttachmentSummary(
    names: List<String>,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.mindrecord_write_media_attached, names.joinToString(", ")),
        color = AfternoteDesign.colors.gray6,
        modifier = modifier,
    )
}

/**
 * 첨부 실패 안내 — **표시 시점에** 문자열로 푼다.
 *
 * `Context.getString` 을 상태에 담으면 로케일·설정이 바뀌어도 문구가 따라가지 않고,
 * Compose lint(`LocalContextGetResourceValueCall`)도 그 경로를 막는다 (#731 리뷰).
 */
private data class MediaErrorMessage(
    @param:StringRes val resId: Int,
    val formatArg: String? = null,
) {
    @Composable
    fun resolve(): String = formatArg?.let { stringResource(resId, it) } ?: stringResource(resId)
}

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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.sheet.MediaSelectBottomSheet
import com.afternote.core.ui.sheet.MediaSheetItem
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.TextStyleState
import com.afternote.feature.mindrecord.presentation.model.TextStyleType
import com.afternote.feature.mindrecord.presentation.util.escapeHtml
import com.afternote.feature.mindrecord.presentation.util.mediaDisplayName
import com.afternote.feature.mindrecord.presentation.util.mediaImageSize
import com.afternote.feature.mindrecord.presentation.util.toBodyLinkHrefOrNull
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import kotlinx.coroutines.launch
import com.afternote.core.ui.R as CoreUiR

/**
 * HTML 직렬화 가능한 리치 텍스트 입력 영역.
 *
 * - 외부에서 받은 [value] 는 **비어 있지 않은 값으로 바뀔 때마다** 에디터에 실린다. 화면이
 *   먼저 뜨고 값이 나중에 도착하는 경로(이어쓰기·수정 프리필)가 있어서다. 에디터가 이미 들고
 *   있는 HTML 과 같으면 다시 쓰지 않으므로 사용자가 방금 친 글자는 되돌아가지 않는다.
 *
 *   **null·빈 문자열은 무시한다** — 지우는 신호가 아니라 «아직 안 왔다» 로 본다. 그래서
 *   `<p>A</p>` 뒤에 `""` 가 와도 에디터에는 A 가 남는다. 이 컴포넌트로 본문을 비우는 경로는
 *   없고, 있다면 재생성이 맞다 (#1018).
 * - 사용자 입력이 발생하면 [onValueChange] 로 직렬화된 HTML 문자열을 emit 한다.
 *   서버 페이로드의 `content` 필드에 그대로 실어 보내면 된다.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WriteTextField(
    modifier: Modifier = Modifier,
    value: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    onSaveDraftClick: () -> Unit,
    onDraftCountClick: () -> Unit,
    draftCount: Int? = null,
    /**
     * 갤러리에서 고른 이미지를 서버에 업로드하고 URL 을 반환하는 업로더
     * (`POST /files/presigned-url` → S3 PUT). 업로드 실패(반환값 null) 시 본문에 삽입하지 않는다.
     *
     * **null 을 넘기지 않는다.** 업로더가 없으면 로컬 `content://` URI 가 본문과 저장 데이터에
     * 그대로 들어가는데, 그 주소는 다른 기기·수신자에게 아무 의미가 없다 (#731).
     *
     * **돌려주는 값은 업로드된 전체 URL 이다** — 그대로 본문 `src`·`href` 에 들어가고, 서버가
     * 기대하는 `fileKey` 로 바꾸는 것은 제출 직전 `toWireContent` 한 곳뿐이다 (#1125).
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

    // 외부 value 는 «화면이 먼저 뜨고 값이 나중에 도착하는» 경로(이어쓰기·수정 프리필)가
    // 있어 첫 컴포지션 1회만 시드하면 본문이 실리지 않는다. 종전에는 호출부 둘이 각자
    // key(draftLoaded) 로 컴포넌트를 통째로 재생성해 우회했는데, 재마운트는 진행 중인
    // 업로드 스코프·피커 콜백·IME 조합 상태를 함께 날린다 (#1018).
    //
    // 에디터가 이미 들고 있는 HTML 과 같으면 다시 쓰지 않는다 — 사용자가 방금 친 글자를
    // 되돌리지 않기 위해서다. onValueChange 로 올라간 값이 그대로 되돌아오는 것이 정상
    // 경로이므로, 이 가드가 없으면 매 입력마다 커서가 튄다.
    LaunchedEffect(value) {
        if (!value.isNullOrEmpty() && value != state.toHtml()) state.setHtml(value)
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
            textAlign = state.currentParagraphStyle.textAlign.orStart(),
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
            // **에디터에는 업로드 URL 을 넣는다.** 서버에 나가는 값은 fileKey 여야 하지만
            // (전체 URL 을 보내면 그 앞에 호스트를 한 번 더 붙여 접근 불가한 주소가 저장된다 —
            // #549·#731 실서버 실측), 그 변환은 제출 직전 `toWireContent` 한 곳에서 한다.
            //
            // 종전에는 여기서 URL 의 스킴·호스트를 떼어 키를 역산했다. 변환 지점이 둘이 되고,
            // CDN 이 경로 프리픽스를 붙이면 조용히 틀린 키가 됐다. 이제 ViewModel 이 presigned
            // 응답의 `fileKey` 를 URL 과 짝지어 들고 있다가 제출 때 그대로 치환한다 (#1125).
            val html =
                if (asImage) {
                    // 크기를 비워 두면 직렬화 때 width="0" height="0" 이 붙어 어디서도 보이지
                    // 않는다. 다만 고정값을 박으면 세로 사진이 본문에 4:3 으로 박제되므로
                    // 원본 비율로 높이를 계산한다 (#731 리뷰).
                    val (imageWidth, imageHeight) = context.mediaImageSize(uri, MEDIA_IMAGE_WIDTH_PX)
                    // `displayName` 은 파일을 넘긴 앱(content provider)이 정하는 값이라 따옴표가
                    // 들어올 수 있다. 이스케이프하지 않으면 `alt` 가 그 자리에서 닫히고 뒤따르는
                    // `width`/`height` 가 값으로 먹혀 **이미지가 저장된 본문에서 사라진다** (#1067 리뷰).
                    "<img src=\"$uploadedUrl\" alt=\"${displayName.escapeHtml()}\" width=\"$imageWidth\" " +
                        "height=\"$imageHeight\" />"
                } else {
                    // 링크 텍스트도 같은 출처다 — 이름 속 태그가 마크업으로 살아난다.
                    "<a href=\"$uploadedUrl\">${displayName.escapeHtml()}</a>"
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

    // 본문 편집기의 접근 가능한 이름. 눈에 보이는 안내 문구는 아래에서 **형제 노드**로 그려서
    // 편집기 자신의 semantics 에는 잡히지 않는다 — 그대로 두면 스크린리더가 화면의 대부분을
    // 차지하는 이 타깃을 이름 없이 읽는다 (#1179 리뷰의 후보 전량 스캔에서 드러났다).
    // 보이는 문구와 읽히는 이름을 같은 문자열로 묶는다.
    val editorLabel = stringResource(R.string.mindrecord_write_field_placeholder)

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
                        .semantics { contentDescription = editorLabel }
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
                    // 같은 문자열이 편집기의 이름으로 이미 실려 있다. 이 형제 노드를 접근성
                    // 트리에 남기면 비어 있을 때 같은 문장을 두 번 읽는다 (#1179 리뷰).
                    // Material 의 TextField 도 placeholder 를 필드 노드에 합치고 따로 짚지 않는다.
                    modifier = Modifier.padding(16.dp).clearAndSetSemantics {},
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
                // **하단 툴바와 같은 배선이다.** 스타일 툴바가 열리면 하단 툴바를 덮으므로
                // 같은 두 affordance(링크·«T»)를 그대로 갖는다. 종전에는 두 인자를 넘기지
                // 않아 no-op 디폴트가 먹었고, **눌러도 아무 일이 없는 버튼 두 개**였다 (#1540).
                onLinkClick = { sheet = KeyboardSheet.MediaSelect },
                onTypeClick = { showTextStyleToolbar = !showTextStyleToolbar },
            )
        }

        BottomToolbar(
            modifier = Modifier.imePadding(),
            onTextStyleClick = { showTextStyleToolbar = !showTextStyleToolbar },
            // 스타일 패널과 같은 값을 본다 — 두 툴바의 정렬 선택 상태가 갈리면 안 된다 (#1179).
            textAlign = styleState.textAlign,
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
            // core:ui 정본을 쓴다. 종전에는 같은 시트를 모듈마다 다시 적어 4벌이었고,
            // 그 사본들이 시안(4327:72281) 지오메트리에서 조금씩 어긋나 있었다 (#642 · #1615).
            MediaSelectBottomSheet(
                onDismiss = { sheet = KeyboardSheet.None },
                items =
                    listOf(
                        MediaSheetItem(
                            iconRes = CoreUiR.drawable.core_ui_ic_image,
                            label = stringResource(CoreUiR.string.core_ui_media_sheet_image),
                            onClick = {
                                sheet = KeyboardSheet.None
                                imageLauncher.launch("image/*")
                            },
                        ),
                        MediaSheetItem(
                            iconRes = CoreUiR.drawable.core_ui_ic_mic,
                            label = stringResource(CoreUiR.string.core_ui_media_sheet_voice),
                            onClick = {
                                sheet = KeyboardSheet.None
                                voiceLauncher.launch("audio/*")
                            },
                        ),
                        MediaSheetItem(
                            iconRes = CoreUiR.drawable.core_ui_ic_file,
                            label = stringResource(CoreUiR.string.core_ui_media_sheet_file),
                            onClick = {
                                sheet = KeyboardSheet.None
                                fileLauncher.launch("*/*")
                            },
                        ),
                        MediaSheetItem(
                            iconRes = CoreUiR.drawable.core_ui_ic_link,
                            label = stringResource(CoreUiR.string.core_ui_media_sheet_link),
                            onClick = { sheet = KeyboardSheet.LinkAdd },
                        ),
                    ),
            )
        }

        KeyboardSheet.LinkAdd -> {
            LinkBottomSheet(
                onDismiss = { sheet = KeyboardSheet.None },
                // 검증 없이 이어붙이면 `javascript:` 가 그대로 저장되고, 따옴표 하나로 속성이 닫힌다.
                // 본문은 수신자가 나중에 열람하는 값이라 저장되는 순간 남에게 실린다 (#1067).
                //
                // 거절(false)은 시트가 받아 사유를 띄운다 — 조용히 무시하면 사용자는 «완료를 눌렀는데
                // 안 들어갔다» 만 본다. 안내를 걷는 시점(입력을 고치는 순간)도 입력 상태를 가진 시트가
                // 안다 (#1067 리뷰).
                onConfirm = { url ->
                    val href = url.toBodyLinkHrefOrNull()
                    if (href == null) {
                        false
                    } else {
                        keepEditorFocus {
                            // 링크 텍스트는 사용자가 적은 원문을 보여 준다 — punycode 로 바뀐 호스트를
                            // 보여 주면 자기가 넣은 주소를 못 알아본다. 표시용도 이스케이프한다.
                            state.setHtml(state.toHtml() + "<a href=\"$href\">${url.trim().escapeHtml()}</a>")
                        }
                        sheet = KeyboardSheet.None
                        true
                    }
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
        WriteTextField(
            onDraftCountClick = {},
            onSaveDraftClick = {},
        )
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

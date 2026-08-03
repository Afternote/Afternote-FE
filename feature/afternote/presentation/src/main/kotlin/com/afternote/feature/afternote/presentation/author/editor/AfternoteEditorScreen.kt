package com.afternote.feature.afternote.presentation.author.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import com.afternote.core.ui.modifierextention.addFocusCleaner
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.author.editor.state.rememberAfternoteEditorState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Duration.Companion.milliseconds

private const val EDITOR_MESSAGES_SNAPSHOT_DEBOUNCE_MS = 1_000L

/**
 * 애프터노트 수정/작성 화면
 *
 * 피그마 디자인 기반:
 * - 헤더 (뒤로가기, 타이틀, 등록 버튼)
 * - 종류 선택 드롭다운
 * - 서비스명 선택 드롭다운
 * - 계정 정보 입력 (아이디, 비밀번호)
 * - 계정 처리 방법 선택 (라디오 버튼)
 * - 처리 방법 리스트 (체크박스)
 * - 남기실 말씀 (멀티라인 텍스트 필드; Process Death 대비 [snapshotFlow] + debounce로 폼 동기화)
 *
 * 추모 곡 목록은 [com.afternote.feature.afternote.presentation.AfternoteHostViewModel.playlistSongs] SSOT의 스냅샷을
 * [liveSongs]로 전달받아 표시한다 (Compose 상태 홀더에 직접 의존하지 않는다).
 */
@OptIn(FlowPreview::class)
@Composable
fun AfternoteEditorScreen(
    form: EditorFormState,
    modifier: Modifier = Modifier,
    callbacks: AfternoteEditorScreenCallbacks = AfternoteEditorScreenCallbacks(),
    state: AfternoteEditorState = rememberAfternoteEditorState(),
    liveSongs: List<Song> = emptyList(),
    saveError: String? = null,
    thumbnailUploadFailed: Boolean = false,
    isPrefillLoading: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val thumbnailUploadFailedMessage = stringResource(R.string.afternote_editor_thumbnail_upload_failed)

    // 화면 재진입 시 폼 SSOT의 leaveMessageBlocks를 휘발성 SnapshotStateList<EditorMessage>에 한 번 동기화한다.
    // (TextFieldState는 rememberSaveable로 복원되지만 EditorMessage SnapshotStateList는 비저장 상태라 폼에서 재구성한다.)
    LaunchedEffect(state) {
        state.syncEditorMessagesFromForm(form.leaveMessageBlocks)
    }

    LaunchedEffect(form.leaveMessageBlocksRestoreGeneration) {
        if (form.leaveMessageBlocksRestoreGeneration != 0L) {
            state.syncEditorMessagesFromForm(form.leaveMessageBlocks)
        }
    }

    // 타이핑 자동 저장: 각 블록의 TextFieldState(UI 소유)를 snapshotFlow 로 관찰해 순수 문자열
    // 스냅샷으로 변환하고, 1s 디바운스로 묶어 폼 SSOT 에 반영한다. key=size 라 블록 추가/삭제 시
    // 재시작해 새 블록 상태도 관찰에 편입. 디바운스 창 안의 이탈 손실은 아래 DisposableEffect 가 맡는다.
    LaunchedEffect(state.editorMessages.size) {
        snapshotFlow {
            state.editorMessages.map { msg ->
                EditorMessageTextBlock(
                    title = msg.titleState.text.toString(),
                    body = msg.contentState.text.toString(),
                )
            }
        }.distinctUntilChanged()
            .debounce(EDITOR_MESSAGES_SNAPSHOT_DEBOUNCE_MS.milliseconds)
            .collect { blocks ->
                state.persistEditorMessagesFromTyping(blocks)
            }
    }

    // 화면 이탈 시 디바운스 윈도우(1s) 안의 미반영 타이핑이 폼 SSOT에 도달하지 못하는 손실을 방지한다.
    DisposableEffect(state) {
        onDispose {
            val blocks =
                state.editorMessages.map { msg ->
                    EditorMessageTextBlock(
                        title = msg.titleState.text.toString(),
                        body = msg.contentState.text.toString(),
                    )
                }
            state.persistEditorMessagesFromTyping(blocks)
        }
    }

    LaunchedEffect(saveError) {
        saveError?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                withDismissAction = true,
            )
        }
    }

    LaunchedEffect(thumbnailUploadFailed) {
        if (thumbnailUploadFailed) {
            snackbarHostState.showSnackbar(
                message = thumbnailUploadFailedMessage,
                withDismissAction = true,
            )
            callbacks.onThumbnailUploadErrorConsumed()
        }
    }

    LaunchedEffect(liveSongs) {
        state.syncMemorialPlaylistSongs(liveSongs)
    }

    val memorialPhotoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            state.onMemorialPhotoSelected(uri)
        }
    val memorialVideoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            state.onFuneralVideoSelected(uri)
        }

    // 작성 도중 이탈 가드: 진입 시점 스냅샷 대비 변경이 있으면 뒤로가기 시 이탈 확인 팝업을 띄운다.
    // 입력이 debounce 로 휘발성 폼 상태에만 반영되어 pop 시 소실되기 때문. '내용 존재'가 아니라 '변경' 기준인
    // 이유는 수정 모드(prefill)에서 무변경 이탈에도 매번 경고하게 되어서다. 스냅샷은 프리필 적용 완료
    // (isPrefillLoading=false 전환) 후 1회 캡처하고, 하위 화면 왕복·프로세스 복원에도 유지되도록 saveable 로 둔다.
    var showExitConfirm by rememberSaveable { mutableStateOf(false) }
    var baselineContentSignature by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(isPrefillLoading) {
        if (!isPrefillLoading && baselineContentSignature == null) {
            baselineContentSignature = editorContentSignature(form, state)
        }
    }
    val hasUnsavedChanges by remember(form, baselineContentSignature) {
        derivedStateOf {
            baselineContentSignature != null &&
                editorContentSignature(form, state) != baselineContentSignature
        }
    }
    val onBackAttempt: () -> Unit = {
        focusManager.clearFocus()
        if (hasUnsavedChanges) {
            showExitConfirm = true
        } else {
            callbacks.onBackClick()
        }
    }

    // 변경이 없을 때는 시스템 기본 뒤로가기를 유지한다 (predictive back 애니메이션 보존).
    BackHandler(enabled = hasUnsavedChanges) { onBackAttempt() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.afternote_editor_screen_title),
                onBackClick = onBackAttempt,
                actions = {
                    Text(
                        text = stringResource(R.string.afternote_editor_submit),
                        style = AfternoteDesign.typography.bodySmallB,
                        color = AfternoteDesign.colors.gray9,
                        modifier =
                            Modifier.clickable(
                                onClick = {
                                    focusManager.clearFocus()
                                    callbacks.onRegisterClick()
                                },
                            ),
                    )
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .addFocusCleaner(focusManager),
        ) {
            EditorContent(
                state = state,
                form = form,
                liveSongs = liveSongs,
                isPrefillLoading = isPrefillLoading,
                onNavigateToAddSong = callbacks.onNavigateToAddSong,
                onNavigateToSelectReceiver = callbacks.onNavigateToSelectReceiver,
                onPhotoAddClick = {
                    memorialPhotoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onVideoAddClick = {
                    memorialVideoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                    )
                },
                onThumbnailBytesReady = callbacks.onThumbnailBytesReady,
                onThumbnailExtractionFailed = callbacks.onThumbnailExtractionFailed,
            )

            AfternoteEditorDialogs(state = state)

            if (showExitConfirm) {
                Popup(
                    type = PopupType.Variant2,
                    message = stringResource(R.string.afternote_editor_exit_confirm_message),
                    onConfirm = {
                        showExitConfirm = false
                        callbacks.onBackClick()
                    },
                    onDismiss = { showExitConfirm = false },
                    confirmText = stringResource(R.string.afternote_editor_exit_confirm_confirm),
                    dismissText = stringResource(R.string.afternote_editor_exit_confirm_cancel),
                )
            }
        }
    }
}

/**
 * 사용자가 에디터에서 편집할 수 있는 값 전부를 한 줄 문자열로 직렬화한 상태 지문.
 * 진입 직후 값을 기준으로 저장해 두고, 뒤로가기 시점 값과 문자열 비교가 다르면 "작성 내용이
 * 바뀌었다"로 판단해 이탈 확인 팝업을 띄운다.
 *
 * 폼은 필드를 골라 담지 않고 통째로 직렬화하되, 판정에서 뺄 것만 [EditorFormState.copy]로
 * 기본값 치환한다 — 폼에 필드가 새로 생기면 자동으로 판정에 포함되므로(빠짐 불가능),
 * 유지보수 대상은 아래 제외 목록뿐이다. 제외를 빠뜨리면 데이터 소실이 아니라
 * "팝업이 한 번 더 뜨는" 눈에 보이는 오탐으로 드러난다.
 */
internal fun editorContentSignature(
    form: EditorFormState,
    state: AfternoteEditorState,
): String {
    val comparableForm =
        form.copy(
            // 식별자·자동 파생값 — 사용자 편집이 아니므로 판정 제외.
            loadedItemId = null,
            memorialThumbnailUrl = null,
            leaveMessageBlocksRestoreGeneration = 0L,
            // 남기실 말씀은 debounce 전 라이브 입력(state.editorMessages)으로 판정하므로 스냅샷은 제외.
            leaveMessageBlocks = emptyList(),
            // 카테고리 구경 자체는 변경으로 치지 않는다 — 전환이 selectedService(null)·processingMethods(빈)를
            // 함께 리셋하므로(#468 정책) 카테고리만 중립화하면 구경 왕복은 진입 상태와 같아진다.
            // selectedService 는 그대로 비교한다: null=미선택이라 서비스 선택 자체가 변경으로 잡힌다.
            selectedCategory = EditorCategory.SOCIAL,
        )
    return listOf(
        comparableForm.toString(),
        state.idState.text,
        state.passwordState.text,
        state.editorMessages.map { "${it.titleState.text}\u0001${it.contentState.text}" },
    ).joinToString("\u0002")
}

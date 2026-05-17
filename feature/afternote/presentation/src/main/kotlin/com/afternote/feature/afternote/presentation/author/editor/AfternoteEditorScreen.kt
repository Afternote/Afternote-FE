package com.afternote.feature.afternote.presentation.author.editor

import android.net.Uri
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import com.afternote.core.ui.modifierextention.addFocusCleaner
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.author.editor.state.rememberAfternoteEditorState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

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
 * [graphSongs]로 전달받아 표시한다 (Compose 상태 홀더에 직접 의존하지 않는다).
 */
@OptIn(FlowPreview::class)
@Composable
fun AfternoteEditorScreen(
    form: EditorFormState,
    modifier: Modifier = Modifier,
    callbacks: AfternoteEditorScreenCallbacks = AfternoteEditorScreenCallbacks(),
    state: AfternoteEditorState = rememberAfternoteEditorState(),
    graphSongs: List<Song> = emptyList(),
    saveError: AfternoteEditorSaveError? = null,
    isPrefillLoading: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 화면 재진입 시 폼 SSOT의 messageBlocks를 휘발성 SnapshotStateList<EditorMessage>에 한 번 동기화한다.
    // (TextFieldState는 rememberSaveable로 복원되지만 EditorMessage SnapshotStateList는 비저장 상태라 폼에서 재구성한다.)
    LaunchedEffect(state) {
        state.syncEditorMessagesFromForm(form.messageBlocks)
    }

    LaunchedEffect(form.messageBlocksRestoreGeneration) {
        if (form.messageBlocksRestoreGeneration != 0L) {
            state.syncEditorMessagesFromForm(form.messageBlocks)
        }
    }

    LaunchedEffect(state.editorMessages.size) {
        snapshotFlow {
            state.editorMessages.map { msg ->
                EditorMessageTextBlock(
                    title = msg.titleState.text.toString(),
                    body = msg.contentState.text.toString(),
                )
            }
        }.distinctUntilChanged()
            .debounce(EDITOR_MESSAGES_SNAPSHOT_DEBOUNCE_MS)
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
        saveError?.let { err ->
            snackbarHostState.showSnackbar(
                message = err.message,
                withDismissAction = true,
            )
        }
    }

    LaunchedEffect(graphSongs) {
        state.syncMemorialPlaylistSongs(graphSongs)
    }

    val memorialPhotoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            state.onMemorialPhotoSelected(uri)
        }
    val memorialVideoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            state.onFuneralVideoSelected(uri)
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.afternote_editor_screen_title),
                onBackClick = callbacks.onBackClick,
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
                graphSongs = graphSongs,
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
            )

            AfternoteEditorDialogs(state = state)
        }
    }
}

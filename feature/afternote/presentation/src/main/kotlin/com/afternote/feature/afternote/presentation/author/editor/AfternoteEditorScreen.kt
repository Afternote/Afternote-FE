package com.afternote.feature.afternote.presentation.author.editor

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.addFocusCleaner
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.domain.model.ListItem
import com.afternote.feature.afternote.presentation.author.editor.mapper.editScreenLabelRes
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.LoadFromExistingAccountParams
import com.afternote.feature.afternote.presentation.author.editor.state.LoadFromExistingParams
import com.afternote.feature.afternote.presentation.author.editor.state.LoadFromExistingProcessingParams
import com.afternote.feature.afternote.presentation.author.editor.state.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.state.rememberAfternoteEditorState
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteLightTheme

private const val TAG = "AfternoteEditorScreen"

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
 * - 남기실 말씀 (멀티라인 텍스트 필드)
 */
@Composable
fun AfternoteEditorScreen(
    modifier: Modifier = Modifier,
    callbacks: AfternoteEditorScreenCallbacks = AfternoteEditorScreenCallbacks(),
    state: AfternoteEditorState = rememberAfternoteEditorState(),
    playlistStateHolder: MemorialPlaylistStateHolder? = null,
    initialListItem: ListItem? = null,
    saveError: AfternoteEditorSaveError? = null,
) {
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val editScreenCategoryDisplayString =
        initialListItem?.let { stringResource(it.type.editScreenLabelRes) }

    LaunchedEffect(saveError) {
        saveError?.let { err ->
            snackbarHostState.showSnackbar(
                message = err.message,
                withDismissAction = true,
            )
        }
    }

    LaunchedEffect(initialListItem?.id, editScreenCategoryDisplayString) {
        val item =
            initialListItem ?: run {
                Log.d(TAG, "LaunchedEffect: initialListItem is null, skipping loadFromExisting")
                return@LaunchedEffect
            }
        Log.d(
            TAG,
            "LaunchedEffect: item.id=${item.id}, state.loadedItemId=${state.loadedItemId}, " +
                "needsLoad=${state.loadedItemId != item.id}",
        )
        if (state.loadedItemId != item.id) {
            state.loadFromExisting(
                LoadFromExistingParams(
                    itemId = item.id,
                    serviceName = item.serviceName,
                    categoryDisplayString = editScreenCategoryDisplayString!!,
                    account =
                        LoadFromExistingAccountParams(
                            id = item.account.id,
                            password = item.account.password,
                        ),
                    processing =
                        LoadFromExistingProcessingParams(
                            message = item.processing.message,
                            accountMethodName = item.processing.accountMethod,
                            informationMethodName = item.processing.informationMethod,
                            methods =
                                item.processing.methods.map {
                                    ProcessingMethodItem(
                                        it.id,
                                        it.text,
                                    )
                                },
                            galleryMethods =
                                item.processing.galleryMethods.map {
                                    ProcessingMethodItem(
                                        it.id,
                                        it.text,
                                    )
                                },
                        ),
                ),
            )
        }
    }

    LaunchedEffect(playlistStateHolder) {
        playlistStateHolder?.let { state.setPlaylistStateHolder(it) }
    }

    val songCount by remember {
        playlistStateHolder?.songs?.let {
            derivedStateOf { it.size }
        } ?: derivedStateOf { state.playlistSongCount }
    }

    LaunchedEffect(songCount) {
        if (playlistStateHolder != null) {
            state.updatePlaylistSongCount()
        }
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
                title = "애프터노트 작성하기",
                onBackClick = callbacks.onBackClick,
                actions = {
                    TextButton(
                        onClick = {
                            focusManager.clearFocus()
                            callbacks.onRegisterClick(state.createRegisterPayload())
                        },
                    ) {
                        Text(
                            text = "등록",
                            style = AfternoteDesign.typography.bodySmallB,
                            color = AfternoteDesign.colors.gray9,
                        )
                    }
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
            EditContent(
                state = state,
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
                bottomPadding = paddingValues,
            )

            AfternoteEditorDialogs(state = state)
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=390dp,height=844dp,dpi=420,isRound=false",
)
@Composable
private fun AfternoteEditorScreenPreview() {
    AfternoteLightTheme {
        AfternoteEditorScreen(
            callbacks = AfternoteEditorScreenCallbacks(onBackClick = {}),
        )
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=390dp,height=844dp,dpi=420,isRound=false",
    name = "갤러리 및 파일",
)
@Composable
private fun AfternoteEditorScreenGalleryAndFilePreview() {
    AfternoteLightTheme {
        val state =
            rememberAfternoteEditorState().apply {
                onCategorySelected(EditorCategory.GALLERY.displayLabel)
            }
        AfternoteEditorScreen(
            callbacks = AfternoteEditorScreenCallbacks(onBackClick = {}),
            state = state,
        )
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=390dp,height=844dp,dpi=420,isRound=false",
    name = "추모 가이드라인",
)
@Composable
private fun AfternoteEditorScreenMemorialGuidelinePreview() {
    AfternoteLightTheme {
        val state =
            rememberAfternoteEditorState().apply {
                onCategorySelected(EditorCategory.MEMORIAL.displayLabel)
            }
        AfternoteEditorScreen(
            callbacks = AfternoteEditorScreenCallbacks(onBackClick = {}),
            state = state,
        )
    }
}

package com.afternote.feature.afternote.presentation.author.edit

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.expand.addFocusCleaner
import com.afternote.feature.afternote.domain.model.Item
import com.afternote.feature.afternote.domain.model.ProcessingMethod
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditState
import com.afternote.feature.afternote.presentation.author.edit.model.LoadFromExistingAccountParams
import com.afternote.feature.afternote.presentation.author.edit.model.LoadFromExistingParams
import com.afternote.feature.afternote.presentation.author.edit.model.LoadFromExistingProcessingParams
import com.afternote.feature.afternote.presentation.author.edit.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.edit.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.edit.model.rememberAfternoteEditState
import com.afternote.feature.afternote.presentation.author.edit.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.edit.provider.FakeAfternoteEditDataProvider
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteLightTheme
import com.afternote.feature.afternote.presentation.shared.AfternoteEmbeddedMainBottomBar
import com.afternote.feature.afternote.presentation.shared.AfternoteTopBar
import com.afternote.feature.afternote.presentation.shared.DataProviderLocals
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "AfternoteEditScreen"

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
fun AfternoteEditScreen(
    modifier: Modifier = Modifier,
    callbacks: AfternoteEditScreenCallbacks = AfternoteEditScreenCallbacks(),
    state: AfternoteEditState = rememberAfternoteEditState(),
    playlistStateHolder: MemorialPlaylistStateHolder? = null,
    initialItem: Item? = null,
    saveError: AfternoteEditSaveError? = null,
) {
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saveError) {
        saveError?.let { err ->
            snackbarHostState.showSnackbar(
                message = err.message,
                withDismissAction = true,
            )
        }
    }

    LaunchedEffect(initialItem?.id) {
        val item =
            initialItem ?: run {
                Log.d(TAG, "LaunchedEffect: initialItem is null, skipping loadFromExisting")
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
                    categoryDisplayString = AfternoteItemMapper.categoryStringForEditScreen(item.type),
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AfternoteTopBar(
                title = "애프터노트 작성하기",
                onBackClick = callbacks.onBackClick,
                actionIcon = Icons.Default.Check,
                actionContentDescription = "작성 완료",
                onActionClick = {
                    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                    val date = dateFormat.format(Date())
                    val processingMethods =
                        state.processingMethods.map {
                            ProcessingMethod(it.id, it.text)
                        }
                    val galleryProcessingMethods =
                        state.galleryProcessingMethods.map {
                            ProcessingMethod(it.id, it.text)
                        }
                    callbacks.onRegisterClick(
                        RegisterAfternotePayload(
                            serviceName =
                                if (state.selectedCategory == CATEGORY_MEMORIAL_GUIDELINE) {
                                    CATEGORY_MEMORIAL_GUIDELINE
                                } else {
                                    state.selectedService
                                },
                            date = date,
                            accountId = state.idState.text.toString(),
                            password = state.passwordState.text.toString(),
                            message = state.messageState.text.toString(),
                            accountProcessingMethod = state.selectedProcessingMethod.name,
                            informationProcessingMethod = state.selectedInformationProcessingMethod.name,
                            processingMethods = processingMethods,
                            galleryProcessingMethods = galleryProcessingMethods,
                            atmosphere = state.getAtmosphereForSave(),
                        ),
                    )
                },
            )
        },
        bottomBar = {
            AfternoteEmbeddedMainBottomBar(
                selectedNavTab = state.selectedBottomNavItem,
                onTabClick = { item ->
                    state.onBottomNavItemSelected(item)
                    callbacks.onBottomNavTabSelected(item)
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

            AfternoteEditDialogs(state = state)
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=390dp,height=844dp,dpi=420,isRound=false",
)
@Composable
private fun AfternoteEditScreenPreview() {
    AfternoteLightTheme {
        CompositionLocalProvider(
            DataProviderLocals.LocalAfternoteEditDataProvider provides FakeAfternoteEditDataProvider(),
        ) {
            AfternoteEditScreen(
                callbacks = AfternoteEditScreenCallbacks(onBackClick = {}),
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=390dp,height=844dp,dpi=420,isRound=false",
    name = "갤러리 및 파일",
)
@Composable
private fun AfternoteEditScreenGalleryAndFilePreview() {
    AfternoteLightTheme {
        CompositionLocalProvider(
            DataProviderLocals.LocalAfternoteEditDataProvider provides FakeAfternoteEditDataProvider(),
        ) {
            val state =
                rememberAfternoteEditState().apply {
                    onCategorySelected(CATEGORY_GALLERY_AND_FILE)
                }
            AfternoteEditScreen(
                callbacks = AfternoteEditScreenCallbacks(onBackClick = {}),
                state = state,
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=390dp,height=844dp,dpi=420,isRound=false",
    name = "추모 가이드라인",
)
@Composable
private fun AfternoteEditScreenMemorialGuidelinePreview() {
    AfternoteLightTheme {
        CompositionLocalProvider(
            DataProviderLocals.LocalAfternoteEditDataProvider provides FakeAfternoteEditDataProvider(),
        ) {
            val state =
                rememberAfternoteEditState().apply {
                    onCategorySelected(CATEGORY_MEMORIAL_GUIDELINE)
                }
            AfternoteEditScreen(
                callbacks = AfternoteEditScreenCallbacks(onBackClick = {}),
                state = state,
            )
        }
    }
}

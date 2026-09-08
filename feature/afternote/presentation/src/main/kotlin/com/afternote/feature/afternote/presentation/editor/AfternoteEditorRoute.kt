package com.afternote.feature.afternote.presentation.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.processing.AfternoteProcessingMethodDefaults
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.editor.state.rememberAfternoteEditorState

/**
 * 작성자 에디터 화면: type-safe editor flow + 단방향 이벤트.
 *
 * 홈의 `visibleItems` 스냅샷은 에디터에 전달하지 않는다. 라우트의 `itemId`·`initialType`·`isDraft`를 assisted factory에 전달한다.
 *
 * **수정·이어쓰기 데이터 로드:** [AfternoteEditorViewModel]이 전달받은 식별자와 draft 여부로
 * Repository의 `getDetail` 또는 `getDraftDetail`을 선택한다. 저장된 폼과 복원 표식은
 * [androidx.lifecycle.SavedStateHandle]이 보존하므로 재생성 시 서버 응답이 작성 중 입력을 덮지 않는다.
 */
@Composable
internal fun AfternoteEditorNavigation(
    editViewModel: AfternoteEditorViewModel,
    onNavigateToMemorialPlaylist: () -> Unit,
    onNavigateToSelectReceiver: () -> Unit,
    onPopBackStack: () -> Unit,
    onSaveSuccessNavigateHome: () -> Unit,
) {
    val uiState by editViewModel.uiState.collectAsStateWithLifecycle()
    val state =
        rememberAfternoteEditorState(
            getCurrentForm = editViewModel::currentForm,
            setType = editViewModel::setType,
            setService = editViewModel::setService,
            setMemorialPhoto = editViewModel::setMemorialPhoto,
            removeMemorialPhoto = editViewModel::removeMemorialPhoto,
            setMemorialVideo = editViewModel::setMemorialVideo,
            removeMemorialVideo = editViewModel::removeMemorialVideo,
            setMemorialAudio = editViewModel::setMemorialAudio,
            removeMemorialAudio = editViewModel::removeMemorialAudio,
            addReceiverIfAbsent = editViewModel::addReceiverIfAbsent,
            applyPrefill = editViewModel::applyPrefill,
            setMemorialThumbnail = editViewModel::setMemorialThumbnail,
            deleteReceiver = editViewModel::deleteReceiver,
            replaceReceiversIfEmpty = editViewModel::replaceReceiversIfEmpty,
            addProcessingMethod = editViewModel::addProcessingMethod,
            deleteProcessingMethod = editViewModel::deleteProcessingMethod,
            editProcessingMethod = editViewModel::editProcessingMethod,
        )

    val selectedType = uiState.form.selectedType
    val defaultProcessingMethods =
        AfternoteProcessingMethodDefaults.defaultsFor(selectedType).map { stringResource(it) }
    val isProcessingMethodDefaultsInitializing = remember(selectedType) { mutableStateOf(true) }

    LaunchedEffect(selectedType) {
        editViewModel.initializeProcessingMethodDefaults(
            type = selectedType,
            methods = defaultProcessingMethods,
        )
        isProcessingMethodDefaultsInitializing.value = false
    }

    LaunchedEffect(Unit) { editViewModel.refreshAuthorReceivers() }

    LaunchedEffect(uiState.authorReceivers, editViewModel.isEditing) {
        if (!editViewModel.isEditing) {
            state.replaceReceiversIfEmpty(uiState.authorReceivers)
        }
    }

    // 선택 화면이 위에 쌓이는 동안 이 화면은 컴포지션에서 빠지므로, 복귀할 때마다 다시 돈다.
    LaunchedEffect(Unit) {
        tryApplyReceiverSelection(
            editViewModel,
        )
    }

    LaunchedEffect(uiState.pendingSaveSuccessId) {
        if (uiState.pendingSaveSuccessId != null) {
            onSaveSuccessNavigateHome()
            editViewModel.onSaveSuccessConsumed()
        }
    }
    val pendingThumbnailUrl = uiState.pendingThumbnailUrl
    LaunchedEffect(pendingThumbnailUrl) {
        if (pendingThumbnailUrl != null) {
            state.setMemorialThumbnail(pendingThumbnailUrl)
            editViewModel.onThumbnailUploadedConsumed()
        }
    }
    val pendingPrefill = uiState.pendingPrefill
    LaunchedEffect(pendingPrefill) {
        if (pendingPrefill != null) {
            state.applyFormPrefill(pendingPrefill)
            editViewModel.onPrefillConsumed()
        }
    }

    val errorEvent = uiState.errorEvent
    // 오류 하나는 정확히 한 채널로만 간다 — 검증 실패는 확인 팝업, 그 외 전부는 스낵바.
    val validationMessage: String?
    val snackbarMessage: String?
    when (val error = errorEvent?.error) {
        null -> {
            validationMessage = null
            snackbarMessage = null
        }

        is AfternoteEditorError.Validation -> {
            validationMessage = stringResource(error.messageResId())
            snackbarMessage = null
        }

        else -> {
            validationMessage = null
            snackbarMessage = stringResource(error.messageResId())
        }
    }

    val onRegisterClick =
        remember(editViewModel, state) {
            buildOnRegisterClick(
                editViewModel = editViewModel,
                state = state,
            )
        }
    val saveDraft =
        remember(editViewModel, state) {
            buildOnRegisterClick(
                editViewModel = editViewModel,
                state = state,
                asDraft = true,
            )
        }
    // 임시저장 저장 경로는 다 세웠지만 **결과를 볼 화면이 아직 없다** — 저장하면 홈 목록(발행분만)에서
    // 사라지고, 임시저장 목록(#1792)·이어쓰기 진입(#1791)은 다른 PR 로 빠져 있다. 그때까지 버튼을
    // 그리지 않아 «누르면 사라지는» 상태를 만들지 않는다. 두 배선이 들어오면 이 게이트를 지운다.
    //
    // 발행이 끝난 노트의 편집 화면에서도 그리지 않는다 — 결과가 「등록」과 같은데 검증만 느슨해지는
    // 자리라 버튼이 할 일이 없다 ([AfternoteEditorViewModel.isPublishedEdit]). 신규 작성과 임시저장
    // 이어쓰기에서만 뜬다.
    val onSaveDraftClick = saveDraft.takeIf { !editViewModel.isPublishedEdit }
    // 썸네일 실패는 알리는 것으로 끝내지 않는다 — 영상 재선택 없이 되돌릴 액션을 같은 스낵바에 건다.
    // 어느 오류에 거는지는 오류 자체가 말한다 ([offersMemorialThumbnailRetry]).
    val thumbnailRetryAction =
        if (errorEvent?.error?.offersMemorialThumbnailRetry() == true) {
            EditorSnackbarAction(
                label = stringResource(R.string.afternote_editor_thumbnail_retry),
                onPerform = editViewModel::retryMemorialThumbnail,
            )
        } else {
            null
        }
    AfternoteEditorScreen(
        form = uiState.form,
        onBackClick = onPopBackStack,
        onRegisterClick = onRegisterClick,
        onSaveDraftClick = onSaveDraftClick,
        snackbarMessage = snackbarMessage,
        snackbarAction = thumbnailRetryAction,
        onSnackbarMessageConsumed = {
            errorEvent?.let(editViewModel::onErrorConsumed)
        },
        validationMessage = validationMessage,
        onValidationMessageConsumed = {
            errorEvent?.let(editViewModel::onErrorConsumed)
        },
        content = { snackbarHostState ->
            // prefill 을 못 읽었으면 폼을 세우지 않는다 (#705) — 빈 폼으로 저장되면 서버가 기존 기록을
            // 그 빈 값으로 덮는다. 이 갈래에서는 사유와 재시도만 노출하고 «등록» 도 함께 잠근다.
            if (uiState.isPrefillFailed) {
                EditorPrefillErrorBody(onRetry = editViewModel::retryPrefill)
            } else {
                AfternoteEditorBody(
                    state = state,
                    form = uiState.form,
                    onNavigateToMemorialPlaylist = onNavigateToMemorialPlaylist,
                    onNavigateToSelectReceiver = onNavigateToSelectReceiver,
                    onThumbnailBytesReady = editViewModel::uploadMemorialThumbnail,
                    onThumbnailExtractionFailed = editViewModel::onMemorialThumbnailExtractionFailed,
                    thumbnailRetryToken = uiState.memorialThumbnailRetryToken,
                    onCaptureFailed = editViewModel::onMemorialCaptureLaunchFailed,
                    snackbarHostState = snackbarHostState,
                    isPrefillLoading = uiState.isPrefillLoading,
                    isTypeSelectionEnabled = !editViewModel.isEditing,
                )
            }
        },
        state = state,
        // body skeleton과 별개로, 추천 처리 방법 기본값이 들어오기 전 빈 폼을 이탈 기준선으로 잡지 않는다.
        shouldDeferBaselineCapture =
            shouldDeferEditorBaselineCapture(
                isPrefillLoading = uiState.isPrefillLoading,
                isProcessingMethodDefaultsInitializing = isProcessingMethodDefaultsInitializing.value,
                isPrefillFailed = uiState.isPrefillFailed,
            ),
        snackbarMessageKey = errorEvent,
        // 저장 왕복 중과 prefill 실패 중에는 «등록» 을 잠근다 (#705) — 진행 상태를 화면에 실어
        // 유휴처럼 보이지 않게 하고, 읽지 못한 기록을 빈 폼으로 덮는 저장을 아예 시작하지 않는다.
        isSubmitEnabled =
            isEditorSubmitEnabled(
                isSaving = uiState.isSaving,
                isPrefillFailed = uiState.isPrefillFailed,
                isPrefillLoading = uiState.isPrefillLoading,
            ),
    )
}

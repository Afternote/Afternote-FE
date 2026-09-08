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
 * 홈의 `visibleItems` 스냅샷은 에디터에 전달하지 않는다. 식별은 라우트의 `itemId`·`initialType` 정도로 최소화한다.
 *
 * **수정 진입 데이터 로드:** 상세 화면과 같이 [AfternoteEditorViewModel]의 `init`에서
 * [androidx.lifecycle.SavedStateHandle]의 `itemId`만 보고 Repository `getDetail`을 호출한다 (Compose `LaunchedEffect` 위임 없음).
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

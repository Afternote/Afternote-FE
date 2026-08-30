package com.afternote.feature.afternote.presentation.author.editor

import androidx.annotation.StringRes
import androidx.navigation.NavBackStackEntry
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.EditableMemorialVideo
import com.afternote.feature.afternote.presentation.author.navigation.model.SELECTED_RECEIVER_ID_KEY

// 에디터 조립부가 쓰는 순수 헬퍼들이다. Route 파일에는 조립만 남긴다 (#1514).

@StringRes
internal fun AfternoteEditorError.messageResId(): Int =
    when (this) {
        is AfternoteEditorError.Validation -> {
            reason.messageResId
        }

        AfternoteEditorError.Network,
        AfternoteEditorError.Server,
        -> {
            R.string.afternote_editor_save_failed_generic
        }

        AfternoteEditorError.ReceiverSelectionUnavailable -> {
            R.string.afternote_editor_receiver_selection_unavailable
        }

        AfternoteEditorError.PrefillUnavailable -> {
            R.string.afternote_editor_prefill_unavailable
        }

        is AfternoteEditorError.Upload -> {
            when (target) {
                AfternoteEditorError.Upload.Target.THUMBNAIL -> R.string.afternote_editor_thumbnail_upload_failed
                AfternoteEditorError.Upload.Target.THUMBNAIL_EXTRACT -> R.string.afternote_editor_thumbnail_extract_failed
                AfternoteEditorError.Upload.Target.SAVE_MEDIA -> R.string.afternote_editor_save_failed_generic
            }
        }
    }

/**
 * 이 오류의 스낵바에 «다시 시도» 액션을 거는지 — 추모 영상 썸네일 실패 두 갈래에만 건다 (#1550).
 *
 * 판정은 지금 뜨는 오류 자체로 한다. «재시도 가능» 을 별도 불리언으로 들고 있으면 그 스낵바가 닫힌
 * 뒤에도 상태가 남아, 다음에 뜨는 무관한 스낵바(저장 실패 등)에 썸네일 재시도가 붙는다.
 */
internal fun AfternoteEditorError.offersMemorialThumbnailRetry(): Boolean =
    this is AfternoteEditorError.Upload &&
        when (target) {
            AfternoteEditorError.Upload.Target.THUMBNAIL,
            AfternoteEditorError.Upload.Target.THUMBNAIL_EXTRACT,
            -> true

            AfternoteEditorError.Upload.Target.SAVE_MEDIA -> false
        }

/**
 * 수신자 선택 화면이 남긴 id 를 폼에 반영한다.
 *
 * 목록 로드 실패로 id 를 해석할 수 없으면 [AfternoteEditorViewModel.resolveSelectedReceiver] 가 재조회 후
 * 오류 이벤트를 세운다 — 선택이 조용히 사라지지 않는다 (#1405).
 */
internal suspend fun tryApplyReceiverSelectionFromSavedState(
    backStackEntry: NavBackStackEntry,
    viewModel: AfternoteEditorViewModel,
    state: AfternoteEditorState,
) {
    val id = backStackEntry.savedStateHandle[SELECTED_RECEIVER_ID_KEY] as? Long ?: return
    backStackEntry.savedStateHandle.remove<Long>(SELECTED_RECEIVER_ID_KEY)
    val receiver = viewModel.resolveSelectedReceiver(id) ?: return
    state.addReceiverById(id, receiver.name, receiver.label)
}

internal fun buildOnRegisterClick(
    editViewModel: AfternoteEditorViewModel,
    state: AfternoteEditorState,
): () -> Unit =
    {
        // 폼 스냅샷은 한 번만 읽는다 — 필드마다 다시 읽으면 조립 도중 갱신이 끼어 서로 다른 시점의 값이 섞인다.
        val form = state.currentForm()
        val payload =
            SaveAfternotePayloadBuilder.build(
                form = form,
                messageBlocks = state.currentEditorMessageBlocks(),
                accountId =
                    state.idState.text
                        .toString(),
                password =
                    state.passwordState.text
                        .toString(),
            )
        editViewModel.saveAfternote(
            payload = payload,
            selectedReceiverIds = form.afternoteEditReceivers.map { it.id },
            memorialMedia =
                SaveAfternoteMemorialMedia(
                    memorialVideo = form.memorialVideo ?: EditableMemorialVideo.empty(),
                    memorialPhotoUrl = form.memorialPhotoUrl,
                    pickedMemorialPhotoUri = form.pickedMemorialPhotoUri,
                ),
        )
    }

internal fun shouldDeferEditorBaselineCapture(
    isPrefillLoading: Boolean,
    isProcessingMethodDefaultsInitializing: Boolean,
): Boolean = isPrefillLoading || isProcessingMethodDefaultsInitializing

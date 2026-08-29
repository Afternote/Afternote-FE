package com.afternote.feature.afternote.presentation.author.editor

import androidx.annotation.StringRes
import androidx.navigation.NavBackStackEntry
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
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

        is AfternoteEditorError.Upload -> {
            when (target) {
                AfternoteEditorError.Upload.Target.THUMBNAIL -> R.string.afternote_editor_thumbnail_upload_failed
                AfternoteEditorError.Upload.Target.SAVE_MEDIA -> R.string.afternote_editor_save_failed_generic
            }
        }
    }

internal fun tryApplyReceiverSelectionFromSavedState(
    backStackEntry: NavBackStackEntry,
    viewModel: AfternoteEditorViewModel,
    state: AfternoteEditorState,
) {
    val id = backStackEntry.savedStateHandle[SELECTED_RECEIVER_ID_KEY] as? Long ?: return
    backStackEntry.savedStateHandle.remove<Long>(SELECTED_RECEIVER_ID_KEY)
    // 목록 로드 실패 시 선택이 조용히 소실되는 경로, #1405
    val receiver = viewModel.getReceiverById(id) ?: return
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
                    memorialVideoUrl = form.memorialVideoUrl,
                    memorialThumbnailUrl = form.memorialThumbnailUrl,
                    memorialPhotoUrl = form.memorialPhotoUrl,
                    pickedMemorialPhotoUri = form.pickedMemorialPhotoUri,
                ),
        )
    }

internal fun shouldDeferEditorBaselineCapture(
    isPrefillLoading: Boolean,
    isProcessingMethodDefaultsInitializing: Boolean,
): Boolean = isPrefillLoading || isProcessingMethodDefaultsInitializing

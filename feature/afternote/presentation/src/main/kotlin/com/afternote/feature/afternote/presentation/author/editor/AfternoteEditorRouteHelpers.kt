package com.afternote.feature.afternote.presentation.author.editor

import androidx.annotation.StringRes
import androidx.navigation.NavBackStackEntry
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.navigation.model.SELECTED_RECEIVER_IDS_KEY

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

        is AfternoteEditorError.Upload -> {
            when (target) {
                AfternoteEditorError.Upload.Target.THUMBNAIL -> R.string.afternote_editor_thumbnail_upload_failed
                AfternoteEditorError.Upload.Target.SAVE_MEDIA -> R.string.afternote_editor_save_failed_generic
            }
        }
    }

/**
 * 수신자 선택 화면이 남긴 id 전체를 폼에 반영한다 (#1426).
 *
 * 반환 채널은 [LongArray] 다 — 키가 없으면 선택 화면을 거치지 않은 복귀라 아무것도 하지 않는다.
 * 값이 있으면 그게 곧 확정된 수신자 전체이므로 반영은 [AfternoteEditorViewModel.applySelectedReceivers]
 * 에 맡긴다(«추가» 가 아니라 «교체» 인 이유는 그 KDoc 참고).
 */
internal suspend fun tryApplyReceiverSelectionFromSavedState(
    backStackEntry: NavBackStackEntry,
    viewModel: AfternoteEditorViewModel,
) {
    val selectedIds =
        backStackEntry.savedStateHandle.remove<LongArray>(SELECTED_RECEIVER_IDS_KEY)?.toList() ?: return
    viewModel.applySelectedReceivers(selectedIds)
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

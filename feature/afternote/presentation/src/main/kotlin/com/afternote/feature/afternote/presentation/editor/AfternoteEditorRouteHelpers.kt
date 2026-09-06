package com.afternote.feature.afternote.presentation.editor

import androidx.annotation.StringRes
import androidx.navigation.NavBackStackEntry
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.editor.state.EditableMemorialVideo
import com.afternote.feature.afternote.presentation.navigation.model.SELECTED_RECEIVER_IDS_KEY

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
            R.string.afternote_editor_prefill_load_failed
        }

        AfternoteEditorError.PrefillNotReady -> {
            R.string.afternote_editor_prefill_not_ready
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

/**
 * @param asDraft 임시저장 버튼으로 저장한다 (#808). 조립 과정은 정식 등록과 한 줄도 다르지 않다 —
 *   임시저장은 «덜 담은 폼을 그대로» 보내는 것이지 다른 것을 보내는 게 아니다.
 */
internal fun buildOnRegisterClick(
    editViewModel: AfternoteEditorViewModel,
    state: AfternoteEditorState,
    asDraft: Boolean = false,
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
            asDraft = asDraft,
        )
    }

/**
 * 이탈 확인 기준선(진입 시점 폼 스냅샷) 캡처를 미뤄야 하는지.
 *
 * prefill 실패도 «아직 기준선을 잡을 때가 아니다» 에 포함한다 (#705) — 실패 화면의 빈 폼을 기준선으로
 * 잡아 두면 재시도가 성공해 폼이 채워지는 순간 «사용자가 고친 것» 으로 오인돼 뒤로가기마다 이탈 확인
 * 팝업이 뜬다.
 */
internal fun shouldDeferEditorBaselineCapture(
    isPrefillLoading: Boolean,
    isProcessingMethodDefaultsInitializing: Boolean,
    isPrefillFailed: Boolean = false,
): Boolean = isPrefillLoading || isProcessingMethodDefaultsInitializing || isPrefillFailed

/**
 * 등록(저장) 액션을 열어 둘지.
 *
 * prefill 이 **끝나지 않은 동안에도** 잠근다 (#705) — 실패뿐 아니라 아직 읽는 중인 skeleton 상태도
 * 폼이 기본 빈 값이라, 느린 상세 GET 을 앞질러 저장하면 수정(PATCH)이 그 빈 값으로 나가 기존
 * 기록을 덮는다. `isPrefillLoading` 은 편집 진입에서만 true 라 신규 작성은 영향받지 않는다.
 *
 * ViewModel 의 [AfternoteEditorViewModel.saveAfternote] 진입 가드와 **같은 규칙을 두 겹으로** 둔다 —
 * 화면이 막는 것은 사용자 경험이고, 저장 진입점에서 막는 것은 계약이다.
 */
internal fun isEditorSubmitEnabled(
    isSaving: Boolean,
    isPrefillFailed: Boolean,
    isPrefillLoading: Boolean,
): Boolean = !isSaving && !isPrefillFailed && !isPrefillLoading

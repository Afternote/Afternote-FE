package com.afternote.feature.afternote.presentation.author.editor.state

import androidx.annotation.StringRes
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver

/** 저장 전 필수 필드 검증에 대한 실패 유형. */
enum class AfternoteValidationError(
    @param:StringRes val messageResId: Int,
) {
    TITLE_REQUIRED(R.string.afternote_validation_title_required),
    ACCOUNT_CREDENTIALS_REQUIRED(R.string.afternote_validation_account_credentials_required),

    /** 처리 방법 1개 이상 필요 (계정·갤러리 폼 공통). */
    ACTIONS_REQUIRED(R.string.afternote_validation_actions_required),

    /** ESTATE 등 디자인 미확정으로 placeholder 만 노출되는 카테고리에서 저장 시도 시. */
    UNIMPLEMENTED_TYPE(R.string.afternote_validation_unimplemented_category),

    /** 수신자 최소 1명 필요 (모든 카테고리). API 400/475와 동일 메시지. */
    RECEIVERS_REQUIRED(R.string.afternote_validation_receivers_required),

    /**
     * 남기실 말씀에 제목만 쓰고 본문을 비운 블록이 있을 때. 서버가 본문을 필수로 검증해
     * 그대로 보내면 400 이므로 저장 전에 막는다 — 입력한 제목을 조용히 버리지 않기 위함이다.
     */
    LEAVE_MESSAGE_BODY_REQUIRED(R.string.afternote_validation_leave_message_body_required),
}

/**
 * 에디터에서 UI가 소비할 단일 오류 상태.
 *
 * nullable [AfternoteEditorUiState.error]의 `null`은 오류가 없는 정상 상태이고, 값이 있을 때만
 * 오류 종류에 맞는 안내를 노출한다. 화면 표현은 디자인 확정 전까지 기존 Snackbar를 유지한다.
 */
sealed interface AfternoteEditorError {
    data class Validation(
        val reason: AfternoteValidationError,
    ) : AfternoteEditorError

    data object Network : AfternoteEditorError

    data object Server : AfternoteEditorError

    data class Upload(
        val target: Target,
    ) : AfternoteEditorError {
        enum class Target {
            /** 영상 선택 직후 생성한 썸네일 업로드. */
            THUMBNAIL,

            /** 저장 요청을 만들면서 수행하는 추억 노트 사진·영상 업로드. */
            SAVE_MEDIA,
        }
    }
}

/**
 * 에디터 화면의 단일 UI 상태.
 *
 * 일회성 신호(`pending*`)를 Channel 이 아니라 상태로 둔 건 configuration change·process death 뒤
 * 재구독에서도 마지막 신호가 살아남아야 해서다. non-null 이면 UI 가 처리 후 `on*Consumed()` 로 되돌린다.
 *
 * 에디터 오류는 [error] 한 필드에서 종류까지 보존한다. 5xx 본문에 내부 SQL 이 섞여 올 수 있으므로
 * 서버 raw 메시지는 상태에 싣지 않고, UI가 오류 종류를 안전한 로컬 문구로 변환한다.
 */
data class AfternoteEditorUiState(
    val form: EditorFormState = EditorFormState(),
    val authorReceivers: List<AfternoteEditorReceiver> = emptyList(),
    val isSaving: Boolean = false,
    /**
     * 수정 모드 진입 직후 `getDetail()` 응답 → [pendingPrefill] 신호 도착 전까지 true. UI는 이 구간 동안
     * prefill 대상 섹션(서비스명·계정·처리 방법·메시지·추모 미디어 등)을 skeleton placeholder 로 표시한다.
     * 신규 작성 모드(`itemId == null`)는 항상 false.
     */
    val isPrefillLoading: Boolean = false,
    val savedId: Long? = null,
    val error: AfternoteEditorError? = null,
    /** 저장 성공 신호 — UI 가 nav 후 `onSaveSuccessConsumed` 로 reset. */
    val pendingSaveSuccessId: Long? = null,
    /** 추모 영상 썸네일 업로드 완료 신호 — UI 파사드가 form 에 url 적용 후 `onThumbnailUploadedConsumed` 로 reset. */
    val pendingThumbnailUrl: String? = null,
    /** 수정 모드 prefill 데이터 — UI 파사드가 form 에 적용 후 `onPrefillApplied` 로 reset (skeleton 종료 동시). */
    val pendingPrefill: EditorFormPrefill? = null,
)

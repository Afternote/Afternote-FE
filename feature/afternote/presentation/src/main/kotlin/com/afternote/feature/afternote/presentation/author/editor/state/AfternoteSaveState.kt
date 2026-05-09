package com.afternote.feature.afternote.presentation.author.editor.state

import androidx.annotation.StringRes
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver

/**
 * 비동기 검증 실패 시 발생하는 예외 (예: API를 통한 GALLERY 수신자 확인 등).
 * [validationError]로 [AfternoteValidationError.messageResId] 기반 UI 메시지를 표시합니다.
 *
 * [message]는 로깅·Crashlytics 등에서 원인 파악용으로 [validationError] 이름을 담습니다.
 */
class AfternoteValidationException(
    val validationError: AfternoteValidationError,
) : Exception("Validation failed: ${validationError.name}")

/** 저장 전 필수 필드 검증에 대한 실패 유형. */
enum class AfternoteValidationError(
    @param:StringRes val messageResId: Int,
) {
    TITLE_REQUIRED(R.string.afternote_validation_title_required),
    SOCIAL_CREDENTIALS_REQUIRED(R.string.afternote_validation_social_credentials_required),
    SOCIAL_PROCESS_METHOD_REQUIRED(R.string.afternote_validation_social_process_method_required),
    SOCIAL_ACTIONS_REQUIRED(R.string.afternote_validation_social_actions_required),
    GALLERY_ACTIONS_REQUIRED(R.string.afternote_validation_gallery_actions_required),

    /** 수신자 최소 1명 필요 (모든 카테고리). API 400/475와 동일 메시지. */
    RECEIVERS_REQUIRED(R.string.afternote_validation_receivers_required),
    GALLERY_RECEIVERS_REQUIRED(R.string.afternote_validation_gallery_receivers_required),
    PLAYLIST_SONGS_REQUIRED(R.string.afternote_validation_playlist_songs_required),
}

/**
 * 에디터 화면의 단일 UI 상태.
 *
 * CLAUDE.md UI Layer 규칙(*"한 화면당 단일 UI State 객체. loading/error/data 독립 스트림 분리 금지"*)에 따라
 * 폼 SSOT([form]), 작성자 수신자 목록([authorReceivers]), 저장 진행/오류 필드를 한 객체로 묶는다.
 *
 * 저장 **성공**은 일회성 이벤트인
 * [com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorEvent.SaveSuccess]
 * (Channel)로 전달한다. 본 영속 상태는 진행 중·실패·검증 오류만 담는다.
 *
 * [error] 는 네트워크 등 서버 raw 메시지용. ViewModel은 `Context`에 의존하지 않고
 * 리소스 기반 일반 실패 메시지는 [errorRes] 에 [StringRes] ID로 담아 UI에서
 * [androidx.compose.ui.res.stringResource] 로 해석한다 (상세 화면 [com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailUiState.Error] 와 동일 페어).
 */
data class AfternoteEditorUiState(
    val form: EditorFormState = EditorFormState(),
    val authorReceivers: List<AfternoteEditorReceiver> = emptyList(),
    val isSaving: Boolean = false,
    val savedId: Long? = null,
    val validationError: AfternoteValidationError? = null,
    val error: String? = null,
    @param:StringRes val errorRes: Int? = null,
)

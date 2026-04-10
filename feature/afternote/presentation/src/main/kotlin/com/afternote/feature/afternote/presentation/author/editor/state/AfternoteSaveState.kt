package com.afternote.feature.afternote.presentation.author.editor.state

import androidx.annotation.StringRes
import com.afternote.feature.afternote.presentation.R

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
    @StringRes val messageResId: Int,
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
 * 애프터노트 저장(생성/수정)의 UI 진행 상태와 오류.
 *
 * 저장 **성공**은 일회성 이벤트인 [com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorEvent.SaveSuccess]
 * (Channel)로 전달합니다. 이 상태는 진행 중·실패·검증 오류만 담습니다.
 *
 * [savedId]는 성공 직후 스냅샷용으로 남겨 둔 값입니다. 화면에 ID를 계속 표시할 필요가 없고
 * 네비게이션 등도 이벤트만 쓴다면, 중복 소스를 피하기 위해 이벤트 쪽만 두는 편이 MVI/UDF에 가깝습니다.
 *
 * [error]는 네트워크 등 원시 메시지용입니다. 리소스 기반 문구와 통합하려면 프로젝트의 `UiText` 등
 * 공통 타입으로 감싸는 방식을 검토할 수 있습니다.
 */
data class AfternoteSaveState(
    val isSaving: Boolean = false,
    val savedId: Long? = null,
    val error: String? = null,
    val validationError: AfternoteValidationError? = null,
)

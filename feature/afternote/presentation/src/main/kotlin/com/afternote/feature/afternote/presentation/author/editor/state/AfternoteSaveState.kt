package com.afternote.feature.afternote.presentation.author.editor.state

import androidx.annotation.StringRes
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver

/**
 * 비동기 검증 실패 시 발생하는 예외 (예: API를 통한 GALLERY 수신자 확인 등).
 * [validationError]로 [AfternoteValidationError.messageResId] 기반 UI 메시지를 표시합니다.
 *
 * [message]는 로깅·Crashlytics 등에서 원인 파악용으로 [validationError] 이름을 담습니다.
 *
 * 현재 이 예외를 던지는 곳은 없다 — 저장 전 로컬 검증은 `AfternoteEditorValidator` 결과를
 * `validationError` 상태에 바로 넣고 반환하는 경로를 쓴다. 서버가 거절한 검증(수신자 필수 등)은
 * domain 의 `AfternoteAuthoringValidationException` 이 맡는다.
 */
class AfternoteValidationException(
    val validationError: AfternoteValidationError,
) : Exception("Validation failed: ${validationError.name}")

/** 저장 전 필수 필드 검증에 대한 실패 유형. */
enum class AfternoteValidationError(
    @param:StringRes val messageResId: Int,
) {
    TITLE_REQUIRED(R.string.afternote_validation_title_required),
    ACCOUNT_CREDENTIALS_REQUIRED(R.string.afternote_validation_account_credentials_required),

    /** 처리 방법 1개 이상 필요 (계정·갤러리 폼 공통 — [EditorFormState.processingMethods] 단일 리스트 검증). */
    ACTIONS_REQUIRED(R.string.afternote_validation_actions_required),

    /** ESTATE 등 디자인 미확정으로 placeholder 만 노출되는 카테고리에서 저장 시도 시. */
    UNIMPLEMENTED_CATEGORY(R.string.afternote_validation_unimplemented_category),

    /** 수신자 최소 1명 필요 (모든 카테고리). API 400/475와 동일 메시지. */
    RECEIVERS_REQUIRED(R.string.afternote_validation_receivers_required),

    /**
     * 갤러리 수신자 서버 확인용 — 사용처 0건. [AfternoteValidationException] 과 짝인데 그 비동기 검증
     * 자체가 미구현이라 함께 떠 있다. 지우지 말고 그 경로가 붙을 때 같이 살린다.
     */
    GALLERY_RECEIVERS_REQUIRED(R.string.afternote_validation_gallery_receivers_required),
    PLAYLIST_SONGS_REQUIRED(R.string.afternote_validation_playlist_songs_required),
}

/**
 * 에디터 화면의 단일 UI 상태.
 *
 * 일회성 신호(`pending*`)를 Channel 이 아니라 상태로 둔 건 configuration change·process death 뒤
 * 재구독에서도 마지막 신호가 살아남아야 해서다. non-null 이면 UI 가 처리 후 `on*Consumed()` 로 되돌린다.
 *
 * 실패 문구는 [errorRes] 에 [StringRes] ID 로만 싣는다 — 5xx 본문에 내부 SQL 이 섞여 오는 탓에 서버 raw
 * 메시지를 그대로 쓸 수 없고, ViewModel 이 `Context` 에 의존하지 않으려는 이유도 있다.
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
    val validationError: AfternoteValidationError? = null,
    @param:StringRes val errorRes: Int? = null,
    /** 저장 성공 신호 — UI 가 nav 후 `onSaveSuccessConsumed` 로 reset. */
    val pendingSaveSuccessId: Long? = null,
    /** 추모 영상 썸네일 업로드 완료 신호 — UI 파사드가 form 에 url 적용 후 `onThumbnailUploadedConsumed` 로 reset. */
    val pendingThumbnailUrl: String? = null,
    /** 추모 썸네일 업로드 실패 신호 — UI 가 Snackbar 표출 후 `onThumbnailUploadErrorConsumed` 로 reset. Boolean 단일 신호 (실패 사유 분기 없음, 메시지는 [R.string.afternote_editor_thumbnail_upload_failed] 고정). */
    val thumbnailUploadFailed: Boolean = false,
    /** 수정 모드 prefill 데이터 — UI 파사드가 form 에 적용 후 `onPrefillApplied` 로 reset (skeleton 종료 동시). */
    val pendingPrefill: EditorFormPrefill? = null,
)

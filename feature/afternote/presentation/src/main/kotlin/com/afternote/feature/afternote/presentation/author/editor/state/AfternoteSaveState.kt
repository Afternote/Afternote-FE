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
    SOCIAL_ACTIONS_REQUIRED(R.string.afternote_validation_social_actions_required),
    GALLERY_ACTIONS_REQUIRED(R.string.afternote_validation_gallery_actions_required),

    /** BUSINESS·ESTATE 등 디자인 미확정으로 placeholder 만 노출되는 카테고리에서 저장 시도 시. */
    UNIMPLEMENTED_CATEGORY(R.string.afternote_validation_unimplemented_category),

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
일회성 신호 (`pending*` 필드 — 저장 성공·썸네일 업로드 완료·수정 모드 prefill 도착) 는 nullable 로 흡수.
 * non-null = 처리 대기, null = 소비 완료. UI 패턴:
 * `LaunchedEffect(pending*) { if (pending* != null) { 처리; viewModel.on*Consumed() } }`.
 * Channel + ObserveAsEvents 대비 장점 — configuration change · process death · 분할 화면에서
 * StateFlow 영속성 덕에 재구독 시 마지막 신호 재배달 (Channel 은 한 번만 소비 → 손실 가능).
 * Google 공식 가이드: ViewModel events → UI state update.
 *
 * [error] 는 네트워크 등 서버 raw 메시지용. ViewModel은 `Context`에 의존하지 않고
 * 리소스 기반 일반 실패 메시지는 [errorRes] 에 [StringRes] ID로 담아 UI에서
 * [androidx.compose.ui.res.stringResource] 로 해석한다 (상세 화면 [com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailUiState.Error] 와 동일 페어).
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
    val error: String? = null,
    @param:StringRes val errorRes: Int? = null,
    /** 저장 성공 신호 — UI 가 nav 후 `onSaveSuccessConsumed` 로 reset. */
    val pendingSaveSuccessId: Long? = null,
    /** 추모 영상 썸네일 업로드 완료 신호 — UI 파사드가 form 에 url 적용 후 `onThumbnailUploadedConsumed` 로 reset. */
    val pendingThumbnailUrl: String? = null,
    /** 수정 모드 prefill 데이터 — UI 파사드가 form 에 적용 후 `onPrefillApplied` 로 reset (skeleton 종료 동시). */
    val pendingPrefill: EditorFormPrefill? = null,
)

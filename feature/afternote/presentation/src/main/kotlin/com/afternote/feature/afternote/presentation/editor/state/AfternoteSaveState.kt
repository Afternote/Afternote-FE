package com.afternote.feature.afternote.presentation.editor.state

import androidx.annotation.StringRes
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver

/** 저장 전 필수 필드 검증에 대한 실패 유형. */
enum class AfternoteValidationError(
    @param:StringRes val messageResId: Int,
) {
    TITLE_REQUIRED(R.string.afternote_validation_title_required),
    ACCOUNT_CREDENTIALS_REQUIRED(R.string.afternote_validation_account_credentials_required),

    /** 둘 이상의 필수 입력이 동시에 비어 있어 특정 필드 하나로 안내를 좁힐 수 없음. */
    MULTIPLE_REQUIRED_FIELDS(R.string.afternote_validation_multiple_required_fields),

    /** ESTATE 등 디자인 미확정으로 placeholder 만 노출되는 카테고리에서 저장 시도 시. */
    UNIMPLEMENTED_TYPE(R.string.afternote_validation_unimplemented_category),

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
 * 오류 종류에 맞는 안내를 노출한다. 검증 오류는 확인 팝업, 네트워크·서버·업로드 오류는 Snackbar로 표시한다.
 */
sealed interface AfternoteEditorError {
    data class Validation(
        val reason: AfternoteValidationError,
    ) : AfternoteEditorError

    data object Network : AfternoteEditorError

    data object Server : AfternoteEditorError

    /**
     * 수신자 선택 화면이 돌려준 수신자를 폼에 반영하지 못함 (#1405).
     *
     * 에디터의 수신자 목록 로드가 실패하면 id 에 대응하는 이름·관계를 찾을 수 없어 선택이 버려진다.
     * 저장 실패가 아니라 «고른 것이 반영되지 않았다» 는 사실을 알리는 신호다 — 사용자가 다시 고를 수 있어야 한다.
     */
    data object ReceiverSelectionUnavailable : AfternoteEditorError

    /**
     * 수정 진입 prefill 조회가 **실패**했다 (#705).
     *
     * 저장 실패가 아니라 «기존 기록을 읽지 못했다» 는 사실이다. 이 상태에서 저장이 나가면
     * 서버가 빈 폼 값으로 기존 기록을 덮으므로, 화면은 폼 대신 오류·재시도를 보이고 저장은 막는다.
     */
    data object PrefillUnavailable : AfternoteEditorError

    /**
     * 수정 진입 prefill 조회가 **아직 진행 중**이다 (#705).
     *
     * 저장을 막는 이유는 [PrefillUnavailable] 과 같지만 사용자에게 할 말이 다르다 — 실패가 아니라
     * 곧 도착한다. 둘을 한 갈래로 두면 「불러오지 못했습니다」 가 아직 읽는 중인 사용자에게도 나가
     * 사실과 다른 안내가 된다.
     *
     * 화면은 이 상태에서 저장 액션을 이미 잠그므로 여기까지 오는 것은 경합이나 버튼을 거치지 않은
     * 호출뿐이다. 그래도 계약은 저장 진입점에서 지킨다.
     */
    data object PrefillNotReady : AfternoteEditorError

    data class Upload(
        val target: Target,
    ) : AfternoteEditorError {
        enum class Target {
            /** 영상 선택 직후 생성한 썸네일 업로드. */
            THUMBNAIL,

            /** 고른 영상에서 미리보기 프레임을 뽑는 단계. 업로드까지 가지도 못한 실패다. */
            THUMBNAIL_EXTRACT,

            /** 저장 요청을 만들면서 수행하는 추억 노트 사진·영상 업로드. */
            SAVE_MEDIA,
        }
    }
}

/**
 * 한 번의 오류 안내를 식별하는 이벤트.
 *
 * 같은 [error]가 연속으로 발생해도 [occurrence]가 달라 UI effect가 다시 실행된다. 소비할 때는
 * 이 객체 전체를 돌려줘야 이전 Snackbar의 종료 콜백이 더 최신 이벤트를 지우지 않는다.
 */
data class AfternoteEditorErrorEvent(
    val error: AfternoteEditorError,
    val occurrence: Long,
)

/**
 * 에디터 화면의 단일 UI 상태.
 *
 * 일회성 신호(`pending*`)를 Channel 이 아니라 상태로 둔 건 configuration change·process death 뒤
 * 재구독에서도 마지막 신호가 살아남아야 해서다. non-null 이면 UI 가 처리 후 `on*Consumed()` 로 되돌린다.
 *
 * 에디터 오류는 [errorEvent] 한 필드에서 종류와 발생 순서를 보존한다. 5xx 본문에 내부 SQL 이 섞여 올 수 있으므로
 * 서버 raw 메시지는 상태에 싣지 않고, UI가 오류 종류를 안전한 로컬 문구로 변환한다.
 */
data class AfternoteEditorUiState(
    val form: EditorFormState = EditorFormState(),
    val authorReceivers: List<AfternoteEditorReceiver> = emptyList(),
    val isSaving: Boolean = false,
    /**
     * 수정 모드 진입 직후 `getDetail()` 응답 → [pendingPrefill] 신호 도착 전까지 true. UI는 이 구간 동안
     * prefill 대상 섹션(서비스명·계정·처리 방법·메시지·추억 노트 미디어 등)을 skeleton placeholder 로 표시한다.
     * 신규 작성 모드(`itemId == null`)는 항상 false.
     */
    val isPrefillLoading: Boolean = false,
    /**
     * 수정 모드 prefill 조회가 실패해 폼을 채우지 못한 상태 (#705).
     *
     * true 인 동안 화면은 빈 폼이 아니라 오류·재시도를 그리고 저장을 막는다 — 빈 폼으로 저장하면
     * 서버가 기존 기록을 그 빈 값으로 덮는다. 재시도가 성공하면 false 로 돌아간다.
     */
    val isPrefillFailed: Boolean = false,
    val savedId: Long? = null,
    val errorEvent: AfternoteEditorErrorEvent? = null,
    /** 저장 성공 신호 — UI 가 nav 후 `onSaveSuccessConsumed` 로 reset. */
    val pendingSaveSuccessId: Long? = null,
    /** 장례식에 남길 영상 썸네일 업로드 완료 신호 — UI 파사드가 form 에 url 적용 후 `onThumbnailUploadedConsumed` 로 reset. */
    val pendingThumbnailUrl: String? = null,
    /**
     * 추출부터 다시 돌리기 위한 키. 바뀌면 UI 가 프레임 추출을 재발화한다 (#1550).
     *
     * 추출은 `LaunchedEffect(videoUrl)` 키라 같은 영상이 폼에 있는 한 다시 돌지 않고, 저장하면 썸네일
     * 없는 영상이 확정되며, 재편집으로 들어와도 원격 URL 이라 추출을 건너뛴다 — 이 키가 없으면 추출
     * 실패의 복구 경로는 영상을 처음부터 다시 고르는 것뿐이다.
     */
    val memorialThumbnailRetryToken: Int = 0,
    /** 수정 모드 prefill 데이터 — UI 파사드가 form 에 적용 후 `onPrefillApplied` 로 reset (skeleton 종료 동시). */
    val pendingPrefill: EditorFormPrefill? = null,
) {
    val error: AfternoteEditorError?
        get() = errorEvent?.error
}

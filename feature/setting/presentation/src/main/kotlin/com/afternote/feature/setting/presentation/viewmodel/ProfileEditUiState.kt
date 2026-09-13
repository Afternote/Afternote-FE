package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.mvi.UiState

internal sealed interface ProfileEditUiState : UiState {
    data object Loading : ProfileEditUiState

    /**
     * @param profileImageUrl 서버 정본 프로필 사진. 아직 한 장도 없으면 널이다.
     * @param selectedImageUri 이 화면에서 고르기만 하고 아직 저장하지 않은 사진의 content URI.
     *   저장이 성공할 때까지 서버 정본은 건드리지 않는다 — 취소·실패가 기존 사진을 지우지 않는
     *   자리다(#1438).
     */
    data class Success(
        val name: String,
        val phone: String,
        val email: String,
        val profileImageUrl: String? = null,
        val selectedImageUri: String? = null,
        val isUpdating: Boolean = false,
        val pendingEvent: ProfileEditEvent? = null,
    ) : ProfileEditUiState {
        /** 아바타에 그릴 이미지 — 저장 전 선택이 서버 정본을 덮어 보인다. */
        val displayImageUri: String? get() = selectedImageUri ?: profileImageUrl
    }

    data object Error : ProfileEditUiState
}

internal sealed interface ProfileEditEvent {
    data object UpdateSuccess : ProfileEditEvent

    data object UpdateFailure : ProfileEditEvent
}

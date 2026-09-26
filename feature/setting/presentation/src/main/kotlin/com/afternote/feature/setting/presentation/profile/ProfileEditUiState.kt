package com.afternote.feature.setting.presentation.profile

sealed interface ProfileEditUiState {
    data object Loading : ProfileEditUiState

    /**
     * @param profileImageUrl 서버가 내려준 표시용 프로필 사진 주소. 아직 사진이 없으면 널이다.
     * @param selectedImageUri 이 화면에서 고르기만 하고 아직 저장하지 않은 사진의 content URI.
     *   저장이 성공하기 전까지 서버 사진은 건드리지 않는다. 선택 취소나 저장 실패가 기존 사진을
     *   지우지 않는 자리다(#1438).
     */
    data class Success(
        val name: String,
        val phone: String,
        val email: String,
        val profileImageUrl: String?,
        val selectedImageUri: String? = null,
        val isUpdating: Boolean = false,
    ) : ProfileEditUiState {
        /** 아바타에 그릴 사진. 저장 전 선택이 서버 사진보다 앞선다. */
        val displayImageUri: String? get() = selectedImageUri ?: profileImageUrl
    }

    data object Error : ProfileEditUiState
}

sealed interface ProfileEditEvent {
    data object UpdateSuccess : ProfileEditEvent

    data object UpdateFailure : ProfileEditEvent
}

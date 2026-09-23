package com.afternote.feature.setting.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.repository.PhotoUploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `POST /files/presigned-url` 의 `directory` 값. `PATCH users/me` 가 받은 키를 `profiles` 로
 * 승격하는 서버 계약과 짝이다.
 */
private const val PROFILE_UPLOAD_DIRECTORY = "profiles"

@HiltViewModel
internal class ProfileEditViewModel
    @Inject
    constructor(
        private val myProfileRepository: MyProfileRepository,
        private val photoUploadRepository: PhotoUploadRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ProfileEditUiState>(ProfileEditUiState.Loading)
        val uiState = _uiState.asStateFlow()

        private val _events = Channel<ProfileEditEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        init {
            loadProfile()
        }

        private fun loadProfile() {
            viewModelScope.launch {
                runCatchingCancellable { myProfileRepository.getMyProfile() }
                    .onSuccess { user ->
                        _uiState.value =
                            ProfileEditUiState.Success(
                                name = user.name,
                                phone = user.phone.orEmpty(),
                                email = user.email,
                                profileImageUrl = user.profileImageUrl,
                            )
                    }.onFailure {
                        _uiState.value = ProfileEditUiState.Error
                    }
            }
        }

        /** 갤러리에서 고른 사진을 저장 전 선택으로 둔다. 서버 사진은 저장이 성공할 때 바뀐다. */
        fun selectProfileImage(uri: String) {
            _uiState.update { state ->
                // 저장 중 선택은 받지 않는다. 업로드는 저장을 누른 시점의 사진으로 진행되므로,
                // 받아들이면 화면에 보이는 사진과 실제로 올라가는 사진이 갈린다.
                if (state is ProfileEditUiState.Success && !state.isUpdating) {
                    state.copy(selectedImageUri = uri)
                } else {
                    state
                }
            }
        }

        fun updateProfile(
            name: String,
            phone: String,
        ) {
            val current = _uiState.value as? ProfileEditUiState.Success ?: return
            _uiState.update { current.copy(isUpdating = true) }
            viewModelScope.launch {
                runCatchingCancellable {
                    // 업로드가 실패하면 여기서 멈춘다. 수정 요청이 나가지 않으므로 이름·연락처·사진 모두 그대로다.
                    val uploadedKey =
                        current.selectedImageUri?.let { uri ->
                            photoUploadRepository.upload(uri, PROFILE_UPLOAD_DIRECTORY).getOrThrow().fileKey
                        }
                    myProfileRepository.updateMyProfile(
                        name = name.takeIf { it.isNotBlank() },
                        phone = phone.takeIf { it.isNotBlank() },
                        // 서버는 표시용 URL 이 아니라 업로드 키를 받아 승격한다. 널이면 기존 사진을 유지한다.
                        profileImageUrl = uploadedKey,
                    )
                }.onSuccess {
                    _events.send(ProfileEditEvent.UpdateSuccess)
                }.onFailure {
                    _uiState.update { current.copy(isUpdating = false) }
                    _events.send(ProfileEditEvent.UpdateFailure)
                }
            }
        }
    }

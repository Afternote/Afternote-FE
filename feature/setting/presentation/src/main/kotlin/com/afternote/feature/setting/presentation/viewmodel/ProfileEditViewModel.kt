package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileEditViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ProfileEditUiState>(ProfileEditUiState.Loading)
        val uiState = _uiState.asStateFlow()

        private val _events = Channel<ProfileEditEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        /** 진행 중인 조회 — 첫 진입 이후의 ON_RESUME 이 실행 중인 로드와 겹치면 건너뛰기 위한 가드. */
        private var loadJob: Job? = null

        /**
         * 다음 [refreshOnReturn] 이 첫 ON_RESUME(진입 자체)인지. 첫 resume 은 init 로드와 같은
         * 진입이므로 갱신하지 않는다 — VM 필드인 이유는 ReceiverHomeViewModel 의 refreshOnReturn 과
         * 동일, 프로세스 사망 후 복원에서도 init 로드와 수명이 일치한다.
         */
        private var isFirstResume = true

        init {
            loadProfile()
        }

        /** 다른 화면에서 복귀했을 때의 자동 갱신 (#701). 첫 진입은 건너뛰고, 로드가 겹치면 건너뛴다. */
        fun refreshOnReturn() {
            if (isFirstResume) {
                isFirstResume = false
                return
            }
            if (loadJob?.isActive == true || (_uiState.value as? ProfileEditUiState.Success)?.isUpdating == true) return
            loadProfile(keepsStateOnFailure = true)
        }

        private fun loadProfile(keepsStateOnFailure: Boolean = false) {
            loadJob =
                viewModelScope.launch {
                    runCatchingCancellable { userRepository.getMyProfile() }
                        .onSuccess { user ->
                            _uiState.value =
                                ProfileEditUiState.Success(
                                    name = user.name,
                                    phone = user.phone.orEmpty(),
                                    email = user.email,
                                )
                        }.onFailure {
                            if (!keepsStateOnFailure || _uiState.value !is ProfileEditUiState.Success) {
                                _uiState.value = ProfileEditUiState.Error
                            }
                        }
                }
        }

        fun updateProfile(
            name: String,
            phone: String,
        ) {
            val current = _uiState.value as? ProfileEditUiState.Success ?: return
            if (current.isUpdating) return
            loadJob?.cancel()
            _uiState.update { current.copy(isUpdating = true) }
            viewModelScope.launch {
                runCatchingCancellable {
                    userRepository.updateMyProfile(
                        name = name.takeIf { it.isNotBlank() },
                        phone = phone.takeIf { it.isNotBlank() },
                        profileImageUrl = null,
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

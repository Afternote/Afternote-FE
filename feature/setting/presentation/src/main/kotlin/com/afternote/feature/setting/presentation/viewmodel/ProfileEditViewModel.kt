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
        private var loadJob: Job? = null

        init {
            retryLoadProfile()
        }

        fun retryLoadProfile() {
            if (loadJob?.isActive == true) return
            loadJob =
                viewModelScope.launch {
                    _uiState.value = ProfileEditUiState.Loading
                    runCatchingCancellable { userRepository.getMyProfile() }
                        .onSuccess { user ->
                            _uiState.value =
                                ProfileEditUiState.Success(
                                    name = user.name,
                                    phone = user.phone.orEmpty(),
                                    email = user.email,
                                )
                        }.onFailure {
                            _uiState.value = ProfileEditUiState.Error
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

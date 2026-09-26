package com.afternote.feature.setting.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.MyProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProfileEditViewModel
    @Inject
    constructor(
        private val myProfileRepository: MyProfileRepository,
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
                    myProfileRepository.updateMyProfile(
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

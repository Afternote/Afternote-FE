package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProfileEditViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) : MviViewModel<ProfileEditIntent, ProfileEditUiState, ProfileEditReducerEvent>(ProfileEditUiState.Loading) {
        private var loadJob: Job? = null

        override fun onIntent(intent: ProfileEditIntent) {
            when (intent) {
                ProfileEditIntent.RetryLoad -> loadProfile()
                is ProfileEditIntent.UpdateProfile -> updateProfile(intent.name, intent.phone)
                is ProfileEditIntent.ConsumeEvent -> dispatch(ProfileEditReducerEvent.EventConsumed(intent.event))
            }
        }

        override fun reduce(
            state: ProfileEditUiState,
            event: ProfileEditReducerEvent,
        ): ProfileEditUiState =
            when (event) {
                ProfileEditReducerEvent.Loading -> {
                    ProfileEditUiState.Loading
                }

                is ProfileEditReducerEvent.Loaded -> {
                    ProfileEditUiState.Success(event.name, event.phone, event.email)
                }

                ProfileEditReducerEvent.LoadFailed -> {
                    ProfileEditUiState.Error
                }

                ProfileEditReducerEvent.Updating -> {
                    if (state is ProfileEditUiState.Success) state.copy(isUpdating = true, pendingEvent = null) else state
                }

                is ProfileEditReducerEvent.UpdateFinished -> {
                    if (state is ProfileEditUiState.Success) state.copy(isUpdating = false, pendingEvent = event.event) else state
                }

                is ProfileEditReducerEvent.EventConsumed -> {
                    if (state is ProfileEditUiState.Success && state.pendingEvent == event.event) state.copy(pendingEvent = null) else state
                }
            }

        init {
            loadProfile()
        }

        private fun loadProfile() {
            if (loadJob?.isActive == true) return
            loadJob =
                viewModelScope.launch {
                    dispatch(ProfileEditReducerEvent.Loading)
                    runCatchingCancellable { userRepository.getMyProfile() }
                        .onSuccess { user ->
                            dispatch(
                                ProfileEditReducerEvent.Loaded(
                                    name = user.name,
                                    phone = user.phone.orEmpty(),
                                    email = user.email,
                                ),
                            )
                        }.onFailure {
                            dispatch(ProfileEditReducerEvent.LoadFailed)
                        }
                }
        }

        private fun updateProfile(
            name: String,
            phone: String,
        ) {
            val current = currentState as? ProfileEditUiState.Success ?: return
            if (current.isUpdating || current.pendingEvent == ProfileEditEvent.UpdateSuccess) return
            dispatch(ProfileEditReducerEvent.Updating)
            viewModelScope.launch {
                runCatchingCancellable {
                    userRepository.updateMyProfile(
                        name = name.takeIf { it.isNotBlank() },
                        phone = phone.takeIf { it.isNotBlank() },
                        profileImageUrl = null,
                    )
                }.onSuccess {
                    dispatch(ProfileEditReducerEvent.UpdateFinished(ProfileEditEvent.UpdateSuccess))
                }.onFailure {
                    dispatch(ProfileEditReducerEvent.UpdateFinished(ProfileEditEvent.UpdateFailure))
                }
            }
        }
    }

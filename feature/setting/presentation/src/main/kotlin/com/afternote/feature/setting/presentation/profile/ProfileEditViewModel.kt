package com.afternote.feature.setting.presentation.profile

import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.result.runCatchingCancellable
import com.afternote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProfileEditViewModel
    @Inject
    constructor(
        private val myProfileRepository: MyProfileRepository,
    ) : MviViewModel<ProfileEditIntent, ProfileEditUiState, ProfileEditReducerEvent>(ProfileEditUiState.Loading) {
        init {
            loadProfile()
        }

        override fun onIntent(intent: ProfileEditIntent) {
            when (intent) {
                is ProfileEditIntent.UpdateProfile -> updateProfile(intent.name, intent.phone)
                is ProfileEditIntent.ConsumeEvent -> dispatch(ProfileEditReducerEvent.EventConsumed(intent.event))
            }
        }

        override fun reduce(
            state: ProfileEditUiState,
            event: ProfileEditReducerEvent,
        ): ProfileEditUiState =
            when (event) {
                is ProfileEditReducerEvent.Loaded -> {
                    ProfileEditUiState.Success(name = event.name, phone = event.phone, email = event.email)
                }

                ProfileEditReducerEvent.LoadFailed -> {
                    ProfileEditUiState.Error
                }

                ProfileEditReducerEvent.Updating -> {
                    state.updateSuccess { it.copy(isUpdating = true, pendingEvent = null) }
                }

                ProfileEditReducerEvent.UpdateSucceeded -> {
                    state.updateSuccess {
                        it.copy(isUpdating = false, isUpdated = true, pendingEvent = ProfileEditEvent.UpdateSuccess)
                    }
                }

                ProfileEditReducerEvent.UpdateFailed -> {
                    state.updateSuccess { it.copy(isUpdating = false, pendingEvent = ProfileEditEvent.UpdateFailure) }
                }

                is ProfileEditReducerEvent.EventConsumed -> {
                    state.updateSuccess { if (it.pendingEvent == event.event) it.copy(pendingEvent = null) else it }
                }
            }

        private fun loadProfile() {
            viewModelScope.launch {
                runCatchingCancellable { myProfileRepository.getMyProfile() }
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
            if (current.isUpdateLocked) return
            dispatch(ProfileEditReducerEvent.Updating)
            viewModelScope.launch {
                runCatchingCancellable {
                    myProfileRepository.updateMyProfile(
                        name = name.takeIf { it.isNotBlank() },
                        phone = phone.takeIf { it.isNotBlank() },
                        profileImageUrl = null,
                    )
                }.onSuccess {
                    dispatch(ProfileEditReducerEvent.UpdateSucceeded)
                }.onFailure {
                    dispatch(ProfileEditReducerEvent.UpdateFailed)
                }
            }
        }
    }

private inline fun ProfileEditUiState.updateSuccess(
    transform: (ProfileEditUiState.Success) -> ProfileEditUiState.Success,
): ProfileEditUiState = if (this is ProfileEditUiState.Success) transform(this) else this

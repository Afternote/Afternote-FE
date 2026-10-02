package com.afternote.feature.setting.presentation.profile

import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PROFILE_UPLOAD_DIRECTORY = "profiles"

@HiltViewModel
internal class ProfileEditViewModel
    @Inject
    constructor(
        private val myProfileRepository: MyProfileRepository,
        private val photoUploadRepository: PhotoUploadRepository,
    ) : MviViewModel<ProfileEditIntent, ProfileEditUiState, ProfileEditReducerEvent>(ProfileEditUiState.Loading) {
        init {
            loadProfile()
        }

        override fun onIntent(intent: ProfileEditIntent) {
            when (intent) {
                is ProfileEditIntent.SelectPhoto -> dispatch(ProfileEditReducerEvent.PhotoSelected(intent.uri))
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
                    ProfileEditUiState.Success(
                        name = event.name,
                        phone = event.phone,
                        email = event.email,
                        profileImageUrl = event.profileImageUrl,
                    )
                }

                is ProfileEditReducerEvent.PhotoSelected -> {
                    state.updateSuccess { if (it.isUpdating) it else it.copy(selectedImageUri = event.uri) }
                }

                ProfileEditReducerEvent.LoadFailed -> {
                    ProfileEditUiState.Error
                }

                ProfileEditReducerEvent.Updating -> {
                    state.updateSuccess { it.copy(isUpdating = true, pendingEvent = null) }
                }

                // 성공 뒤 화면이 닫히는 사이 재제출을 막도록 isUpdating 을 유지한다.
                ProfileEditReducerEvent.UpdateSucceeded -> {
                    state.updateSuccess { it.copy(pendingEvent = ProfileEditEvent.UpdateSuccess) }
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
                                profileImageUrl = user.profileImageUrl,
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
            if (current.isUpdating) return
            dispatch(ProfileEditReducerEvent.Updating)
            viewModelScope.launch {
                runCatchingCancellable {
                    val uploadedKey =
                        current.selectedImageUri?.let { uri ->
                            photoUploadRepository.upload(uri, PROFILE_UPLOAD_DIRECTORY).getOrThrow().fileKey
                        }
                    myProfileRepository.updateMyProfile(
                        name = name.takeIf { it.isNotBlank() },
                        phone = phone.takeIf { it.isNotBlank() },
                        // 서버는 표시용 URL 대신 업로드 키를 받아 승격한다. 널이면 기존 사진을 유지한다.
                        profileImageUrl = uploadedKey,
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

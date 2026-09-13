package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `POST /files/presigned-url` 의 `directory` 값. 서버 허용 목록
 * (profiles / timeletters / afternotes / mindrecords / documents) 중 프로필 전용 폴더로,
 * `PATCH users/me` 가 `promoteMediaKey("profiles", ...)` 로 승격하는 자리와 짝이다.
 */
private const val PROFILE_UPLOAD_DIRECTORY = "profiles"

@HiltViewModel
internal class ProfileEditViewModel
    @Inject
    constructor(
        private val userRepository: MyProfileRepository,
        private val photoUploadRepository: PhotoUploadRepository,
    ) : MviViewModel<ProfileEditIntent, ProfileEditUiState, ProfileEditReducerEvent>(ProfileEditUiState.Loading) {
        private var loadJob: Job? = null

        private var isFirstResume = true

        override fun onIntent(intent: ProfileEditIntent) {
            when (intent) {
                ProfileEditIntent.RefreshOnReturn -> refreshOnReturn()
                ProfileEditIntent.RetryLoad -> loadProfile()
                is ProfileEditIntent.SelectProfileImage -> dispatch(ProfileEditReducerEvent.ProfileImageSelected(intent.uri))
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
                    val previous = state as? ProfileEditUiState.Success
                    ProfileEditUiState.Success(
                        event.name,
                        event.phone,
                        event.email,
                        profileImageUrl = event.profileImageUrl,
                        // 저장 전 선택은 조회 응답이 지우지 않는다. 재진입 갱신이나 늦게 도착한 조회가
                        // 고른 사진을 되돌리면 사용자에게는 「고른 게 사라졌다」로 보인다.
                        selectedImageUri = previous?.selectedImageUri,
                        pendingEvent = previous?.pendingEvent,
                    )
                }

                ProfileEditReducerEvent.LoadFailed -> {
                    ProfileEditUiState.Error
                }

                is ProfileEditReducerEvent.ProfileImageSelected -> {
                    // 저장 중 선택은 받지 않는다 — 업로드는 시작 시점의 URI 로 진행되므로,
                    // 받아들이면 화면에 보이는 사진과 실제로 올라가는 사진이 갈린다.
                    if (state is ProfileEditUiState.Success && !state.isUpdating) {
                        state.copy(selectedImageUri = event.uri)
                    } else {
                        state
                    }
                }

                ProfileEditReducerEvent.Updating -> {
                    if (state is ProfileEditUiState.Success) state.copy(isUpdating = true, pendingEvent = null) else state
                }

                is ProfileEditReducerEvent.UpdateFinished -> {
                    if (state is ProfileEditUiState.Success) state.copy(isUpdating = false, pendingEvent = event.event) else state
                }

                ProfileEditReducerEvent.UpdateStopped -> {
                    if (state is ProfileEditUiState.Success) state.copy(isUpdating = false) else state
                }

                is ProfileEditReducerEvent.EventConsumed -> {
                    if (state is ProfileEditUiState.Success && state.pendingEvent == event.event) state.copy(pendingEvent = null) else state
                }
            }

        private fun refreshOnReturn() {
            if (isFirstResume) {
                isFirstResume = false
                return
            }
            loadProfile(keepsStateOnFailure = true)
        }

        init {
            loadProfile()
        }

        private fun loadProfile(keepsStateOnFailure: Boolean = false) {
            if (loadJob?.isActive == true || (currentState as? ProfileEditUiState.Success)?.isUpdating == true) return
            loadJob =
                viewModelScope.launch {
                    if (!keepsStateOnFailure) dispatch(ProfileEditReducerEvent.Loading)
                    runCatchingCancellable { userRepository.getMyProfile() }
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
                            if (!keepsStateOnFailure ||
                                currentState !is ProfileEditUiState.Success
                            ) {
                                dispatch(ProfileEditReducerEvent.LoadFailed)
                            }
                        }
                }
        }

        private fun updateProfile(
            name: String,
            phone: String,
        ) {
            val current = currentState as? ProfileEditUiState.Success ?: return
            if (current.isUpdating || current.pendingEvent == ProfileEditEvent.UpdateSuccess) return
            val selectedImageUri = current.selectedImageUri
            loadJob?.cancel()
            dispatch(ProfileEditReducerEvent.Updating)
            viewModelScope.launch {
                try {
                    runCatchingCancellable {
                        // 업로드가 실패하면 여기서 멈춘다 — 수정 요청이 나가지 않으므로 서버 사진은 그대로다.
                        // 보낼 값은 다운로드 URL 이 아니라 fileKey 다. 서버가 `PATCH users/me` 로 받은 키를
                        // 승격(`promoteMediaKey`)하므로, URL 을 보내면 승격 대상을 찾지 못한다.
                        val uploadedKey =
                            selectedImageUri?.let { uri ->
                                photoUploadRepository.upload(uri, PROFILE_UPLOAD_DIRECTORY).getOrThrow().fileKey
                            }
                        userRepository.updateMyProfile(
                            name = name.takeIf { it.isNotBlank() },
                            phone = phone.takeIf { it.isNotBlank() },
                            // 널은 「사진은 건드리지 않는다」는 뜻이다 — 고른 사진이 없으면 기존 값이 유지된다.
                            profileImageUrl = uploadedKey,
                        )
                    }.onSuccess {
                        dispatch(ProfileEditReducerEvent.UpdateFinished(ProfileEditEvent.UpdateSuccess))
                    }.onFailure {
                        dispatch(ProfileEditReducerEvent.UpdateFinished(ProfileEditEvent.UpdateFailure))
                    }
                } finally {
                    // 취소는 성공도 실패도 아니라 신호를 내지 않지만, 진행 중 표시는 반드시 풀어야 한다.
                    // [runCatchingCancellable] 이 취소를 되던지므로 위의 두 갈래는 건너뛰는데, 여기가 없으면
                    // ViewModel 이 살아 있는 채 isUpdating 이 영원히 참으로 굳는다 — 그 순간부터 저장 재시도도,
                    // 사진 선택도, 재진입 갱신도 모두 가드에 걸려 화면이 잠긴다.
                    dispatch(ProfileEditReducerEvent.UpdateStopped)
                }
            }
        }
    }

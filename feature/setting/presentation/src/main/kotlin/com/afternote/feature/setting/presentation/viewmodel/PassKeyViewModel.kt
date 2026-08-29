package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.datastore.UserProfileDataSource
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.user.Passkey
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PassKeyViewModel
    @Inject
    constructor(
        private val dataSource: UserProfileDataSource,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        val isPasskeyRegistered: StateFlow<Boolean?> =
            dataSource
                .isPasskeyRegisteredFlow()
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        fun savePasskeyRegistered() {
            viewModelScope.launch {
                dataSource.savePasskeyRegistered(true)
            }
        }

        /** 서버에서 패스키 등록용 challenge 옵션을 받아온다 — Credential Manager에 그대로 전달할 원본 JSON. */
        suspend fun getPasskeyRegisterOptions(): Result<String> = runCatching { userRepository.getPasskeyRegisterOptions() }

        /**
         * Credential Manager가 반환한 등록 응답 JSON을 서버로 전달해 패스키 등록을 완료한다.
         * 서버 등록이 성공한 뒤에만 로컬 "등록됨" 표시를 남긴다 — 실패 시 다음 진입에서 다시 등록을 유도해야 하므로.
         */
        suspend fun completeRegistration(credentialJson: String): Result<Passkey> =
            runCatching {
                val passkey = userRepository.registerPasskey(credentialJson)
                dataSource.savePasskeyRegistered(true)
                passkey
            }
    }

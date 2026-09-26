package com.afternote.feature.receiver.presentation.recordsbox

import androidx.lifecycle.viewModelScope
import com.afternote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 발신자 등록 화면(15·16) ViewModel — 이름 입력 + 등록 (이슈 #215).
 *
 * 발신자 라벨 등록 API 가 미확정이라 [SenderRegistry] 로컬 stub 에 보관한다.
 * 백엔드 API 확정 시 Repository 호출로 교체.
 *
 * 입력 중인 텍스트는 UI 의 `TextFieldState` 가 보유하며, 등록 버튼 누르는 시점에만
 * 화면이 [submit] 로 전달한다 — ViewModel 이 실시간 입력값을 알 필요가 없으므로 별도 StateFlow 미보관.
 *
 * 등록 완료 신호는 [SenderRegistrationUiState.isRegistered] 로 UI 가 LaunchedEffect 에서 소비
 * 후 [SenderRegistrationIntent.ConsumeRegistered] 로 reset (Google 공식 가이드 — ViewModel events should always result in a UI state update).
 */
@HiltViewModel
internal class SenderRegistrationViewModel
    @Inject
    constructor(
        private val senderRegistry: SenderRegistry,
    ) : MviViewModel<SenderRegistrationIntent, SenderRegistrationUiState, SenderRegistrationReducerEvent>(SenderRegistrationUiState()) {
        override fun onIntent(intent: SenderRegistrationIntent) {
            when (intent) {
                is SenderRegistrationIntent.Submit -> submit(intent.name)
                SenderRegistrationIntent.ConsumeRegistered -> dispatch(SenderRegistrationReducerEvent.RegisteredConsumed)
            }
        }

        override fun reduce(
            state: SenderRegistrationUiState,
            event: SenderRegistrationReducerEvent,
        ): SenderRegistrationUiState = reduceSenderRegistration(state, event)

        private fun submit(name: String) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return
            viewModelScope.launch {
                senderRegistry.register(trimmed)
                dispatch(SenderRegistrationReducerEvent.Registered)
            }
        }
    }

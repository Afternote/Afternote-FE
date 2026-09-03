package com.afternote.feature.afternote.presentation.editor.receiver

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.user.Receiver
import com.afternote.feature.afternote.presentation.editor.mapper.toAfternoteEditorReceivers
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 수신자 선택 화면 UI 상태. */
data class SelectReceiverUiState(
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val receivers: List<AfternoteEditorReceiver> = emptyList(),
    val selectedReceiverId: Long? = null,
)

/**
 * 애프터노트 에디터의 수신자 선택 화면 ViewModel (#540).
 *
 * 서버 `GET users/receivers` 는 액세스 토큰으로 호출자를 식별하므로 파라미터가 없다 —
 * 별도의 userId 없이 [UserRepository.getReceivers] 를 그대로 쓴다.
 *
 * 선택은 단일이다: 완료 시 에디터에는 수신자 id 하나만 SavedStateHandle 로 돌려주고
 * (`SELECTED_RECEIVER_ID_KEY`), 여러 명 지정은 화면 재진입 반복으로 한다 —
 * 폼 쪽 `addReceiverIfAbsent` 가 중복 추가를 거른다.
 *
 * 설정의 수신자 등록 화면 왕복(#1427)은 이 ViewModel 이 소유한다. 등록 화면은 setting 소유라
 * 새 수신자 id 를 돌려주지 않으므로, 진입 직전의 목록을 [SavedStateHandle] 에 적어 두고 복귀 후
 * 재조회에서 **새로 생긴 id** 를 가려내 선택한다. 두 값 모두 SavedStateHandle 에 있어 등록 화면에
 * 머무는 동안 프로세스가 재생성돼도 복귀 시 선택이 살아난다.
 */
@HiltViewModel
class SelectReceiverViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val errorReporter: ErrorReporter,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                SelectReceiverUiState(selectedReceiverId = savedStateHandle.get<Long>(SELECTED_RECEIVER_ID_STATE_KEY)),
            )
        val uiState: StateFlow<SelectReceiverUiState> = _uiState.asStateFlow()

        /** 진행 중인 조회. 늦게 시작한 조회가 이기도록 새 조회가 이전 것을 끊는다. */
        private var loadJob: Job? = null

        init {
            refresh()
        }

        /** 수신자 목록을 (재)조회한다. 실패 화면의 "다시 시도" 도 여기로 온다. */
        fun refresh() {
            load(selectNewlyRegistered = false)
        }

        /**
         * "수신자 등록하기"/"새 수신자 등록" 진입 직전에 호출 (#1427).
         *
         * 지금 목록을 적어 두는 것이 곧 «등록 왕복 중» 표시다 — 복귀 시 이 스냅샷에 없던 id 가
         * 방금 등록한 수신자다.
         */
        fun onReceiverRegisterStart() {
            savedStateHandle[KNOWN_RECEIVER_IDS_STATE_KEY] =
                _uiState.value.receivers
                    .map { it.id }
                    .toLongArray()
        }

        /**
         * 등록 화면에서 돌아왔을 때(ON_RESUME) 호출 (#1427).
         *
         * [onReceiverRegisterStart] 를 거치지 않은 진입에서는 아무것도 하지 않는다 — 첫 진입의
         * ON_RESUME 이 `init` 조회를 한 번 더 돌리지 않게 하는 가드다. 등록을 취소하고 돌아온
         * 경우에도 새 id 가 없어 선택은 그대로 남는다.
         */
        fun refreshAfterReceiverRegister() {
            if (savedStateHandle.get<LongArray>(KNOWN_RECEIVER_IDS_STATE_KEY) == null) return
            load(selectNewlyRegistered = true)
        }

        /** 같은 수신자를 다시 탭하면 해제, 다른 수신자를 탭하면 교체하는 단일 선택. */
        fun toggleReceiverSelection(receiverId: Long) {
            _uiState.update { state ->
                state.copy(
                    selectedReceiverId = if (state.selectedReceiverId == receiverId) null else receiverId,
                )
            }
            persistSelection(_uiState.value.selectedReceiverId)
        }

        private fun load(selectNewlyRegistered: Boolean) {
            // 프로세스 재생성 직후엔 init 조회와 등록 복귀 조회가 함께 뜬다. 끊지 않으면 먼저 뜬
            // init 조회가 나중에 끝나면서 방금 등록한 수신자 선택을 덮어쓴다.
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true, loadFailed = false) }
                    runCatchingCancellable { userRepository.getReceivers() }
                        .onSuccess { receivers ->
                            val newlyRegisteredId =
                                if (selectNewlyRegistered) newlyRegisteredIdOrNull(receivers) else null
                            if (selectNewlyRegistered) {
                                savedStateHandle.remove<LongArray>(KNOWN_RECEIVER_IDS_STATE_KEY)
                            }
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    receivers = receivers.toAfternoteEditorReceivers(),
                                    // 재조회로 목록에서 사라진 수신자를 가리키는 선택은 해제한다 —
                                    // 남겨 두면 완료 버튼이 이미 없는 id 를 에디터로 돌려보낸다.
                                    selectedReceiverId =
                                        newlyRegisteredId
                                            ?: state.selectedReceiverId?.takeIf { selected ->
                                                receivers.any { it.receiverId == selected }
                                            },
                                )
                            }
                            persistSelection(_uiState.value.selectedReceiverId)
                        }.onFailure { e ->
                            errorReporter.recordAfternoteFailure(AfternoteFailureStage.RECEIVER_SELECT_LOAD, e)
                            _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                        }
                }
        }

        /**
         * 등록 왕복 전 스냅샷에 없던 id — 방금 등록한 수신자다.
         *
         * 한 번의 왕복에서 새로 생기는 id 는 하나다. 둘 이상이면(다른 기기에서 함께 등록되는 등)
         * 어느 쪽이 방금 등록한 것인지 알 수 없어 자동 선택하지 않는다 — 엉뚱한 수신자를 골라
         * 두는 것보다 사용자가 직접 고르게 하는 편이 낫다.
         */
        private fun newlyRegisteredIdOrNull(receivers: List<Receiver>): Long? {
            val known = savedStateHandle.get<LongArray>(KNOWN_RECEIVER_IDS_STATE_KEY)?.toSet() ?: return null
            return receivers.map { it.receiverId }.filterNot { it in known }.singleOrNull()
        }

        private fun persistSelection(receiverId: Long?) {
            savedStateHandle[SELECTED_RECEIVER_ID_STATE_KEY] = receiverId
        }

        private companion object {
            /** 프로세스 재생성 후에도 살아남아야 하는 화면 내 선택. */
            const val SELECTED_RECEIVER_ID_STATE_KEY = "select_receiver_selected_id"

            /** 등록 왕복 진입 직전의 목록 스냅샷. 존재 자체가 «등록 왕복 중» 표시다. */
            const val KNOWN_RECEIVER_IDS_STATE_KEY = "select_receiver_known_receiver_ids"
        }
    }

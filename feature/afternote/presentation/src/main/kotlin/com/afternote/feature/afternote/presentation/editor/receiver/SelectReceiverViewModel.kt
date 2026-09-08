package com.afternote.feature.afternote.presentation.editor.receiver

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.feature.afternote.presentation.editor.mapper.toAfternoteEditorReceivers
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import dagger.hilt.android.lifecycle.HiltViewModel
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
    /** 서버에서 불러온 내 수신자 목록 — 설정 › 수신자 관리에서 등록한 그 사람들. 화면의 행이다. */
    val receivers: List<AfternoteEditorReceiver> = emptyList(),
    /**
     * 체크된 수신자 id — 체크한 순서를 보존한다. 완료 시 이 목록이 그대로 에디터로 돌아간다.
     *
     * 화면을 열 때 에디터 폼에 담겨 있던 수신자로 시작하고([SelectReceiverViewModel.applyPreselection]),
     * 목록이 오면 목록에 없는 id 는 뺀다([SelectReceiverViewModel.refresh]).
     *
     * 빈 목록이 «아무도 선택하지 않음» 이다. null 로 없음을 표현하지 않는다 (#1426).
     */
    val selectedReceiverIds: List<Long> = emptyList(),
)

/**
 * 애프터노트 에디터의 수신자 선택 화면 ViewModel (#540).
 *
 * 서버 `GET users/receivers` 는 액세스 토큰으로 호출자를 식별하므로 파라미터가 없다 —
 * 별도의 userId 없이 [UserReceiverRepository.getReceivers] 를 그대로 쓴다.
 *
 * 선택은 복수다 (#1426): 한 번 진입해 여러 명을 고르고, 완료 시 확정된 id 전체를
 * `SELECTED_RECEIVER_IDS_KEY` 로 에디터에 돌려준다. 화면은 에디터 폼에 이미 담겨 있던 수신자를
 * 체크된 상태로 열고([applyPreselection]), 돌려주는 목록이 곧 폼의 수신자 전체가 된다 —
 * 화면에서 체크를 푼 수신자는 폼에서도 빠진다.
 *
 * 화면 내 선택과 «폼 수신자는 이미 넣었다» 는 사실은 [SavedStateHandle] 에 적어 둔다 (#1427). 백그라운드에서
 * 프로세스가 재생성돼도 선택이 그대로 복원되고, 복귀 시 다시 도는 [applyPreselection] 이 사용자가 푼
 * 폼 수신자를 되살리지 않는다.
 */
@HiltViewModel
class SelectReceiverViewModel
    @Inject
    constructor(
        private val userReceiverRepository: UserReceiverRepository,
        private val errorReporter: ErrorReporter,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                SelectReceiverUiState(
                    selectedReceiverIds = savedStateHandle.get<LongArray>(SELECTED_RECEIVER_IDS_STATE_KEY)?.toList().orEmpty(),
                ),
            )
        val uiState: StateFlow<SelectReceiverUiState> = _uiState.asStateFlow()

        private var isPreselectionApplied: Boolean
            get() = savedStateHandle[PRESELECTION_APPLIED_STATE_KEY] ?: false
            set(value) {
                savedStateHandle[PRESELECTION_APPLIED_STATE_KEY] = value
            }

        init {
            refresh()
        }

        /**
         * 에디터 폼에 이미 담겨 있던 수신자를 체크된 상태로 연다 (#1426).
         *
         * 화면 최초 진입에만 반영한다. 이 함수는 폼의 수신자를 선택에 **더하는** 것이고 폼은 완료 전엔
         * 안 바뀌므로, 리컴포지션·회전·다른 화면에서 재진입할 때 다시 넣으면 사용자가 방금 푼 체크가
         * 되살아난다 — `LaunchedEffect(Unit)` 은 리컴포지션만 막고, 회전·재진입·프로세스 재생성은
         * [SavedStateHandle] 에 적힌 [isPreselectionApplied] 가 막는다.
         *
         * 폼의 id 는 화면이 뜨자마자 오고 목록은 서버 응답이라 그보다 늦다. 목록에 없는 id 는 [refresh] 가
         * 목록을 받으며 뺀다.
         */
        fun applyPreselection(formReceiverIds: List<Long>) {
            if (isPreselectionApplied) return
            isPreselectionApplied = true
            if (formReceiverIds.isEmpty()) return
            _uiState.update { state ->
                state.copy(selectedReceiverIds = (formReceiverIds + state.selectedReceiverIds).distinct())
            }
            persistSelection(_uiState.value.selectedReceiverIds)
        }

        /** 수신자 목록을 (재)조회한다. 실패 화면의 "다시 시도" 도 여기로 온다. */
        fun refresh() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, loadFailed = false) }
                runCatchingCancellable { userReceiverRepository.getReceivers() }
                    .onSuccess { receivers ->
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                receivers = receivers.toAfternoteEditorReceivers(),
                                // 목록에 없는 수신자 선택은 해제한다 — 폼에서 넘어왔지만 설정에서 지워진 id,
                                // 재조회로 사라진 id. 남겨 두면 완료 버튼이 이미 없는 id 를 에디터로 돌려보낸다.
                                selectedReceiverIds =
                                    state.selectedReceiverIds.filter { selected ->
                                        receivers.any { it.receiverId == selected }
                                    },
                            )
                        }
                        persistSelection(_uiState.value.selectedReceiverIds)
                    }.onFailure { e ->
                        errorReporter.recordAfternoteFailure(AfternoteFailureStage.RECEIVER_SELECT_LOAD, e)
                        _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                    }
            }
        }

        /** 탭한 수신자를 선택 목록에 더하고, 이미 선택된 수신자를 다시 탭하면 그 항목만 뺀다 (#1426). */
        fun toggleReceiverSelection(receiverId: Long) {
            _uiState.update { state ->
                val selected = state.selectedReceiverIds
                state.copy(
                    selectedReceiverIds =
                        if (receiverId in selected) selected - receiverId else selected + receiverId,
                )
            }
            persistSelection(_uiState.value.selectedReceiverIds)
        }

        private fun persistSelection(receiverIds: List<Long>) {
            savedStateHandle[SELECTED_RECEIVER_IDS_STATE_KEY] = receiverIds.toLongArray()
        }

        private companion object {
            /** 프로세스 재생성 후에도 살아남아야 하는 화면 내 선택. */
            const val SELECTED_RECEIVER_IDS_STATE_KEY = "select_receiver_selected_ids"

            /** 폼 수신자를 선택에 이미 넣었다는 표시. 재생성 뒤 [applyPreselection] 이 다시 넣지 않게 한다. */
            const val PRESELECTION_APPLIED_STATE_KEY = "select_receiver_preselection_applied"
        }
    }

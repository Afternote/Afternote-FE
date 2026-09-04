package com.afternote.feature.afternote.presentation.editor.receiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.afternote.presentation.editor.mapper.toAfternoteEditorReceivers
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver
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
    val receivers: List<AfternoteEditorReceiver> = emptyList(),
    /**
     * 사용자가 고른(또는 폼에서 넘어온) 수신자 id 원본 — 탭한 순서를 보존한다.
     *
     * 목록에 없는 id 도 여기엔 남는다. 화면과 에디터가 보는 선택은 [selectedReceiverIds] 다.
     */
    val chosenReceiverIds: List<Long> = emptyList(),
) {
    /**
     * 화면에 그려지고 완료 시 에디터로 돌아가는 선택 — [chosenReceiverIds] 중 로드된 [receivers] 에 있는 id 만.
     *
     * 원본과 목록의 교집합으로 **파생**한다. 초기 선택([SelectReceiverViewModel.applyPreselection])과
     * 목록 응답 중 어느 쪽이 먼저 오든 결과가 같고, 재조회로 목록에서 사라진 수신자는 저절로 빠진다 —
     * 남겨 두면 완료 버튼이 이미 없는 id 를 에디터로 돌려보낸다.
     *
     * 빈 목록이 «아무도 선택하지 않음» 이다. null 로 없음을 표현하지 않는다 (#1426).
     */
    val selectedReceiverIds: List<Long>
        get() = chosenReceiverIds.filter { id -> receivers.any { it.id == id } }
}

/**
 * 애프터노트 에디터의 수신자 선택 화면 ViewModel (#540).
 *
 * 서버 `GET users/receivers` 는 액세스 토큰으로 호출자를 식별하므로 파라미터가 없다 —
 * 별도의 userId 없이 [UserRepository.getReceivers] 를 그대로 쓴다.
 *
 * 선택은 복수다 (#1426): 한 번 진입해 여러 명을 고르고, 완료 시 확정된 id 전체를
 * `SELECTED_RECEIVER_IDS_KEY` 로 에디터에 돌려준다. 화면은 에디터 폼에 이미 있는 수신자를
 * 선택 상태로 열고([applyPreselection]), 돌려주는 목록이 곧 폼의 수신자 전체가 된다 —
 * 화면에서 푼 수신자는 폼에서도 빠진다.
 */
@HiltViewModel
class SelectReceiverViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SelectReceiverUiState())
        val uiState: StateFlow<SelectReceiverUiState> = _uiState.asStateFlow()

        private var isPreselectionApplied = false

        init {
            refresh()
        }

        /**
         * 에디터 폼에 이미 있는 수신자를 선택 상태로 연다 (#1426).
         *
         * 화면 최초 진입에만 반영한다 — 재구성마다 다시 넣으면 사용자가 방금 푼 선택이 되살아난다.
         * 목록 응답보다 먼저 와도 늦게 와도 된다 — 보이는 선택은
         * [SelectReceiverUiState.selectedReceiverIds] 가 목록과의 교집합으로 파생한다.
         */
        fun applyPreselection(receiverIds: List<Long>) {
            if (isPreselectionApplied) return
            isPreselectionApplied = true
            if (receiverIds.isEmpty()) return
            _uiState.update { state ->
                state.copy(chosenReceiverIds = (receiverIds + state.chosenReceiverIds).distinct())
            }
        }

        /** 수신자 목록을 (재)조회한다. 실패 화면의 "다시 시도" 도 여기로 온다. */
        fun refresh() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, loadFailed = false) }
                runCatchingCancellable { userRepository.getReceivers() }
                    .onSuccess { receivers ->
                        _uiState.update { state ->
                            // 재조회로 목록에서 사라진 수신자 선택은 따로 지우지 않는다 —
                            // selectedReceiverIds 가 목록과의 교집합으로 파생되므로 저절로 빠진다.
                            state.copy(
                                isLoading = false,
                                receivers = receivers.toAfternoteEditorReceivers(),
                            )
                        }
                    }.onFailure { e ->
                        errorReporter.recordAfternoteFailure(AfternoteFailureStage.RECEIVER_SELECT_LOAD, e)
                        _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                    }
            }
        }

        /** 탭한 수신자를 선택 목록에 더하고, 이미 선택된 수신자를 다시 탭하면 그 항목만 뺀다 (#1426). */
        fun toggleReceiverSelection(receiverId: Long) {
            _uiState.update { state ->
                val chosen = state.chosenReceiverIds
                state.copy(
                    chosenReceiverIds =
                        if (receiverId in chosen) chosen - receiverId else chosen + receiverId,
                )
            }
        }
    }

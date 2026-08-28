package com.afternote.feature.afternote.presentation.author.editor.receiver.select

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.afternote.presentation.author.editor.mapper.toAfternoteEditorReceivers
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
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
 */
@HiltViewModel
class SelectReceiverViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val internalState = MutableStateFlow(SelectReceiverUiState())
        val uiState: StateFlow<SelectReceiverUiState> = internalState.asStateFlow()

        init {
            refresh()
        }

        /** 수신자 목록을 (재)조회한다. 실패 화면의 "다시 시도" 도 여기로 온다. */
        fun refresh() {
            viewModelScope.launch {
                internalState.update { it.copy(isLoading = true, loadFailed = false) }
                runCatchingCancellable { userRepository.getReceivers() }
                    .onSuccess { receivers ->
                        internalState.update { state ->
                            state.copy(
                                isLoading = false,
                                receivers = receivers.toAfternoteEditorReceivers(),
                                // 재조회로 목록에서 사라진 수신자를 가리키는 선택은 해제한다 —
                                // 남겨 두면 완료 버튼이 이미 없는 id 를 에디터로 돌려보낸다.
                                selectedReceiverId =
                                    state.selectedReceiverId?.takeIf { selected ->
                                        receivers.any { it.receiverId == selected }
                                    },
                            )
                        }
                    }.onFailure { e ->
                        errorReporter.recordAfternoteFailure(AfternoteFailureStage.RECEIVER_SELECT_LOAD, e)
                        internalState.update { it.copy(isLoading = false, loadFailed = true) }
                    }
            }
        }

        /** 같은 수신자를 다시 탭하면 해제, 다른 수신자를 탭하면 교체하는 단일 선택. */
        fun toggleReceiverSelection(receiverId: Long) {
            internalState.update { state ->
                state.copy(
                    selectedReceiverId = if (state.selectedReceiverId == receiverId) null else receiverId,
                )
            }
        }
    }

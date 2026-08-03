package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 홈 탭 MEMORIES 카드에 얹을 **가장 최근 데일리질문 답변** 한 건.
 *
 * 카드가 "질문 + 답변" 꼴이라 두 출처(일기·데일리질문) 중 형태가 맞는 쪽만 쓴다.
 * 조회 실패는 화면에 올리지 않는다 — 홈의 보조 카드라 실패해도 작성 유도 문구로 떨어지는
 * 편이 낫고, 같은 화면의 다른 실패 안내와 겹치면 소음이 된다.
 */
@HiltViewModel
class MemoriesCardViewModel
    @Inject
    constructor(
        private val repository: DailyQuestionRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MemoriesCardUiState())
        val uiState: StateFlow<MemoriesCardUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        /** 홈 복귀·기록 저장 후 최신 기록을 다시 집는다. */
        fun refreshOnReturn() = load()

        private fun load() {
            viewModelScope.launch {
                repository
                    .getList()
                    .onSuccess { questions ->
                        // 서버는 최신순으로 내려주지만 순서를 계약으로 보지 않는다 — 첫 건만 쓴다.
                        val latest = questions.firstOrNull { !it.isDraft }
                        _uiState.update {
                            it.copy(question = latest?.title, answer = latest?.content)
                        }
                    }
            }
        }
    }

/** null 이면 표시할 기록이 없다 — 카드가 작성 유도 문구로 떨어진다. */
data class MemoriesCardUiState(
    val question: String? = null,
    val answer: String? = null,
)

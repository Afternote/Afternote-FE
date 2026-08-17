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
 * 조회 실패는 화면에 올리지 않는다 — 홈의 보조 카드라 기록 0건과 같은 모습(질문·답변 줄이
 * 그려지지 않음)으로 떨어지는 편이 낫고, 같은 화면의 다른 실패 안내와 겹치면 소음이 된다.
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
                        // 첫 건이 곧 최신이다 — `GET /api/v1/daily-questions` 가 명세에서
                        // "특정 날짜 혹은 전체 답변 목록을 최신순으로 조회합니다" 로 정렬을 계약한다.
                        // 같은 API 가 `draftOnly` 생략 시 제출 완료분만 주지만, 파라미터를 무시하는
                        // 서버를 만나도 초안이 카드에 오르지 않도록 `!isDraft` 재확인은 남겨둔다.
                        val latest = questions.firstOrNull { !it.isDraft }
                        _uiState.update {
                            it.copy(question = latest?.title, answer = latest?.content)
                        }
                    }
            }
        }
    }

/** null 이면 표시할 기록이 없다 — 카드가 질문·답변 줄을 그리지 않는다 (0건 시안 미확정, #559). */
data class MemoriesCardUiState(
    val question: String? = null,
    val answer: String? = null,
)

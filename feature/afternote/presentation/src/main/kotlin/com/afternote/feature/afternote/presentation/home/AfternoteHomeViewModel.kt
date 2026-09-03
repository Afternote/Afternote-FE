package com.afternote.feature.afternote.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import com.afternote.feature.afternote.presentation.shared.component.ListItemUiModel
import com.afternote.feature.afternote.presentation.shared.util.getIconResForService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 페이지네이션·새로고침은 Paging 3가 담당하며, ViewModel은 카테고리 필터만 보유한다.
 * CUD 후 목록 갱신은 Repository 내부에서 자동 처리되므로 수동 refresh는 불필요하다.
 */
@HiltViewModel
class AfternoteHomeViewModel
    @Inject
    constructor(
        private val afternoteRepository: AfternoteRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        /** 선택된 종류 필터. `null` 은 전체다. */
        private val _selectedType = MutableStateFlow<AfternoteType?>(null)
        val selectedType: StateFlow<AfternoteType?> = _selectedType.asStateFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val pagedAfternotes: Flow<PagingData<ListItemUiModel>> =
            _selectedType
                .flatMapLatest { type ->
                    afternoteRepository
                        .getPagedAfternotes(type)
                        .map { pagingData -> pagingData.map { it.toUiModel() } }
                }.cachedIn(viewModelScope)

        fun selectTab(tab: AfternoteType?) {
            if (_selectedType.value == tab) return
            _selectedType.value = tab
        }

        /**
         * 마지막으로 기록한 목록 실패의 예외 타입. 같은 장애가 이어지는 동안 재기록을 막는 기준이다 —
         * 예외 «인스턴스» 로 비교하면 Paging 이 재시도마다 새 예외를 만들어 중복 억제가 성립하지 않는다.
         */
        private var lastReportedFailureType: String? = null

        /**
         * 목록 로드(refresh·append) 실패를 계측한다 (#705).
         *
         * Paging 은 실패를 `LoadState.Error` 로만 알리고 삼키므로, 화면이 알려 주지 않으면 콘솔에
         * 흔적이 남지 않는다. 사용자가 «다시 시도» 를 연타하면 같은 실패가 그 횟수만큼 도착하는데,
         * Crashlytics non-fatal 은 최근 8건만 보관하므로 한 장애가 보관함을 통째로 밀어낸다.
         * 그래서 «장애가 이어지는 동안 한 번» 만 기록하고, 로드가 성공하면([onListLoadSucceeded])
         * 다음 실패를 새 사건으로 다시 받는다.
         */
        fun onListLoadFailed(throwable: Throwable) {
            val failureType = throwable.javaClass.name
            if (lastReportedFailureType == failureType) return
            lastReportedFailureType = failureType
            errorReporter.recordAfternoteFailure(AfternoteFailureStage.LIST_LOAD, throwable)
        }

        /** 목록 로드가 성공해 실패 구간이 끝났음을 알린다 — 다음 실패는 새 사건으로 기록된다. */
        fun onListLoadSucceeded() {
            lastReportedFailureType = null
        }
    }

private fun ListItem.toUiModel(): ListItemUiModel =
    ListItemUiModel(
        id = id,
        serviceName = serviceName,
        date = date,
        iconResId = getIconResForService(serviceName, type),
        type = type,
    )

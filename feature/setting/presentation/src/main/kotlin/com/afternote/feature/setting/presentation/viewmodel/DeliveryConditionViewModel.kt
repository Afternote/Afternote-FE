package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.delivery.ConditionState
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryConditionViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val receiverId = savedStateHandle.toRoute<SettingRoute.AfterDeliveryRoute>().receiverId

        private val _uiState = MutableStateFlow(DeliveryConditionUiState())
        val uiState: StateFlow<DeliveryConditionUiState> = _uiState.asStateFlow()

        private val _saveSuccess = Channel<Unit>(Channel.BUFFERED)
        val saveSuccess = _saveSuccess.receiveAsFlow()

        /** 진행 중인 조회 — 첫 진입 이후의 ON_RESUME 이 실행 중인 로드와 겹치면 건너뛰기 위한 가드. */
        private var loadJob: Job? = null

        /**
         * 다음 [refreshOnReturn] 이 첫 ON_RESUME(진입 자체)인지. 첫 resume 은 init 로드와 같은
         * 진입이므로 갱신하지 않는다 — VM 필드인 이유는 ReceiverHomeViewModel 의 refreshOnReturn 과
         * 동일, 프로세스 사망 후 복원에서도 init 로드와 수명이 일치한다.
         */
        private var isFirstResume = true

        private var conditionEditRevision = 0
        private var savedConditionRevision = 0

        init {
            loadDeliveryConditions()
        }

        /** 다른 화면에서 복귀했을 때의 자동 갱신 (#701). 첫 진입은 건너뛰고, 로드가 겹치면 건너뛴다. */
        fun refreshOnReturn() {
            if (isFirstResume) {
                isFirstResume = false
                return
            }
            if (loadJob?.isActive == true || _uiState.value.isSaving) return
            loadDeliveryConditions(isAutomatic = true)
        }

        private fun loadDeliveryConditions(isAutomatic: Boolean = false) {
            loadJob =
                viewModelScope.launch {
                    if (!isAutomatic) _uiState.update { it.copy(isLoading = true) }
                    runCatchingCancellable { userRepository.getReceiverDeliveryConditions(receiverId) }
                        .onSuccess { response ->
                            val representative =
                                response.conditions.firstOrNull {
                                    it.contentType == DeliveryContentType.TIME_LETTER
                                }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isInitialized = true,
                                    conditionType =
                                        if (conditionEditRevision != savedConditionRevision) {
                                            it.conditionType
                                        } else {
                                            representative?.conditionType ?: DeliveryConditionType.INACTIVITY
                                        },
                                    inactivityPeriod =
                                        if (conditionEditRevision != savedConditionRevision) {
                                            it.inactivityPeriod
                                        } else {
                                            representative?.inactivityPeriod ?: InactivityPeriod.ONE_YEAR
                                        },
                                    conditions = response.conditions,
                                    error = it.error.takeUnless { error -> error == DeliveryConditionError.LOAD_FAILED },
                                )
                            }
                        }.onFailure {
                            if (!isAutomatic || !_uiState.value.isInitialized) {
                                _uiState.update { it.copy(isLoading = false, error = DeliveryConditionError.LOAD_FAILED) }
                            }
                        }
                }
        }

        fun onConditionTypeSelected(index: Int) {
            conditionEditRevision++
            val conditionType =
                if (index == 1) DeliveryConditionType.RECEIVER_REQUEST else DeliveryConditionType.INACTIVITY
            _uiState.update { it.copy(conditionType = conditionType) }
        }

        fun onSave() {
            val state = _uiState.value
            if (!state.isInitialized || state.isSaving) return
            loadJob?.cancel()
            val savingRevision = conditionEditRevision

            val hasTimeLetterCondition =
                state.conditions.any { it.contentType == DeliveryContentType.TIME_LETTER }
            val updatedConditions =
                state.conditions
                    .map { condition ->
                        if (condition.contentType == DeliveryContentType.TIME_LETTER) {
                            condition.copy(
                                conditionType = state.conditionType,
                                inactivityPeriod =
                                    state.inactivityPeriod.takeIf {
                                        state.conditionType == DeliveryConditionType.INACTIVITY
                                    },
                            )
                        } else {
                            condition
                        }
                    }.let { conditions ->
                        if (hasTimeLetterCondition) {
                            conditions
                        } else {
                            conditions +
                                defaultCondition(DeliveryContentType.TIME_LETTER).copy(
                                    conditionType = state.conditionType,
                                    inactivityPeriod =
                                        state.inactivityPeriod.takeIf {
                                            state.conditionType == DeliveryConditionType.INACTIVITY
                                        },
                                )
                        }
                    }

            _uiState.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                runCatchingCancellable {
                    userRepository.updateReceiverDeliveryConditions(receiverId, updatedConditions)
                }.onSuccess { response ->
                    savedConditionRevision = savingRevision
                    _uiState.update { it.copy(isSaving = false, conditions = response.conditions, error = null) }
                    _saveSuccess.send(Unit)
                }.onFailure {
                    _uiState.update { it.copy(isSaving = false, error = DeliveryConditionError.SAVE_FAILED) }
                }
            }
        }

        private fun defaultCondition(contentType: DeliveryContentType) =
            DeliveryConditionItem(
                contentType = contentType,
                conditionType = DeliveryConditionType.INACTIVITY,
                inactivityPeriod = InactivityPeriod.ONE_YEAR,
                state = ConditionState.ACTIVE,
                fulfilled = false,
                gracePeriodStartedAt = null,
                fulfilledAt = null,
            )
    }

package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.lifecycle.viewModelScope
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

/** 열람 신청 entry가 소유하는 발신자 키와 기존 본인 확인 캐시 관문. */
@HiltViewModel(assistedFactory = DeliveryVerificationFlowViewModel.Factory::class)
internal class DeliveryVerificationFlowViewModel
    @AssistedInject
    constructor(
        @Assisted route: ReceiverRoute.DeliveryVerificationFlowRoute,
        identityVerificationRepository: IdentityVerificationRepository,
    ) : MviViewModel<DeliveryVerificationFlowIntent, DeliveryVerificationFlowUiState, DeliveryVerificationFlowReducerEvent>(
            DeliveryVerificationFlowUiState(senderId = route.senderId),
        ) {
        init {
            viewModelScope.launch {
                identityVerificationRepository.isVerified(route.senderId).collect {
                    dispatch(DeliveryVerificationFlowReducerEvent.IdentityVerificationChanged(it))
                }
            }
        }

        // 사용자 입력은 각 단계 Screen의 Intent와 기존 네비게이션 콜백이 처리한다.
        override fun onIntent(intent: DeliveryVerificationFlowIntent) = Unit

        override fun reduce(
            state: DeliveryVerificationFlowUiState,
            event: DeliveryVerificationFlowReducerEvent,
        ): DeliveryVerificationFlowUiState = reduceDeliveryVerificationFlow(state, event)

        @AssistedFactory
        interface Factory {
            fun create(route: ReceiverRoute.DeliveryVerificationFlowRoute): DeliveryVerificationFlowViewModel
        }
    }

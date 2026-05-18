package com.afternote.feature.afternote.presentation.receiver.navigation

import com.afternote.feature.afternote.domain.repository.receiver.ReceiverRepository
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.IdentityVerificationGate
import com.afternote.feature.afternote.presentation.receiver.recordsbox.SenderRegistry
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 수신자 그래프의 [ReceiverNavActions] 구현체가 ViewModel 스코프 밖에서 필요한 의존성을 받기 위한 EntryPoint.
 *
 * "기록 열람하기"(디자인 12) → 수신자 홈 진입 시 발신자별 authCode 를 글로벌 헤더 컨텍스트에 복원해야 하고,
 * "열람 신청하기"(디자인 11) 진입 시 본인 확인 캐시 상태로 분기해야 한다.
 * 모두 NavGraph 액션 레이어(app 모듈) 에서 일어나 ViewModel 주입 통로가 없어 Hilt EntryPoint 로 받는다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiverFlowEntryPoint {
    fun senderRegistry(): SenderRegistry

    fun receiverRepository(): ReceiverRepository

    fun identityVerificationGate(): IdentityVerificationGate
}

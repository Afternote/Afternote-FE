package com.afternote.feature.afternote.presentation.receiver.navigation

import com.afternote.feature.afternote.presentation.receiver.deliveryverification.IdentityVerificationGate
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 수신자 그래프의 [ReceiverNavActions] 구현체가 ViewModel 스코프 밖에서 필요한 의존성을 받기 위한 EntryPoint.
 *
 * "열람 신청하기"(디자인 11) 진입 시 본인 확인 캐시 상태로 분기하기 위해 [IdentityVerificationGate] 가 필요하다.
 * NavGraph 액션 레이어(app 모듈) 는 ViewModel 주입 통로가 없어 Hilt EntryPoint 로 받는다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiverFlowEntryPoint {
    fun identityVerificationGate(): IdentityVerificationGate
}

package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.afternote.feature.afternote.presentation.receiver.navigation.model.ReceiverRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * 열람 신청 흐름 — 본인 확인(2·3·4) + 마스터 키(5) + 서류 업로드(6·7·8) + 완료(9) — 전체에 걸친
 * flow-scoped ViewModel. [ReceiverRoute.DeliveryVerificationFlowRoute] (nested graph 진입점) 에 binding 되어
 * 자식 라우트들은 `hiltViewModel(parentBackStackEntry)` 로 동일 인스턴스를 공유한다.
 *
 * 1차 도입(#220) 범위:
 * - `senderId` 단일 보유 (자식 라우트에서 nav arg 중복 박지 않음)
 * - [IdentityVerificationGate.isVerified] read-only 위임 — nested graph 시작 시점에 본인 확인 캐시 분기를
 *   자식이 직접 의존하지 않고 flow VM 통해 결정
 *
 * 후속(2차 PR 이후) 으로 옮길 책임:
 * - `authCode`·`ReceiverIdentity` 등 누적 흐름 상태 보유
 * - [com.afternote.feature.afternote.presentation.receiver.recordsbox.SenderRegistry.attachIdentity] 호출 시점 위임
 * - [IdentityVerificationGate] 자체의 흡수·싱글톤 제거
 */
@HiltViewModel
class DeliveryVerificationFlowViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        identityGate: IdentityVerificationGate,
    ) : ViewModel() {
        val senderId: String =
            savedStateHandle.toRoute<ReceiverRoute.DeliveryVerificationFlowRoute>().senderId

        /** 본인 확인 캐시 여부 — Intro 진입 시 즉시 MasterKey 로 jump 할지 판단용. */
        val isIdentityVerified: StateFlow<Boolean> = identityGate.isVerified
    }

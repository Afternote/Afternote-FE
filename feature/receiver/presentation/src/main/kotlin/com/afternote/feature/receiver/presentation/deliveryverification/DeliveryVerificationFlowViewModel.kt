package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 열람 신청 흐름 — 본인 확인(2·3·4) + 마스터 키(5) + 서류 업로드(6·7·8) + 완료(9) — 전체에 걸친
 * flow-scoped ViewModel. [ReceiverRoute.DeliveryVerificationFlowRoute] (nested graph 진입점) 에 binding 되어
 * 자식 라우트들은 `hiltViewModel(parentBackStackEntry)` 로 동일 인스턴스를 공유한다.
 *
 * 1차 도입(#220) 범위:
 * - `senderId` 단일 보유 (자식 라우트에서 nav arg 중복 박지 않음)
 * - [IdentityVerificationRepository.isVerified] read-only 위임 — nested graph 시작 시점에 본인 확인 캐시 분기를
 *   자식이 직접 의존하지 않고 flow VM 통해 결정
 *
 * 후속(2차 PR 이후) 으로 옮길 책임:
 * - `masterKey`·`ReceiverIdentity` 등 누적 흐름 상태 보유
 * - [com.afternote.feature.receiver.presentation.recordsbox.SenderRegistry.attachIdentity] 호출 시점 위임
 * - [IdentityVerificationRepository] 자체의 흡수·싱글톤 제거
 */
@HiltViewModel
class DeliveryVerificationFlowViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        identityVerificationRepository: IdentityVerificationRepository,
    ) : ViewModel() {
        val senderId: String =
            savedStateHandle.toRoute<ReceiverRoute.DeliveryVerificationFlowRoute>().senderId

        /**
         * 본인 확인 캐시 여부 — Intro 진입 시 즉시 MasterKey 로 jump 할지 판단용.
         * 이 흐름의 [senderId] 에 대한 인증만 본다 — 다른 발신자 인증은 이 관문을 열지 않는다 (#597).
         *
         * gate 의 Flow 를 viewModelScope 안에서 StateFlow 로 변환 (collectAsStateWithLifecycle 가 즉시 값 필요).
         *
         * `WhileSubscribed(5_000)`: 구독자 있을 때만 upstream(repository 캐시) collect.
         * 모든 구독자 사라진 후 5초 더 기다리고 멈춤 — configuration change(회전·다크모드 등) 의
         * destroy→recreate 갭(수십 ms) 동안 upstream 재구독 회피용 grace. 5초 후엔 자원 해제.
         * (Google 공식 권장값 — Architecture guide / State production 섹션)
         */
        val isIdentityVerified: StateFlow<Boolean> =
            identityVerificationRepository.isVerified(senderId).stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false,
            )
    }

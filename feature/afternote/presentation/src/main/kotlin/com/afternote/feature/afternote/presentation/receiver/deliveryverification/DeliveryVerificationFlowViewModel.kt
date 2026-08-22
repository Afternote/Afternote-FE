package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.presentation.receiver.navigation.model.ReceiverRoute
import com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository
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
 * [IdentityVerificationRepository.isVerified] read-only 위임 — nested graph 시작 시점에 본인 확인 캐시 분기를
 *   자식이 직접 의존하지 않고 flow VM 통해 결정
 */
@HiltViewModel
class DeliveryVerificationFlowViewModel
    @Inject
    constructor(
        identityVerificationRepository: IdentityVerificationRepository,
    ) : ViewModel() {
        /**
         * 본인 확인 캐시 여부 — Intro 진입 시 즉시 MasterKey 로 jump 할지 판단용.
         *
         * gate 의 Flow 를 viewModelScope 안에서 StateFlow 로 변환 (collectAsStateWithLifecycle 가 즉시 값 필요).
         *
         * `WhileSubscribed(5_000)`: 구독자 있을 때만 upstream(DataStore) collect.
         * 모든 구독자 사라진 후 5초 더 기다리고 멈춤 — configuration change(회전·다크모드 등) 의
         * destroy→recreate 갭(수십 ms) 동안 DataStore 재읽기 회피용 grace. 5초 후엔 자원 해제.
         * (Google 공식 권장값 — Architecture guide / State production 섹션)
         */
        val isIdentityVerified: StateFlow<Boolean> =
            identityVerificationRepository.isVerified.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false,
            )
    }

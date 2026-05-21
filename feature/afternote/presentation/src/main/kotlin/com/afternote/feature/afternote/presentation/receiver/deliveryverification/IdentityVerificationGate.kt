package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 수신자 본인 확인(designs 2·3·4) 완료 여부 게이트 — 발신자 상세 → 열람 신청 진입 시 분기점.
 *
 * 백엔드 `receiver-auth/email/` 엔드포인트가 미구현이라 본 PR(이슈 #215) 범위에서는 본인 확인 UI 도
 * 함께 보류 상태. 본 클래스는 *분기점만 미리 잡아두는 stub* — `isVerified` 가 false 일 때 호출자가
 * "디자인 대기" placeholder 로 처리하고, 디자인 확정 시 본인 확인 화면을 끼워 넣은 뒤 [markVerified] 로
 * 캐시한다.
 *
 * 캐시는 프로세스 메모리에만 보관하며 앱 재시작 시 사라진다 (SenderRegistry 와 동일한 stub 정책).
 * 영구 보관이 필요해지면 DataStore 기반으로 교체한다.
 */
@Singleton
class IdentityVerificationGate
    @Inject
    constructor() {
        private val _isVerified = MutableStateFlow(false)
        val isVerified: StateFlow<Boolean> = _isVerified.asStateFlow()

        fun markVerified() {
            _isVerified.value = true
        }
    }

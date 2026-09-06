package com.afternote.feature.receiver.presentation.recordsbox

import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.repository.SenderRegistryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 받은 기록함의 발신자 카드 접근 창구.
 *
 * 백엔드의 *발신자 라벨 등록 / 발신자 리스트 조회* API 가 미확정이라 등록·조회는
 * 아직 로컬 구현이 담당한다. 실제 DataStore 스키마와 오류 처리는 presentation 밖의
 * [SenderRegistryRepository]에 숨겨, 화면은 저장 방식을 알지 못한다.
 *
 * #598의 수신자별 격리 계약은 여기에 추가하지 않는다. 이 창구는 #599의 프로세스 재시작
 * 복원만 담당한다.
 */
@Singleton
class SenderRegistry
    @Inject
    constructor(
        private val repository: SenderRegistryRepository,
    ) {
        val senders: Flow<List<SenderEntry>> = repository.senders

        /**
         * 새 발신자 등록. 이름은 호출자가 trim·검증한 값을 넘긴다. 동일 이름 중복 허용 (사용자가
         * 부여하는 별칭이라 동명이인 발신자 있을 수 있음).
         */
        suspend fun register(name: String): Result<SenderEntry> = repository.register(name)

        suspend fun findById(id: String): Result<SenderEntry?> = repository.findById(id)

        /**
         * 마스터 키 검증 성공 직후 호출. masterKey + verify 응답 정보를 카드에 결합한다. 이후
         * "기록 열람하기"(12) 진입 시 이 masterKey 를 ReceiverRepository 에 복원해 헤더 컨텍스트를 잡는다.
         */
        suspend fun attachIdentity(
            id: String,
            masterKey: String,
            identity: ReceiverIdentity,
        ): Result<SenderEntry?> = repository.attachIdentity(id = id, masterKey = masterKey, identity = identity)

        /** 발신자 상세에서 최근 조회한 열람 신청 상태를 캐시. */
        suspend fun updateVerificationStatus(
            id: String,
            status: DeliveryVerificationStatus,
        ): Result<SenderEntry?> = repository.updateVerificationStatus(id = id, status = status)
    }

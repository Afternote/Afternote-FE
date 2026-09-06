package com.afternote.feature.receiver.presentation.recordsbox

import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 받은 기록함의 발신자 카드 in-memory stub registry (이슈 #215).
 *
 * 백엔드의 *발신자 라벨 등록 / 발신자 리스트 조회* API 가 미확정이라, 발신자 등록·조회·캐시를
 * 일단 프로세스 메모리에 보관한다. 앱 재시작 시 사라지는 한계가 있으나 디자인 흐름 확인에는 충분.
 *
 * 백엔드 API 가 확정되면 본 클래스를 [com.afternote.feature.afternote.domain] 레이어의
 * Repository 구현으로 대체한다.
 *
 * `@Singleton` 으로 두어 ViewModel 간 동일 인스턴스를 공유한다 (Hilt 기본 SingletonComponent).
 */
@Singleton
class SenderRegistry
    @Inject
    constructor() {
        private val _senders = MutableStateFlow<List<SenderEntry>>(emptyList())
        val senders: StateFlow<List<SenderEntry>> = _senders

        /**
         * 새 발신자 등록. 이름은 호출자가 trim·검증한 값을 넘긴다. 동일 이름 중복 허용 (사용자가
         * 부여하는 별칭이라 동명이인 발신자 있을 수 있음).
         */
        fun register(name: String): SenderEntry {
            val entry =
                SenderEntry(
                    id = UUID.randomUUID().toString(),
                    name = name,
                )
            _senders.update { it + entry }
            return entry
        }

        fun findById(id: String): SenderEntry? = _senders.value.firstOrNull { it.id == id }

        /**
         * 마스터 키 검증 성공 직후 호출. masterKey + verify 응답 정보를 카드에 결합한다. 이후
         * "기록 열람하기"(12) 진입 시 이 masterKey 를 ReceiverRepository 에 복원해 헤더 컨텍스트를 잡는다.
         */
        fun attachIdentity(
            id: String,
            masterKey: String,
            identity: ReceiverIdentity,
        ): SenderEntry? =
            updateById(id) { entry ->
                entry.copy(
                    masterKey = masterKey,
                    realSenderName = identity.senderName,
                    relation = identity.relation,
                )
            }

        /** 발신자 상세에서 최근 조회한 열람 신청 상태를 캐시. */
        fun updateVerificationStatus(
            id: String,
            status: DeliveryVerificationStatus,
        ): SenderEntry? = updateById(id) { entry -> entry.copy(verificationStatus = status) }

        private inline fun updateById(
            id: String,
            transform: (SenderEntry) -> SenderEntry,
        ): SenderEntry? {
            var updated: SenderEntry? = null
            _senders.update { list ->
                list.map { entry ->
                    if (entry.id == id) {
                        transform(entry).also { updated = it }
                    } else {
                        entry
                    }
                }
            }
            return updated
        }
    }

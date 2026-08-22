package com.afternote.feature.afternote.presentation.receiver.recordsbox

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 받은 기록함 화면 전환 사이에서 서버 항목 스냅샷을 공유하는 in-memory store.
 *
 * 목록 원본은 `receiver-auth/record-boxes` 응답이며, 저장된 접근 코드로 [ReceivedRecordsViewModel]이
 * 다시 채운다.
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
         * 마스터 키 검증 성공 직후 호출. authCode + verify 응답 정보를 카드에 결합한다. 이후
         * "기록 열람하기"(12) 진입 시 이 authCode 를 ReceiverRepository 에 복원해 헤더 컨텍스트를 잡는다.
         */
        fun attachIdentity(
            id: String,
            authCode: String,
            identity: ReceiverIdentity,
        ): SenderEntry? =
            updateById(id) { entry ->
                entry.copy(
                    authCode = authCode,
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

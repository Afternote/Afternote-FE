package com.afternote.feature.afternote.presentation.receiver.recordsbox

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
    }

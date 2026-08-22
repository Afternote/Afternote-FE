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
class ReceivedRecordStore
    @Inject
    constructor() {
        private val _recordBoxes = MutableStateFlow<List<ReceivedRecordItem>>(emptyList())
        val recordBoxes: StateFlow<List<ReceivedRecordItem>> = _recordBoxes
        private var recordBoxesById: Map<Long, ReceivedRecordItem> = emptyMap()

        fun findByRecordBoxId(recordBoxId: Long): ReceivedRecordItem? = recordBoxesById[recordBoxId]

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

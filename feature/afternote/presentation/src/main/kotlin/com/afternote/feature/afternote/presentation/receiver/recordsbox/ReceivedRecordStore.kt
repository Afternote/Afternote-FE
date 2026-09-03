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

        /** 조회 응답만 정본으로 사용해 이전 접근 코드의 항목이 섞이지 않게 한다. */
        fun replaceRecordBoxes(entries: List<ReceivedRecordItem>) {
            val entriesById = LinkedHashMap<Long, ReceivedRecordItem>(entries.size)
            entries.forEach { entry ->
                if (entriesById.containsKey(entry.recordBoxId)) {
                    throw DuplicateRecordBoxIdException(entry.recordBoxId)
                }
                entriesById[entry.recordBoxId] = entry
            }

            recordBoxesById = entriesById
            _recordBoxes.value = entries
        }

        /** 접근 코드가 바뀌거나 사라지면 이전 응답 스냅샷을 제거한다. */
        fun clear() {
            recordBoxesById = emptyMap()
            _recordBoxes.value = emptyList()
        }
    }

internal class DuplicateRecordBoxIdException(
    val recordBoxId: Long,
) : RuntimeException("Duplicate recordBoxId in received record boxes: $recordBoxId")

// [WIP / 소속 미확정] MemorySpace — ViewModel (더미 상태 방출).
// 경로: feature/mindrecord/presentation/.../viewmodel/
// API 스펙 확정 후 loadDummyMemories()를 Repository 연동으로 교체하면 UI는 그대로 둔 채 데이터 소스만 갈아끼울 수 있음.

package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.memoryspace.MemoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** MemorySpace 화면용. init에서 더미 목록을 채우며, API 연동 시 Repository 주입으로 데이터 소스만 교체하면 됨. */
@HiltViewModel
class MemorySpaceViewModel
    @Inject
    constructor() : ViewModel() {
        private val _memories = MutableStateFlow<List<MemoryItem>>(emptyList())
        val memories: StateFlow<List<MemoryItem>> = _memories.asStateFlow()

        init {
            loadDummyMemories()
        }

        // TODO: [MOCK_CLEANUP] 백엔드 API 스펙 확정 시 Repository 연동으로 교체 및 삭제.
        private fun loadDummyMemories() {
            _memories.value =
                listOf(
                    MemoryItem(
                        id = 1,
                        imageRes = R.drawable.mindrecord_img,
                        title = "기억 1",
                        date = "2024.11.11",
                        content =
                            "이 순간은 나에게 특별한 의미가 있었습니다. 햇살이 창문을 통해 들어오는 고요한 오후, 나만의 생각에 잠길 수 있었던 시간.\n\n" +
                                "일상 속에서 찾은 작은 평화의 순간들이 모여 나의 삶을 더 풍요롭게 만들어갑니다.",
                        tags = listOf("평온", "일상", "감사"),
                    ),
                    MemoryItem(
                        id = 2,
                        imageRes = R.drawable.mindrecord_img,
                        title = "기억 2",
                        date = "2024.11.12",
                        content = "두 번째 기억 내용.",
                        tags = listOf("추억"),
                    ),
                    MemoryItem(
                        id = 3,
                        imageRes = R.drawable.mindrecord_img,
                        title = "기억 3",
                        date = "2024.11.13",
                        content = "세 번째 기억 내용.",
                        tags = listOf("여행", "힐링"),
                    ),
                    MemoryItem(
                        id = 4,
                        imageRes = R.drawable.mindrecord_img,
                        title = "기억 4",
                        date = "2024.11.14",
                        content = "네 번째 기억 내용.",
                        tags = listOf("가족"),
                    ),
                )
        }
    }

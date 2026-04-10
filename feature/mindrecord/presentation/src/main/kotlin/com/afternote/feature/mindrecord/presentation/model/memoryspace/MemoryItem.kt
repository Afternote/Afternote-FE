// [WIP / 소속 미확정] MemorySpace — UI 모델(화면 상태용 데이터 클래스).
// 경로: feature/mindrecord/presentation/.../model/memoryspace/
// 도메인 엔티티가 아님. API 스펙 확정 후 domain/로 내릴지 여부는 담당자 결정.

package com.afternote.feature.mindrecord.presentation.model.memoryspace

/** MemorySpaceScreen에서 렌더링하는 메모리 항목 UI 모델. */
data class MemoryItem(
    val id: Int,
    val imageRes: Int,
    val title: String,
    val date: String,
    val content: String,
    val tags: List<String>,
)

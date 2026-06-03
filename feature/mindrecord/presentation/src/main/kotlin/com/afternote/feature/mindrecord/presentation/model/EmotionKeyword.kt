package com.afternote.feature.mindrecord.presentation.model

/**
 * 주간 리포트의 감정 키워드 1건.
 *
 * size·offset·color 같은 UI 좌표는 [com.afternote.feature.mindrecord.presentation.component.EmotionKeywordCard]
 * 안에서 키워드 개수(0~4)에 따라 슬롯으로 결정한다. ViewModel 은 keyword·count 만 노출한다
 * (메모리 노트: VM 은 Context/색·치수 금지).
 */
data class EmotionKeyword(
    val keyword: String,
    val count: Int,
)

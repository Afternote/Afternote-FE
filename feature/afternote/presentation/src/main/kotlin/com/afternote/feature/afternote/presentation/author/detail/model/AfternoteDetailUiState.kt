package com.afternote.feature.afternote.presentation.author.detail.model
import com.afternote.feature.afternote.domain.model.author.Detail

/**
 * 애프터노트 상세 화면 UI 상태.
 */
data class AfternoteDetailUiState(
    /** 홈 요약 등에서 가져온 현재 사용자 표시명(상세 UI 라벨용). */
    val authorDisplayName: String = "",
    val isLoading: Boolean = false,
    val detail: Detail? = null,
    val error: String? = null,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val deleteError: String? = null,
)

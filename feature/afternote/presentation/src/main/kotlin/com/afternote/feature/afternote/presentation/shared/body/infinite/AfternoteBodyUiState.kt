package com.afternote.feature.afternote.presentation.shared.body.infinite

import androidx.compose.runtime.Stable
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel

@Stable
data class AfternoteBodyUiState(
    /** 첫 구독·VM 기동 직후와 같이 아직 목록 스냅샷이 없을 때 로딩으로 취급 */
    val isLoading: Boolean = true,
    val visibleItems: List<ListItemUiModel>,
    val selectedCategory: AfternoteCategory = AfternoteCategory.ALL,
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
    val paginationError: String? = null,
)

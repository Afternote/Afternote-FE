package com.afternote.feature.afternote.presentation.shared.component

import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.domain.AfternoteType

/**
 * Display model for a single row in the shared 애프터노트 list (writer main and receiver list).
 * Both features map their domain/UI models to this for a unified look.
 *
 * @param type 분류용 서비스 타입. 항목 클릭 시 detail 화면 분기에 사용.
 */
@Immutable
data class ListItemUiModel(
    val id: Long,
    val serviceName: String,
    val date: String,
    val iconResId: Int,
    val type: AfternoteType,
)

package com.afternote.feature.afternote.presentation.shared.component

import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.presentation.shared.util.getIconResForService

/** 목록 카드는 홈·임시저장 목록이 같은 것을 쓴다 — 변환도 한 곳에 둔다. */
fun ListItem.toUiModel(): ListItemUiModel =
    ListItemUiModel(
        id = id,
        serviceName = serviceName,
        date = date,
        iconResId = getIconResForService(serviceName, type),
        type = type,
    )

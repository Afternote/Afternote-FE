package com.afternote.feature.mindrecord.data.mapper

import com.afternote.feature.mindrecord.data.dto.DeepThoughtItemDto
import com.afternote.feature.mindrecord.domain.model.DeepThought

fun DeepThoughtItemDto.toDomain(): DeepThought =
    DeepThought(
        id = deepThoughtId,
        createdAt = createdAt,
        title = title,
    )

package com.afternote.feature.mindrecord.data.mapper

import com.afternote.feature.mindrecord.data.dto.DeepThoughtCategoryCreateRequest
import com.afternote.feature.mindrecord.data.dto.DeepThoughtCategoryItem
import com.afternote.feature.mindrecord.data.dto.DeepThoughtCategoryMutationResponse
import com.afternote.feature.mindrecord.data.dto.DeepThoughtCategoryUpdateRequest
import com.afternote.feature.mindrecord.data.dto.DeepThoughtCreateRequest
import com.afternote.feature.mindrecord.data.dto.DeepThoughtListItem
import com.afternote.feature.mindrecord.data.dto.DeepThoughtUpdateRequest
import com.afternote.feature.mindrecord.data.dto.RandomDeepThoughtResponse
import com.afternote.feature.mindrecord.domain.model.DeepThought
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategory
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategoryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategoryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCreatePayload
import com.afternote.feature.mindrecord.domain.model.DeepThoughtUpdatePayload
import com.afternote.feature.mindrecord.domain.model.RandomDeepThought

fun DeepThoughtListItem.toDomain(): DeepThought =
    DeepThought(
        deepThoughtId = deepThoughtId,
        title = title,
        content = content,
        category = category,
        isDraft = isDraft,
        tags = tag,
        imageUrl = imageUrl,
    )

fun RandomDeepThoughtResponse.toDomain(): RandomDeepThought =
    RandomDeepThought(
        title = title,
        createdAt = createdAt,
    )

fun DeepThoughtCreatePayload.toRequest(): DeepThoughtCreateRequest =
    DeepThoughtCreateRequest(
        title = title,
        content = content,
        isDraft = isDraft,
        category = category,
        tag = tags,
        imageUrl = imageUrl,
    )

fun DeepThoughtUpdatePayload.toRequest(): DeepThoughtUpdateRequest =
    DeepThoughtUpdateRequest(
        title = title,
        content = content,
        isDraft = isDraft,
        category = category,
        tag = tags,
        imageUrl = imageUrl,
    )

fun DeepThoughtCategoryItem.toDomain(): DeepThoughtCategory =
    DeepThoughtCategory(
        categoryId = categoryId,
        title = title,
    )

fun DeepThoughtCategoryMutationResponse.toDomain(): DeepThoughtCategory =
    DeepThoughtCategory(
        categoryId = categoryId,
        title = title,
    )

fun DeepThoughtCategoryCreatePayload.toRequest(): DeepThoughtCategoryCreateRequest =
    DeepThoughtCategoryCreateRequest(
        category = category,
        deepThoughtId = deepThoughtId,
    )

fun DeepThoughtCategoryUpdatePayload.toRequest(): DeepThoughtCategoryUpdateRequest =
    DeepThoughtCategoryUpdateRequest(
        category = category,
    )

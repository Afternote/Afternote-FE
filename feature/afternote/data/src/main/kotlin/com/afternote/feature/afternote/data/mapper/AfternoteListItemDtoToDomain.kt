package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternoteListItem
import com.afternote.feature.afternote.domain.model.author.ListItem

/**
 * Maps server DTOs to domain models at the boundary only.
 */

fun AfternoteListItem.toDomain() =
    ListItem(
        id = afternoteId.toString(),
        serviceName = title,
        date = formatDateFromServer(createdAt),
        type = categoryToServiceType(category),
    )

fun List<AfternoteListItem>.toDomainList() = map { it.toDomain() }

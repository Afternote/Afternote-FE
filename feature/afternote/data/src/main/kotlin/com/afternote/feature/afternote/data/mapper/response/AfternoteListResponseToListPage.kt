package com.afternote.feature.afternote.data.mapper.response

import com.afternote.feature.afternote.data.dto.AfternoteListResponse
import com.afternote.feature.afternote.data.mapper.toDomainList
import com.afternote.feature.afternote.domain.model.author.ListPage

fun AfternoteListResponse.toListPage() =
    ListPage(
        listItems = content.toDomainList(),
        hasNext = hasNext,
    )

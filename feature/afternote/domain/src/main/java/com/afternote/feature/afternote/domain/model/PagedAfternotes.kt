package com.kuit.afternote.feature.afternote.domain.model

import com.afternote.feature.afternote.domain.model.Item

/**
 * One page of afternotes from GET /afternotes.
 *
 * @param items Items for this page
 * @param hasNext Whether more pages are available
 */
data class PagedAfternotes(
    val items: List<Item>,
    val hasNext: Boolean,
)

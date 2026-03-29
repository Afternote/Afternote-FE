package com.afternote.feature.afternote.presentation.author.edit.ui.provider

import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditReceiver
import com.afternote.feature.afternote.presentation.shared.model.dummy.preview.TimeLetterPreviewItem
import javax.inject.Inject

/**
 * Non-mock implementation (empty until API-backed data is wired).
 */
class RealReceiverDataProvider
    @Inject
    constructor() : ReceiverDataProvider {
        override fun getReceiverList(): List<AfternoteEditReceiver> = emptyList()

        override fun getDefaultReceiverTitle(): String = ""

        override fun getAfternoteListSeedsForReceiverList(): List<Pair<String, String>> = emptyList()

        override fun getAfternoteListSeedsForReceiverDetail(): List<Pair<String, String>> = emptyList()

        override fun getTimeLetterItemsForPreview(): List<TimeLetterPreviewItem> = emptyList()
    }

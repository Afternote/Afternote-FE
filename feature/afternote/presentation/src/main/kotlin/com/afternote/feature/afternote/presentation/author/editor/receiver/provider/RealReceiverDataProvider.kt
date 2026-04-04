package com.afternote.feature.afternote.presentation.author.editor.receiver.provider

import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.shared.model.dummy.preview.TimeLetterPreviewItem
import javax.inject.Inject

/**
 * Non-mock implementation (empty until API-backed data is wired).
 */
class RealReceiverDataProvider
    @Inject
    constructor() : ReceiverDataProvider {
        override fun getReceiverList(): List<AfternoteEditorReceiver> = emptyList()

        override fun getDefaultReceiverTitle(): String = ""

        override fun getAfternoteListSeedsForReceiverList(): List<Pair<String, String>> = emptyList()

        override fun getAfternoteListSeedsForReceiverDetail(): List<Pair<String, String>> = emptyList()

        override fun getTimeLetterItemsForPreview(): List<TimeLetterPreviewItem> = emptyList()
    }

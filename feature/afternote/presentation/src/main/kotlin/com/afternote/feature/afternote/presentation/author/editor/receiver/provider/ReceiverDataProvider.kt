package com.afternote.feature.afternote.presentation.author.editor.receiver.provider

import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.shared.model.dummy.preview.TimeLetterPreviewItem

/**
 * Preview / dev data for receiver-related UI. Real app wiring can replace [RealReceiverDataProvider].
 */
interface ReceiverDataProvider {
    fun getReceiverList(): List<AfternoteEditorReceiver>

    fun getDefaultReceiverTitle(): String

    fun getAfternoteListSeedsForReceiverList(): List<Pair<String, String>>

    fun getAfternoteListSeedsForReceiverDetail(): List<Pair<String, String>>

    fun getTimeLetterItemsForPreview(): List<TimeLetterPreviewItem>
}

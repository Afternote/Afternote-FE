package com.afternote.feature.afternote.presentation.author.editor.receiver.provider

import com.afternote.feature.afternote.presentation.shared.model.dummy.AfternoteEditorReceiverDummies
import com.afternote.feature.afternote.presentation.shared.model.dummy.preview.ReceiverPreviewDummies
import javax.inject.Inject

/**
 * Fake [ReceiverDataProvider] backed by in-app dummy data.
 */
class FakeReceiverDataProvider
    @Inject
    constructor() : ReceiverDataProvider {
        override fun getReceiverList() = AfternoteEditorReceiverDummies.defaultList

        override fun getDefaultReceiverTitle(): String = ReceiverPreviewDummies.defaultReceiverTitle()

        override fun getAfternoteListSeedsForReceiverList() = ReceiverPreviewDummies.defaultAfternoteListSeedsForReceiverList()

        override fun getAfternoteListSeedsForReceiverDetail() = ReceiverPreviewDummies.defaultAfternoteListSeedsForReceiverDetail()

        override fun getTimeLetterItemsForPreview() = ReceiverPreviewDummies.sampleTimeLetterPreviewItems()
    }

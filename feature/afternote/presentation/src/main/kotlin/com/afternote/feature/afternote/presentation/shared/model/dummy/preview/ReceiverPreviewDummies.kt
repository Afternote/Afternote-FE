package com.afternote.feature.afternote.presentation.shared.model.dummy.preview

import com.afternote.feature.afternote.presentation.shared.model.dummy.afternote.AfternoteHomeDummies

object ReceiverPreviewDummies {
    fun defaultReceiverTitle(): String = "수신자"

    fun defaultAfternoteListSeedsForReceiverList(): List<Pair<String, String>> = AfternoteHomeDummies.defaultAfternoteList()

    fun defaultAfternoteListSeedsForReceiverDetail(): List<Pair<String, String>> =
        listOf(
            "인스타그램" to "2025.02.01",
        )

    fun sampleTimeLetterPreviewItems(): List<TimeLetterPreviewItem> =
        listOf(
            TimeLetterPreviewItem(title = "타임레터 미리보기", subtitle = ""),
        )
}

package com.afternote.feature.afternote.presentation.shared.model.dummy

import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver

/** Fake receiver rows for edit / provider previews (not core — avoids core → feature coupling). */
object AfternoteEditorReceiverDummies {
    val defaultList: List<AfternoteEditorReceiver> =
        listOf(
            AfternoteEditorReceiver(
                id = "receiver_1",
                name = "김지은",
                label = "딸",
            ),
            AfternoteEditorReceiver(
                id = "receiver_2",
                name = "김혜성",
                label = "아들",
            ),
            AfternoteEditorReceiver(
                id = "receiver_3",
                name = "박서연",
                label = "조카",
            ),
            AfternoteEditorReceiver(
                id = "receiver_4",
                name = "황은주",
                label = "언니",
            ),
            AfternoteEditorReceiver(
                id = "receiver_5",
                name = "황은경",
                label = "동생",
            ),
        )
}

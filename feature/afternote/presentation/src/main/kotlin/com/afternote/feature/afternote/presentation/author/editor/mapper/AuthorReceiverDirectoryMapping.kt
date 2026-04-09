package com.afternote.feature.afternote.presentation.author.editor.mapper

import com.afternote.feature.afternote.domain.model.author.AuthorReceiverDirectoryEntry
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver

internal fun AuthorReceiverDirectoryEntry.toAfternoteEditorReceiver(): AfternoteEditorReceiver =
    AfternoteEditorReceiver(
        id = receiverId.toString(),
        name = name,
        label = relation,
    )

/** [AuthorReceiverDirectoryEntry] 목록을 Edit 화면용 [AfternoteEditorReceiver] 목록으로 변환합니다. */
internal fun Iterable<AuthorReceiverDirectoryEntry>.toAfternoteEditorReceivers(): List<AfternoteEditorReceiver> =
    map { it.toAfternoteEditorReceiver() }

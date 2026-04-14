package com.afternote.feature.afternote.presentation.author.editor.mapper

import com.afternote.feature.afternote.domain.model.author.AuthorReceiverEntry
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver

internal fun AuthorReceiverEntry.toAfternoteEditorReceiver(): AfternoteEditorReceiver =
    AfternoteEditorReceiver(
        id = receiverId.toString(),
        name = name,
        label = relation,
    )

/** [AuthorReceiverEntry] 목록을 Edit 화면용 [AfternoteEditorReceiver] 목록으로 변환합니다. */
internal fun Iterable<AuthorReceiverEntry>.toAfternoteEditorReceivers(): List<AfternoteEditorReceiver> =
    map { it.toAfternoteEditorReceiver() }

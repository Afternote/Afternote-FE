package com.afternote.feature.afternote.presentation.editor.mapper

import com.afternote.core.model.user.Receiver
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver

private fun Receiver.toAfternoteEditorReceiver(): AfternoteEditorReceiver =
    AfternoteEditorReceiver(
        id = receiverId,
        name = name,
        label = relation,
    )

/** 사용자가 등록한 수신자([Receiver]) 목록을 에디터 표시 모델로 변환합니다. */
internal fun List<Receiver>.toAfternoteEditorReceivers(): List<AfternoteEditorReceiver> = map { it.toAfternoteEditorReceiver() }

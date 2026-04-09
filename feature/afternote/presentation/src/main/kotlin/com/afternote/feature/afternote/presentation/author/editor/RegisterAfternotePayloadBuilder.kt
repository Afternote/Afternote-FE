package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.feature.afternote.domain.model.ProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessagesCodec
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 등록 API용 [RegisterAfternotePayload] 조립. 날짜·메시지 직렬화 등은 State Holder가 아닌 여기서 처리한다.
 */
object RegisterAfternotePayloadBuilder {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    fun fromEditorState(state: AfternoteEditorState): RegisterAfternotePayload {
        val date = LocalDate.now().format(dateFormatter)
        val socialMethods =
            state.processingMethods.map {
                ProcessingMethod(it.id, it.text)
            }
        val galleryMethods =
            state.galleryProcessingMethods.map {
                ProcessingMethod(it.id, it.text)
            }
        val messageBlocks =
            state.editorMessages.map { msg ->
                EditorMessageTextBlock(
                    title = msg.titleState.text.toString(),
                    body = msg.contentState.text.toString(),
                )
            }
        val fullMessage = EditorMessagesCodec.serializeBlocksToPersisted(messageBlocks)

        return RegisterAfternotePayload(
            serviceName =
                if (state.selectedCategory == EditorCategory.MEMORIAL) {
                    EditorCategory.MEMORIAL.displayLabel
                } else {
                    state.selectedService
                },
            date = date,
            accountId = state.idState.text.toString(),
            password = state.passwordState.text.toString(),
            message = fullMessage,
            accountProcessingMethod = state.selectedProcessingMethod.name,
            informationProcessingMethod = state.selectedInformationProcessingMethod.name,
            processingMethods = socialMethods,
            galleryProcessingMethods = galleryMethods,
            atmosphere = state.getAtmosphereForSave(),
        )
    }
}

package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.feature.afternote.domain.model.author.ProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessagesCodec
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 신규 생성·수정 저장 공통으로 서버에 보낼 [RegisterAfternotePayload] 조립.
 * 날짜·메시지 직렬화 등은 State Holder가 아닌 여기서 처리한다.
 */
object SaveAfternotePayloadBuilder {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    fun fromEditorState(state: AfternoteEditorState): RegisterAfternotePayload {
        val form = state.formState.value
        val date = LocalDate.now().format(dateFormatter)
        val socialMethods =
            form.socialProcessingMethods.map {
                ProcessingMethod(it.id, it.text)
            }
        val galleryMethods =
            form.galleryProcessingMethods.map {
                ProcessingMethod(it.id, it.text)
            }
        val fullMessage =
            EditorMessagesCodec.serializeBlocksToPersisted(form.messageBlocks)

        return RegisterAfternotePayload(
            serviceName =
                if (form.selectedCategory == EditorCategory.MEMORIAL) {
                    EditorCategory.MEMORIAL.displayLabel
                } else {
                    form.selectedService
                },
            date = date,
            accountId = state.idState.text.toString(),
            password = state.passwordState.text.toString(),
            message = fullMessage,
            accountProcessingMethod = form.selectedProcessingMethod.name,
            informationProcessingMethod = form.selectedInformationProcessingMethod.name,
            processingMethods = socialMethods,
            galleryProcessingMethods = galleryMethods,
            atmosphere = state.getAtmosphereForSave(),
        )
    }
}

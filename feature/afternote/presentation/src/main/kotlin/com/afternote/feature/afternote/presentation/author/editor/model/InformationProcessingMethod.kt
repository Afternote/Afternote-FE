package com.afternote.feature.afternote.presentation.author.editor.model

import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodOption

/**
 * 정보 처리 방법 (갤러리 및 파일용).
 * 갤러리는 수신자 지정만 지원하며, 수신자는 설정 > 수신자 목록(GET /users/receivers)에서 선택합니다.
 */
enum class InformationProcessingMethod(
    override val title: String,
    override val description: String,
) : ProcessingMethodOption {
    /** 수신자 목록(설정)에 등록된 수신자에게 정보 전달 */
    TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER(
        title = "수신자에게 정보 전달",
        description = "수신자가 직접 로그인하여 정보를 관리할 수 있도록 계정 정보를 전달합니다.",
    ),

    /** 추가 수신자에게 정보 전달 */
    TRANSFER_TO_ADDITIONAL_AFTERNOTE_EDIT_RECEIVER(
        title = "추가 수신자에게 정보 전달",
        description = "설정된 수신자 이외의 추가적인 수신자를 지정하여 해당 정보만 전달합니다.",
    ),
}

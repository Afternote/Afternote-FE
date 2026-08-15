package com.afternote.feature.afternote.presentation.shared.model

import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock

/**
 * 상세 화면의 "남기신 말씀" 한 덩어리.
 *
 * 발신자 상세와 수신 상세가 같은 [com.afternote.feature.afternote.presentation.shared.detail.MessageSection]
 * 을 쓰므로 공용 UI 모델로 둔다. 제목은 선택 입력이라 없으면 빈 문자열이다.
 */
@Immutable
data class MessageBlockUiModel(
    val title: String = "",
    val body: String = "",
)

fun List<LeaveMessageBlock>.toMessageBlockUiModels(): List<MessageBlockUiModel> =
    map { block ->
        MessageBlockUiModel(
            title = block.title.orEmpty(),
            body = block.body,
        )
    }

package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.compose.runtime.Immutable

/**
 * 카테고리별 수신 상세 UI 모델.
 *
 * 발신자([com.afternote.feature.afternote.presentation.author.detail.DetailContentUiModel])와 달리
 * 수신자 카드·작성자 표시명을 보유하지 않는다(받은 본인이 수신자이므로).
 */
sealed interface ReceivedDetailContentUiModel {
    data class SocialNetwork(
        val content: ReceivedSocialNetworkDetailContent,
    ) : ReceivedDetailContentUiModel

    data class Gallery(
        val content: ReceivedGalleryDetailContent,
    ) : ReceivedDetailContentUiModel

    /** 추모 카테고리 수신 상세 디자인 미정. 일단 폴백 화면으로 표시. */
    data object MemorialPending : ReceivedDetailContentUiModel

    /** 서버가 알 수 없는 category 를 내려준 경우. */
    data object Unknown : ReceivedDetailContentUiModel
}

@Immutable
data class ReceivedSocialNetworkDetailContent(
    val serviceName: String = "",
    val accountId: String = "",
    val password: String = "",
    val accountProcessingMethod: String = "",
    val processingMethods: List<String> = emptyList(),
    val message: String = "",
    val finalWriteDate: String = "",
)

@Immutable
data class ReceivedGalleryDetailContent(
    val serviceName: String = "",
    val finalWriteDate: String = "",
    val processingMethodTitle: String = "",
    val processingMethods: List<String> = emptyList(),
    val message: String = "",
)

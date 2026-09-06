package com.afternote.feature.afternote.presentation.detail.account

import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.shared.model.MessageBlockUiModel
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel

/**
 * Display data for [AccountDetailScreen].
 *
 * 소셜 네트워크·비즈니스 두 카테고리가 같은 표시 필드를 공유한다 (이슈 #467).
 * 카탈로그 밖 서비스의 아이콘을 고를 때 서버 category인 [type]을 사용한다.
 */
@Immutable
data class AccountDetailContent(
    val serviceName: String = "",
    val type: AfternoteType = AfternoteType.SOCIAL_NETWORK,
    val accountId: String = "",
    val password: String = "",
    val processingMethods: List<String> = emptyList(),
    val messageBlocks: List<MessageBlockUiModel> = emptyList(),
    val finalWriteDate: String = "",
    val afternoteEditReceivers: List<ReceiverUiModel> = emptyList(),
)

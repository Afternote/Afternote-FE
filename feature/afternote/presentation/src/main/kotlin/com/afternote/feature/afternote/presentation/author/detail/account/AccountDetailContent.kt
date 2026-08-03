package com.afternote.feature.afternote.presentation.author.detail.account

import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel

/**
 * Display data for [AccountDetailScreen].
 *
 * 소셜 네트워크·비즈니스 두 카테고리가 공유한다 (이슈 #467) — 그래서 아이콘 결정에 필요한
 * 카테고리를 화면이 상수로 알 수 없고, [type] 으로 실어 나른다.
 */
@Immutable
data class AccountDetailContent(
    val serviceName: String = "",
    val type: AfternoteType = AfternoteType.SOCIAL_NETWORK,
    val userName: String = "",
    val accountId: String = "",
    val password: String = "",
    val processingMethods: List<String> = emptyList(),
    val message: String = "",
    val finalWriteDate: String = "",
    val afternoteEditReceivers: List<ReceiverUiModel> = emptyList(),
)

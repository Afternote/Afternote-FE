package com.afternote.feature.afternote.presentation.author.detail.account

import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel

/**
 * Display data for [AccountDetailScreen].
 *
 * Used for 소셜 네트워크 category afternote detail.
 */
@Immutable
data class AccountDetailContent(
    val serviceName: String = "",
    val userName: String = "",
    val accountId: String = "",
    val password: String = "",
    val processingMethods: List<String> = emptyList(),
    val message: String = "",
    val finalWriteDate: String = "",
    val afternoteEditReceivers: List<ReceiverUiModel> = emptyList(),
)

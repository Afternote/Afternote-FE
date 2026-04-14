package com.afternote.feature.afternote.presentation.author.detail.socialnetwork

import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel

/**
 * Display data for [SocialNetworkDetailScreen].
 *
 * Used for 소셜 네트워크 category afternote detail.
 */
@Immutable
data class SocialNetworkDetailContent(
    val serviceName: String = "",
    val userName: String = "",
    val accountId: String = "",
    val password: String = "",
    val accountProcessingMethod: String = "",
    val processingMethods: List<String> = emptyList(),
    val message: String = "",
    val finalWriteDate: String = "",
    val afternoteEditReceivers: List<ReceiverUiModel> = emptyList(),
    val iconResId: Int = R.drawable.feature_afternote_img_insta_pattern,
)

package com.afternote.feature.afternote.presentation.author.edit.social
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.presentation.author.edit.model.AccountSection
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditReceiverSection
import com.afternote.feature.afternote.presentation.author.edit.processing.model.ProcessingMethodSection

/**
 * SocialNetworkEditContent 함수의 매개변수를 묶는 data class
 */
@Immutable
data class SocialNetworkEditContentParams(
    val messageState: TextFieldState,
    val accountSection: AccountSection,
    val recipientSection: AfternoteEditReceiverSection? = null,
    val processingMethodSection: ProcessingMethodSection = ProcessingMethodSection(),
)

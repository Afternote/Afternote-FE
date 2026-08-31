package com.afternote.feature.setting.presentation.viewmodel

import androidx.annotation.StringRes
import com.afternote.core.domain.error.ReceiverRequestRejectedException
import com.afternote.core.ui.UiText

internal fun Throwable.toReceiverFailureMessage(
    @StringRes fallbackResId: Int,
): UiText =
    (this as? ReceiverRequestRejectedException)?.userMessage?.let(UiText::Dynamic)
        ?: UiText.Resource(fallbackResId)

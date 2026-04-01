package com.afternote.feature.afternote.presentation.shared

import androidx.compose.runtime.staticCompositionLocalOf
import com.afternote.feature.afternote.presentation.author.edit.provider.AfternoteEditDataProvider

object DataProviderLocals {
    val LocalAfternoteEditDataProvider =
        staticCompositionLocalOf<AfternoteEditDataProvider> {
            error("AfternoteEditDataProvider not provided")
        }
}

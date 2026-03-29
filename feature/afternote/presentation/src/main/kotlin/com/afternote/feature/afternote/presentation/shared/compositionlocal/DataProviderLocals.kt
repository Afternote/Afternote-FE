package com.afternote.feature.afternote.presentation.shared.compositionlocal

import androidx.compose.runtime.staticCompositionLocalOf
import com.afternote.feature.afternote.presentation.author.edit.ui.provider.AfternoteEditDataProvider

object DataProviderLocals {
    val LocalAfternoteEditDataProvider =
        staticCompositionLocalOf<AfternoteEditDataProvider> {
            error("AfternoteEditDataProvider not provided")
        }
}

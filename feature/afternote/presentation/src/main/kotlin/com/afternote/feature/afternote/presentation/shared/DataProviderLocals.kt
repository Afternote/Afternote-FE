package com.afternote.feature.afternote.presentation.shared

import androidx.compose.runtime.staticCompositionLocalOf
import com.afternote.feature.afternote.presentation.author.editor.provider.AfternoteEditorDataProvider

object DataProviderLocals {
    val LocalAfternoteEditorDataProvider =
        staticCompositionLocalOf<AfternoteEditorDataProvider> {
            error("AfternoteEditorDataProvider not provided")
        }
}

package com.afternote.feature.afternote.presentation.author.editor.model
import androidx.compose.runtime.Immutable

/**
 * 정보 처리 방법 섹션
 */
@Immutable
data class InfoMethodSection(
    val selectedMethod: InformationProcessingMethod,
    val onMethodSelected: (InformationProcessingMethod) -> Unit,
)

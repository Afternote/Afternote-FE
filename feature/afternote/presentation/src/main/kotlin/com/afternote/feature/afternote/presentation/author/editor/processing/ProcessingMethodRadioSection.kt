package com.afternote.feature.afternote.presentation.author.editor.processing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afternote.feature.afternote.presentation.author.editor.EditorSectionLabel
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodOption
import com.afternote.feature.afternote.presentation.shared.SelectableRadioCard

/**
 * 처리 방법 라디오 섹션 (라벨 + [SelectableRadioCard] 리스트).
 *
 * 두 enum([com.afternote.feature.afternote.presentation.author.editor.processing.model.AccountProcessingMethod],
 * [com.afternote.feature.afternote.presentation.author.editor.model.InformationProcessingMethod]) 모두
 * [ProcessingMethodOption]을 구현하므로 단일 generic 컴포넌트로 통일합니다.
 */
@Composable
fun <T : ProcessingMethodOption> ProcessingMethodRadioSection(
    label: String,
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        EditorSectionLabel(text = label, isRequired = true)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                SelectableRadioCard(
                    title = option.title,
                    description = option.description,
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

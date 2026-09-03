package com.afternote.feature.afternote.presentation.editor.processing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.EditorSectionLabel
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodSection

/**
 * "처리 방법 리스트" 라벨 + [ProcessingMethodList] 묶음.
 *
 * Social/Gallery 등 여러 에디터 콘텐츠에서 동일한 라벨·간격·콜백 위임 구조가
 * 반복되어 한 컴포넌트로 정리합니다.
 */
@Composable
fun ProcessingMethodListSection(
    section: ProcessingMethodSection,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EditorSectionLabel(
            text = stringResource(R.string.afternote_editor_label_process_method_list),
            isRequired = false,
        )
        ProcessingMethodList(
            items = section.items,
            onItemAdded = section.onItemAdded,
            onItemDeleteClick = section.onItemDeleteClick,
            onItemEdited = section.onItemEdited,
        )
    }
}

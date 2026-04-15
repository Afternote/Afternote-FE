package com.afternote.feature.afternote.presentation.author.editor.processing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.PlusBadgeButton
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import kotlinx.coroutines.launch

/**
 * 처리 방법 리스트 컴포넌트
 *
 * 람다 등 불안정한 참조를 단일 data class로 묶지 않고 평탄한 인자로 받아, 부모 리컴포지션 시
 * 스킵 가능 범위를 넓힙니다(호출부에서 [androidx.compose.runtime.rememberUpdatedState] 등과 함께 사용 권장).
 */
@Composable
fun ProcessingMethodList(
    items: List<ProcessingMethodItem>,
    onItemAdded: (String) -> Unit,
    onItemDeleteClick: (String) -> Unit,
    onItemEdited: (String, String) -> Unit,
    onTextFieldVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    initialShowTextField: Boolean = false,
    initialExpandedItemId: String? = null,
    state: ProcessingMethodListState =
        rememberProcessingMethodListState(
            initialShowTextField = initialShowTextField,
        ),
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // items 참조가 바뀔 때마다 실행되지만, [ProcessingMethodListState.initializeExpandedStates]는
    // 기존 키의 expanded/편집 상태를 보존하고 신규 id만 시드하며, 제거된 행의 상태만 정리한다.
    LaunchedEffect(items, initialExpandedItemId) {
        state.initializeExpandedStates(items, initialExpandedItemId)
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(color = AfternoteDesign.colors.white, shape = RoundedCornerShape(6.dp))
                .border(
                    width = 1.dp,
                    color = AfternoteDesign.colors.gray2,
                    shape = RoundedCornerShape(6.dp),
                ).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(17.dp)) {
            items.forEach { item ->
                key(item.id) {
                    ProcessingMethodCheckbox(
                        item = item,
                        expanded = state.expandedStates[item.id] ?: false,
                        isEditing = state.editingItemId == item.id,
                        callbacks =
                            ProcessingMethodCheckboxCallbacks(
                                onMoreClick = {
                                    focusManager.clearFocus()
                                    state.toggleItemExpanded(item.id)
                                },
                                onDismissDropdown = {
                                    state.expandedStates[item.id] = false
                                },
                                onEditClick = {
                                    state.expandedStates[item.id] = false
                                    // Defer editing to next frame so DropdownMenu dismiss settles first
                                    scope.launch {
                                        withFrameNanos { }
                                        state.startEditing(item.id)
                                    }
                                },
                                onDeleteClick = { onItemDeleteClick(item.id) },
                                onEditConfirmed = { newText ->
                                    onItemEdited(item.id, newText)
                                    state.stopEditing()
                                },
                            ),
                    )
                }
            }
        }

        if (state.showTextField) {
            Spacer(modifier = Modifier.height(6.dp))
            AddItemTextField(
                onItemAdded = onItemAdded,
                onVisibilityChanged = onTextFieldVisibilityChanged,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PlusBadgeButton(
            contentDescription = stringResource(R.string.afternote_editor_content_description_add),
            onClick = { state.toggleTextField() },
            plusSize = 13.dp,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProcessingMethodListPreview() {
    AfternoteTheme {
        ProcessingMethodList(
            items =
                listOf(
                    ProcessingMethodItem("1", "게시물 내리기"),
                    ProcessingMethodItem("2", "댓글 비활성화"),
                ),
            onItemDeleteClick = {},
            onItemAdded = {},
            onItemEdited = { _, _ -> },
            onTextFieldVisibilityChanged = {},
            initialShowTextField = true,
        )
    }
}

@Preview(showBackground = true, name = "드롭다운 펼쳐진 상태")
@Composable
private fun ProcessingMethodListWithDropdownPreview() {
    AfternoteTheme {
        ProcessingMethodList(
            items =
                listOf(
                    ProcessingMethodItem("1", "게시물 내리기"),
                    ProcessingMethodItem("2", "댓글 비활성화"),
                    ProcessingMethodItem("3", "추모 계정으로 전환하기"),
                ),
            onItemDeleteClick = {},
            onItemAdded = {},
            onItemEdited = { _, _ -> },
            onTextFieldVisibilityChanged = {},
            initialShowTextField = false,
            initialExpandedItemId = "1",
        )
    }
}

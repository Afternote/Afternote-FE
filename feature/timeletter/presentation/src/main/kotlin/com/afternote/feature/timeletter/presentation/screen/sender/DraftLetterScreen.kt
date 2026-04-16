package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.timeletter.domain.DraftLetter
import com.afternote.feature.timeletter.presentation.component.DraftLetterItem
import com.afternote.feature.timeletter.presentation.component.TimeLetterTextButton
import com.afternote.feature.timeletter.presentation.viewmodel.DraftLetterUiState

@Composable
fun DraftLetterScreen(
    uiState: DraftLetterUiState = DraftLetterUiState(),
    onBackClick: () -> Unit = {},
    onEditCompleteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = "임시저장된 레터",
                onBackClick = onBackClick,
                actions = {
                    TimeLetterTextButton(
                        text = if (uiState.isEditMode) "완료" else "수정",
                        isActive = uiState.isEditMode,
                        onClick = onEditCompleteClick,
                    )
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            items(
                items = uiState.drafts,
                key = { it.id },
            ) { draft ->
                DraftLetterItem(
                    draft = draft,
                    isEditMode = uiState.isEditMode,
                    isSelected = draft.id in uiState.selectedIds,
                )
            }
        }
    }
}



// 2. 일반 보기 상태 (목록 있음)
@Preview(showBackground = true, name = "일반 - 목록 있음")
@Composable
private fun DraftLetterScreenNormalPreview() {
    DraftLetterScreen(
        uiState = DraftLetterUiState(
            drafts = listOf(
                DraftLetter(id = 1L, title = "첫 번째 레터", recipientName = "김철수", opendate = "2026-12-25"),
                DraftLetter(
                    id = 2L,
                    title = "두 번째 레터",
                    recipientName = "이영희",
                    opendate = "2026-06-01"
                ),
                DraftLetter(id = 3L, title = null, recipientName = null, opendate = null),
            ),
            isEditMode = false,
        )
    )
}

// 3. 수정 모드 (선택된 항목 있음)
@Preview(showBackground = true, name = "수정 모드 - 항목 선택됨")
@Composable
private fun DraftLetterScreenEditModePreview() {
    DraftLetterScreen(
        uiState = DraftLetterUiState(
            drafts = listOf(
                DraftLetter(id = 1L, title = "첫 번째 레터", recipientName = "김철수", opendate = "2026-12-25"),
                DraftLetter(id = 2L, title = "두 번째 레터", recipientName = "이영희", opendate = "2026-06-01"),
            ),
            isEditMode = true,
            selectedIds = setOf(1L),
        )
    )
}
package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.R
import com.afternote.core.ui.asString
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftCategory
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftItem
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftListUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftListViewModel
import java.time.format.DateTimeFormatter
import com.afternote.feature.mindrecord.presentation.R as MindRecordR

/**
 * 임시저장 목록 화면. Figma 노드 1716:13536 — 카테고리 필터 칩 + 총 개수 + 항목 리스트.
 */
@Composable
fun DraftListScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    viewModel: DraftListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(MindRecordR.string.mindrecord_draft_list_title),
                onBackClick = onBackClick,
                actions = {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AfternoteDesign.colors.gray2)
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(MindRecordR.string.mindrecord_draft_list_edit),
                            style = AfternoteDesign.typography.bodySmallB,
                            color = AfternoteDesign.colors.gray6,
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                DraftListUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is DraftListUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(text = state.message.asString(), color = AfternoteDesign.colors.gray9)
                    }
                }

                is DraftListUiState.Success -> {
                    SuccessContent(
                        state = state,
                        onFilterChanged = viewModel::onFilterChanged,
                    )
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    state: DraftListUiState.Success,
    onFilterChanged: (DraftCategory) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FilterRow(
            selected = state.filter,
            totalCount = state.totalCount,
            onFilterChanged = onFilterChanged,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (state.filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(MindRecordR.string.mindrecord_draft_list_empty),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray6,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.filtered, key = { "${it.category.name}-${it.id}" }) { item ->
                    DraftRow(item)
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    selected: DraftCategory,
    totalCount: Int,
    onFilterChanged: (DraftCategory) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Row(
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(AfternoteDesign.colors.gray2)
                        .clickable { expanded = true }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selected.label(),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray9,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.core_ui_arrowdown),
                    contentDescription = null,
                    tint = AfternoteDesign.colors.gray9,
                    modifier = Modifier.size(8.dp),
                )
            }
            if (expanded) {
                FilterDropdown(
                    selected = selected,
                    onSelect = {
                        onFilterChanged(it)
                        expanded = false
                    },
                    onDismiss = { expanded = false },
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "총 ",
                style = AfternoteDesign.typography.footnoteCaption,
                color = AfternoteDesign.colors.gray9,
            )
            Text(
                text = totalCount.toString(),
                style = AfternoteDesign.typography.footnoteCaption,
                color = AfternoteDesign.colors.gray6,
            )
            Text(
                text = "개",
                style = AfternoteDesign.typography.footnoteCaption,
                color = AfternoteDesign.colors.gray9,
            )
        }
    }
}

@Composable
private fun FilterDropdown(
    selected: DraftCategory,
    onSelect: (DraftCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        alignment = Alignment.TopStart,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            modifier = Modifier.width(132.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            shadowElevation = 10.dp,
        ) {
            Column {
                DraftCategory.entries.forEach { category ->
                    Text(
                        text = category.label(),
                        style = AfternoteDesign.typography.bodySmallR,
                        color = AfternoteDesign.colors.gray9,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(category) }
                                .padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftRow(item: DraftItem) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.category.label(),
                style = AfternoteDesign.typography.footnoteCaption,
                color = AfternoteDesign.colors.gray6,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = DateFormatter.format(item.date),
                style = AfternoteDesign.typography.footnoteCaption,
                color = AfternoteDesign.colors.gray6,
            )
        }
        Text(
            text = item.content,
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.gray9,
        )
        HorizontalDivider(thickness = 0.5.dp, color = AfternoteDesign.colors.gray3)
    }
}

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy. MM. dd.")

@Composable
private fun DraftCategory.label(): String =
    when (this) {
        DraftCategory.All -> stringResource(MindRecordR.string.mindrecord_draft_list_filter_all)
        DraftCategory.DailyQuestion -> stringResource(MindRecordR.string.mindrecord_draft_list_filter_daily)
        DraftCategory.Diary -> stringResource(MindRecordR.string.mindrecord_draft_list_filter_diary)
        DraftCategory.DeepThought -> stringResource(MindRecordR.string.mindrecord_draft_list_filter_deep_thought)
    }

@Preview(showBackground = true)
@Composable
private fun DraftListScreenPreview() {
    AfternoteTheme {
        // ViewModel 의존이 있어 Preview는 비워둠
    }
}

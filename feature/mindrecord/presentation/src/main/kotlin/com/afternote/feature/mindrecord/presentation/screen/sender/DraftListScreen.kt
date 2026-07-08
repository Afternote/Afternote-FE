package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.R
import com.afternote.core.ui.asString
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.mindrecord.presentation.util.htmlToPlainText
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftCategory
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftItem
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftListUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftListViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.afternote.feature.mindrecord.presentation.R as MindRecordR

/**
 * 임시저장 목록 화면. Figma 노드 2372:22842 (기본) / 2372:23669 (선택 모드) 리디자인.
 *
 * - 상단 우측 "선택"/"완료" 버튼으로 다중 선택 모드 토글
 * - 선택 모드에서 항목 좌측 체크 서클 + 하단 "전체 삭제 | 선택 삭제" 바 노출
 * - 삭제 완료 시 상단 토스트("임시 저장된 기록이 삭제 되었습니다") 노출
 */
@Composable
fun DraftListScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    viewModel: DraftListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeletedToast by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.deletedEvent.collect {
            showDeletedToast = true
            delay(DELETED_TOAST_DURATION_MILLIS)
            showDeletedToast = false
        }
    }

    val selectionMode = (uiState as? DraftListUiState.Success)?.selectionMode == true

    Scaffold(
        containerColor = AfternoteDesign.colors.gray1,
        topBar = {
            DetailTopBar(
                title = "임시 저장된 기록",
                onBackClick = onBackClick,
                actions = {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AfternoteDesign.colors.gray2)
                                .clickable { viewModel.onSelectionModeToggled() }
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = if (selectionMode) "완료" else "선택",
                            style = AfternoteDesign.typography.bodySmallB,
                            color = AfternoteDesign.colors.gray6,
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
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
                        onItemCheckedChanged = viewModel::onItemCheckedChanged,
                        onDeleteAllClick = viewModel::onDeleteAllClick,
                        onDeleteSelectedClick = viewModel::onDeleteSelectedClick,
                    )
                }
            }

            if (showDeletedToast) {
                DeletedToast(modifier = Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun SuccessContent(
    state: DraftListUiState.Success,
    onItemCheckedChanged: (DraftItem) -> Unit,
    onDeleteAllClick: () -> Unit,
    onDeleteSelectedClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(10.dp))
            TotalCountRow(
                selectionMode = state.selectionMode,
                count = if (state.selectionMode) state.selectedCount else state.totalCount,
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (state.items.isEmpty()) {
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
                    contentPadding = PaddingValues(bottom = if (state.selectionMode) 84.dp else 20.dp),
                ) {
                    items(state.items, key = { it.key }) { item ->
                        DraftRow(
                            item = item,
                            selectionMode = state.selectionMode,
                            checked = item.key in state.selectedKeys,
                            onCheckedChanged = onItemCheckedChanged,
                        )
                    }
                }
            }
        }

        if (state.selectionMode) {
            DeleteActionBar(
                onDeleteAllClick = onDeleteAllClick,
                onDeleteSelectedClick = onDeleteSelectedClick,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp),
            )
        }
    }
}

@Composable
private fun TotalCountRow(
    selectionMode: Boolean,
    count: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "총",
            style = AfternoteDesign.typography.footnoteCaption,
            color = AfternoteDesign.colors.gray9,
        )
        Text(
            text = count.toString(),
            style = AfternoteDesign.typography.footnoteCaption,
            color = AfternoteDesign.colors.gray6,
        )
        Text(
            text = if (selectionMode) "개 선택" else "개",
            style = AfternoteDesign.typography.footnoteCaption,
            color = AfternoteDesign.colors.gray9,
        )
    }
}

@Composable
private fun DraftRow(
    item: DraftItem,
    selectionMode: Boolean,
    checked: Boolean,
    onCheckedChanged: (DraftItem) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (selectionMode) {
                        Modifier.clickable { onCheckedChanged(item) }
                    } else {
                        Modifier
                    },
                ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (selectionMode) {
                DraftCheckCircle(checked = checked)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (item.recipientName != null) {
                            Text(
                                text = "수신인",
                                style = AfternoteDesign.typography.footnoteCaption,
                                color = AfternoteDesign.colors.gray6,
                            )
                            Text(
                                text = item.recipientName,
                                style = AfternoteDesign.typography.footnoteCaption,
                                color = AfternoteDesign.colors.gray6,
                            )
                        } else {
                            Text(
                                text = item.category.label(),
                                style = AfternoteDesign.typography.footnoteCaption,
                                color = AfternoteDesign.colors.gray6,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "작성일",
                            style = AfternoteDesign.typography.footnoteCaption,
                            color = AfternoteDesign.colors.gray6,
                        )
                        Text(
                            text = DateFormatter.format(item.date),
                            style = AfternoteDesign.typography.footnoteCaption,
                            color = AfternoteDesign.colors.gray6,
                        )
                    }
                }
                Text(
                    text = item.content.htmlToPlainText(),
                    style = AfternoteDesign.typography.bodySmallR,
                    color = AfternoteDesign.colors.gray9,
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(thickness = 0.5.dp, color = AfternoteDesign.colors.gray3)
    }
}

@Composable
private fun DraftCheckCircle(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(20.dp)
                .clip(CircleShape)
                .then(
                    if (checked) {
                        Modifier.background(AfternoteDesign.colors.gray9)
                    } else {
                        Modifier.border(width = 1.5.dp, color = AfternoteDesign.colors.gray4, shape = CircleShape)
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painter = painterResource(R.drawable.core_ui_ic_check),
                contentDescription = null,
                tint = AfternoteDesign.colors.white,
            )
        }
    }
}

@Composable
private fun DeleteActionBar(
    onDeleteAllClick: () -> Unit,
    onDeleteSelectedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AfternoteDesign.colors.gray9)
                .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onDeleteAllClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "전체 삭제",
                style = AfternoteDesign.typography.captionLargeB,
                color = AfternoteDesign.colors.white,
            )
        }
        Box(
            modifier =
                Modifier
                    .width(1.dp)
                    .height(12.dp)
                    .background(AfternoteDesign.colors.white),
        )
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onDeleteSelectedClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "선택 삭제",
                style = AfternoteDesign.typography.captionLargeB,
                color = AfternoteDesign.colors.white,
            )
        }
    }
}

@Composable
private fun DeletedToast(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(AfternoteDesign.colors.gray9)
                .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "임시 저장된 기록이 삭제 되었습니다",
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.white,
        )
    }
}

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy. MM. dd.")

private const val DELETED_TOAST_DURATION_MILLIS = 2_000L

@Composable
private fun DraftCategory.label(): String =
    when (this) {
        DraftCategory.DailyQuestion -> stringResource(MindRecordR.string.mindrecord_draft_list_filter_daily)
        DraftCategory.Diary -> stringResource(MindRecordR.string.mindrecord_draft_list_filter_diary)
        DraftCategory.DeepThought -> stringResource(MindRecordR.string.mindrecord_draft_list_filter_deep_thought)
    }

@Preview(showBackground = true)
@Composable
private fun DraftListSelectionPreview() {
    AfternoteTheme {
        SuccessContent(
            state =
                DraftListUiState.Success(
                    items =
                        List(4) { index ->
                            DraftItem(
                                id = index.toLong(),
                                category = DraftCategory.Diary,
                                content = "지은아 결혼을 축하해",
                                date = LocalDate.of(2029, 11, 20),
                                recipientName = "김지은",
                            )
                        },
                    selectionMode = true,
                    selectedKeys = setOf("Diary-0"),
                ),
            onItemCheckedChanged = {},
            onDeleteAllClick = {},
            onDeleteSelectedClick = {},
        )
    }
}

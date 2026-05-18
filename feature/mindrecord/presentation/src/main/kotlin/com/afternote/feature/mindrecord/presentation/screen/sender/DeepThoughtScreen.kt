package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.DailyCalendar
import com.afternote.feature.mindrecord.presentation.component.DeepThoughtCard
import com.afternote.feature.mindrecord.presentation.component.FlowTags
import com.afternote.feature.mindrecord.presentation.component.MindRecordEmptyState
import com.afternote.feature.mindrecord.presentation.model.DeepThoughtModel
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.model.Tag
import com.afternote.feature.mindrecord.presentation.viewmodel.DeepThoughtListUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DeepThoughtListViewModel

@Composable
fun DeepThoughtScreen(
    modifier: Modifier = Modifier,
    isListView: Boolean = true,
    viewModel: DeepThoughtListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        DeepThoughtListUiState.Loading -> {
            LoadingBox(modifier)
        }

        is DeepThoughtListUiState.Error -> {
            ErrorBox(message = state.message, modifier = modifier)
        }

        is DeepThoughtListUiState.Success -> {
            DeepThoughtContent(
                modifier = modifier,
                isListView = isListView,
                tags = state.tags,
                selectedTag = state.selectedTag,
                items = state.items,
                onTagClick = viewModel::onTagSelected,
            )
        }
    }
}

@Composable
private fun DeepThoughtContent(
    isListView: Boolean,
    tags: List<Tag>,
    selectedTag: Tag?,
    onTagClick: (Tag?) -> Unit,
    items: List<DeepThoughtModel>,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("전체 카테고리", "카테고리", "카테고리", "카테고리")

    if (isListView && items.isEmpty()) {
        MindRecordEmptyState(modifier = modifier)
        return
    }

    if (isListView) {
        Column(modifier = modifier) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedIndex,
                edgePadding = 0.dp,
                divider = {},
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier =
                            Modifier.tabIndicatorOffset(
                                selectedIndex,
                                matchContentSize = false,
                            ),
                        width = 80.dp,
                        color = Color(0xFF1F1F1F),
                    )
                },
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedIndex == index) Color(0xFF1F1F1F) else AfternoteDesign.colors.gray4,
                            )
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(com.afternote.feature.mindrecord.presentation.R.drawable.mindrecord_mark),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "TAGS",
                    style = AfternoteDesign.typography.mono,
                    color = Color(0xFF000000).copy(alpha = 0.4f),
                )

                HorizontalDivider(modifier = Modifier.padding(start = 12.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowTags(
                tags = tags,
                selectedTag = selectedTag,
                onclick = { onTagClick(null) },
                onTagClick = { onTagClick(it) },
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { it.id }) { DeepThoughtCard(it) }
            }
        }
    } else {
        LazyColumn(modifier = modifier) {
            item {
                DailyCalendar(
                    year = 2026,
                    month = 3,
                    type = MindRecordCategoryUi.DeepThought,
                    onNextMonth = {},
                    onPrevMonth = {},
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "DAILY ANSWER",
                        style = AfternoteDesign.typography.mono,
                        color = Color(0xFF000000).copy(alpha = 0.4f),
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 12.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(items, key = { it.id }) {
                DeepThoughtCard(it)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorBox(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Text(text = message, color = AfternoteDesign.colors.gray9)
    }
}

@Preview(showBackground = true)
@Composable
private fun DeepThoughtScreenPreviewTrue() {
    AfternoteTheme {
        DeepThoughtContent(
            modifier = Modifier,
            isListView = true,
            tags = emptyList(),
            selectedTag = null,
            items = emptyList(),
            onTagClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeepThoughtScreenPreviewFalse() {
    AfternoteTheme {
        DeepThoughtContent(
            modifier = Modifier,
            isListView = false,
            tags = emptyList(),
            selectedTag = null,
            items = emptyList(),
            onTagClick = {},
        )
    }
}

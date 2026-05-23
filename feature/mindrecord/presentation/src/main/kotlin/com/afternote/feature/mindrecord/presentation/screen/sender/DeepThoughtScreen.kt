package com.afternote.feature.mindrecord.presentation.screen.sender

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.CategoryActionSheet
import com.afternote.feature.mindrecord.presentation.component.CategoryNameDialog
import com.afternote.feature.mindrecord.presentation.component.CategorySettingBottomSheet
import com.afternote.feature.mindrecord.presentation.component.DailyCalendar
import com.afternote.feature.mindrecord.presentation.component.DeepThoughtCard
import com.afternote.feature.mindrecord.presentation.component.FlowTags
import com.afternote.feature.mindrecord.presentation.component.MindRecordEmptyState
import com.afternote.feature.mindrecord.presentation.model.CategoryUiModel
import com.afternote.feature.mindrecord.presentation.model.DeepThoughtModel
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.model.Tag
import com.afternote.feature.mindrecord.presentation.viewmodel.DeepThoughtCategoryViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.DeepThoughtListUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DeepThoughtListViewModel

@Composable
fun DeepThoughtScreen(
    modifier: Modifier = Modifier,
    isListView: Boolean = true,
    viewModel: DeepThoughtListViewModel = hiltViewModel(),
    categoryViewModel: DeepThoughtCategoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categoryState by categoryViewModel.uiState.collectAsStateWithLifecycle()

    var showCategorySheet by remember { mutableStateOf(false) }
    var addingCategory by remember { mutableStateOf(false) }
    var menuTarget by remember { mutableStateOf<CategoryUiModel?>(null) }
    var renameTarget by remember { mutableStateOf<CategoryUiModel?>(null) }

    // 카테고리 변경 시 목록 새로고침
    LaunchedEffect(categoryState.categories) {
        viewModel.refresh()
    }

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
                categories = categoryState.categories.ifEmpty { state.categories },
                selectedCategory = state.selectedCategory,
                items = state.items,
                onTagClick = viewModel::onTagSelected,
                onCategorySelect = viewModel::onCategorySelected,
                onCategorySettingClick = { showCategorySheet = true },
            )
        }
    }

    if (showCategorySheet) {
        CategorySettingBottomSheet(
            categories = categoryState.categories,
            onDismiss = { showCategorySheet = false },
            onBackClick = { showCategorySheet = false },
            onAddCategory = { addingCategory = true },
            onMenuClick = { menuTarget = it },
        )
    }

    menuTarget?.let { target ->
        CategoryActionSheet(
            targetName = target.name,
            onDismiss = { menuTarget = null },
            onRename = {
                renameTarget = target
                menuTarget = null
            },
            onDelete = {
                target.id.toLongOrNull()?.let(categoryViewModel::deleteCategory)
                menuTarget = null
            },
        )
    }

    if (addingCategory) {
        CategoryNameDialog(
            title = "새 카테고리 만들기",
            initialName = "",
            confirmLabel = "추가",
            onDismiss = { addingCategory = false },
            onConfirm = { name ->
                categoryViewModel.addCategory(name = name)
                addingCategory = false
            },
        )
    }

    renameTarget?.let { target ->
        CategoryNameDialog(
            title = "카테고리 이름 수정",
            initialName = target.name,
            confirmLabel = "저장",
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                target.id.toLongOrNull()?.let { id ->
                    categoryViewModel.renameCategory(categoryId = id, newName = name)
                }
                renameTarget = null
            },
        )
    }
}

@Composable
private fun DeepThoughtContent(
    isListView: Boolean,
    tags: List<Tag>,
    selectedTag: Tag?,
    categories: List<CategoryUiModel>,
    selectedCategory: String?,
    onTagClick: (Tag?) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onCategorySettingClick: () -> Unit,
    items: List<DeepThoughtModel>,
    modifier: Modifier = Modifier,
) {
    val tabs =
        buildList {
            add(null to "전체 카테고리")
            categories.forEach { add(it.name to it.name) }
        }
    val selectedIndex =
        tabs
            .indexOfFirst { it.first == selectedCategory }
            .coerceAtLeast(0)

    if (isListView && items.isEmpty() && categories.isEmpty()) {
        MindRecordEmptyState(modifier = modifier)
        return
    }

    if (isListView) {
        Column(modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryScrollableTabRow(
                    modifier = Modifier.weight(1f),
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
                    tabs.forEachIndexed { index, (key, title) ->
                        Tab(
                            selected = selectedIndex == index,
                            onClick = { onCategorySelect(key) },
                            text = {
                                Text(
                                    text = title,
                                    color =
                                        if (selectedIndex == index) {
                                            AfternoteDesign.colors.gray9
                                        } else {
                                            AfternoteDesign.colors.gray4
                                        },
                                )
                            },
                        )
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.core_ui_settings),
                    contentDescription = "카테고리 설정",
                    tint = AfternoteDesign.colors.gray9,
                    modifier =
                        Modifier
                            .padding(end = 16.dp)
                            .size(20.dp)
                            .clickable(onClick = onCategorySettingClick),
                )
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
                    color = AfternoteDesign.colors.black.copy(alpha = 0.4f),
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
                        color = AfternoteDesign.colors.black.copy(alpha = 0.4f),
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
            categories = emptyList(),
            selectedCategory = null,
            items = emptyList(),
            onTagClick = {},
            onCategorySelect = {},
            onCategorySettingClick = {},
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
            categories = emptyList(),
            selectedCategory = null,
            items = emptyList(),
            onTagClick = {},
            onCategorySelect = {},
            onCategorySettingClick = {},
        )
    }
}

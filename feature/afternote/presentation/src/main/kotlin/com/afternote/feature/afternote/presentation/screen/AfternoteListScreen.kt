package com.afternote.feature.afternote.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afternote.core.ui.TopBar
import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.domain.model.Item
import com.afternote.feature.afternote.presentation.viewmodel.AfternoteListUiState
import com.afternote.feature.afternote.presentation.viewmodel.AfternoteListViewModel

@Composable
fun AfternoteListScreen(
    onItemClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AfternoteListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= totalItems - 2
        }.collect { nearEnd ->
            if (nearEnd) viewModel.loadNextPage()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopBar() },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Text(
                text = "나의 애프터노트",
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 22.5.dp, vertical = 16.dp),
            )

            when (val state = uiState) {
                is AfternoteListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is AfternoteListUiState.Success -> {
                    AfternoteFilterRow(
                        selectedType = state.selectedType,
                        onTypeSelected = { viewModel.loadList(it) },
                        modifier = Modifier.padding(horizontal = 22.5.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 22.5.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            AfternoteListItemRow(
                                item = item,
                                onClick = {
                                    onItemClick(item.id.toLongOrNull() ?: return@AfternoteListItemRow)
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }

                is AfternoteListUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadList() }) {
                                Text("다시 시도")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AfternoteFilterRow(
    selectedType: AfternoteServiceType?,
    onTypeSelected: (AfternoteServiceType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedType == null,
            onClick = { onTypeSelected(null) },
            label = { Text("전체") },
        )
        FilterChip(
            selected = selectedType == AfternoteServiceType.SOCIAL_NETWORK,
            onClick = { onTypeSelected(AfternoteServiceType.SOCIAL_NETWORK) },
            label = { Text("소셜") },
        )
        FilterChip(
            selected = selectedType == AfternoteServiceType.GALLERY_AND_FILES,
            onClick = { onTypeSelected(AfternoteServiceType.GALLERY_AND_FILES) },
            label = { Text("갤러리") },
        )
        FilterChip(
            selected = selectedType == AfternoteServiceType.MEMORIAL,
            onClick = { onTypeSelected(AfternoteServiceType.MEMORIAL) },
            label = { Text("추모") },
        )
    }
}

@Composable
private fun AfternoteListItemRow(
    item: Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.serviceName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.date,
                fontSize = 12.sp,
            )
        }
        Button(onClick = onClick) {
            Text("보기")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteListScreenPreview() {
    AfternoteListScreen()
}

package com.afternote.feature.mindrecord.presentation.screen.receiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.mindrecord.domain.model.MindRecordSummary
import com.afternote.feature.mindrecord.domain.model.MindRecordType
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.component.ReceiverDiaryGridCard
import com.afternote.feature.mindrecord.presentation.component.ReceiverMindRecordTopBar
import com.afternote.feature.mindrecord.presentation.component.ReceiverRecordCard
import com.afternote.feature.mindrecord.presentation.component.ReceiverRecordDetailSheet
import com.afternote.feature.mindrecord.presentation.viewmodel.ReceiverMindRecordUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.ReceiverMindRecordViewModel
import androidx.compose.foundation.lazy.grid.items as gridItems

/**
 * 수신자(추모자) 마음의 기록 화면 — 데일리질문 / 일기 2개 탭 + 필터 바텀시트.
 *
 * Figma 노드 1727-19620 / 1727-19688 (2 메인 탭) + 1727-25357 / 25054 / 23247 /
 * 23886 (필터 바텀시트 4 상태) + 1727-24804 (필터 적용 헤더).
 */
@Composable
fun ReceiverMindRecordScreen(
    modifier: Modifier = Modifier,
    viewModel: ReceiverMindRecordViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var filterSheetVisible by remember { mutableStateOf(false) }
    // 탭한 기록의 본문을 여는 시트. 목록 응답이 이미 본문을 갖고 있어 추가 조회가 없다 (#618).
    var openedRecordId by remember { mutableStateOf<Long?>(null) }

    // 화면을 떠났다 돌아오면 다시 조회한다 — 작성·삭제하고 복귀했을 때 목록이 낡은 채로
    // 남지 않게 한다. 마인드레코드 홈이 쓰는 결선과 같다 (#702).
    //
    // ON_RESUME 은 화면 off/on·홈 버튼 복귀에서도 발화하므로 로딩을 방출하지 않는
    // `refreshOnReturn()` 을 쓴다. 최초 진입의 중복 호출은 VM 이 진행 중인 Job 으로 막는다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshOnReturn()
    }

    // 앱바가 없으면 이 화면은 막다른 곳이 된다 — 실패 시 화면에 남는 것이 오류 문구 하나뿐이라
    // 시스템 백키 외에 빠져나갈 수단이 없었다 (#614).
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.mindrecord_receiver_title),
                onBackClick = onBackClick,
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                ReceiverMindRecordUiState.Loading -> {
                    LoadingBody()
                }

                is ReceiverMindRecordUiState.Error -> {
                    ErrorBox(message = state.message.asString(), onRetry = viewModel::refresh)
                }

                is ReceiverMindRecordUiState.Success -> {
                    SuccessContent(
                        state = state,
                        onFilterClick = { filterSheetVisible = true },
                        // 카드 탭의 목적지는 이 화면 안의 시트다. 종전에는 화면이 받아 두고
                        // 아무도 넘기지 않는 `onRecordClick` 파라미터가 있었는데(#618 이 지목한
                        // 그 콜백), 남겨 두면 나중에 실제 네비게이션이 붙는 순간 시트가 열리며
                        // 화면까지 전환된다. 소유자를 하나로 둔다.
                        onRecordClick = { id -> openedRecordId = id },
                    )
                }
            }
        }
    }

    val openedRecord = findOpenedRecord(uiState, openedRecordId)
    openedRecord?.let { record ->
        ReceiverRecordDetailSheet(
            record = record,
            onDismiss = { openedRecordId = null },
        )
    }

    if (filterSheetVisible && uiState is ReceiverMindRecordUiState.Success) {
        ReceiverMindRecordFilterSheet(
            current = (uiState as ReceiverMindRecordUiState.Success).filter,
            onDismiss = { filterSheetVisible = false },
            onApply = { applied ->
                viewModel.applyFilter(applied)
                filterSheetVisible = false
            },
            onReset = {
                viewModel.resetFilter()
                filterSheetVisible = false
            },
        )
    }
}

@Composable
private fun SuccessContent(
    state: ReceiverMindRecordUiState.Success,
    onFilterClick: () -> Unit,
    onRecordClick: (Long) -> Unit,
) {
    val tabs = remember { listOf(ReceiverMindRecordTab.DailyQuestion, ReceiverMindRecordTab.Diary) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState { tabs.size }
    LaunchedEffect(selectedIndex) { pagerState.animateScrollToPage(selectedIndex) }
    LaunchedEffect(pagerState.currentPage) { selectedIndex = pagerState.currentPage }

    Column(modifier = Modifier.fillMaxSize()) {
        ReceiverMindRecordTopBar(filter = state.filter, onFilterClick = onFilterClick)
        Spacer(modifier = Modifier.height(8.dp))
        PrimaryTabRow(
            selectedTabIndex = selectedIndex,
            // 지정하지 않으면 M3 baseline surface(#FEF7FF)가 나와 시안 배경(#FAFAFA)과 어긋난다.
            containerColor = Color.Transparent,
            divider = {},
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedIndex, matchContentSize = false),
                    width = 80.dp,
                    color = AfternoteDesign.colors.gray9,
                )
            },
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    text = {
                        Text(
                            text = tab.title,
                            color = if (selectedIndex == index) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.gray4,
                        )
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (tabs[page]) {
                ReceiverMindRecordTab.DailyQuestion -> {
                    RecordList(records = state.dailyQuestions, onClick = onRecordClick)
                }

                ReceiverMindRecordTab.Diary -> {
                    DiaryGrid(records = state.diaries, onClick = onRecordClick)
                }
            }
        }
    }
}

@Composable
private fun RecordList(
    records: List<MindRecordSummary>,
    onClick: (Long) -> Unit,
) {
    if (records.isEmpty()) {
        EmptyBox()
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(records, key = { it.id }) { record ->
            ReceiverRecordCard(record = record, onClick = { onClick(record.id) })
        }
    }
}

@Composable
private fun DiaryGrid(
    records: List<MindRecordSummary>,
    onClick: (Long) -> Unit,
) {
    if (records.isEmpty()) {
        EmptyBox()
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        gridItems(records, key = { it.id }) { record ->
            ReceiverDiaryGridCard(record = record, onClick = { onClick(record.id) })
        }
    }
}

@Composable
private fun ErrorBox(
    message: String,
    onRetry: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                color = AfternoteDesign.colors.gray9,
                textAlign = TextAlign.Center,
            )
            // 종전에는 콜백을 받아 두고 쓰지 않아, 화면에 재시도 수단이 없었다 (#614).
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.mindrecord_error_retry),
                    style = AfternoteDesign.typography.bodySmallB,
                    color = AfternoteDesign.colors.gray9,
                )
            }
        }
    }
}

@Composable
private fun EmptyBox() {
    Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Text(text = "기록이 없습니다.", color = AfternoteDesign.colors.gray6)
    }
}

private enum class ReceiverMindRecordTab(
    val title: String,
    val type: MindRecordType,
) {
    DailyQuestion("데일리 질문", MindRecordType.DAILY_QUESTION),
    Diary("일기", MindRecordType.DIARY),
}

@Preview(showBackground = true)
@Composable
private fun ReceiverMindRecordScreenPreview() {
    AfternoteTheme {
        SuccessContent(
            state =
                ReceiverMindRecordUiState.Success(
                    dailyQuestions = emptyList(),
                    diaries = emptyList(),
                ),
            onFilterClick = {},
            onRecordClick = {},
        )
    }
}

/**
 * 탭한 기록을 화면이 들고 있는 목록에서 찾는다 (#618).
 *
 * 두 탭(데일리질문·일기)이 같은 시트를 쓰므로 양쪽을 함께 뒤진다. 목록 응답이 이미
 * 본문을 갖고 있어 여기서 찾은 항목을 그대로 펼치면 되고, 추가 조회가 필요 없다.
 *
 * 필터·정렬로 목록에서 빠진 항목은 자연히 null 이 되어 시트가 닫힌다.
 */
internal fun findOpenedRecord(
    uiState: ReceiverMindRecordUiState,
    openedRecordId: Long?,
): MindRecordSummary? {
    if (openedRecordId == null) return null
    val success = uiState as? ReceiverMindRecordUiState.Success ?: return null
    return (success.dailyQuestions + success.diaries).firstOrNull { it.id == openedRecordId }
}

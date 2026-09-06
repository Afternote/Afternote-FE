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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.mindrecord.presentation.component.MindRecordErrorBox
import com.afternote.feature.mindrecord.presentation.util.htmlToPlainText
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftCategory
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftDeleteOutcome
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftItem
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftListUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftListViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import com.afternote.feature.mindrecord.presentation.R as MindRecordR

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy. MM. dd.")
private const val DELETED_TOAST_DURATION_MS = 2_000L

/**
 * 임시저장 목록 화면. Figma 2372:22842 (기본) / 2372:23669 (선택 모드) / 2372:25044 (삭제 토스트).
 */
@Composable
fun DraftListScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    /** 일기 draft 탭 → 이어쓰기. (draftId, 해당 달 `yyyy-MM`) 전달. */
    onDiaryDraftClick: (Long, String) -> Unit,
    /** 데일리질문 draft 탭 → 이어쓰기. 당일이 지난 draft 는 이 경로로만 열 수 있다 (#770). */
    onDailyQuestionDraftClick: (Long) -> Unit,
    viewModel: DraftListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectionMode by remember { mutableStateOf(false) }
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }

    // 화면을 떠났다 돌아오면 다시 조회한다 — 작성·삭제하고 복귀했을 때 목록이 낡은 채로
    // 남지 않게 한다. 마인드레코드 홈이 쓰는 결선과 같다 (#702).
    //
    // ON_RESUME 은 화면 off/on·홈 버튼 복귀에서도 발화하므로 로딩을 방출하지 않는
    // `refreshOnReturn()` 을 쓴다. 최초 진입의 중복 호출은 VM 이 진행 중인 Job 으로 막는다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshOnReturn()
    }

    // 갱신으로 사라진 항목의 키가 선택에 남으면 "총 N개 선택" 이 과대 표시되고, 선택
    // 삭제가 빈 목록으로 나간다. 선택은 화면 remember 라 갱신에도 살아남는다 (리뷰 지적).
    val visibleKeys = (uiState as? DraftListUiState.Success)?.items?.map { it.key() }?.toSet()
    LaunchedEffect(visibleKeys) {
        if (visibleKeys != null) {
            selectedKeys = selectedKeys intersect visibleKeys
            if (selectedKeys.isEmpty()) selectionMode = false
        }
    }

    val deleteOutcome = (uiState as? DraftListUiState.Success)?.deleteOutcome
    LaunchedEffect(deleteOutcome) {
        when (deleteOutcome) {
            null -> {
                Unit
            }

            DraftDeleteOutcome.AllDeleted -> {
                selectedKeys = emptySet()
                selectionMode = false
                delay(DELETED_TOAST_DURATION_MS)
                viewModel.consumeDeleteOutcome()
            }

            is DraftDeleteOutcome.SomeFailed -> {
                // 실패한 항목만 다시 선택된 상태로 남겨 그대로 재시도할 수 있게 한다.
                selectedKeys = deleteOutcome.failedItems.mapTo(mutableSetOf()) { it.key() }
                selectionMode = true
                delay(DELETED_TOAST_DURATION_MS)
                viewModel.consumeDeleteOutcome()
            }
        }
    }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(MindRecordR.string.mindrecord_draft_list_title),
                onBackClick = onBackClick,
                actions = {
                    // Figma 2372:22854 — 우측 상단 "선택" / "완료" 토글 버튼
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AfternoteDesign.colors.gray2)
                                .clickable(role = Role.Button) {
                                    selectionMode = !selectionMode
                                    if (!selectionMode) selectedKeys = emptySet()
                                }.padding(horizontal = 18.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text =
                                stringResource(
                                    if (selectionMode) {
                                        MindRecordR.string.mindrecord_draft_list_done
                                    } else {
                                        MindRecordR.string.mindrecord_draft_list_select
                                    },
                                ),
                            style = AfternoteDesign.typography.bodySmallB,
                            color = AfternoteDesign.colors.gray6,
                        )
                    }
                },
            )
        },
        modifier = modifier,
        containerColor = Color.Transparent,
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                DraftListUiState.Loading -> {
                    LoadingBody()
                }

                is DraftListUiState.Error -> {
                    MindRecordErrorBox(
                        message = state.message.asString(),
                        onRetry = viewModel::retry,
                    )
                }

                is DraftListUiState.Success -> {
                    SuccessContent(
                        state = state,
                        selectionMode = selectionMode,
                        selectedKeys = selectedKeys,
                        onToggleSelect = { key ->
                            selectedKeys =
                                if (key in selectedKeys) selectedKeys - key else selectedKeys + key
                        },
                        onDeleteAll = viewModel::deleteAll,
                        isDeleteSelectedEnabled = selectedKeys.isNotEmpty(),
                        onDeleteSelected = {
                            viewModel.delete(state.items.filter { it.key() in selectedKeys })
                        },
                        onItemClick = { item ->
                            when (item.category) {
                                DraftCategory.Diary -> {
                                    onDiaryDraftClick(item.id, YearMonth.from(item.date).toString())
                                }

                                DraftCategory.DailyQuestion -> {
                                    onDailyQuestionDraftClick(item.id)
                                }

                                // 목록에 담기는 항목은 두 종류뿐이라 도달하지 않는다.
                                DraftCategory.All -> {
                                    Unit
                                }
                            }
                        },
                    )

                    // Figma 2372:25211 — 삭제 완료 토스트
                    when (val outcome = state.deleteOutcome) {
                        null -> {
                            Unit
                        }

                        DraftDeleteOutcome.AllDeleted -> {
                            DeletedToast(
                                modifier =
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                            )
                        }

                        // 완료 토스트와 같은 자리에 실패를 알린다 — 지워지지 않은 항목이 목록에
                        // 남아 있는데 "삭제되었습니다" 가 뜨면 사용자는 지워진 줄 안다.
                        is DraftDeleteOutcome.SomeFailed -> {
                            DraftDeleteFailedToast(
                                failedCount = outcome.failedItems.size,
                                modifier =
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    state: DraftListUiState.Success,
    selectionMode: Boolean,
    selectedKeys: Set<String>,
    onToggleSelect: (String) -> Unit,
    onDeleteAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onItemClick: (DraftItem) -> Unit,
    isDeleteSelectedEnabled: Boolean = true,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Text(
                text =
                    if (selectionMode) {
                        stringResource(MindRecordR.string.mindrecord_draft_list_total_selected, selectedKeys.size)
                    } else {
                        stringResource(MindRecordR.string.mindrecord_draft_list_total, state.totalCount)
                    },
                style = AfternoteDesign.typography.footnoteCaption,
                color = AfternoteDesign.colors.gray9,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            if (state.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(MindRecordR.string.mindrecord_draft_list_empty),
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.gray6,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.items, key = { it.key() }) { item ->
                        DraftRow(
                            item = item,
                            selectionMode = selectionMode,
                            selected = item.key() in selectedKeys,
                            onToggleSelect = { onToggleSelect(item.key()) },
                            onClick = { onItemClick(item) },
                        )
                    }
                }
            }
        }

        // Figma 2372:24660 — 선택 모드 하단 "전체 삭제 | 선택 삭제" 바.
        // core 정본을 쓴다. 자체 구현은 같은 시안을 근거로 적었는데 코너·라벨·divider·여백이
        // 4/4 어긋나 있었고, core 는 4/4 일치한다 (#634).
        if (selectionMode) {
            AfternoteButton(
                text = stringResource(MindRecordR.string.mindrecord_draft_list_delete_all),
                onClick = onDeleteAll,
                type = AfternoteButtonType.Variant5,
                secondaryText = stringResource(MindRecordR.string.mindrecord_draft_list_delete_selected),
                onSecondaryClick = onDeleteSelected,
                // 0개 선택 상태에서 빈 목록으로 삭제를 부르지 않도록 막는다 (#442).
                // 정본이 이 상태를 담게 해서 수렴과 방어가 함께 성립한다.
                isSecondaryEnabled = isDeleteSelectedEnabled,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun DraftRow(
    item: DraftItem,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (selectionMode) {
                        Modifier.clickable(role = Role.Checkbox, onClick = onToggleSelect)
                    } else {
                        Modifier.clickable(onClick = onClick)
                    },
                ).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            AfternoteCircularCheckbox(
                state = if (selected) CheckboxState.Default else CheckboxState.None,
                size = 24.dp,
                onClick = onToggleSelect,
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.category.label(),
                    style = AfternoteDesign.typography.footnoteCaption,
                    color = AfternoteDesign.colors.gray6,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text =
                        stringResource(
                            MindRecordR.string.mindrecord_draft_list_written_date,
                            DateFormatter.format(item.date),
                        ),
                    style = AfternoteDesign.typography.footnoteCaption,
                    color = AfternoteDesign.colors.gray6,
                )
            }
            Text(
                text = item.title.ifBlank { item.content.htmlToPlainText() },
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray9,
            )
            HorizontalDivider(thickness = 0.5.dp, color = AfternoteDesign.colors.gray3)
        }
    }
}

@Composable
private fun DeletedToast(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AfternoteDesign.colors.gray9)
                .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(MindRecordR.string.mindrecord_draft_list_deleted_toast),
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.white,
        )
    }
}

/**
 * 삭제 실패 안내.
 *
 * 완료 토스트와 같은 자리·같은 모양을 쓰되 문구만 다르다 — 실패 표시 시안이 따로 없어
 * 새 표현을 만들지 않았다. 확정되면 이 컴포저블만 교체하면 된다.
 */
@Composable
private fun DraftDeleteFailedToast(
    failedCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AfternoteDesign.colors.gray9)
                .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text =
                stringResource(
                    MindRecordR.string.mindrecord_draft_list_delete_failed_toast,
                    failedCount,
                ),
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.white,
        )
    }
}

private fun DraftItem.key(): String = "${category.name}-$id"

@Composable
private fun DraftCategory.label(): String =
    when (this) {
        DraftCategory.All -> stringResource(MindRecordR.string.mindrecord_draft_list_filter_all)
        DraftCategory.DailyQuestion -> stringResource(MindRecordR.string.mindrecord_draft_list_filter_daily)
        DraftCategory.Diary -> stringResource(MindRecordR.string.mindrecord_draft_list_filter_diary)
    }

@Preview(showBackground = true)
@Composable
private fun DraftRowPreview() {
    AfternoteTheme {
        Column {
            DraftRow(
                item =
                    DraftItem(
                        id = 1L,
                        category = DraftCategory.Diary,
                        title = "지은아 결혼을 축하해",
                        content = "내용",
                        date = LocalDate.of(2029, 11, 20),
                    ),
                selectionMode = true,
                selected = true,
                onToggleSelect = {},
                onClick = {},
            )
            AfternoteButton(
                text = "전체 삭제",
                onClick = {},
                type = AfternoteButtonType.Variant5,
                secondaryText = "선택 삭제",
                onSecondaryClick = {},
            )
        }
    }
}

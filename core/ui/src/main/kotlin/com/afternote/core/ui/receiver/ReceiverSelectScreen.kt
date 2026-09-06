package com.afternote.core.ui.receiver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.afternote.core.common.util.KoreanConsonantUtil
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.KoreanConsonantIndex
import com.afternote.core.ui.ProfileImage
import com.afternote.core.ui.R
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import kotlinx.coroutines.launch

/**
 * 수신자 선택 공용 화면 (#791, 시안 3631:24820) — 복수 선택.
 *
 * 검색 필드 + 초성 인덱스(ㄱ~ㅎ) + 선택 목록 + 하단 완료 버튼 조립.
 * 데이터 조회·ViewModel·내비게이션은 소비 기능이 소유하고, 이 화면은
 * 목록·현재 선택·콜백을 받는다. 선택/해제 규칙(재탭 시 해제 등)은
 * 소비자가 [onReceiverToggle] 에서 결정한다.
 *
 * [selectedReceiverIds] 가 비면 «아무도 선택하지 않음» 이다 — 완료 버튼이 비활성이고
 * [onConfirmClick] 도 나가지 않는다. null 로 «없음» 을 표현하지 않는다 (#1426).
 *
 * 단일 선택 소비자(설정 수신자 목록)는 아래의 `selectedReceiverId` 오버로드를 그대로 쓴다 —
 * 복수화(#1426)는 새 오버로드를 더한 것이지 기존 계약을 바꾼 게 아니다.
 *
 * 문구 3종은 시안(3631:24820) 기준 공용 기본값을 쓴다 — 작성 플로우(애프터노트·타임레터·
 * 마음의 기록)는 모두 같은 문구라 그대로 두면 된다. 설정만 "수신자 목록" 으로 [title] 을
 * 덮는데, 그 화면은 #631 로 선택이 아닌 관리 화면이 되면서 이 컴포넌트 소비를 그만둔다.
 *
 * @param listReplacement 검색 필드 아래 목록 영역을 통째로 대체할 상태 화면 —
 *   로딩·조회 실패·빈 목록처럼 소비 기능마다 다른 상태를 끼운다. null 이면 목록을 그린다.
 */
@Composable
fun ReceiverSelectScreen(
    receivers: List<ReceiverSelectItem>,
    selectedReceiverIds: List<Long>,
    onReceiverToggle: (Long) -> Unit,
    onBackClick: () -> Unit,
    onConfirmClick: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.core_ui_receiver_select_title),
    searchPlaceholder: String = stringResource(R.string.core_ui_receiver_select_search_placeholder),
    confirmText: String = stringResource(R.string.core_ui_receiver_select_confirm),
    listReplacement: (@Composable () -> Unit)? = null,
) {
    ReceiverSelectScaffold(
        receivers = receivers,
        selectedReceiverIds = selectedReceiverIds,
        onReceiverToggle = onReceiverToggle,
        onBackClick = onBackClick,
        onConfirmClick = { onConfirmClick(selectedReceiverIds) },
        modifier = modifier,
        title = title,
        searchPlaceholder = searchPlaceholder,
        confirmText = confirmText,
        listReplacement = listReplacement,
    )
}

/**
 * 수신자 선택 공용 화면 — 단일 선택 오버로드.
 *
 * 설정 수신자 목록처럼 한 명만 돌려주는 소비 경로가 쓴다. 복수 오버로드에 [selectedReceiverId]
 * 를 담아 위임할 뿐이라, 그림과 완료 버튼 활성 조건은 두 모드가 같은 구현을 공유한다.
 */
@Composable
fun ReceiverSelectScreen(
    receivers: List<ReceiverSelectItem>,
    selectedReceiverId: Long?,
    onReceiverToggle: (Long) -> Unit,
    onBackClick: () -> Unit,
    onConfirmClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.core_ui_receiver_select_title),
    searchPlaceholder: String = stringResource(R.string.core_ui_receiver_select_search_placeholder),
    confirmText: String = stringResource(R.string.core_ui_receiver_select_confirm),
    listReplacement: (@Composable () -> Unit)? = null,
) {
    ReceiverSelectScaffold(
        receivers = receivers,
        selectedReceiverIds = listOfNotNull(selectedReceiverId),
        onReceiverToggle = onReceiverToggle,
        onBackClick = onBackClick,
        onConfirmClick = { selectedReceiverId?.let(onConfirmClick) },
        modifier = modifier,
        title = title,
        searchPlaceholder = searchPlaceholder,
        confirmText = confirmText,
        listReplacement = listReplacement,
    )
}

@Composable
private fun ReceiverSelectScaffold(
    receivers: List<ReceiverSelectItem>,
    selectedReceiverIds: List<Long>,
    onReceiverToggle: (Long) -> Unit,
    onBackClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier,
    title: String,
    searchPlaceholder: String,
    confirmText: String,
    listReplacement: (@Composable () -> Unit)?,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = title,
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            AfternoteButton(
                text = confirmText,
                onClick = onConfirmClick,
                type =
                    if (selectedReceiverIds.isNotEmpty()) {
                        AfternoteButtonType.Default
                    } else {
                        AfternoteButtonType.Un
                    },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(top = 10.dp),
        ) {
            val searchState = rememberTextFieldState()

            AfternoteTextField(
                state = searchState,
                placeholder = searchPlaceholder,
                type = TextFieldType.Search,
                imeAction = ImeAction.Search,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            if (listReplacement != null) {
                listReplacement()
            } else {
                ReceiverSelectList(
                    receivers = receivers,
                    searchQuery = searchState.text.toString(),
                    selectedReceiverIds = selectedReceiverIds,
                    onReceiverToggle = onReceiverToggle,
                )
            }
        }
    }
}

@Composable
private fun ReceiverSelectList(
    receivers: List<ReceiverSelectItem>,
    searchQuery: String,
    selectedReceiverIds: List<Long>,
    onReceiverToggle: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedConsonant by remember { mutableStateOf<Char?>(null) }

    val groupedReceivers =
        remember(receivers, searchQuery) {
            val filtered =
                if (searchQuery.isBlank()) receivers else receivers.filter { it.name.contains(searchQuery) }
            KoreanConsonantUtil.groupByInitialConsonant(filtered) { it.name }
        }

    // 섹션 헤더 없이 항목만 그리므로 스크롤 인덱스도 items.size 로만 누적한다.
    val consonantIndexMap =
        remember(groupedReceivers) {
            var index = 0
            buildMap {
                groupedReceivers.forEach { (consonant, items) ->
                    put(consonant, index)
                    index += items.size
                }
            }
        }

    LaunchedEffect(listState, consonantIndexMap) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisibleItemIndex ->
                selectedConsonant =
                    consonantIndexMap.entries
                        .filter { it.value <= firstVisibleItemIndex }
                        .maxByOrNull { it.value }
                        ?.key
            }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 20.dp),
        ) {
            groupedReceivers.forEach { (_, items) ->
                items(items, key = { it.id }) { receiver ->
                    ReceiverSelectRow(
                        receiver = receiver,
                        isSelected = receiver.id in selectedReceiverIds,
                        onToggle = { onReceiverToggle(receiver.id) },
                    )
                }
            }
            item { Spacer(modifier = Modifier.padding(14.dp)) }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            KoreanConsonantIndex(
                selectedConsonant = selectedConsonant,
                onConsonantSelect = { consonant ->
                    selectedConsonant = consonant
                    consonantIndexMap[consonant]?.let { index ->
                        coroutineScope.launch { listState.scrollToItem(index) }
                    }
                },
            )
        }
    }
}

@Composable
private fun ReceiverSelectRow(
    receiver: ReceiverSelectItem,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                // 체크 아이콘엔 contentDescription 이 없어 «선택됨» 을 읽을 다른 통로가 없다.
                // 복수 선택에서는 어느 행이 선택돼 있는지가 화면의 핵심 정보라 행에 노출한다 (#1426).
                .semantics { selected = isSelected }
                .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileImage(size = 50.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = receiver.name,
                style = AfternoteDesign.typography.captionLargeB,
            )
            Spacer(modifier = Modifier.padding(top = 5.dp))
            Text(
                text = receiver.relation,
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray8,
            )
        }
        AfternoteCircularCheckbox(
            state = if (isSelected) CheckboxState.Default else CheckboxState.None,
            onClick = onToggle,
            size = 20.dp,
        )
    }
}

package com.afternote.feature.timeletter.presentation.screen.recipient

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.calendar.CalendarGridContent
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.HomeTopBar
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetter
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.presentation.component.TimeLetterBlockItem
import com.afternote.feature.timeletter.presentation.component.TimeLetterListItem
import com.afternote.feature.timeletter.presentation.component.ViewToggleButton
import com.afternote.feature.timeletter.presentation.viewmodel.RecipientTimeletterUiState
import com.afternote.feature.timeletter.presentation.viewmodel.RecipientTimeletterViewModel
import com.afternote.feature.timeletter.presentation.viewmodel.ViewMode
import java.time.LocalDate

@Composable
fun RecipientTimeletterScreen(
    onLetterClick: (Long) -> Unit = {},
    // TODO: 수신 타임레터 화면의 진입 경로가 기획되면 설정 화면 이동 콜백을 연결한다.
    onSettingClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: RecipientTimeletterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load()
    }

    var currentYear by remember { mutableIntStateOf(LocalDate.now().year) }
    var currentMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = { HomeTopBar(onSettingClick = onSettingClick) },
    ) { innerPadding ->
        when (val state = uiState) {
            is RecipientTimeletterUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is RecipientTimeletterUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "타임레터를 불러올 수 없습니다.",
                            style = AfternoteDesign.typography.bodySmallR,
                            color = AfternoteDesign.colors.gray6,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.load() }) {
                            Text("다시 시도")
                        }
                    }
                }
            }

            is RecipientTimeletterUiState.Success -> {
                RecipientTimeletterContent(
                    letters = state.letters,
                    currentYear = currentYear,
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    onDateSelect = { day ->
                        selectedDate = LocalDate.of(currentYear, currentMonth, day)
                    },
                    onLetterClick = onLetterClick,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun RecipientTimeletterContent(
    letters: ReceivedTimeLetterList,
    currentYear: Int,
    currentMonth: Int,
    selectedDate: LocalDate,
    onDateSelect: (Int) -> Unit,
    onLetterClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewMode by remember { mutableStateOf(ViewMode.List) }
    val selectedDateLetters =
        letters.timeLetters.filter {
            it.deliveredAt?.take(10) == selectedDate.toString()
        }

    LazyColumn(modifier = modifier) {
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("타임레터", style = AfternoteDesign.typography.h1)
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    "서연님이 남긴 마음을 확인해보세요",
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray6,
                )
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Today’s letter",
                    style = AfternoteDesign.typography.mono,
                )
                Spacer(modifier = Modifier.width(16.dp))
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 1.dp,
                    color = AfternoteDesign.colors.gray3,
                )
            }
        }

        if (selectedDateLetters.isNotEmpty()) {
            items(selectedDateLetters) { letter ->
                TimeLetterBlockItem(
                    letter = letter.toTimeLetter(),
                    receiverNameMap = mapOf(letter.id to (letter.senderName ?: "")),
                    showMetaInfo = false,
                    modifier =
                        Modifier
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clickable { onLetterClick(letter.timeLetterReceiverId) },
                )
            }
        }

        item {
            CalendarGridContent(
                currentYear = currentYear,
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                onDateSelect = onDateSelect,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = AfternoteDesign.colors.gray2,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                ViewToggleButton(viewMode = viewMode, onViewModeChange = { viewMode = it })
            }
        }

        items(letters.timeLetters) { letter ->
            val timeLetter = letter.toTimeLetter()
            val senderNameMap = mapOf(letter.id to (letter.senderName ?: ""))
            val itemModifier =
                Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clickable { onLetterClick(letter.timeLetterReceiverId) }
            if (viewMode == ViewMode.Block) {
                TimeLetterBlockItem(
                    letter = timeLetter,
                    receiverNameMap = senderNameMap,
                    showMetaInfo = false,
                    modifier = itemModifier,
                )
            } else {
                TimeLetterListItem(
                    letter = timeLetter,
                    receiverNameMap = senderNameMap,
                    modifier = itemModifier,
                )
            }
        }
    }
}

private fun ReceivedTimeLetter.toTimeLetter() =
    TimeLetter(
        id = id,
        title = title,
        sendAt = deliveredAt,
        deliveredAt = deliveredAt,
        status = status,
        blocks = blocks,
        receiverIds = listOf(id),
    )

private val previewLetters =
    ReceivedTimeLetterList(
        timeLetters =
            listOf(
                ReceivedTimeLetter(
                    id = 1L,
                    timeLetterReceiverId = 1L,
                    title = "채연아 20번째 생일을 축하해",
                    sendAt = "2027-11-24T00:00:00",
                    status = TimeLetterStatus.SENT,
                    senderName = "박경민",
                    deliveredAt = "2027-11-24T00:00:00",
                    createdAt = "2026-01-01T00:00:00",
                    isRead = true,
                    blocks = emptyList(),
                ),
                ReceivedTimeLetter(
                    id = 2L,
                    timeLetterReceiverId = 2L,
                    title = "10년 뒤의 너에게",
                    sendAt = "2036-05-30T00:00:00",
                    status = TimeLetterStatus.SENT,
                    senderName = "이하늘",
                    deliveredAt = "2036-05-30T00:00:00",
                    createdAt = "2026-01-15T00:00:00",
                    isRead = false,
                    blocks = emptyList(),
                ),
            ),
        totalCount = 2,
    )

@Preview(showBackground = true)
@Composable
private fun RecipientTimeletterScreenPreview() {
    Scaffold(
        containerColor = AfternoteDesign.colors.gray1,
        topBar = { HomeTopBar() },
    ) { innerPadding ->
        RecipientTimeletterContent(
            letters = previewLetters,
            currentYear = 2027,
            currentMonth = 11,
            selectedDate = LocalDate.of(2027, 11, 24),
            onDateSelect = {},
            onLetterClick = {},
            modifier = Modifier.padding(innerPadding),
        )
    }
}

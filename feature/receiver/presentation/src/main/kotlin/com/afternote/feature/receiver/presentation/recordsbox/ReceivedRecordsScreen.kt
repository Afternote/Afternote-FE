package com.afternote.feature.receiver.presentation.recordsbox

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.button.FAB.AfternoteFloatingActionButton
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.receiver.presentation.R

/**
 * 받은 기록함 — 수신자가 등록한 발신자 카드 리스트.
 *
 * Welcome 의 "전달 받은 기록 확인하기" 에서 진입. 본인 확인 상태와 무관하게 진입 가능하며,
 * 발신자별 열람 신청 흐름은 발신자 상세 화면의 "열람 신청하기" 에서 시작한다 (이슈 #215).
 *
 * 빈 상태(14) / 채워진 상태(13) 를 sender 리스트 유무로 분기. 카드는 시안에 배치된 변형(Variant4)
 * 따름: 프로필 동그라미 + 이름 + 우측 chevron.
 */
@Composable
fun ReceivedRecordsScreen(
    onBackClick: () -> Unit,
    onAddSenderClick: () -> Unit,
    onSenderClick: (SenderEntry) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceivedRecordsViewModel = hiltViewModel(),
) {
    val senders by viewModel.senders.collectAsStateWithLifecycle()
    ReceivedRecordsScreenContent(
        senders = senders,
        onBackClick = onBackClick,
        onAddSenderClick = onAddSenderClick,
        onSenderClick = onSenderClick,
        modifier = modifier,
    )
}

@Composable
internal fun ReceivedRecordsScreenContent(
    senders: List<SenderEntry>,
    onBackClick: () -> Unit,
    onAddSenderClick: () -> Unit,
    onSenderClick: (SenderEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.receiver_records_box_title),
                onBackClick = onBackClick,
            )
        },
        floatingActionButton = {
            // 시안(plus_button 48×48, 글리프 16.67dp) 정합: core/ui 기본 56dp 대신 48dp opt-in (#481).
            AfternoteFloatingActionButton(onClick = onAddSenderClick, size = 48.dp, iconSize = 17.dp)
        },
    ) { paddingValues ->
        if (senders.isEmpty()) {
            ReceivedRecordsEmptyContent(
                modifier =
                    Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
            )
        } else {
            ReceivedRecordsList(
                senders = senders,
                onSenderClick = onSenderClick,
                modifier =
                    Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ReceivedRecordsEmptyContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.receiver_records_box_empty),
            style = AfternoteDesign.typography.bodySmallB,
            color = AfternoteDesign.colors.gray4,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReceivedRecordsList(
    senders: List<SenderEntry>,
    onSenderClick: (SenderEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = senders, key = { it.id }) { sender ->
            SenderCard(
                sender = sender,
                onClick = { onSenderClick(sender) },
            )
        }
    }
}

@Composable
private fun SenderCard(
    sender: SenderEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(com.afternote.core.ui.R.drawable.core_ui_ic_profile_placeholder),
            contentDescription = null,
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape),
        )

        Spacer(modifier = Modifier.size(10.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = sender.name,
                style = AfternoteDesign.typography.bodySmallB,
                color = AfternoteDesign.colors.gray9,
            )
        }

        RightArrowIcon(
            modifier = Modifier.size(16.dp),
            tint = AfternoteDesign.colors.gray8,
        )
    }
}

package com.afternote.feature.setting.presentation.receiver

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.ProfileImage
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.receiver.ReceiverProfileRow
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R

/**
 * 설정 > 수신자 관리 목록 화면 (#631).
 *
 * 이전에는 공용 선택 UI([com.afternote.core.ui.receiver.ReceiverSelectScreen])를 그대로
 * 소비해 체크박스·완료 버튼이 있는 "선택 화면"으로 동작했다. 여기서는 목록 열람·수정 진입만
 * 다루는 관리 화면으로, 행 탭이 곧바로 수신자 수정 화면(#595)으로 이동한다.
 *
 * 수신자가 0건이면 목록 대신 [ReceiverManageEmpty] 안내를 그린다 (#556). 이 화면엔 검색창이
 * 없어서 검색 결과 0건 축은 여기 해당하지 않는다. 그 축은 검색어를 쥔 core:ui 안에서 닫는다.
 */
@Composable
fun ReceiverManageScreen(
    receivers: List<ReceiverListItem>,
    onBackClick: () -> Unit,
    onReceiverClick: (Long) -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.settings_recipient_list),
                onBackClick = onBackClick,
            )
        },
    ) { innerPadding ->
        if (receivers.isEmpty()) {
            ReceiverManageEmpty(
                onRegisterClick = onRegisterClick,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp),
            ) {
                items(receivers, key = { it.receiverId }) { receiver ->
                    ReceiverManageRow(
                        receiver = receiver,
                        onClick = { onReceiverClick(receiver.receiverId) },
                    )
                }
            }
        }
    }
}

/**
 * 수신자 0건 빈 상태 (#556). 시안 3628:23840 이 안내 문구와 [수신자 등록하기] 를 함께 둔다.
 *
 * 문구·일러스트 구성은 같은 성격의 확정 시안(4163:20979)을 그대로 구현한 애프터노트
 * `SelectReceiverEmpty` 를 준거로 한다 (제목 → 8dp → 설명 → 56dp → 일러스트).
 * 등록 CTA 는 Scaffold bottomBar 가 아니라 본문 안에 둔다. 뒤에 목록 상태 슬롯(#1281)으로
 * 이 본체를 통째로 옮길 때 빈 상태만 CTA 를 잃지 않게 하려는 것이다.
 */
@Composable
private fun ReceiverManageEmpty(
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(horizontal = 20.dp)
                .padding(top = 40.dp, bottom = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_recipient_empty),
            style = AfternoteDesign.typography.h1,
            color = AfternoteDesign.colors.black,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_recipient_empty_description),
            // 애프터노트 빈 상태와 같은 근거다. 시안의 H3 는 weight 400 인데 레포 `h3` 토큰은 Bold 다.
            style = AfternoteDesign.typography.h3.copy(fontWeight = FontWeight.Normal),
            color = AfternoteDesign.colors.gray6,
        )
        Spacer(modifier = Modifier.height(56.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ProfileImage()
        }
        Spacer(modifier = Modifier.weight(1f))
        AfternoteButton(
            text = stringResource(R.string.settings_recipient_empty_register),
            onClick = onRegisterClick,
        )
    }
}

@Composable
private fun ReceiverManageRow(
    receiver: ReceiverListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ReceiverProfileRow(
        name = receiver.name,
        relation = receiver.relation,
        onClick = onClick,
        modifier = modifier,
        trailing = {
            Image(
                painterResource(R.drawable.ic_right_arrow),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun ReceiverManageScreenPrev() {
    ReceiverManageScreen(
        receivers =
            listOf(
                ReceiverListItem(receiverId = 1L, name = "박경민", relation = "친구"),
                ReceiverListItem(receiverId = 2L, name = "김철수", relation = "가족"),
                ReceiverListItem(receiverId = 3L, name = "이영희", relation = "연인"),
            ),
        onBackClick = {},
        onReceiverClick = {},
        onRegisterClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun ReceiverManageScreenEmptyPrev() {
    ReceiverManageScreen(
        receivers = emptyList(),
        onBackClick = {},
        onReceiverClick = {},
        onRegisterClick = {},
    )
}

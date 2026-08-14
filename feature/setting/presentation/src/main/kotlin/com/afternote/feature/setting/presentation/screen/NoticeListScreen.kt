package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.domain.Notice
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.NoticeListItem
import java.time.LocalDate

// 설정 - 공지사항
@Composable
fun NoticeListScreen(
    notices: List<Notice>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.settings_support_notice),
                onBackClick = onBackClick,
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        if (notices.isEmpty()) {
            NoticeEmptyState(
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
                        .padding(innerPadding),
            ) {
                items(notices) { notice ->
                    NoticeListItem(notice = notice)
                }
            }
        }
    }
}

@Composable
private fun NoticeEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_notice_empty),
            style = AfternoteDesign.typography.bodyLargeR,
            color = AfternoteDesign.colors.gray8,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoticeListScreenPrev() {
    NoticeListScreen(
        onBackClick = {},
        notices =
            listOf(
                Notice(
                    date = LocalDate.of(2022, 11, 20),
                    title = "서비스 점검 안내",
                    content = "서버 점검으로 인해 서비스가 일시 중단됩니다.",
                ),
                Notice(
                    date = LocalDate.of(2023, 3, 5),
                    title = "개인정보 처리방침 변경 안내",
                    content = "개인정보 처리방침이 변경되었습니다.",
                ),
            ),
    )
}

@Preview(showBackground = true)
@Composable
private fun NoticeListScreenEmptyPrev() {
    NoticeListScreen(
        onBackClick = {},
        notices = emptyList(),
    )
}

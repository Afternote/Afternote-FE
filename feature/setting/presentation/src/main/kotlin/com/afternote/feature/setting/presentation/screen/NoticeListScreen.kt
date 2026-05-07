package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.domain.Notice
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
                title = "공지사항",
                onBackClick = onBackClick,
            )
        },
    ) { innerPadding ->
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

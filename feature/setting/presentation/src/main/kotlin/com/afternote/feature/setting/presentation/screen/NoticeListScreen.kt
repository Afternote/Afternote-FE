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
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.domain.Notice
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.NoticeListItem

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

package com.afternote.core.ui.scaffold.topbar

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.ViewModeSwitcher
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    // TopAppBar -> CenterAlignedTopAppBar 로 변경
    CenterAlignedTopAppBar(
        title = {
            // Row로 감싸서 강제 정렬할 필요가 없어집니다.
            Text(
                text = title,
                style = AfternoteDesign.typography.bodyLargeB,
                // CenterAlignedTopAppBar가 알아서 정중앙에 꽂아줍니다.
            )
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.core_ui_arrow_left),
                        contentDescription = stringResource(R.string.core_ui_content_description_back),
                    )
                }
            }
        },
        actions = {
            actions()
            Spacer(modifier = Modifier.width(17.dp))
        },
        modifier = modifier,
    )
}

@Preview(showBackground = true, name = "Light Mode - List")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
private fun DailyRecordTopBarPreview() {
    var isListMode by remember { mutableStateOf(true) }

    AfternoteTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 1. 둘 다 있는 경우 (기존과 동일)
                DetailTopBar(
                    title = "데일리 질문 (Full)",
                    onBackClick = { /* 뒤로가기 액션 */ },
                    actions = {
                        ViewModeSwitcher(
                            isListView = isListMode,
                            onViewChange = { isListMode = it },
                            image1 = R.drawable.core_ui_list,
                            image2 = R.drawable.core_ui_calendar,
                        )
                    },
                )

                // 2. 뒤로 가기만 있는 경우 (actions 생략)
                DetailTopBar(
                    title = "상세 화면 (Back Only)",
                    onBackClick = { /* 뒤로가기 액션 */ },
                )

                // 3. 둘 다 없는 경우 (타이틀만 존재)
                DetailTopBar(
                    title = "메인 홈 (Title Only)",
                )
            }
        }
    }
}

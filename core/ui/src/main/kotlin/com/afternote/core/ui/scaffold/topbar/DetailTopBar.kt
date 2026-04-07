package com.afternote.core.ui.scaffold.topbar

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
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
    onBackClick: (() -> Unit)? = null, // 1. Nullable 처리 및 기본값 null
    actions: @Composable RowScope.() -> Unit = {}, // 2. 네이밍 변경(action -> actions) 및 기본값 빈 람다
) {
    TopAppBar(
        title = {
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = AfternoteDesign.typography.bodyLargeB,
                    textAlign = TextAlign.Center,
                )
            }
        },
        navigationIcon = {
            // 3. onBackClick이 전달되었을 때만 아이콘 렌더링
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                ) {
                    Icon(
                        painterResource(R.drawable.core_ui_arrow_left),
                        contentDescription = "뒤로 가기",
                    )
                }
            }
        },
        actions = actions, // 4. 그대로 전달 (비어있으면 아무것도 안 그림)
        modifier = modifier.padding(end = 17.dp),
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

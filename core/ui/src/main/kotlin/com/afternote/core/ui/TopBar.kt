package com.afternote.core.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.component.ViewModeSwitcher
import com.afternote.core.ui.theme.AfternoteTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(modifier: Modifier = Modifier) {
    TopAppBar(
        navigationIcon = {
            Image(
                painter = painterResource(R.drawable.core_ui_logo),
                contentDescription = null,
                modifier = Modifier.size(90.dp),
            )
        },
        title = { },
        actions = {
            Row {
                Image(
                    painter = painterResource(R.drawable.core_ui_user),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )

                Spacer(modifier = Modifier.width(15.dp))

                Image(
                    painter = painterResource(R.drawable.core_ui_settings),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    isListView: Boolean,
    onBackClick: () -> Unit,
    onToggleClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = "데일리 질문",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
            ) {
                Icon(
                    painterResource(R.drawable.core_ui_arrow_left),
                    contentDescription = null,
                )
            }
        },
        actions = {
            ViewModeSwitcher(
                isListView = isListView,
                onViewChange = {
                    onToggleClick(it)
                },
            )
        },
        modifier = modifier.padding(end = 17.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun TopBarPreview() {
    TopBar()
}

@Preview(showBackground = true, name = "Light Mode - List")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
private fun DailyRecordTopBarPreview() {
    var isListMode by remember { mutableStateOf(true) }

    AfternoteTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 실제 컴포넌트 호출
                TopBar(
                    isListView = isListMode,
                    onBackClick = { /* 뒤로가기 로그 확인 */ },
                    onToggleClick = { newValue ->
                        isListMode = newValue // 클릭 시 프리뷰 상태가 변하도록 연결
                    },
                )
            }
        }
    }
}

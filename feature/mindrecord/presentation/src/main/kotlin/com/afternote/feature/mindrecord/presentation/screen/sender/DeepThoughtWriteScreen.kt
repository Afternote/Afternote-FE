package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Gray2
import com.afternote.core.ui.theme.Gray6
import com.afternote.core.ui.theme.Gray7
import com.afternote.core.ui.theme.Gray9
import com.afternote.feature.mindrecord.presentation.component.DailyDeepThoughtCard

@Composable
fun DeepThoughtWriteScreen(modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            DetailTopBar(
                title = "깊은 생각 기록하기",
                onBackClick = {},
                action = {
                    Button(
                        onClick = {},
                        shape = RoundedCornerShape(6.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Gray2,
                            ),
                    ) {
                        Text(
                            text = "등록",
                            style = MaterialTheme.typography.titleSmall,
                            color = Gray6,
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            DailyDeepThoughtCard(modifier = Modifier.height(150.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "카테고리",
                    style = MaterialTheme.typography.titleSmall,
                    color = Gray7,
                )
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "나의 가치관",
                            style = MaterialTheme.typography.displayMedium,
                            color = Gray9,
                        )

                        Icon(
                            painter = painterResource(R.drawable.core_ui_arrowdown),
                            contentDescription = null,
                        )
                    }

                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeepThoughtWriteScreenPreview() {
    AfternoteTheme {
        DeepThoughtWriteScreen()
    }
}

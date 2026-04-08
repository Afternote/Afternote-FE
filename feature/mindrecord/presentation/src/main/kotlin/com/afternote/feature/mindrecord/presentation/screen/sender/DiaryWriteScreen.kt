package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.WriteTextField
import java.time.LocalDate

@Composable
fun DiaryWriteScreen(modifier: Modifier = Modifier) {
    var titleState = rememberTextFieldState()
    Scaffold(
        topBar = {
            DetailTopBar(
                title = "일기 기록하기",
                onBackClick = {},
                action = {
                    Button(
                        onClick = {},
                        shape = RoundedCornerShape(6.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = AfternoteDesign.colors.gray2,
                            ),
                    ) {
                        Text(
                            text = "등록",
                            style = MaterialTheme.typography.titleSmall,
                            color = AfternoteDesign.colors.gray6,
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = LocalDate.now().toString(),
                    style = MaterialTheme.typography.displayMedium,
                    color = AfternoteDesign.colors.gray9,
                )

                Icon(
                    painter = painterResource(R.drawable.core_ui_arrowdown),
                    contentDescription = null,
                )
            }

            HorizontalDivider()

            TextField(
                state = titleState,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                label = {
                    Text(
                        text = "제목을 입력하세요.",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFF000000).copy(0.2f),
                    )
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(com.afternote.feature.mindrecord.presentation.R.drawable.mindrecord_emotion),
                    contentDescription = null,
                    tint = Color(0xFF000000).copy(0.4f),
                )

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "오늘의 기분",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color(0xFF000000).copy(0.4f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF000000).copy(0.05f))
                            .size(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "\uD83D\uDE0A",
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF000000).copy(0.05f))
                            .size(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "\uD83D\uDE10",
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF000000).copy(0.05f))
                            .size(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "\uD83D\uDE22",
                    )
                }
            }
            Row {
                Button(
                    onClick = {},
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                        ),
                    border = BorderStroke(1.dp, color = Color(0xFF000000).copy(0.05f)),
                ) {
                    Icon(
                        painter = painterResource(com.afternote.feature.mindrecord.presentation.R.drawable.mindrecord_pos),
                        contentDescription = null,
                        tint = Color(0xFF000000).copy(0.6f),
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "위치 추가",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color(0xFF000000).copy(0.6f),
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Button(
                    onClick = {},
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                        ),
                    border = BorderStroke(1.dp, color = Color(0xFF000000).copy(0.05f)),
                ) {
                    Icon(
                        painter = painterResource(com.afternote.feature.mindrecord.presentation.R.drawable.mindrecord_picture),
                        contentDescription = null,
                        tint = Color(0xFF000000).copy(0.6f),
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "사진 추가",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color(0xFF000000).copy(0.6f),
                    )
                }
            }

            WriteTextField()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryWriteScreenPreview() {
    AfternoteTheme {
        DiaryWriteScreen()
    }
}

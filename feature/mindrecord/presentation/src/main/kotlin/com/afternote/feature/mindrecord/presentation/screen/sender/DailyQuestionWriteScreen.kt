package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.TopBar
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Gray2
import com.afternote.core.ui.theme.Gray6
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionWriteHeaderCard
import com.afternote.feature.mindrecord.presentation.component.WriteTextField
import java.time.LocalDate

@Composable
fun DailyQuestionWriteScreen(modifier : Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopBar(
                title = LocalDate.now().toString(),
                action = {
                    Button(
                        onClick = {},
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gray2
                        )
                    ) {
                        Text(
                            text = "등록",
                            style = MaterialTheme.typography.titleSmall,
                            color = Gray6
                        )
                    }
                },
                onBackClick = {}
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column {
            Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 20.dp)) {
                DailyQuestionWriteHeaderCard()
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "YOUR ANSWER",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color(0xFF000000).copy(alpha = 0.4f),
                    )

                    HorizontalDivider(modifier = Modifier.padding(start = 12.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

            }
            Column {
                WriteTextField()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyQuestionWriteScreenPreview() {
    AfternoteTheme {
        DailyQuestionWriteScreen()
    }
}
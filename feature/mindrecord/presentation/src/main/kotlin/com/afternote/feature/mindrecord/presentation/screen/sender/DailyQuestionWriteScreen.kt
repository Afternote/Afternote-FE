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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Red
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionWriteHeaderCard
import com.afternote.feature.mindrecord.presentation.component.WriteTextField
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.SubmitState
import java.time.LocalDate

@Composable
fun DailyQuestionWriteScreen(
    modifier: Modifier = Modifier,
    onSubmitSuccess: () -> Unit = {},
    onBackClick: () -> Unit = {},
    viewModel: DailyQuestionWriteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSubmitSuccess by rememberUpdatedState(onSubmitSuccess)

    LaunchedEffect(uiState.submitState) {
        if (uiState.submitState is SubmitState.Succeeded) {
            currentOnSubmitSuccess()
            viewModel.consumeSubmitResult()
        }
    }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = LocalDate.now().toString(),
                actions = {
                    Button(
                        onClick = { viewModel.submit() },
                        enabled = uiState.canSubmit,
                        shape = RoundedCornerShape(6.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = AfternoteDesign.colors.gray2,
                            ),
                    ) {
                        Text(
                            text = "등록",
                            style = AfternoteDesign.typography.bodySmallB,
                            color = AfternoteDesign.colors.gray6,
                        )
                    }
                },
                onBackClick = onBackClick,
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column {
            Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 20.dp)) {
                val headerText =
                    when {
                        uiState.isQuestionLoading -> "오늘의 질문을 불러오는 중..."
                        uiState.questionLoadError != null -> uiState.questionLoadError!!
                        uiState.questionContent.isNotEmpty() -> uiState.questionContent
                        else -> "오늘의 질문이 없습니다."
                    }
                DailyQuestionWriteHeaderCard(
                    questionText = headerText,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "YOUR ANSWER",
                        style = AfternoteDesign.typography.mono,
                        color = AfternoteDesign.colors.black.copy(alpha = 0.4f),
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 12.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))

                val errorMessage = (uiState.submitState as? SubmitState.Failed)?.message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = Red,
                        style = AfternoteDesign.typography.captionLargeR,
                    )
                }
            }
            WriteTextField(
                value = uiState.answer,
                onValueChange = viewModel::onAnswerChanged,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyQuestionWriteScreenPreview() {
    AfternoteTheme {
        // Preview는 ViewModel 없이 호출 불가 — 컴파일 확인용 placeholder
    }
}

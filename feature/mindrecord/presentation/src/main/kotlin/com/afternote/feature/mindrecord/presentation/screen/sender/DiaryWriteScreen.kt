package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.R
import com.afternote.core.ui.asString
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Red
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.presentation.component.BottomSheetCalendar
import com.afternote.feature.mindrecord.presentation.component.WriteTextField
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.SubmitState
import com.afternote.feature.mindrecord.presentation.R as MindRecordR

@Composable
fun DiaryWriteScreen(
    modifier: Modifier = Modifier,
    onSubmitSuccess: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onDraftListClick: () -> Unit = {},
    viewModel: DiaryWriteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
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
                title = stringResource(MindRecordR.string.mindrecord_diary_write_title),
                onBackClick = onBackClick,
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
                            text = stringResource(MindRecordR.string.mindrecord_action_register),
                            style = AfternoteDesign.typography.bodySmallB,
                            color = AfternoteDesign.colors.gray6,
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = uiState.date.toString(),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray9,
                )
                IconButton(onClick = { showPicker = !showPicker }) {
                    Icon(
                        painter = painterResource(R.drawable.core_ui_arrowdown),
                        contentDescription = null,
                    )
                }
            }

            HorizontalDivider()

            TextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChanged,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                placeholder = {
                    Text(
                        text = stringResource(MindRecordR.string.mindrecord_diary_write_title_placeholder),
                        style = AfternoteDesign.typography.h2,
                        color = AfternoteDesign.colors.black.copy(alpha = 0.2f),
                    )
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val selectedMood = uiState.mood
                if (selectedMood != null) {
                    Text(text = selectedMood.emoji())
                } else {
                    Icon(
                        painter = painterResource(com.afternote.feature.mindrecord.presentation.R.drawable.mindrecord_emotion),
                        contentDescription = null,
                        tint = Color(0xFF000000).copy(0.4f),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(MindRecordR.string.mindrecord_diary_write_mood_label),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.black.copy(alpha = 0.4f),
                )
                Spacer(modifier = Modifier.width(12.dp))

                MoodChip("😊", selected = uiState.mood == TodayMood.HAPPY) {
                    viewModel.onMoodSelected(TodayMood.HAPPY)
                }
                Spacer(modifier = Modifier.width(8.dp))
                MoodChip("😐", selected = uiState.mood == TodayMood.SOSO) {
                    viewModel.onMoodSelected(TodayMood.SOSO)
                }
                Spacer(modifier = Modifier.width(8.dp))
                MoodChip("😢", selected = uiState.mood == TodayMood.SAD) {
                    viewModel.onMoodSelected(TodayMood.SAD)
                }
            }

            val errorMessage = (uiState.submitState as? SubmitState.Failed)?.message?.asString()
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Red,
                    style = AfternoteDesign.typography.captionLargeR,
                )
            }

            WriteTextField(
                value = uiState.content,
                onValueChange = viewModel::onContentChanged,
                onSaveDraftClick = { viewModel.submit(isDraft = true) },
                onDraftCountClick = onDraftListClick,
            )
        }

        if (showPicker) {
            BottomSheetCalendar(
                initialDate = uiState.date,
                onDismiss = { showPicker = false },
                onDateSelect = { date ->
                    viewModel.onDateSelected(date)
                    showPicker = false
                },
            )
        }
    }
}

@Composable
private fun MoodChip(
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(
                    if (selected) {
                        AfternoteDesign.colors.gray2
                    } else {
                        Color(0xFF000000).copy(0.05f)
                    },
                ).size(32.dp)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji)
    }
}

private fun TodayMood.emoji(): String =
    when (this) {
        TodayMood.HAPPY -> "😊"
        TodayMood.SOSO -> "😐"
        TodayMood.SAD -> "😢"
    }

@Preview(showBackground = true)
@Composable
private fun DiaryWriteScreenPreview() {
    AfternoteTheme {
        // ViewModel 의존이 있어 Preview는 비워둠
    }
}

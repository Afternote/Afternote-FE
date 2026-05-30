package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.clickable
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
import com.afternote.feature.mindrecord.presentation.component.CategorySettingBottomSheet
import com.afternote.feature.mindrecord.presentation.component.DailyDeepThoughtCard
import com.afternote.feature.mindrecord.presentation.component.WriteTextField
import com.afternote.feature.mindrecord.presentation.model.CategoryUiModel
import com.afternote.feature.mindrecord.presentation.viewmodel.DeepThoughtWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.SubmitState
import com.afternote.feature.mindrecord.presentation.R as MindRecordR

@Composable
fun DeepThoughtWriteScreen(
    modifier: Modifier = Modifier,
    onSubmitSuccess: () -> Unit = {},
    onBackClick: () -> Unit = {},
    viewModel: DeepThoughtWriteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCategorySheet by remember { mutableStateOf(false) }
    val currentOnSubmitSuccess by rememberUpdatedState(onSubmitSuccess)

    // VM 은 Context 를 들고 있지 않으므로, 초기 진입 시 화면에서 기본 카테고리 문자열을 시드한다.
    val defaultCategory = stringResource(MindRecordR.string.mindrecord_deep_thought_write_default_category)
    LaunchedEffect(Unit) {
        if (uiState.category.isBlank()) {
            viewModel.onCategoryChanged(defaultCategory)
        }
    }

    LaunchedEffect(uiState.submitState) {
        if (uiState.submitState is SubmitState.Succeeded) {
            currentOnSubmitSuccess()
            viewModel.consumeSubmitResult()
        }
    }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(MindRecordR.string.mindrecord_deep_thought_write_title),
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
            DailyDeepThoughtCard(modifier = Modifier.height(150.dp))

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
                        text = stringResource(MindRecordR.string.mindrecord_deep_thought_write_title_label),
                        style = AfternoteDesign.typography.h3,
                        color = AfternoteDesign.colors.black.copy(alpha = 0.2f),
                    )
                },
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(MindRecordR.string.mindrecord_deep_thought_write_category_label),
                    style = AfternoteDesign.typography.bodySmallB,
                    color = AfternoteDesign.colors.gray7,
                )
                Column(
                    modifier =
                        Modifier
                            .padding(start = 12.dp)
                            .clickable { showCategorySheet = true },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = uiState.category,
                            style = AfternoteDesign.typography.captionLargeR,
                            color = AfternoteDesign.colors.gray9,
                        )
                        Icon(
                            painter = painterResource(R.drawable.core_ui_arrowdown),
                            contentDescription = null,
                            modifier =
                                Modifier.padding(start = 8.dp),
                        )
                    }
                    HorizontalDivider()
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
            )
        }

        if (showCategorySheet) {
            CategorySettingBottomSheet(
                categories =
                    listOf(
                        CategoryUiModel(
                            "1",
                            stringResource(MindRecordR.string.mindrecord_deep_thought_write_default_category),
                            Color(0xFF1A1A1A),
                        ),
                        CategoryUiModel(
                            "2",
                            stringResource(MindRecordR.string.mindrecord_deep_thought_sample_category_today),
                            Color(0xFFFFB3A7),
                        ),
                        CategoryUiModel(
                            "3",
                            stringResource(MindRecordR.string.mindrecord_deep_thought_sample_category_retrospective),
                            Color(0xFFA8C8E8),
                        ),
                    ),
                onDismiss = { showCategorySheet = false },
                onBackClick = { showCategorySheet = false },
                onAddCategory = { },
                onCategoryClick = { category ->
                    viewModel.onCategoryChanged(category.name)
                    showCategorySheet = false
                },
                onMenuClick = { },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeepThoughtWriteScreenPreview() {
    AfternoteTheme {
        // ViewModel 의존이 있어 Preview는 비워둠
    }
}

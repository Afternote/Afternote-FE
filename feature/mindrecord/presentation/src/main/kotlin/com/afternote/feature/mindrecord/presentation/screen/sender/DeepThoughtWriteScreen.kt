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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.CategorySettingBottomSheet
import com.afternote.feature.mindrecord.presentation.component.DailyDeepThoughtCard
import com.afternote.feature.mindrecord.presentation.component.WriteTextField
import com.afternote.feature.mindrecord.presentation.model.CategoryUiModel

@Composable
fun DeepThoughtWriteScreen(modifier: Modifier = Modifier) {
    var showCategorySheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = "깊은 생각 기록하기",
                onBackClick = {},
                actions = {
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
            DailyDeepThoughtCard(modifier = Modifier.height(150.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "카테고리",
                    style = AfternoteDesign.typography.bodySmallB,
                    color = AfternoteDesign.colors.gray7,
                )
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "나의 가치관",
                            style = AfternoteDesign.typography.captionLargeR,
                            color = AfternoteDesign.colors.gray9,
                        )

                        Icon(
                            painter = painterResource(R.drawable.core_ui_arrowdown),
                            contentDescription = null,
                        )
                    }

                    HorizontalDivider()

                }

            }

            WriteTextField()
        }

        if (showCategorySheet) {
            CategorySettingBottomSheet(
                categories = listOf(
                    CategoryUiModel("1", "나의 가치관", Color(0xFF1A1A1A)),
                    CategoryUiModel("2", "오늘 떠올린 생각", Color(0xFFFFB3A7)),
                    CategoryUiModel("3", "인생을 되돌아 보며", Color(0xFFA8C8E8)),
                ),
                onDismiss = { showCategorySheet = false },
                onBackClick = { showCategorySheet = false },
                onAddCategory = { /* 새 카테고리 생성 화면으로 */ },
                onMenuClick = { category -> /* 수정/삭제 메뉴 */ },
            )
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

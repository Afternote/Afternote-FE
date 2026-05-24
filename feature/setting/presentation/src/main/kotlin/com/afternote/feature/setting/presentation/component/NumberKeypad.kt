package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign

private val keyRows =
    listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("del", "0", "확인"),
    )

@Composable
fun NumberKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        keyRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .aspectRatio(2f)
                                .clickable {
                                    when (key) {
                                        "del" -> onDeleteClick()
                                        "확인" -> onConfirmClick()
                                        else -> onDigitClick(key)
                                    }
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        when (key) {
                            "del" -> {
                                Icon(
                                    painter = painterResource(R.drawable.core_ui_arrow_left),
                                    contentDescription = stringResource(R.string.core_ui_content_description_back),
                                )
                            }

                            "확인" -> {
                                Text(
                                    text = "확인",
                                    style = AfternoteDesign.typography.textField,
                                    color = AfternoteDesign.colors.gray5,
                                )
                            }

                            else -> {
                                Text(
                                    text = key,
                                    style = AfternoteDesign.typography.h2.copy(fontSize = 30.sp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.core.ui.R as CoreR

private val relationOptions = listOf("가족", "친구", "직장동료", "기타")

@Composable
fun ReceiverRegisterScreen(modifier: Modifier = Modifier) {
    val nameState = rememberTextFieldState()
    var selectedRelation by remember { mutableStateOf<String?>(null) }
    var relationExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DetailTopBar("수신자 등록")
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier.padding(innerPadding),
        ) {
            item {
                Box {
                    Image(
                        painterResource(R.drawable.ic_default_profile),
                        contentDescription = "기본 사진",
                    )
                }
            }
            item {
                Text("이름")
                AfternoteTextField(
                    state = nameState,
                    placeholder = "이름을 입력하세요",
                )
            }
            item {
                Text("연락처")
                AfternoteTextField(
                    state = nameState,
                    placeholder = "연락처를 지정해주세요",
                )
            }
            item {
                Text("관계")
                val shape = RoundedCornerShape(8.dp)
                Box {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(AfternoteDesign.colors.white, shape)
                                .border(1.dp, AfternoteDesign.colors.gray2, shape)
                                .clickable { relationExpanded = true }
                                .padding(horizontal = 24.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = selectedRelation ?: "관계를 선택하세요",
                            style = AfternoteDesign.typography.textField,
                            color = if (selectedRelation != null) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.gray4,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            painter = painterResource(CoreR.drawable.core_ui_arrowdown),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(18.dp)
                                    .rotate(if (relationExpanded) 180f else 0f),
                            tint = AfternoteDesign.colors.gray6,
                        )
                    }
                    DropdownMenu(
                        expanded = relationExpanded,
                        onDismissRequest = { relationExpanded = false },
                        modifier = Modifier.background(AfternoteDesign.colors.white),
                    ) {
                        relationOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        style = AfternoteDesign.typography.textField,
                                        color = AfternoteDesign.colors.gray9,
                                    )
                                },
                                onClick = {
                                    selectedRelation = option
                                    relationExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                Text("이메일")
                AfternoteTextField(
                    state = nameState,
                    placeholder = "afternote@email.com",
                )
            }
        }
    }
}

@Preview
@Composable
private fun ReceiverRegisterScreenPrev() {
    ReceiverRegisterScreen()
}

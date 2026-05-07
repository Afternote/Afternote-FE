package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R

@Composable
fun RecipientSetScreen(modifier: Modifier = Modifier) {
    val phoneState = rememberTextFieldState()
    val emailState = rememberTextFieldState()
    var isEditing by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar("수신인 설정")
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painterResource(R.drawable.ic_default_profile),
                        contentDescription = "프로필",
                        modifier = Modifier.size(134.dp),
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("이름", style = AfternoteDesign.typography.bodySmallB)
                    Text("관계", style = AfternoteDesign.typography.bodySmallB)
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("연락처", style = AfternoteDesign.typography.bodySmallB)
                    if (isEditing) {
                        AfternoteTextField(
                            state = phoneState,
                            placeholder = "연락처를 입력하세요",
                        )
                    } else {
                        ReadOnlyField(
                            value = phoneState.text.toString(),
                            placeholder = "연락처를 입력하세요",
                        )
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("이메일", style = AfternoteDesign.typography.bodySmallB)
                    if (isEditing) {
                        AfternoteTextField(
                            state = emailState,
                            placeholder = "afternote@email.com",
                        )
                    } else {
                        ReadOnlyField(
                            value = emailState.text.toString(),
                            placeholder = "afternote@email.com",
                        )
                    }
                }
            }
            item {
                Text("전달할 내용")
            }
            item {
                Spacer(Modifier.height(8.dp))
                if (isEditing) {
                    AfternoteButton(
                        text = "저장하기",
                        onClick = { isEditing = false },
                        type = AfternoteButtonType.Default,
                    )
                } else {
                    AfternoteButton(
                        text = "수정하기",
                        onClick = { isEditing = true },
                        type = AfternoteButtonType.Default,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ReadOnlyField(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(AfternoteDesign.colors.white, shape)
                .border(1.dp, AfternoteDesign.colors.gray2, shape)
                .padding(horizontal = 24.dp, vertical = 13.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = value.ifEmpty { placeholder },
            style = AfternoteDesign.typography.textField,
            color = if (value.isNotEmpty()) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.gray4,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipientSetScreenPrev() {
    RecipientSetScreen()
}

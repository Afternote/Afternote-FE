package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R

// 설정 - 회원 탈퇴 안내
@Composable
fun WithdrawGuideScreen(
    userName: String,
    userEmail: String,
    onBackClick: () -> Unit,
    onCancelClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var agreed by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            DetailTopBar(
                title = "회원 탈퇴 안내",
                onBackClick = onBackClick,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
        ) {
            item { Spacer(Modifier.height(20.dp)) }
            item { TopMessageSection() }
            item { Spacer(Modifier.height(24.dp)) }
            item { AccountSection(userName = userName, userEmail = userEmail) }
            item { Spacer(Modifier.height(24.dp)) }
            item { DeleteItemsSection() }
            item { Spacer(Modifier.height(24.dp)) }
            item { CheckListSection() }
            item { Spacer(Modifier.height(24.dp)) }
            item { PrivacySection() }
            item { Spacer(Modifier.height(24.dp)) }
            item { AgreeCheckbox(checked = agreed, onCheckedChange = { agreed = it }) }
            item { Spacer(Modifier.height(16.dp)) }
            item {
                BottomButtons(
                    agreed = agreed,
                    onCancelClick = onCancelClick,
                    onConfirmClick = onConfirmClick,
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TopMessageSection() {
    Text(
        text = stringResource(R.string.withdraw_top_message),
        style = AfternoteDesign.typography.bodyBase,
        color = AfternoteDesign.colors.gray9,
    )
}

@Composable
private fun AccountSection(
    userName: String,
    userEmail: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(82.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color = AfternoteDesign.colors.gray2)
                .padding(start = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.withdraw_account_section),
            style = AfternoteDesign.typography.textField,
            color = AfternoteDesign.colors.gray9,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Text(
                text = userName,
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray9,
            )

            Text(
                text = " ($userEmail)",
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray6,
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DeleteItemsSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = AfternoteDesign.colors.gray3)
        Spacer(modifier = Modifier.padding(top = 10.dp))
        Text(
            text = stringResource(R.string.withdraw_delete_section),
            style = AfternoteDesign.typography.textField,
            color = AfternoteDesign.colors.gray9,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.withdraw_delete_description),
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.gray8,
        )
        Spacer(Modifier.height(8.dp))
        listOf(
            R.string.withdraw_delete_item_1,
            R.string.withdraw_delete_item_2,
            R.string.withdraw_delete_item_3,
            R.string.withdraw_delete_item_4,
        ).forEach { resId ->
            BulletItem(text = stringResource(resId))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.withdraw_delete_warning),
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.gray8,
        )
    }
}

@Composable
private fun CheckListSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = AfternoteDesign.colors.gray3)
        Spacer(modifier = Modifier.padding(top = 10.dp))
        Text(
            text = stringResource(R.string.withdraw_check_section),
            style = AfternoteDesign.typography.textField,
            color = AfternoteDesign.colors.gray9,
        )
        Spacer(Modifier.height(8.dp))
        listOf(
            R.string.withdraw_check_item_1,
            R.string.withdraw_check_item_2,
            R.string.withdraw_check_item_3,
            R.string.withdraw_check_item_4,
        ).forEach { resId ->
            BulletItem(text = stringResource(resId))
        }
    }
}

@Composable
private fun PrivacySection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = AfternoteDesign.colors.gray3)
        Spacer(modifier = Modifier.padding(top = 10.dp))
        Text(
            text = stringResource(R.string.withdraw_privacy_section),
            style = AfternoteDesign.typography.textField,
            color = AfternoteDesign.colors.gray9,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.withdraw_privacy_description),
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.gray8,
        )
    }
}

@Composable
private fun AgreeCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        AfternoteCircularCheckbox(
            state = if (checked) CheckboxState.Default else CheckboxState.None,
            size = 20.dp,
            onClick = { onCheckedChange(!checked) },
        )
        Spacer(modifier = Modifier.padding(12.dp))
        Text(
            text = stringResource(R.string.withdraw_agree_checkbox),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray9,
        )
    }
}

@Composable
private fun BottomButtons(
    agreed: Boolean,
    onCancelClick: () -> Unit,
    onConfirmClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AfternoteButton(
            text = stringResource(R.string.withdraw_cancel_button),
            onClick = onCancelClick,
            type = AfternoteButtonType.Default,
            modifier = Modifier.fillMaxWidth(),
        )
        AfternoteButton(
            text = stringResource(R.string.withdraw_confirm_button),
            onClick = onConfirmClick,
            type = if (agreed) AfternoteButtonType.Default else AfternoteButtonType.Un,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BulletItem(text: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
    ) {
        Text(
            text = "•",
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray7,
            modifier = Modifier.padding(end = 6.dp),
        )
        Text(
            text = text,
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray7,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WithdrawGuideScreenPrev() {
    WithdrawGuideScreen(
        userName = "홍길동",
        userEmail = "hong@example.com",
        onBackClick = {},
        onCancelClick = {},
        onConfirmClick = {},
    )
}

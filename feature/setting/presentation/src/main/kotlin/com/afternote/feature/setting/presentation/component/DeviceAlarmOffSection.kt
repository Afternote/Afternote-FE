package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationUiState

@Composable
fun DeviceAlarmOffSection(
    uiState: PushNotificationUiState,
    onSmsChecked: (Boolean) -> Unit,
    onEmailChecked: (Boolean) -> Unit,
    onPushChecked: (Boolean) -> Unit,
) {
    Column {
        // 기기 알림 설정 헤더
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.device_alarm_setting),
                style = AfternoteDesign.typography.bodyLargeR,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.device_alarm_off),
                style = AfternoteDesign.typography.captionLargeR,
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.device_alarm_guide),
            style = AfternoteDesign.typography.bodySmallR,
        )

        Spacer(Modifier.height(35.dp))

        Divider(thickness = 0.8.dp, color = AfternoteDesign.colors.gray3)
        Spacer(Modifier.height(18.dp))
        // 마케팅 알림 섹션
        Text(
            text = stringResource(R.string.marketing_alarm_setting),
            style = AfternoteDesign.typography.bodyBase,
        )
        Spacer(Modifier.height(38.dp))

        LabeledCheckboxRow(
            label = stringResource(R.string.sms),
            checked = uiState.isSmsChecked,
            onCheckedChange = onSmsChecked,
        )
        LabeledCheckboxRow(
            label = stringResource(R.string.email),
            checked = uiState.isEmailChecked,
            onCheckedChange = onEmailChecked,
        )
        LabeledCheckboxRow(
            label = stringResource(R.string.push_alarm),
            checked = uiState.isPushChecked,
            onCheckedChange = onPushChecked,
        )

        Spacer(Modifier.height(32.dp))

        // 안내 문구 bullet list
        val guides =
            listOf(
                stringResource(R.string.marketing_guide_1),
                stringResource(R.string.marketing_guide_2),
                stringResource(R.string.marketing_guide_3),
            )
        guides.forEach { guide ->
            Row(modifier = Modifier.padding(vertical = 14.dp)) {
                Text("• ")
                Text(
                    guide,
                    style = AfternoteDesign.typography.bodySmallR,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceAlarmOffSectionPrev() {
    DeviceAlarmOffSection(
        uiState = PushNotificationUiState(),
        onSmsChecked = {},
        onEmailChecked = {},
        onPushChecked = {},
    )
}

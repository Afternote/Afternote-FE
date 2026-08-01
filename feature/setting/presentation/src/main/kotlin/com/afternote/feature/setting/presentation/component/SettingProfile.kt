package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.setting.presentation.R

@Composable
fun SettingProfile(
    name: String,
    email: String,
    onInquiryClick: () -> Unit,
    onNoticeClick: () -> Unit,
    onRecipientListClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .height(180.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painterResource(R.drawable.ic_default_profile),
                contentDescription = "기본 이미지",
                modifier = Modifier.size(60.dp),
            )
            Spacer(modifier = Modifier.padding(12.dp))
            Column {
                Text(
                    name,
                    style = AfternoteDesign.typography.bodyLargeB,
                    color = AfternoteDesign.colors.gray9,
                )
                Text(
                    email,
                    style = AfternoteDesign.typography.bodySmallR,
                    color = AfternoteDesign.colors.gray5,
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Image(
                painterResource(R.drawable.ic_right_arrow),
                contentDescription = "화살표",
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.padding(top = 22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val items =
                listOf(
                    stringResource(R.string.settings_support_inquiry) to onInquiryClick,
                    stringResource(R.string.settings_support_notice) to onNoticeClick,
                    stringResource(R.string.settings_recipient_list) to onRecipientListClick,
                )

            items.forEach { (label, onClick) ->
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clickable(onClick = onClick),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painterResource(R.drawable.ic_list),
                        contentDescription = label,
                        modifier = Modifier.size(40.dp),
                    )
                    Text(
                        label,
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.gray7,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingProfilePrev() {
    SettingProfile(
        name = "박서연",
        email = "afternote@email.com",
        onInquiryClick = {},
        onNoticeClick = {},
        onRecipientListClick = {},
    )
}

package com.afternote.feature.setting.presentation.home.component

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.setting.presentation.R
import com.afternote.core.ui.R as CoreUiR

@Composable
internal fun SettingProfile(
    name: String,
    email: String,
    profileImageUrl: String?,
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
            val defaultProfile = painterResource(R.drawable.setting_ic_default_profile)
            val profileDescription = stringResource(CoreUiR.string.core_ui_content_description_profile_image)
            if (profileImageUrl.isNullOrBlank()) {
                Image(
                    painter = defaultProfile,
                    contentDescription = profileDescription,
                    modifier = Modifier.size(60.dp).clip(CircleShape),
                )
            } else {
                AsyncImage(
                    model = profileImageUrl,
                    contentDescription = profileDescription,
                    modifier = Modifier.size(60.dp).clip(CircleShape),
                    placeholder = defaultProfile,
                    error = defaultProfile,
                    contentScale = ContentScale.Crop,
                )
            }
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
                painterResource(R.drawable.setting_ic_right_arrow),
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
                    stringResource(R.string.setting_support_inquiry) to onInquiryClick,
                    stringResource(R.string.setting_support_notice) to onNoticeClick,
                    stringResource(R.string.setting_recipient_list) to onRecipientListClick,
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
                        painterResource(R.drawable.setting_ic_list),
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
